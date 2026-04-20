const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');

const db = getFirestore();
const BATCH_DELETE_LIMIT = 400;

function mergeDocRefs(...snapshots) {
  const map = new Map();
  snapshots.forEach((snapshot) => {
    (snapshot?.docs || []).forEach((doc) => map.set(doc.ref.path, doc.ref));
  });
  return Array.from(map.values());
}

async function hardDeleteUserData(userId) {
  const deleteRefsInChunks = async (refs) => {
    for (let i = 0; i < refs.length; i += BATCH_DELETE_LIMIT) {
      const chunk = refs.slice(i, i + BATCH_DELETE_LIMIT);
      const batch = db.batch();
      chunk.forEach((ref) => batch.delete(ref));
      await batch.commit();
    }
  };

  const deleteQueryBatch = async (query) => {
    while (true) {
      const snap = await query.limit(BATCH_DELETE_LIMIT).get();
      if (snap.empty) break;
      const batch = db.batch();
      snap.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
      if (snap.size < BATCH_DELETE_LIMIT) break;
    }
  };

  // Delete user posts + nested post comments/likes
  const [postsByOwnerId, postsByOwnerIdLegacy] = await Promise.all([
    db.collection('posts').where('ownerId', '==', userId).get(),
    db.collection('posts').where('ownerid', '==', userId).get(),
  ]);
  const postRefs = mergeDocRefs(postsByOwnerId, postsByOwnerIdLegacy);
  for (const postRef of postRefs) {
    const [postComments, postLikes] = await Promise.all([
      postRef.collection('comments').get(),
      postRef.collection('likes').get(),
    ]);
    await deleteRefsInChunks(postComments.docs.map((d) => d.ref));
    await deleteRefsInChunks(postLikes.docs.map((d) => d.ref));
    await postRef.delete().catch(() => null);
  }

  // Delete user comments
  const [commentsTopLevel, commentsByAuthorId, commentsByAuthorIdLegacy] = await Promise.all([
    db.collection('comments').where('authorId', '==', userId).get(),
    db.collectionGroup('comments').where('authorId', '==', userId).get(),
    db.collectionGroup('comments').where('author_id', '==', userId).get(),
  ]);
  const commentRefs = mergeDocRefs(commentsTopLevel, commentsByAuthorId, commentsByAuthorIdLegacy);
  for (const commentRef of commentRefs) {
    const nestedLikes = await commentRef.collection('likes').get();
    await deleteRefsInChunks(nestedLikes.docs.map((d) => d.ref));
    await commentRef.delete().catch(() => null);
  }

  // Delete top-level likes
  await deleteQueryBatch(db.collection('likes').where('userId', '==', userId));
  await deleteQueryBatch(db.collectionGroup('likes').where('userId', '==', userId));

  // Delete follows both sides
  await deleteQueryBatch(db.collection('follows').where('followerId', '==', userId));
  await deleteQueryBatch(db.collection('follows').where('followedId', '==', userId));

  // Delete notifications bucket
  const userNotifSnapshot = await db.collection('notifications')
    .doc(userId)
    .collection('user_notifications')
    .get();
  if (!userNotifSnapshot.empty) {
    const notifBatch = db.batch();
    userNotifSnapshot.docs.forEach((doc) => notifBatch.delete(doc.ref));
    notifBatch.delete(db.collection('notifications').doc(userId));
    await notifBatch.commit();
  } else {
    await db.collection('notifications').doc(userId).delete().catch(() => null);
  }
  await deleteQueryBatch(
    db.collectionGroup('user_notifications').where('fromUserId', '==', userId)
  );

  // Delete device tokens
  const tokensSnapshot = await db.collection('device_tokens')
    .doc(userId)
    .collection('tokens')
    .get();
  if (!tokensSnapshot.empty) {
    const tokenBatch = db.batch();
    tokensSnapshot.docs.forEach((doc) => tokenBatch.delete(doc.ref));
    tokenBatch.delete(db.collection('device_tokens').doc(userId));
    await tokenBatch.commit();
  } else {
    await db.collection('device_tokens').doc(userId).delete().catch(() => null);
  }

  // Delete account_deletion_requests legacy doc
  await db.collection('account_deletion_requests').doc(userId).delete().catch(() => null);

  // Delete split user collections.
  await db.collection('users_public').doc(userId).delete().catch(() => null);
  await db.collection('users_private').doc(userId).delete().catch(() => null);

  // Finally delete user document and auth account
  await db.collection('users').doc(userId).delete().catch(() => null);
  await getAuth().deleteUser(userId).catch(() => null);
}

async function createUserNotification(userId, type, title, message, meta = {}) {
  const ref = db.collection('notifications')
    .doc(userId)
    .collection('user_notifications')
    .doc();
  await ref.set({
    id: ref.id,
    toUserId: userId,
    fromUserId: null,
    type,
    title,
    message,
    deepLink: 'notifications',
    createdAt: FieldValue.serverTimestamp(),
    isRead: false,
    meta
  });
}

/**
 * Request account deletion (30-day grace period)
 */
