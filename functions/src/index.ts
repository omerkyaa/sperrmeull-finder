import { initializeApp } from "firebase-admin/app";
import {
  FieldValue,
  getFirestore,
  Timestamp,
} from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentWritten,
} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

initializeApp();

const db = getFirestore();
const messaging = getMessaging();
const REGION = "europe-west1";
const NOTIFICATION_DEDUP_WINDOW_MS = 60_000;

type NotificationType = "follow" | "like" | "comment";

function safeString(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
}

async function getUserPublicProfile(uid: string): Promise<{
  username: string;
  photoUrl: string;
}> {
  const doc = await db.collection("users_public").doc(uid).get();
  const data = doc.data() ?? {};
  const username =
    safeString(data.username) ||
    safeString(data.displayname) ||
    safeString(data.nickname) ||
    "Someone";
  const photoUrl = safeString(data.photoUrl) || safeString(data.photourl);
  return { username, photoUrl };
}

async function getUserTokens(uid: string): Promise<string[]> {
  const tokens = new Set<string>();
  const privateDoc = await db.collection("users_private").doc(uid).get();
  const privateData = privateDoc.data() ?? {};

  const fcmTokens = privateData.fcmTokens;
  if (fcmTokens && typeof fcmTokens === "object" && !Array.isArray(fcmTokens)) {
    for (const [key, enabled] of Object.entries(
      fcmTokens as Record<string, unknown>,
    )) {
      if (enabled === true && key.trim().length > 0) {
        tokens.add(key.trim());
      }
    }
  }

  const legacyArray = privateData.device_tokens;
  if (Array.isArray(legacyArray)) {
    for (const token of legacyArray) {
      const clean = safeString(token);
      if (clean) tokens.add(clean);
    }
  }

  return Array.from(tokens);
}

function buildNotificationMessage(
  type: NotificationType,
  actorName: string,
): { title: string; message: string; deepLink: string } {
  if (type === "follow") {
    return {
      title: "New Follower",
      message: `${actorName} started following you`,
      deepLink: "notifications",
    };
  }
  if (type === "comment") {
    return {
      title: "New Comment",
      message: `${actorName} commented on your post`,
      deepLink: "notifications",
    };
  }
  return {
    title: "New Like",
    message: `${actorName} liked your post`,
    deepLink: "notifications",
  };
}

async function writeNotificationAndPush(params: {
  targetUid: string;
  actorUid: string;
  type: NotificationType;
  postId?: string;
  commentId?: string;
}): Promise<void> {
  const { targetUid, actorUid, type, postId, commentId } = params;
  if (!targetUid || !actorUid || targetUid === actorUid) return;

  const actorProfile = await getUserPublicProfile(actorUid);
  const content = buildNotificationMessage(type, actorProfile.username);

  const dedupSince = Timestamp.fromMillis(Date.now() - NOTIFICATION_DEDUP_WINDOW_MS);
  let dedupQuery = db
    .collection("notifications")
    .doc(targetUid)
    .collection("user_notifications")
    .where("fromUserId", "==", actorUid)
    .where("type", "==", type)
    .where("createdAt", ">=", dedupSince)
    .limit(1);
  if (postId) {
    dedupQuery = dedupQuery.where("postId", "==", postId);
  }
  const duplicate = await dedupQuery.get();
  if (!duplicate.empty) {
    logger.info("Skip duplicate notification in dedup window", {
      targetUid,
      actorUid,
      type,
      postId: postId ?? null,
    });
    return;
  }

  const ref = db
    .collection("notifications")
    .doc(targetUid)
    .collection("user_notifications")
    .doc();

  const payload: Record<string, unknown> = {
    id: ref.id,
    toUserId: targetUid,
    fromUserId: actorUid,
    type,
    title: content.title,
    message: content.message,
    deepLink: content.deepLink,
    isRead: false,
    createdAt: FieldValue.serverTimestamp(),
    actorUsername: actorProfile.username,
    actorPhotoUrl: actorProfile.photoUrl,
    postId: postId ?? null,
    commentId: commentId ?? null,
    pushHandled: true,
  };

  await ref.set(payload);

  const tokens = await getUserTokens(targetUid);
  if (tokens.length === 0) {
    logger.info("No FCM tokens for target user", { targetUid, type });
    return;
  }

  const message = {
    tokens,
    notification: {
      title: content.title,
      body: content.message,
    },
    data: {
      type,
      postId: postId ?? "",
      commentId: commentId ?? "",
      fromUserId: actorUid,
    },
    android: {
      priority: "high" as const,
    },
  };

  const response = await messaging.sendEachForMulticast(message);
  const invalidTokens: string[] = [];

  response.responses.forEach((result, idx) => {
    if (!result.success) {
      const code = result.error?.code ?? "";
      if (
        code === "messaging/invalid-registration-token" ||
        code === "messaging/registration-token-not-registered"
      ) {
        invalidTokens.push(tokens[idx]);
      }
    }
  });

  if (invalidTokens.length > 0) {
    const privateRef = db.collection("users_private").doc(targetUid);
    const updates: Record<string, unknown> = {};
    for (const token of invalidTokens) {
      updates[`fcmTokens.${token}`] = FieldValue.delete();
    }
    updates.updated_at = Timestamp.now();
    await privateRef.set(updates, { merge: true });
  }
}

