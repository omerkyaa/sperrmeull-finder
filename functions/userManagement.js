const {onCall} = require('firebase-functions/v2/https');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');

const db = getFirestore();
const authAdmin = getAuth();

function requireReason(reason) {
  if (!reason || typeof reason !== 'string' || reason.trim().length === 0) {
    throw new Error('invalid-argument: explanation is required');
  }
  return reason.trim();
}

async function createUserNotification(userId, type, title, message, meta = {}, fromUserId = null) {
  const ref = db.collection('notifications')
    .doc(userId)
    .collection('user_notifications')
    .doc();
  await ref.set({
    id: ref.id,
    toUserId: userId,
    fromUserId: fromUserId,
    type,
    title,
    message,
    postId: meta.postId || null,
    commentId: meta.commentId || null,
    deepLink: meta.deepLink || 'notifications',
    createdAt: FieldValue.serverTimestamp(),
    isRead: false,
    meta
  });
}

function mergeDocRefs(...snapshots) {
  const map = new Map();
  snapshots.forEach((snapshot) => {
    if (!snapshot || !snapshot.docs) return;
    snapshot.docs.forEach((doc) => map.set(doc.ref.path, doc.ref));
  });
  return Array.from(map.values());
}

async function patchUserDocuments(userId, updates) {
  await Promise.all([
    db.collection('users').doc(userId).set(updates, {merge: true}),
    db.collection('users_private').doc(userId).set(updates, {merge: true}),
  ]);
}

/**
 * Ban Types:
 * - SOFT_BAN: Temporary ban (3 days, 1 week, 1 month) - User can still exist but blocked in app
 * - HARD_BAN: Permanent ban - Firebase Auth disabled, cannot login
 * - DELETE_ACCOUNT: Complete account deletion - Auth + Firestore + all data removed
 */

/**
 * Soft Ban - Temporary block (3 days, 1 week, 1 month)
 * User document updated, Firebase Auth stays active
 */
