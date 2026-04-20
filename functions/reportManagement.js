const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');
const {getStorage} = require('firebase-admin/storage');

const db = getFirestore();

function requireAdmin(auth) {
  if (!auth) {
    throw new HttpsError('unauthenticated', 'User must be authenticated');
  }
}

function requireReason(reason) {
  if (!reason || typeof reason !== 'string' || reason.trim().length === 0) {
    throw new HttpsError('invalid-argument', 'A non-empty explanation is required');
  }
  return reason.trim();
}

function hasSuperAdminClaim(auth) {
  return auth?.token?.super_admin === true;
}

function getStoragePathFromUrl(url) {
  try {
    if (!url || typeof url !== 'string') return null;
    const decodedUrl = decodeURIComponent(url);
    const match = decodedUrl.match(/\/o\/(.+?)\?/);
    return match ? match[1] : null;
  } catch (error) {
    console.error('Error extracting storage path:', error);
    return null;
  }
}

async function hardDeletePost(targetId, adminUid) {
  const postRef = db.collection('posts').doc(targetId);
  const postSnap = await postRef.get();
  if (!postSnap.exists) {
    throw new HttpsError('not-found', 'Post not found');
  }

  const postData = postSnap.data() || {};

  const [topLevelComments, topLevelLikes, subComments, subLikes] = await Promise.all([
    db.collection('comments').where('postId', '==', targetId).get(),
    db.collection('likes').where('postId', '==', targetId).get(),
    postRef.collection('comments').get(),
    postRef.collection('likes').get()
  ]);

  const batch = db.batch();
  topLevelComments.docs.forEach((d) => batch.delete(d.ref));
  topLevelLikes.docs.forEach((d) => batch.delete(d.ref));
  subComments.docs.forEach((d) => batch.delete(d.ref));
  subLikes.docs.forEach((d) => batch.delete(d.ref));
  batch.delete(postRef);
  await batch.commit();

  const bucket = getStorage().bucket();
  const images = Array.isArray(postData.images) ? postData.images : [];
  for (const imageUrl of images) {
    const path = getStoragePathFromUrl(imageUrl);
    if (!path) continue;
    try {
      await bucket.file(path).delete();
    } catch (e) {
      console.warn(`Could not delete storage file ${path}:`, e.message);
    }
  }

  return {ownerId: postData.ownerid || postData.ownerId || null};
}

async function hardDeleteComment(targetId) {
  let commentRef = db.collection('comments').doc(targetId);
  let commentSnap = await commentRef.get();

  if (!commentSnap.exists) {
    const byCommentId = await db.collectionGroup('comments')
      .where('comment_id', '==', targetId)
      .limit(1)
      .get();

    if (!byCommentId.empty) {
      commentRef = byCommentId.docs[0].ref;
      commentSnap = byCommentId.docs[0];
    }
  }

  if (!commentSnap.exists) {
    throw new HttpsError('not-found', 'Comment not found');
  }

  const commentData = commentSnap.data() || {};

  const [topLevelLikes, nestedLikes] = await Promise.all([
    db.collection('likes').where('commentId', '==', targetId).get(),
    commentRef.collection('likes').get()
  ]);

  const batch = db.batch();
  topLevelLikes.docs.forEach((d) => batch.delete(d.ref));
  nestedLikes.docs.forEach((d) => batch.delete(d.ref));
  batch.delete(commentRef);
  await batch.commit();

  const postId = commentData.postId || commentData.post_id;
  if (postId) {
    await db.collection('posts').doc(postId).update({
      comments_count: FieldValue.increment(-1)
    });
  }

  return {ownerId: commentData.authorId || commentData.author_id || null};
}

async function notifyUser(userId, type, title, message, data = {}) {
  const timestamp = FieldValue.serverTimestamp();
  const scopedRef = db.collection('notifications')
    .doc(userId)
    .collection('user_notifications')
    .doc();
  await scopedRef.set({
    id: scopedRef.id,
    toUserId: userId,
    fromUserId: data.fromUserId || null,
    type,
    title,
    message,
    isRead: false,
    deepLink: data.deepLink || 'notifications',
    postId: data.postId || null,
    commentId: data.commentId || null,
    meta: data,
    createdAt: timestamp
  });
}

/**
 * Trigger: When a new report is created
 * Action: Notify all admins of high-priority reports
 */
