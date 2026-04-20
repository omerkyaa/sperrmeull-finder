const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {getApps, initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getMessaging} = require('firebase-admin/messaging');

if (!getApps().length) {
  initializeApp();
}

const db = getFirestore();
const messaging = getMessaging();

const TOKENS_SUBCOLLECTION = 'tokens';
const MAX_MULTICAST_TOKENS = 500;

function sanitizeString(value, fallback = '') {
  if (typeof value !== 'string') return fallback;
  return value.trim();
}

function mapNotificationType(rawType) {
  const type = sanitizeString(rawType, 'system').toLowerCase();
  switch (type) {
    case 'like':
    case 'comment':
    case 'follow':
    case 'premium':
    case 'xp':
    case 'honesty':
      return type;
    default:
      return 'system';
  }
}

async function getUserPushTokens(userId) {
  const tokens = new Set();

  // Primary source: device_tokens/{uid}/tokens/{token}
  const subcollectionSnapshot = await db
    .collection('device_tokens')
    .doc(userId)
    .collection(TOKENS_SUBCOLLECTION)
    .where('granted', '==', true)
    .get();

  subcollectionSnapshot.docs.forEach((doc) => {
    const token = sanitizeString(doc.get('token'));
    if (token.length > 0) tokens.add(token);
  });

  // Fallback source: users/{uid}.device_tokens[]
  if (tokens.size === 0) {
    const userDoc = await db.collection('users').doc(userId).get();
    const deviceTokens = userDoc.get('device_tokens');
    if (Array.isArray(deviceTokens)) {
      deviceTokens.forEach((token) => {
        const cleanToken = sanitizeString(token);
        if (cleanToken.length > 0) tokens.add(cleanToken);
      });
    }
  }

  return Array.from(tokens);
}

async function removeInvalidTokens(userId, invalidTokens) {
  if (!invalidTokens.length) return;

  const batch = db.batch();
  invalidTokens.forEach((token) => {
    batch.delete(
      db.collection('device_tokens')
        .doc(userId)
        .collection(TOKENS_SUBCOLLECTION)
        .doc(token)
    );
  });

  batch.update(db.collection('users').doc(userId), {
    device_tokens: FieldValue.arrayRemove(...invalidTokens),
    updated_at: FieldValue.serverTimestamp(),
  });

  await batch.commit();
}

exports.onNotificationCreated = onDocumentCreated(
  'notifications/{userId}/user_notifications/{notificationId}',
  async (event) => {
    const notificationDoc = event.data;
    if (!notificationDoc) return;

    const userId = sanitizeString(event.params.userId);
    if (!userId) return;

    const payload = notificationDoc.data() || {};
    if (payload.pushHandled === true) {
      return;
    }
    const toUserId = sanitizeString(payload.toUserId, userId);
    const fromUserId = sanitizeString(payload.fromUserId);
    if (fromUserId && fromUserId === toUserId) {
      return;
    }

    const title = sanitizeString(payload.title, 'SperrmullFinder');
    const body = sanitizeString(payload.message, '');
    const type = mapNotificationType(payload.type);
    const postId = sanitizeString(payload.postId);
    const deepLink = sanitizeString(payload.deepLink, 'notifications');

    const tokens = await getUserPushTokens(toUserId);
    if (!tokens.length) {
      console.log(`No push tokens found for user ${toUserId}`);
      return;
    }

    const invalidTokens = [];
    const chunks = [];
    for (let i = 0; i < tokens.length; i += MAX_MULTICAST_TOKENS) {
      chunks.push(tokens.slice(i, i + MAX_MULTICAST_TOKENS));
    }

    for (const chunk of chunks) {
      const message = {
        tokens: chunk,
        notification: {
          title,
          body,
        },
        data: {
          type,
          title,
          body,
          postId,
          deepLink,
          fromUserId,
          userId: fromUserId,
        },
        android: {
          priority: 'high',
        },
      };

      const response = await messaging.sendEachForMulticast(message);
      response.responses.forEach((result, index) => {
        if (!result.success) {
          const token = chunk[index];
          const code = result.error?.code || '';
          if (
            code === 'messaging/invalid-registration-token' ||
            code === 'messaging/registration-token-not-registered'
          ) {
            invalidTokens.push(token);
          }
        }
      });
    }

    if (invalidTokens.length) {
      await removeInvalidTokens(toUserId, Array.from(new Set(invalidTokens)));
    }
  }
);

exports.onPostCreatedNotifyNearbyPremium = onDocumentCreated(
  'posts/{postId}',
  async () => {
    // Reserved for premium nearby push logic; kept as no-op for compatibility.
    return;
  }
);