exports.softBanUser = onCall(async (request) => {
  const {auth: authContext, data} = request;
  
  // Verify admin or super_admin
  if (!authContext?.token?.admin && !authContext?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {userId, durationDays, reason, reportId} = data;
  const adminReason = requireReason(reason);
  
  if (!userId || !durationDays) {
    throw new Error('invalid-argument: userId and durationDays are required');
  }
  
  try {
    const banUntil = new Date(Date.now() + durationDays * 86400000); // days to milliseconds
    
    // Update user document
    await patchUserDocuments(userId, {
      isBanned: true,
      banType: 'SOFT_BAN',
      banUntil: banUntil,
      banReason: adminReason,
      bannedBy: authContext.uid,
      bannedAt: FieldValue.serverTimestamp(),
      authDisabled: false
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: authContext.uid,
      action: 'soft_ban_user',
      targetType: 'user',
      targetId: userId,
      reason: adminReason,
      metadata: {
        durationDays: durationDays,
        banUntil: banUntil.toISOString(),
        reportId: reportId || null,
        banType: 'SOFT_BAN'
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Send ban notification to user
    await createUserNotification(
      userId,
      'admin_penalty',
      'Account Temporarily Suspended',
      `Your account has been suspended for ${durationDays} days. Reason: ${adminReason}. You can login again after ${banUntil.toLocaleDateString()}.`,
      {
        banType: 'SOFT_BAN',
        durationDays: durationDays,
        banUntil: banUntil.toISOString(),
        reason: adminReason
      },
      authContext.uid
    );
    
    console.log(`User ${userId} soft banned by admin ${authContext.uid} for ${durationDays} days`);
    
    return {
      success: true,
      message: `User soft banned for ${durationDays} days`,
      banType: 'SOFT_BAN',
      banUntil: banUntil.toISOString()
    };
  } catch (error) {
    console.error('Error soft banning user:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Hard Ban - Permanent ban with Firebase Auth disabled
 * User cannot login, all access blocked
 */
exports.hardBanUser = onCall(async (request) => {
  const {auth: authContext, data} = request;
  
  // Verify admin or super_admin
  if (!authContext?.token?.admin && !authContext?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {userId, reason, reportId} = data;
  const adminReason = requireReason(reason);
  
  if (!userId) {
    throw new Error('invalid-argument: userId is required');
  }
  
  try {
    // Disable Firebase Auth account
    await authAdmin.updateUser(userId, {
      disabled: true
    });
    
    // Update user document
    await patchUserDocuments(userId, {
      isBanned: true,
      banType: 'HARD_BAN',
      banUntil: null, // permanent
      banReason: adminReason,
      bannedBy: authContext.uid,
      bannedAt: FieldValue.serverTimestamp(),
      authDisabled: true
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: authContext.uid,
      action: 'hard_ban_user',
      targetType: 'user',
      targetId: userId,
      reason: adminReason,
      metadata: {
        reportId: reportId || null,
        banType: 'HARD_BAN',
        authDisabled: true
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Send ban notification (user won't be able to see it after logout)
    await createUserNotification(
      userId,
      'admin_penalty',
      'Account Permanently Banned',
      `Your account has been permanently banned. Reason: ${adminReason}. You can no longer access this service.`,
      {
        banType: 'HARD_BAN',
        permanent: true,
        reason: adminReason
      },
      authContext.uid
    );
    
    console.log(`User ${userId} hard banned by admin ${authContext.uid} - Auth disabled`);
    
    return {
      success: true,
      message: 'User permanently banned (Auth disabled)',
      banType: 'HARD_BAN',
      authDisabled: true
    };
  } catch (error) {
    console.error('Error hard banning user:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Delete Account - Complete account deletion
 * Removes Firebase Auth, Firestore user doc, and all related data
 */
exports.deleteUserAccount = onCall(async (request) => {
  const {auth: authContext, data} = request;
  
  // Verify super_admin only (dangerous operation)
  if (!authContext?.token?.super_admin) {
    throw new Error('permission-denied: Super admin access required for account deletion');
  }
  
  const {userId, reason, reportId} = data;
  const adminReason = requireReason(reason);
  
  if (!userId) {
    throw new Error('invalid-argument: userId is required');
  }
  
  try {
    // Log action BEFORE deletion
    await db.collection('admin_logs').add({
      adminId: authContext.uid,
      action: 'delete_user_account',
      targetType: 'user',
      targetId: userId,
      reason: adminReason,
      metadata: {
        reportId: reportId || null,
        deletionType: 'ADMIN_FORCED',
        timestamp: new Date().toISOString()
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Get user data for archival
    const userDoc = await db.collection('users').doc(userId).get();
    const userData = userDoc.data();
    
    // Archive deleted user (for compliance/audit)
    await db.collection('deleted_users').doc(userId).set({
      ...userData,
      deletedBy: authContext.uid,
      deletionReason: adminReason,
      deletedAt: FieldValue.serverTimestamp(),
      deletionType: 'ADMIN_FORCED'
    });

    if (reportId) {
      await db.collection('reports').doc(reportId).update({
        status: 'APPROVED',
        resolvedBy: authContext.uid,
        resolvedAt: FieldValue.serverTimestamp(),
        adminNotes: `Account deleted by super admin. Explanation: ${adminReason}`
      });
    }
    
    // Delete user posts (support ownerid + ownerId)
    const [postsByOwnerId, postsByOwnerIdLegacy] = await Promise.all([
      db.collection('posts').where('ownerId', '==', userId).get(),
      db.collection('posts').where('ownerid', '==', userId).get()
    ]);
    const postRefs = mergeDocRefs(postsByOwnerId, postsByOwnerIdLegacy);
    for (const postRef of postRefs) {
      const [postComments, postLikes] = await Promise.all([
        postRef.collection('comments').get(),
        postRef.collection('likes').get()
      ]);
      const nestedBatch = db.batch();
      postComments.docs.forEach((d) => nestedBatch.delete(d.ref));
      postLikes.docs.forEach((d) => nestedBatch.delete(d.ref));
      nestedBatch.delete(postRef);
      await nestedBatch.commit();
    }
    console.log(`Deleted ${postRefs.length} posts (and nested data) for user ${userId}`);
    
    // Delete user comments (top-level + collectionGroup)
    const [commentsTopLevel, commentsByAuthorId, commentsByAuthorIdLegacy] = await Promise.all([
      db.collection('comments').where('authorId', '==', userId).get(),
      db.collectionGroup('comments').where('authorId', '==', userId).get(),
      db.collectionGroup('comments').where('author_id', '==', userId).get()
    ]);
    const commentRefs = mergeDocRefs(commentsTopLevel, commentsByAuthorId, commentsByAuthorIdLegacy);
    for (const commentRef of commentRefs) {
      const nestedLikes = await commentRef.collection('likes').get();
      const cBatch = db.batch();
      nestedLikes.docs.forEach((d) => cBatch.delete(d.ref));
      cBatch.delete(commentRef);
      await cBatch.commit();
    }
    console.log(`Deleted ${commentRefs.length} comments for user ${userId}`);
    
    // Delete likes
    const likesSnapshot = await db.collection('likes').where('userId', '==', userId).get();
    const likesBatch = db.batch();
    likesSnapshot.docs.forEach(doc => likesBatch.delete(doc.ref));
    await likesBatch.commit();
    
    // Delete follows (both follower and followed)
    const followsAsFollower = await db.collection('follows').where('followerId', '==', userId).get();
    const followsAsFollowed = await db.collection('follows').where('followedId', '==', userId).get();
    const followsBatch = db.batch();
    followsAsFollower.docs.forEach(doc => followsBatch.delete(doc.ref));
    followsAsFollowed.docs.forEach(doc => followsBatch.delete(doc.ref));
    await followsBatch.commit();
    
    // Delete notifications (top-level + user scoped subcollection)
    const notificationsSnapshot = await db.collection('notifications').where('userId', '==', userId).get();
    const notificationsBatch = db.batch();
    notificationsSnapshot.docs.forEach(doc => notificationsBatch.delete(doc.ref));
    await notificationsBatch.commit();

    const userNotifSnapshot = await db.collection('notifications')
      .doc(userId)
      .collection('user_notifications')
      .get();
    const scopedNotifBatch = db.batch();
    userNotifSnapshot.docs.forEach((doc) => scopedNotifBatch.delete(doc.ref));
    scopedNotifBatch.delete(db.collection('notifications').doc(userId));
    await scopedNotifBatch.commit();
    
    // Delete blocked users
    const blockedSnapshot = await db.collection('blocked_users').where('blockerId', '==', userId).get();
    const blockedBatch = db.batch();
    blockedSnapshot.docs.forEach(doc => blockedBatch.delete(doc.ref));
    await blockedBatch.commit();

    // Delete followers tree docs
    const followingTree = await db.collection('followers').doc(userId).collection('following').get();
    const followersTree = await db.collection('followers').doc(userId).collection('followers').get();
    const followerTreeBatch = db.batch();
    followingTree.docs.forEach((doc) => followerTreeBatch.delete(doc.ref));
    followersTree.docs.forEach((doc) => followerTreeBatch.delete(doc.ref));
    followerTreeBatch.delete(db.collection('followers').doc(userId));
    await followerTreeBatch.commit();
    
    // Delete user document
    await db.collection('users').doc(userId).delete();
    
    // Delete Firebase Auth account (LAST STEP)
    await authAdmin.deleteUser(userId);
    
    console.log(`✅ User ${userId} completely deleted by admin ${authContext.uid}`);
    
    return {
      success: true,
      message: 'User account completely deleted',
      deletionType: 'COMPLETE',
      deletedItems: {
        posts: postRefs.length,
        comments: commentRefs.length,
        likes: likesSnapshot.size,
        follows: followsAsFollower.size + followsAsFollowed.size,
        notifications: notificationsSnapshot.size,
        blocked: blockedSnapshot.size
      }
    };
  } catch (error) {
    console.error('Error deleting user account:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Unban a user
 * Requires admin or super_admin role
 */
exports.unbanUser = onCall(async (request) => {
  const {auth: authContext, data} = request;
  
  // Verify admin or super_admin
  if (!authContext?.token?.admin && !authContext?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {userId, reason} = data;
  const adminReason = requireReason(reason);
  
  if (!userId) {
    throw new Error('invalid-argument: userId is required');
  }
  
  try {
    await authAdmin.updateUser(userId, {disabled: false});

    // Update user document
    await patchUserDocuments(userId, {
      isBanned: false,
      banType: null,
      banUntil: null,
      banReason: null,
      authDisabled: false
      // Keep bannedBy and bannedAt for history
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: authContext.uid,
      action: 'unban_user',
      targetType: 'user',
      targetId: userId,
      reason: adminReason,
      metadata: {},
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Send unban notification to user
    await createUserNotification(
      userId,
      'report',
      'Account Unbanned',
      `Your account ban has been lifted. Reason: ${adminReason}`,
      {
        reason: adminReason
      },
      authContext.uid
    );
    
    console.log(`User ${userId} unbanned by admin ${authContext.uid}`);
    
    return {success: true, message: 'User unbanned successfully'};
  } catch (error) {
    console.error('Error unbanning user:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Adjust user honesty score
 * Requires admin or super_admin role
 * 
 * ⚠️ SOFT-REMOVED: Gamification features are disabled in MVP
 * This function is kept for backward compatibility but should not be called
 * Honesty system will be re-enabled in future version
 */
/*
exports.adjustHonesty = onCall(async (request) => {
  throw new Error('unavailable: Honesty/Gamification features are currently disabled');
});
*/