async function updatePostCounter(postId: string, key: "likes_count" | "comments_count", delta: number): Promise<void> {
  const postRef = db.collection("posts").doc(postId);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(postRef);
    if (!snap.exists) return;
    const currentRaw = snap.get(key);
    const current = typeof currentRaw === "number" ? currentRaw : 0;
    const next = Math.max(0, current + delta);
    tx.update(postRef, {
      [key]: next,
      updated_at: FieldValue.serverTimestamp(),
    });
  });
}

export const onPostLikeWritten = onDocumentWritten(
  {
    document: "posts/{postId}/likes/{uid}",
    region: REGION,
  },
  async (event) => {
    const beforeExists = event.data?.before.exists ?? false;
    const afterExists = event.data?.after.exists ?? false;
    if (beforeExists === afterExists) return;

    const postId = safeString(event.params.postId);
    if (!postId) return;

    const delta = !beforeExists && afterExists ? 1 : -1;
    await updatePostCounter(postId, "likes_count", delta);

    if (delta < 0) return;
    const afterData = asRecord(event.data?.after.data());
    const actorUid = safeString(afterData.userId) || safeString(event.params.uid);
    if (!actorUid) return;

    const postDoc = await db.collection("posts").doc(postId).get();
    const postOwnerUid = safeString(postDoc.get("ownerid"));
    await writeNotificationAndPush({
      targetUid: postOwnerUid,
      actorUid,
      type: "like",
      postId,
    });
  },
);

export const onPostCommentWritten = onDocumentWritten(
  {
    document: "posts/{postId}/comments/{commentId}",
    region: REGION,
  },
  async (event) => {
    const beforeExists = event.data?.before.exists ?? false;
    const afterExists = event.data?.after.exists ?? false;
    if (beforeExists === afterExists) return;

    const postId = safeString(event.params.postId);
    if (!postId) return;

    const delta = !beforeExists && afterExists ? 1 : -1;
    await updatePostCounter(postId, "comments_count", delta);

    if (delta < 0) return;
    const afterData = asRecord(event.data?.after.data());
    const actorUid = safeString(afterData.authorId);
    const commentId = safeString(event.params.commentId);
    if (!actorUid) return;

    const postDoc = await db.collection("posts").doc(postId).get();
    const postOwnerUid = safeString(postDoc.get("ownerid"));
    await writeNotificationAndPush({
      targetUid: postOwnerUid,
      actorUid,
      type: "comment",
      postId,
      commentId,
    });
  },
);

export const onFollowCreated = onDocumentCreated(
  {
    document: "follows/{followId}",
    region: REGION,
  },
  async (event) => {
    const data = asRecord(event.data?.data());
    const actorUid = safeString(data.followerId);
    const targetUid = safeString(data.followedId);
    await writeNotificationAndPush({
      targetUid,
      actorUid,
      type: "follow",
    });
  },
);

export const onLegacyFollowDeleted = onDocumentDeleted(
  {
    document: "follows/{followId}",
    region: REGION,
  },
  async () => {
    return;
  },
);

const adminManagement = require("../adminManagement");
export const setAdminRole = adminManagement.setAdminRole;
export const removeAdminRole = adminManagement.removeAdminRole;
export const getMyAdminRole = adminManagement.getMyAdminRole;

const userManagement = require("../userManagement");
export const softBanUser = userManagement.softBanUser;
export const hardBanUser = userManagement.hardBanUser;
export const deleteUserAccount = userManagement.deleteUserAccount;
export const unbanUser = userManagement.unbanUser;

const premiumManagement = require("../premiumManagement");
export const grantPremium = premiumManagement.grantPremium;
export const revokePremium = premiumManagement.revokePremium;
export const reconcilePremiumStatus = premiumManagement.reconcilePremiumStatus;

const contentModeration = require("../contentModeration");
export const deletePost = contentModeration.deletePost;
export const deleteComment = contentModeration.deleteComment;
export const restorePost = contentModeration.restorePost;

const notifications = require("../notifications");
export const sendAdminNotification = notifications.sendAdminNotification;
export const sendWarning = notifications.sendWarning;
const notificationTriggers = require("../notificationTriggers");
export const onNotificationCreated = notificationTriggers.onNotificationCreated;
export const onPostCreatedNotifyNearbyPremium =
  notificationTriggers.onPostCreatedNotifyNearbyPremium;

const reportManagement = require("../reportManagement");
export const onReportCreated = reportManagement.onReportCreated;
export const dismissReport = reportManagement.dismissReport;
export const warnUser = reportManagement.warnUser;
export const deleteReportedContent = reportManagement.deleteReportedContent;

const accountDeletion = require("../accountDeletion");
export const requestAccountDeletion = accountDeletion.requestAccountDeletion;
export const cancelAccountDeletion = accountDeletion.cancelAccountDeletion;
export const processScheduledDeletions = accountDeletion.processScheduledDeletions;
export const sendDeletionReminders = accountDeletion.sendDeletionReminders;

const revenueCatWebhookModule = require("../revenueCatWebhook");
export const revenueCatWebhook = revenueCatWebhookModule.revenueCatWebhook;
