#!/usr/bin/env node

/**
 * One-shot Firestore backfill for legacy field normalization.
 *
 * Usage:
 *   node scripts/backfill-legacy-data.js --dry-run
 *   node scripts/backfill-legacy-data.js --apply
 */

const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const NOW_TS = admin.firestore.Timestamp.now();
const BATCH_LIMIT = 400;

function isBlank(value) {
  return typeof value !== 'string' || value.trim() === '';
}

function firstNonBlank(...values) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim() !== '') {
      return value.trim();
    }
  }
  return null;
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== '';
}

function parseFlags(argv) {
  const hasApply = argv.includes('--apply');
  const hasDryRun = argv.includes('--dry-run') || !hasApply;
  return { apply: hasApply, dryRun: hasDryRun };
}

async function* streamCollection(collectionName, pageSize = 500) {
  let lastDoc = null;
  while (true) {
    let query = db.collection(collectionName).limit(pageSize);
    if (lastDoc) query = query.startAfter(lastDoc);
    const snap = await query.get();
    if (snap.empty) return;
    for (const doc of snap.docs) {
      yield doc;
    }
    if (snap.size < pageSize) return;
    lastDoc = snap.docs[snap.docs.length - 1];
  }
}

function buildPostUpdates(data) {
  const updates = {};

  if (isBlank(data.ownerid)) {
    const ownerId = firstNonBlank(
      data.ownerId,
      data.owner_id,
      data.userId,
      data.userid,
      data.authorId,
      data.author_id
    );
    if (ownerId) updates.ownerid = ownerId;
  }

  if (isBlank(data.owner_display_name)) {
    const ownerName = firstNonBlank(
      data.ownerDisplayName,
      data.owner_name,
      data.ownerName,
      data.ownername,
      data.displayName,
      data.displayname,
      data.username,
      data.nickname
    );
    if (ownerName) updates.owner_display_name = ownerName;
  }

  if (isBlank(data.owner_photo_url)) {
    const ownerPhoto = firstNonBlank(
      data.ownerPhotoUrl,
      data.owner_photo,
      data.photoUrl,
      data.photourl,
      data.avatarUrl,
      data.profilePhotoUrl
    );
    if (ownerPhoto) updates.owner_photo_url = ownerPhoto;
  }

  if (Object.keys(updates).length > 0) {
    updates.updated_at = NOW_TS;
  }
  return updates;
}

function buildUsersPublicUpdates(data) {
  const updates = {};

  const displayName = firstNonBlank(
    data.displayname,
    data.displayName,
    data.username,
    data.userName,
    data.nickname
  );
  const photoUrl = firstNonBlank(
    data.photoUrl,
    data.photourl,
    data.avatarUrl,
    data.profilePhotoUrl
  );
  const nickname = firstNonBlank(
    data.nickname,
    data.nickName,
    data.username,
    data.userName
  );

  if (displayName && data.displayname !== displayName) updates.displayname = displayName;
  if (displayName && data.displayName !== displayName) updates.displayName = displayName;
  if (photoUrl && data.photoUrl !== photoUrl) updates.photoUrl = photoUrl;
  if (photoUrl && data.photourl !== photoUrl) updates.photourl = photoUrl;
  if (nickname && data.nickname !== nickname) updates.nickname = nickname;

  if (Object.keys(updates).length > 0) {
    updates.updatedAt = NOW_TS;
  }
  return updates;
}

function buildFollowUpdates(data) {
  const updates = {};

  const followerId = firstNonBlank(
    data.followerId,
    data.follower_id,
    data.userId,
    data.user_id,
    data.sourceUserId
  );
  const followedId = firstNonBlank(
    data.followedId,
    data.followed_id,
    data.followingId,
    data.following_id,
    data.targetUserId
  );

  if (followerId && data.followerId !== followerId) updates.followerId = followerId;
  if (followedId && data.followedId !== followedId) updates.followedId = followedId;
  if (data.isActive === undefined && data.is_active !== undefined) {
    updates.isActive = !!data.is_active;
  } else if (data.isActive === undefined) {
    updates.isActive = true;
  }
  if (!hasValue(data.createdAt) && hasValue(data.created_at)) {
    updates.createdAt = data.created_at;
  } else if (!hasValue(data.createdAt)) {
    updates.createdAt = NOW_TS;
  }

  return updates;
}

async function runBackfill(collectionName, buildUpdates, apply) {
  let scanned = 0;
  let changed = 0;
  let batch = db.batch();
  let batchOps = 0;

  for await (const doc of streamCollection(collectionName)) {
    scanned += 1;
    const data = doc.data() || {};
    const updates = buildUpdates(data);
    if (Object.keys(updates).length === 0) continue;

    changed += 1;
    if (apply) {
      batch.update(doc.ref, updates);
      batchOps += 1;
      if (batchOps >= BATCH_LIMIT) {
        await batch.commit();
        batch = db.batch();
        batchOps = 0;
      }
    }
  }

  if (apply && batchOps > 0) {
    await batch.commit();
  }

  return { scanned, changed };
}

async function main() {
  const flags = parseFlags(process.argv.slice(2));
  console.log(`Mode: ${flags.apply ? 'APPLY' : 'DRY-RUN'}`);

  const posts = await runBackfill('posts', buildPostUpdates, flags.apply);
  const usersPublic = await runBackfill('users_public', buildUsersPublicUpdates, flags.apply);
  const follows = await runBackfill('follows', buildFollowUpdates, flags.apply);

  console.log('--- Backfill Report ---');
  console.log(`posts: scanned=${posts.scanned}, changed=${posts.changed}`);
  console.log(`users_public: scanned=${usersPublic.scanned}, changed=${usersPublic.changed}`);
  console.log(`follows: scanned=${follows.scanned}, changed=${follows.changed}`);
}

main().catch((error) => {
  console.error('Backfill failed:', error);
  process.exitCode = 1;
});