exports.onReportCreated = onDocumentCreated('reports/{reportId}', async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    console.log('No data in snapshot');
    return null;
  }
  
  const report = snapshot.data();
  const reportId = event.params.reportId;
  
  console.log(`New report created: ${reportId}, priority: ${report.priority}, type: ${report.type}`);
  
  // Notify admins for HIGH and CRITICAL priority reports
  if (report.priority === 'HIGH' || report.priority === 'CRITICAL') {
    try {
      // Get all admins
      const adminsSnapshot = await db.collection('users')
        .where('isAdmin', '==', true)
        .get();
      
      if (adminsSnapshot.empty) {
        console.log('No admins found to notify');
        return null;
      }
      
      // Create notifications for each admin
      const batch = db.batch();
      const timestamp = FieldValue.serverTimestamp();
      
      adminsSnapshot.docs.forEach((adminDoc) => {
        const adminId = adminDoc.id;
        const notificationRef = db.collection('notifications')
          .doc(adminId)
          .collection('user_notifications')
          .doc();
        
        batch.set(notificationRef, {
          id: notificationRef.id,
          toUserId: adminId,
          fromUserId: report.reporterId || null,
          type: 'ADMIN_REPORT',
          title: `New ${report.priority} Priority Report`,
          message: `${report.reporterName} reported a ${report.type}: ${report.reason}`,
          meta: {
            reportId: reportId,
            reportType: report.type,
            reportPriority: report.priority,
            targetId: report.targetId,
            reporterId: report.reporterId
          },
          deepLink: 'notifications',
          isRead: false,
          createdAt: timestamp
        });
      });
      
      await batch.commit();
      console.log(`Notified ${adminsSnapshot.size} admins about report ${reportId}`);
      
    } catch (error) {
      console.error('Error notifying admins:', error);
    }
  }
  
  return null;
});

/**
 * Dismiss a report (admin only)
 */
exports.dismissReport = onCall(async (request) => {
  const {auth} = request;
  requireAdmin(auth);
  
  // Check admin status
  const isAdmin = await checkAdminStatus(auth.uid);
  if (!isAdmin) {
    throw new HttpsError('permission-denied', 'Only admins can dismiss reports');
  }
  
  const {reportId, reason} = request.data;
  const adminReason = requireReason(reason);
  
  if (!reportId) {
    throw new HttpsError('invalid-argument', 'reportId is required');
  }
  
  try {
    await db.collection('reports').doc(reportId).update({
      status: 'REJECTED',
      resolvedBy: auth.uid,
      resolvedAt: FieldValue.serverTimestamp(),
      adminNotes: adminReason
    });
    
    // Log the action
    await logAdminAction(auth.uid, 'DISMISS_REPORT', {reportId, reason: adminReason});
    
    console.log(`Report ${reportId} dismissed by ${auth.uid}`);
    return {success: true};
    
  } catch (error) {
    console.error('Error dismissing report:', error);
    throw new HttpsError('internal', 'Failed to dismiss report');
  }
});

/**
 * Warn a user based on report (admin only)
 */
exports.warnUser = onCall(async (request) => {
  const {auth} = request;
  requireAdmin(auth);
  
  const isAdmin = await checkAdminStatus(auth.uid);
  if (!isAdmin) {
    throw new HttpsError('permission-denied', 'Only admins can warn users');
  }
  
  const {reportId, reason} = request.data;
  const adminReason = requireReason(reason);
  
  if (!reportId) {
    throw new HttpsError('invalid-argument', 'reportId is required');
  }
  
  try {
    // Get report to find target user
    const reportDoc = await db.collection('reports').doc(reportId).get();
    if (!reportDoc.exists) {
      throw new HttpsError('not-found', 'Report not found');
    }
    
    const report = reportDoc.data();
    const targetUserId = report.targetOwnerId;
    
    if (!targetUserId) {
      throw new HttpsError('invalid-argument', 'Target user not found in report');
    }
    
    // Send warning notification
    await notifyUser(
      targetUserId,
      'WARNING',
      'Warning from Admin',
      adminReason,
      {
        reportId: reportId,
        reportType: report.type,
        adminId: auth.uid
      }
    );

    // Update report status
    await db.collection('reports').doc(reportId).update({
      status: 'RESOLVED',
      resolvedBy: auth.uid,
      resolvedAt: FieldValue.serverTimestamp(),
      adminNotes: `Warning sent: ${adminReason}`
    });
    
    // Log the action
    await logAdminAction(auth.uid, 'WARN_USER', {reportId, targetUserId, reason: adminReason});
    
    console.log(`User ${targetUserId} warned for report ${reportId}`);
    return {success: true};
    
  } catch (error) {
    console.error('Error warning user:', error);
    throw new HttpsError('internal', error.message || 'Failed to warn user');
  }
});

/**
 * Delete reported content (post or comment) - admin only
 */
