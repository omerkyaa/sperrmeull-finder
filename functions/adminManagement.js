const {onCall} = require('firebase-functions/v2/https');
const {initializeApp} = require('firebase-admin/app');
const {getAuth} = require('firebase-admin/auth');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');

// Initialize Admin SDK (only once)
try {
  initializeApp();
} catch (e) {
  // Already initialized
}

const db = getFirestore();

/**
 * Set admin role for a user
 * Only super_admin can set roles
 */
exports.setAdminRole = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify super_admin
  if (!auth?.token?.super_admin) {
    throw new Error('permission-denied: Only super admins can set roles');
  }
  
  const {uid, role} = data;
  
  if (!uid || !role) {
    throw new Error('invalid-argument: uid and role are required');
  }
  
  // Validate role
  const validRoles = ['admin', 'moderator', 'super_admin'];
  if (!validRoles.includes(role)) {
    throw new Error('invalid-argument: Invalid role');
  }
  
  try {
    // Set custom claims deterministically: keep non-role claims, reset role claims.
    const auth = getAuth();
    const userRecord = await auth.getUser(uid);
    const existingClaims = userRecord.customClaims || {};
    const claims = {
      ...existingClaims,
      admin: false,
      moderator: false,
      super_admin: false,
    };
    claims[role] = true;
    await auth.setCustomUserClaims(uid, claims);
    
    // Store in Firestore for reference
    await db.collection('admin_roles').doc(uid).set({
      role,
      grantedBy: auth.uid,
      grantedAt: FieldValue.serverTimestamp()
    });
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'set_admin_role',
      targetType: 'user',
      targetId: uid,
      reason: `Granted ${role} role`,
      metadata: {role},
      timestamp: FieldValue.serverTimestamp()
    });
    
    console.log(`Admin role ${role} granted to user ${uid} by ${auth.uid}`);
    
    return {success: true, message: `Role ${role} granted successfully`};
  } catch (error) {
    console.error('Error setting admin role:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Remove admin role from a user
 * Only super_admin can remove roles
 */
exports.removeAdminRole = onCall(async (request) => {
  const {auth, data} = request;
  
  // Verify super_admin
  if (!auth?.token?.super_admin) {
    throw new Error('permission-denied: Only super admins can remove roles');
  }
  
  const {uid} = data;
  
  if (!uid) {
    throw new Error('invalid-argument: uid is required');
  }
  
  try {
    const auth = getAuth();
    const userRecord = await auth.getUser(uid);
    const existingClaims = userRecord.customClaims || {};
    await auth.setCustomUserClaims(uid, {
      ...existingClaims,
      admin: false,
      moderator: false,
      super_admin: false
    });
    
    // Remove from Firestore
    await db.collection('admin_roles').doc(uid).delete();
    
    // Log action
    await db.collection('admin_logs').add({
      adminId: auth.uid,
      action: 'remove_admin_role',
      targetType: 'user',
      targetId: uid,
      reason: 'Admin role removed',
      metadata: {},
      timestamp: FieldValue.serverTimestamp()
    });
    
    console.log(`Admin role removed from user ${uid} by ${auth.uid}`);
    
    return {success: true, message: 'Admin role removed successfully'};
  } catch (error) {
    console.error('Error removing admin role:', error);
    throw new Error(`internal: ${error.message}`);
  }
});

/**
 * Get current user's admin role
 */
exports.getMyAdminRole = onCall(async (request) => {
  const {auth} = request;
  
  if (!auth) {
    throw new Error('unauthenticated: User must be authenticated');
  }
  
  try {
    const role = auth.token.super_admin ? 'super_admin' :
                 auth.token.admin ? 'admin' :
                 auth.token.moderator ? 'moderator' :
                 null;
    
    return {role};
  } catch (error) {
    console.error('Error getting admin role:', error);
    throw new Error(`internal: ${error.message}`);
  }
});
