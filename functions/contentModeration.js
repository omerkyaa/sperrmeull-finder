const {onCall} = require('firebase-functions/v2/https');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getStorage} = require('firebase-admin/storage');

const db = getFirestore();

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
    deepLink: meta.postId ? `post:${meta.postId}` : 'notifications',
    createdAt: FieldValue.serverTimestamp(),
    isRead: false,
    meta
  });
}

function requireReason(reason) {
  if (!reason || typeof reason !== 'string' || reason.trim().length === 0) {
    throw new Error('invalid-argument: explanation is required');
  }
  return reason.trim();
}

/**
 * Helper function to extract storage path from URL
 */
function getStoragePathFromUrl(url) {
  try {
    const decodedUrl = decodeURIComponent(url);
    const match = decodedUrl.match(/\/o\/(.+?)\?/);
    return match ? match[1] : null;
  } catch (error) {
    console.error('Error extracting storage path:', error);
    return null;
  }
}

/**
 * Delete a post
 * Requires admin or moderator role
 */
exports.deletePost = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or moderator
  if (!auth?.token?.admin && !auth?.token?.moderator && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Moderator access required');
  }
  
  const {postId, reason, reportId} = data;
  const adminReason = requireReason(reason);
  
  if (!postId) {
    throw new Error('invalid-argument: postId is required');
  }
  
  try {
    const postRef = db.collection('posts').doc(postId);
    const postDoc = await postRef.get();
    
    if (!postDoc.exists) {
      throw new Error('not-found: Post not found');
    }
    
    const postData = postDoc.data();
    const ownerId = postData.ownerid || postData.ownerId;
    
    // Soft delete: mark as removed
    await postRef.update({
      status: 'removed',
      removedBy: auth.uid,
      removedAt: FieldValue.serverTimestamp(),
      removalReason: adminReason
    });
    
    // Delete associated images from Storage
    const images = postData.images || [];
    const bucket = getStorage().bucket();
    
    for (const imageUrl of images) {
      try {
        const imagePath = getStoragePathFromUrl(imageUrl);
        if (imagePath) {
          await bucket.file(imagePath).delete();
          console.log(`Deleted image: ${imagePath}`);
        }
      } catch (err) {
        console.error('Error deleting image:', err);
        // Continue with other images even if one fails
      }
    }
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'delete_post',
      targetType: 'post',
      targetId: postId,
      reason: adminReason,
      metadata: {
        reportId: reportId || null,
        ownerId: ownerId,
        imageCount: images.length
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Notify post owner
    await createUserNotification(
      ownerId,
      'report',
      'Post Removed',
      `Your post was removed by a moderator. Reason: ${adminReason}`,
      {postId, reason: adminReason},
      auth.uid
    );
    
    // If this was from a report, update the report
    if (reportId) {
      await db.collection('reports').doc(reportId).update({
        status: 'resolved',
        resolvedAt: FieldValue.serverTimestamp(),
        resolvedBy: auth.uid,
        adminNotes: `Post deleted: ${adminReason}`
      });
    }
    
    console.log(`Post ${postId} deleted by admin ${auth.uid}`);
    
    return {success: true, message: 'Post deleted successfully'};
  } catch (error) {
    console.error('Error deleting post:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Delete a comment
 * Requires admin or moderator role
 */
exports.deleteComment = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or moderator
  if (!auth?.token?.admin && !auth?.token?.moderator && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Moderator access required');
  }
  
  const {commentId, reason, reportId} = data;
  const adminReason = requireReason(reason);
  
  if (!commentId) {
    throw new Error('invalid-argument: commentId is required');
  }
  
  try {
    const commentRef = db.collection('comments').doc(commentId);
    const commentDoc = await commentRef.get();
    
    if (!commentDoc.exists) {
      throw new Error('not-found: Comment not found');
    }
    
    const commentData = commentDoc.data();
    const authorId = commentData.authorId;
    const postId = commentData.postId;
    
    // Soft delete: mark as removed
    await commentRef.update({
      text: '[Removed by moderator]',
      isRemoved: true,
      removedBy: auth.uid,
      removedAt: FieldValue.serverTimestamp(),
      removalReason: adminReason
    });
    
    // Decrement comment count on post
    await db.collection('posts').doc(postId).update({
      comments_count: FieldValue.increment(-1)
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'delete_comment',
      targetType: 'comment',
      targetId: commentId,
      reason: adminReason,
      metadata: {
        reportId: reportId || null,
        authorId: authorId,
        postId: postId
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Notify comment author
    await createUserNotification(
      authorId,
      'report',
      'Comment Removed',
      `Your comment was removed by a moderator. Reason: ${adminReason}`,
      {commentId, postId, reason: adminReason},
      auth.uid
    );
    
    // If this was from a report, update the report
    if (reportId) {
      await db.collection('reports').doc(reportId).update({
        status: 'resolved',
        resolvedAt: FieldValue.serverTimestamp(),
        resolvedBy: auth.uid,
        adminNotes: `Comment deleted: ${adminReason}`
      });
    }
    
    console.log(`Comment ${commentId} deleted by admin ${auth.uid}`);
    
    return {success: true, message: 'Comment deleted successfully'};
  } catch (error) {
    console.error('Error deleting comment:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Restore a deleted post
 * Requires admin or super_admin role
 */
exports.restorePost = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or super_admin
  if (!auth?.token?.admin && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {postId, reason} = data;
  
  if (!postId || !reason) {
    throw new Error('invalid-argument: postId and reason are required');
  }
  
  try {
    const postRef = db.collection('posts').doc(postId);
    const postDoc = await postRef.get();
    
    if (!postDoc.exists) {
      throw new Error('not-found: Post not found');
    }
    
    // Restore post
    await postRef.update({
      status: 'active',
      restoredBy: auth.uid,
      restoredAt: FieldValue.serverTimestamp(),
      restorationReason: reason
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'restore_post',
      targetType: 'post',
      targetId: postId,
      reason: reason,
      metadata: {},
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Notify post owner
    const ownerId = postDoc.data().ownerid;
    await createUserNotification(
      ownerId,
      'report',
      'Post Restored',
      `Your post has been restored. Reason: ${reason}`,
      {postId, reason: reason},
      auth.uid
    );
    
    console.log(`Post ${postId} restored by admin ${auth.uid}`);
    
    return {success: true, message: 'Post restored successfully'};
  } catch (error) {
    console.error('Error restoring post:', error);
    throw new Error(`internal: ${error.message}`);
  }
});
