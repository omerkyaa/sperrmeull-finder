const {onCall} = require('firebase-functions/v2/https');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');

const db = getFirestore();

/**
 * Helper function to create notification document
 */
async function createNotification(userId, title, body, deeplink) {
  const ref = db
    .collection('notifications')
    .doc(userId)
    .collection('user_notifications')
    .doc();
  await ref.set({
    id: ref.id,
    toUserId: userId,
    fromUserId: null,
    type: 'report',
    title,
    message: body,
    deepLink: deeplink || 'notifications',
    createdAt: FieldValue.serverTimestamp(),
    isRead: false,
    meta: {}
  });
}

/**
 * Send admin notification to users
 * Requires admin or super_admin role
 */
exports.sendAdminNotification = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or super_admin
  if (!auth?.token?.admin && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {targetType, targetIds, title, body, deeplink} = data;
  // targetType: 'all' | 'premium' | 'specific' | 'city'
  
  if (!targetType || !title || !body) {
    throw new Error('invalid-argument: targetType, title, and body are required');
  }
  
  try {
    let userQuery;
    let count = 0;
    
    switch (targetType) {
      case 'all':
        // Send to all users
        userQuery = db.collection('users');
        break;
        
      case 'premium':
        // Send to premium users only
        userQuery = db.collection('users').where('ispremium', '==', true);
        break;
        
      case 'specific':
        // Send to specific user IDs
        if (!targetIds || targetIds.length === 0) {
          throw new Error('invalid-argument: targetIds required for specific target type');
        }
        
        for (const userId of targetIds) {
          await createNotification(userId, title, body, deeplink);
          
          count++;
        }
        
        // Log action
        await db.collection('admin_logs').add({
          adminId: auth.uid,
          action: 'send_notification',
          targetType: 'specific_users',
          targetId: targetIds.join(','),
          reason: title,
          metadata: {
            count: count,
            body: body,
            deeplink: deeplink || null
          },
          timestamp: FieldValue.serverTimestamp()
        });
        
        return {success: true, count: count, message: `Notification sent to ${count} users`};
        
      case 'city':
        // Send to users in specific city
        if (!targetIds || targetIds.length === 0) {
          throw new Error('invalid-argument: targetIds (city name) required for city target type');
        }
        const city = targetIds[0];
        userQuery = db.collection('users').where('city', '==', city);
        break;
        
      default:
        throw new Error('invalid-argument: Invalid targetType');
    }
    
    // For 'all', 'premium', and 'city' target types
    if (userQuery) {
      const usersSnapshot = await userQuery.get();
      
      // Process in batches to avoid memory issues
      const batchSize = 500;
      const users = usersSnapshot.docs;
      
      for (let i = 0; i < users.length; i += batchSize) {
        const batch = users.slice(i, i + batchSize);
        const promises = [];
        
        for (const userDoc of batch) {
          const userId = userDoc.id;
          
          // Create notification document
          promises.push(createNotification(userId, title, body, deeplink));
          
          count++;
        }
        
        await Promise.all(promises);
        console.log(`Processed batch ${Math.floor(i / batchSize) + 1}, total: ${count}`);
      }
    }
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'send_notification',
      targetType: targetType,
      targetId: targetType === 'city' ? targetIds[0] : 'multiple',
      reason: title,
      metadata: {
        count: count,
        body: body,
        deeplink: deeplink || null
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    console.log(`Admin notification sent to ${count} users by ${auth.uid}`);
    
    return {
      success: true,
      count: count,
      message: `Notification sent to ${count} users`
    };
  } catch (error) {
    console.error('Error sending admin notification:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Send warning notification to a user
 * Requires admin or moderator role
 */
exports.sendWarning = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or moderator
  if (!auth?.token?.admin && !auth?.token?.moderator && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Moderator access required');
  }
  
  const {userId, reason, reportId} = data;
  
  if (!userId || !reason) {
    throw new Error('invalid-argument: userId and reason are required');
  }
  
  try {
    // Create warning notification
    const warningRef = db
      .collection('notifications')
      .doc(userId)
      .collection('user_notifications')
      .doc();
    await warningRef.set({
      id: warningRef.id,
      toUserId: userId,
      fromUserId: auth.uid,
      type: 'report',
      title: 'Warning from Moderator',
      message: `You have received a warning. Reason: ${reason}. Please review our community guidelines.`,
      createdAt: FieldValue.serverTimestamp(),
      isRead: false,
      deepLink: 'notifications',
      meta: {
        reason: reason,
        reportId: reportId || null
      }
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'send_warning',
      targetType: 'user',
      targetId: userId,
      reason: reason,
      metadata: {
        reportId: reportId || null
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // If this was from a report, update the report
    if (reportId) {
      await db.collection('reports').doc(reportId).update({
        status: 'resolved',
        resolvedAt: FieldValue.serverTimestamp(),
        resolvedBy: auth.uid,
        adminNotes: `Warning sent: ${reason}`
      });
    }
    
    console.log(`Warning sent to user ${userId} by ${auth.uid}`);
    
    return {success: true, message: 'Warning sent successfully'};
  } catch (error) {
    console.error('Error sending warning:', error);
    throw new Error(`internal: ${error.message}`);
  }
});
