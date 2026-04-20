const {onCall} = require('firebase-functions/v2/https');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');

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
    deepLink: 'premium',
    createdAt: FieldValue.serverTimestamp(),
    isRead: false,
    meta
  });
}

/**
 * Grant premium to a user
 * Requires admin or super_admin role
 */
exports.grantPremium = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or super_admin
  if (!auth?.token?.admin && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {userId, duration, reason} = data; // duration in days
  
  if (!userId || !duration || !reason) {
    throw new Error('invalid-argument: userId, duration, and reason are required');
  }
  
  try {
    const premiumUntil = new Date(Date.now() + duration * 86400000); // days to milliseconds
    
    // Update user document
    await db.collection('users').doc(userId).update({
      ispremium: true,
      premiumuntil: premiumUntil,
      premiumGrantedBy: 'admin',
      premiumGrantReason: reason,
      premiumGrantedAt: FieldValue.serverTimestamp()
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'grant_premium',
      targetType: 'user',
      targetId: userId,
      reason: reason,
      metadata: {
        durationDays: duration,
        premiumUntil: premiumUntil.toISOString()
      },
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Create purchase record for tracking
    await db.collection('purchases').doc(userId).collection('admin_grants').add({
      type: 'premium',
      durationDays: duration,
      grantedBy: auth.uid,
      reason: reason,
      premiumUntil: premiumUntil,
      createdAt: FieldValue.serverTimestamp()
    });
    
    // Send notification to user
    await createUserNotification(
      userId,
      'premium',
      'Premium Access Granted',
      `You have been granted ${duration} days of premium access by an administrator. Enjoy your premium features!`,
      {
        durationDays: duration,
        premiumUntil: premiumUntil.toISOString(),
        reason: reason
      },
      auth.uid
    );
    
    console.log(`Premium granted to user ${userId} for ${duration} days by admin ${auth.uid}`);
    
    return {
      success: true,
      message: `Premium granted for ${duration} days`,
      premiumUntil: premiumUntil.toISOString()
    };
  } catch (error) {
    console.error('Error granting premium:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Revoke premium from a user
 * Requires admin or super_admin role
 */
exports.revokePremium = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify admin or super_admin
  if (!auth?.token?.admin && !auth?.token?.super_admin) {
    throw new Error('permission-denied: Admin access required');
  }
  
  const {userId, reason} = data;
  
  if (!userId || !reason) {
    throw new Error('invalid-argument: userId and reason are required');
  }
  
  try {
    // Update user document
    await db.collection('users').doc(userId).update({
      ispremium: false,
      premiumuntil: null,
      premiumRevokedBy: auth.uid,
      premiumRevokeReason: reason,
      premiumRevokedAt: FieldValue.serverTimestamp()
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'revoke_premium',
      targetType: 'user',
      targetId: userId,
      reason: reason,
      metadata: {},
      timestamp: FieldValue.serverTimestamp()
    });
    
    // Send notification to user
    await createUserNotification(
      userId,
      'premium',
      'Premium Access Revoked',
      `Your premium access has been revoked. Reason: ${reason}`,
      {
        reason: reason
      },
      auth.uid
    );
    
    console.log(`Premium revoked from user ${userId} by admin ${auth.uid}`);
    
    return {success: true, message: 'Premium revoked successfully'};
  } catch (error) {
    console.error('Error revoking premium:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Scheduled premium reconciliation.
 * Safety net: if premiumuntil is expired while ispremium=true, deactivate it.
 */
exports.reconcilePremiumStatus = onSchedule('every 12 hours', async () => {
  const now = new Date();
  const usersSnapshot = await db.collection('users')
    .where('ispremium', '==', true)
    .get();

  if (usersSnapshot.empty) {
    console.log('No premium users found for reconciliation.');
    return;
  }

  const batch = db.batch();
  let updatedCount = 0;

  usersSnapshot.docs.forEach((doc) => {
    const data = doc.data() || {};
    const premiumUntil = data.premiumuntil;

    if (!premiumUntil) {
      return;
    }

    const premiumUntilDate = premiumUntil.toDate ? premiumUntil.toDate() : new Date(premiumUntil);
    if (premiumUntilDate <= now) {
      batch.update(doc.ref, {
        ispremium: false,
        premiumuntil: null,
        premiumReconciledAt: FieldValue.serverTimestamp(),
      });
      updatedCount++;
    }
  });

  if (updatedCount > 0) {
    await batch.commit();
  }

  console.log(`Premium reconciliation completed. Updated users: ${updatedCount}`);
});

/**
 * Grant XP to a user
 * Requires admin or super_admin role
 * 
 * ⚠️ SOFT-REMOVED: Gamification features are disabled in MVP
 * This function is kept for backward compatibility but should not be called
 * XP/Level system will be re-enabled in future version
 */
/*
exports.grantXP = onCall(async (request) => {
  throw new Error('unavailable: XP/Gamification features are currently disabled');
});
*/