exports.deleteReportedContent = onCall(async (request) => {
  const {auth} = request;
  requireAdmin(auth);
  
  const role = await getAdminClaims(auth.uid);
  const canDeleteContent = role.super_admin === true || role.admin === true;
  if (!canDeleteContent) {
    throw new HttpsError('permission-denied', 'Only admin or super admin can delete content');
  }
  
  const {reportId, reason} = request.data;
  const adminReason = requireReason(reason);
  
  if (!reportId) {
    throw new HttpsError('invalid-argument', 'reportId is required');
  }
  
  try {
    // Get report to find target content
    const reportDoc = await db.collection('reports').doc(reportId).get();
    if (!reportDoc.exists) {
      throw new HttpsError('not-found', 'Report not found');
    }
    
    const report = reportDoc.data();
    const targetId = report.targetId;
    const targetType = report.type;
    
    let deletedOwnerId = report.targetOwnerId || null;

    // Delete based on type
    if (targetType === 'POST') {
      if (hasSuperAdminClaim(auth)) {
        const result = await hardDeletePost(targetId, auth.uid);
        if (!deletedOwnerId) {
          deletedOwnerId = result.ownerId;
        }
      } else {
        const postRef = db.collection('posts').doc(targetId);
        const postSnap = await postRef.get();
        if (!postSnap.exists) {
          throw new HttpsError('not-found', 'Post not found');
        }
        if (!deletedOwnerId) {
          const postData = postSnap.data() || {};
          deletedOwnerId = postData.ownerid || postData.ownerId || null;
        }

        await postRef.update({
          status: 'removed',
          isDeleted: true,
          deletedAt: FieldValue.serverTimestamp(),
          deletedBy: auth.uid,
          deletionReason: adminReason
        });
      }
      console.log(`Post ${targetId} deleted`);
    } else if (targetType === 'COMMENT') {
      if (hasSuperAdminClaim(auth)) {
        const result = await hardDeleteComment(targetId);
        if (!deletedOwnerId) {
          deletedOwnerId = result.ownerId;
        }
        console.log(`Comment ${targetId} hard deleted`);
      } else {
      // Support both legacy top-level comments and post subcollection comments
        let commentRef = db.collection('comments').doc(targetId);
        let commentSnap = await commentRef.get();

        if (!commentSnap.exists) {
          const byCommentId = await db.collectionGroup('comments')
            .where('comment_id', '==', targetId)
            .limit(1)
            .get();

          if (!byCommentId.empty) {
            commentRef = byCommentId.docs[0].ref;
            commentSnap = byCommentId.docs[0];
          }
        }

        if (!commentSnap.exists) {
          throw new HttpsError('not-found', 'Comment not found');
        }

        const commentData = commentSnap.data() || {};
        if (!deletedOwnerId) {
          deletedOwnerId = commentData.authorId || commentData.author_id || null;
        }

        await commentRef.update({
          isDeleted: true,
          isRemoved: true,
          deletedAt: FieldValue.serverTimestamp(),
          removedAt: FieldValue.serverTimestamp(),
          deletedBy: auth.uid,
          removedBy: auth.uid,
          deletionReason: adminReason,
          removalReason: adminReason,
          content: '[Removed by admin]'
        });

        const postId = commentData.postId || commentData.post_id;
        if (postId) {
          await db.collection('posts').doc(postId).update({
            comments_count: FieldValue.increment(-1)
          });
        }

        console.log(`Comment ${targetId} deleted`);
      }
    } else if (targetType === 'USER') {
      // Deleting accounts is super-admin-only and handled by dedicated function
      if (!(role.super_admin === true)) {
        throw new HttpsError('permission-denied', 'Only super admin can delete user accounts');
      }
      throw new HttpsError(
        'failed-precondition',
        'Use deleteUserAccount action for reported user deletion'
      );
    } else {
      throw new HttpsError('invalid-argument', 'Invalid target type for deletion');
    }
    
    // Update report status
    await db.collection('reports').doc(reportId).update({
      status: 'APPROVED',
      resolvedBy: auth.uid,
      resolvedAt: FieldValue.serverTimestamp(),
      adminNotes: `Content deleted: ${adminReason}`
    });
    
    // Notify content owner
    if (deletedOwnerId) {
      await notifyUser(
        deletedOwnerId,
        'CONTENT_REMOVED',
        'Your content was removed',
        `Your ${targetType.toLowerCase()} was removed. Explanation: ${adminReason}`,
        {
          reportId: reportId,
          contentId: targetId,
          contentType: targetType,
          reason: adminReason
        }
      );
    }
    
    // Log the action
    await logAdminAction(auth.uid, 'DELETE_CONTENT', {
      reportId,
      targetId,
      targetType,
      reason: adminReason
    });
    
    console.log(`Content deleted for report ${reportId}`);
    return {success: true};
    
  } catch (error) {
    console.error('Error deleting content:', error);
    throw new HttpsError('internal', error.message || 'Failed to delete content');
  }
});

/**
 * Check if user has admin privileges
 */
async function checkAdminStatus(uid) {
  try {
    const claims = await getAdminClaims(uid);
    return claims.super_admin === true || claims.admin === true || claims.moderator === true;
  } catch (error) {
    console.error('Error checking admin status:', error);
    return false;
  }
}

async function getAdminClaims(uid) {
  const userRecord = await getAuth().getUser(uid);
  return userRecord.customClaims || {};
}

/**
 * Log admin action to admin_logs collection
 */
async function logAdminAction(adminId, action, data) {
  try {
    const logRef = db.collection('admin_logs').doc();
    await logRef.set({
      id: logRef.id,
      adminId: adminId,
      action: action,
      data: data,
      timestamp: FieldValue.serverTimestamp()
    });
  } catch (error) {
    console.error('Error logging admin action:', error);
    // Don't throw - logging shouldn't break main operation
  }
}