exports.requestAccountDeletion = onCall(async (request) => {
  const {auth, data} = request;
  
  if (!auth) {
    throw new HttpsError('unauthenticated', 'You must be logged in');
  }
  
  const {reason} = data;
  const userId = auth.uid;
  
  try {
    const scheduledFor = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days
    
    // Update user document
    await db.collection('users').doc(userId).update({
      deletionRequested: true,
      deletionRequestedAt: FieldValue.serverTimestamp(),
      deletionScheduledFor: scheduledFor,
      deletionReason: reason || null
    });
    
    // Create account_deletions document
    await db.collection('account_deletions').doc(userId).set({
      userId: userId,
      requestedAt: FieldValue.serverTimestamp(),
      scheduledFor: scheduledFor,
      reason: reason || null,
      status: 'PENDING'
    });
    
    // Send confirmation notification
    await createUserNotification(
      userId,
      'report',
      'Account Deletion Requested',
      'Your account will be deleted in 30 days. You can cancel this at any time.',
      {
        scheduledFor: scheduledFor.toISOString(),
        reason: reason || ''
      }
    );
    
    console.log(`Account deletion requested for user ${userId}`);
    
    return {
      success: true,
      message: 'Account deletion scheduled for 30 days from now',
      scheduledFor: scheduledFor.toISOString()
    };
  } catch (error) {
    console.error('Error requesting account deletion:', error);
    throw new HttpsError('internal', error?.message || 'Failed to request account deletion');
  }
});

/**
 * Cancel account deletion
 */
exports.cancelAccountDeletion = onCall(async (request) => {
  const {auth} = request;
  
  if (!auth) {
    throw new HttpsError('unauthenticated', 'You must be logged in');
  }
  
  const userId = auth.uid;
  
  try {
    // Update user document
    await db.collection('users').doc(userId).update({
      deletionRequested: false,
      deletionRequestedAt: null,
      deletionScheduledFor: null,
      deletionReason: null
    });
    
    // Update account_deletions document (upsert-safe)
    await db.collection('account_deletions').doc(userId).set({
      userId,
      status: 'CANCELLED',
      cancelledAt: FieldValue.serverTimestamp()
    }, {merge: true});
    
    // Send cancellation notification
    await createUserNotification(
      userId,
      'report',
      'Account Deletion Cancelled',
      'Your account deletion has been cancelled. Your account is safe.',
      {}
    );
    
    console.log(`Account deletion cancelled for user ${userId}`);
    
    return {
      success: true,
      message: 'Account deletion cancelled successfully'
    };
  } catch (error) {
    console.error('Error cancelling account deletion:', error);
    throw new HttpsError('internal', error?.message || 'Failed to cancel account deletion');
  }
});

/**
 * Process scheduled deletions (runs daily)
 */
exports.processScheduledDeletions = onSchedule('every day 02:00', async (event) => {
  try {
    const now = new Date();
    
    // Find accounts scheduled for deletion
    const deletionsSnapshot = await db.collection('account_deletions')
      .where('status', '==', 'PENDING')
      .where('scheduledFor', '<=', now)
      .get();
    
    console.log(`Found ${deletionsSnapshot.size} accounts to delete`);
    
    for (const doc of deletionsSnapshot.docs) {
      const userId = doc.id;
      
      try {
        // Permanent deletion after grace period expiration.
        await hardDeleteUserData(userId);
        
        // Update account_deletions status
        await doc.ref.update({
          status: 'COMPLETED',
          completedAt: FieldValue.serverTimestamp()
        });
        
        console.log(`Successfully deleted account ${userId}`);
      } catch (error) {
        console.error(`Error deleting account ${userId}:`, error);
        // Continue with other accounts
      }
    }
    
    return {success: true, processed: deletionsSnapshot.size};
  } catch (error) {
    console.error('Error processing scheduled deletions:', error);
    throw error;
  }
});

/**
 * Send deletion reminder (7 days before)
 */
exports.sendDeletionReminders = onSchedule('every day 10:00', async (event) => {
  try {
    const sevenDaysFromNow = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    const eightDaysFromNow = new Date(Date.now() + 8 * 24 * 60 * 60 * 1000);
    
    // Find accounts scheduled for deletion in 7 days
    const deletionsSnapshot = await db.collection('account_deletions')
      .where('status', '==', 'PENDING')
      .where('scheduledFor', '>=', sevenDaysFromNow)
      .where('scheduledFor', '<=', eightDaysFromNow)
      .get();
    
    console.log(`Sending ${deletionsSnapshot.size} deletion reminders`);
    
    for (const doc of deletionsSnapshot.docs) {
      const userId = doc.id;
      
      await createUserNotification(
        userId,
        'report',
        'Account Deletion Reminder',
        'Your account will be deleted in 7 days. Cancel now if you want to keep your account.',
        {daysRemaining: 7}
      );
    }
    
    return {success: true, sent: deletionsSnapshot.size};
  } catch (error) {
    console.error('Error sending deletion reminders:', error);
    throw error;
  }
});
