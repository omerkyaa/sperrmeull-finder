const {onDocumentUpdated, onDocumentDeleted} = require('firebase-functions/v2/firestore');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore} = require('firebase-admin/firestore');

initializeApp();
const db = getFirestore();

// Import admin management functions
const adminManagement = require('./adminManagement');
exports.setAdminRole = adminManagement.setAdminRole;
exports.removeAdminRole = adminManagement.removeAdminRole;
exports.getMyAdminRole = adminManagement.getMyAdminRole;

// Import user management functions
const userManagement = require('./userManagement');
exports.softBanUser = userManagement.softBanUser;
exports.hardBanUser = userManagement.hardBanUser;
exports.deleteUserAccount = userManagement.deleteUserAccount;
exports.unbanUser = userManagement.unbanUser;
// Note: old banUser replaced with 3-tier system (softBan, hardBan, deleteAccount)

// Import premium management functions
const premiumManagement = require('./premiumManagement');
exports.grantPremium = premiumManagement.grantPremium;
exports.revokePremium = premiumManagement.revokePremium;
exports.reconcilePremiumStatus = premiumManagement.reconcilePremiumStatus;
// Note: grantXP removed (gamification soft-removed)

// Import content moderation functions
const contentModeration = require('./contentModeration');
exports.deletePost = contentModeration.deletePost;
exports.deleteComment = contentModeration.deleteComment;
exports.restorePost = contentModeration.restorePost;

// Import notification functions
const notifications = require('./notifications');
exports.sendAdminNotification = notifications.sendAdminNotification;
exports.sendWarning = notifications.sendWarning;
const notificationTriggers = require('./notificationTriggers');
exports.onNotificationCreated = notificationTriggers.onNotificationCreated;
exports.onPostCreatedNotifyNearbyPremium = notificationTriggers.onPostCreatedNotifyNearbyPremium;

// Import report management functions
const reportManagement = require('./reportManagement');
exports.onReportCreated = reportManagement.onReportCreated;
exports.dismissReport = reportManagement.dismissReport;
exports.warnUser = reportManagement.warnUser;
exports.deleteReportedContent = reportManagement.deleteReportedContent;

// Import account deletion functions
const accountDeletion = require('./accountDeletion');
exports.requestAccountDeletion = accountDeletion.requestAccountDeletion;
exports.cancelAccountDeletion = accountDeletion.cancelAccountDeletion;
exports.processScheduledDeletions = accountDeletion.processScheduledDeletions;
exports.sendDeletionReminders = accountDeletion.sendDeletionReminders;

// RevenueCat webhook sync
const revenueCatWebhook = require('./revenueCatWebhook');
exports.revenueCatWebhook = revenueCatWebhook.revenueCatWebhook;

// Import Firebase Auth Blocking function (beforeSignIn)
// Keep deploy resilient when authBlocking module is not present in this branch.
try {
  // eslint-disable-next-line global-require
  const authBlocking = require('./authBlocking');
  exports.hardbanuser = authBlocking.hardbanuser;
} catch (error) {
  console.warn('authBlocking.js not found, skipping hardbanuser export.');
}

/**
 * Kullanıcı profili güncellendiğinde tüm ilgili collection'ları güncelle
 * Trigger: users/{uid} document update
 */
exports.onUserProfileUpdate = onDocumentUpdated('users/{userId}', async (event) => {
  const change = event.data;
  const userId = event.params.userId;
  const beforeData = change.before.data();
  const afterData = change.after.data();

  // Hangi alanlar değişti?
  const displayNameChanged = beforeData.displayName !== afterData.displayName;
  const photoUrlChanged = beforeData.photoUrl !== afterData.photoUrl;
  const nicknameChanged = beforeData.nickname !== afterData.nickname;
  const beforeIsPremium = beforeData.ispremium ?? beforeData.isPremium ?? false;
  const afterIsPremium = afterData.ispremium ?? afterData.isPremium ?? false;
  const isPremiumChanged = beforeIsPremium !== afterIsPremium;
  const cityChanged = beforeData.city !== afterData.city;

  // Hiçbir denormalized alan değişmediyse işlem yapma
  if (!displayNameChanged && !photoUrlChanged && !nicknameChanged && 
      !isPremiumChanged && !cityChanged) {
    console.log('No denormalized fields changed, skipping update');
    return null;
  }

  console.log(`User ${userId} profile updated. Cascading changes...`);

  // Batch işlemleri için array
  const batchPromises = [];

  try {
    // 1. COMMENTS güncelle
    if (displayNameChanged || photoUrlChanged || nicknameChanged) {
      const commentsSnapshot = await db.collection('comments')
        .where('authorId', '==', userId)
        .get();

      if (!commentsSnapshot.empty) {
        const commentsBatch = db.batch();
        commentsSnapshot.docs.forEach(doc => {
          const updateData = {};
          if (displayNameChanged) updateData.authorName = afterData.displayName;
          if (photoUrlChanged) updateData.authorPhotoUrl = afterData.photoUrl;
          if (nicknameChanged) updateData.authorNickname = afterData.nickname;
          commentsBatch.update(doc.ref, updateData);
        });
        batchPromises.push(commentsBatch.commit());
        console.log(`Updating ${commentsSnapshot.size} comments`);
      }
    }

    // 2. LIKES güncelle
    if (displayNameChanged || photoUrlChanged || nicknameChanged || isPremiumChanged) {
      const likesSnapshot = await db.collection('likes')
        .where('userId', '==', userId)
        .get();

      if (!likesSnapshot.empty) {
        const likesBatch = db.batch();
        likesSnapshot.docs.forEach(doc => {
          const updateData = {};
          if (displayNameChanged) updateData.userDisplayName = afterData.displayName;
          if (photoUrlChanged) updateData.userPhotoUrl = afterData.photoUrl;
          if (nicknameChanged) updateData.userNickname = afterData.nickname;
          if (isPremiumChanged) updateData.userIsPremium = afterIsPremium;
          likesBatch.update(doc.ref, updateData);
        });
        batchPromises.push(likesBatch.commit());
        console.log(`Updating ${likesSnapshot.size} likes`);
      }
    }

    // 3. FOLLOWS güncelle (follower olarak)
    if (displayNameChanged || photoUrlChanged || nicknameChanged || isPremiumChanged || cityChanged) {
      const followsAsFollowerSnapshot = await db.collection('follows')
        .where('followerId', '==', userId)
        .get();

      if (!followsAsFollowerSnapshot.empty) {
        const followerBatch = db.batch();
        followsAsFollowerSnapshot.docs.forEach(doc => {
          const updateData = {};
          if (displayNameChanged) updateData.followerDisplayName = afterData.displayName;
          if (photoUrlChanged) updateData.followerPhotoUrl = afterData.photoUrl;
          if (nicknameChanged) updateData.followerNickname = afterData.nickname;
          if (isPremiumChanged) updateData.followerIsPremium = afterIsPremium;
          if (cityChanged) updateData.followerCity = afterData.city;
          followerBatch.update(doc.ref, updateData);
        });
        batchPromises.push(followerBatch.commit());
        console.log(`Updating ${followsAsFollowerSnapshot.size} follows (as follower)`);
      }
    }

    // 4. FOLLOWS güncelle (followed olarak)
    if (displayNameChanged || photoUrlChanged || nicknameChanged || isPremiumChanged || cityChanged) {
      const followsAsFollowedSnapshot = await db.collection('follows')
        .where('followedId', '==', userId)
        .get();

      if (!followsAsFollowedSnapshot.empty) {
        const followedBatch = db.batch();
        followsAsFollowedSnapshot.docs.forEach(doc => {
          const updateData = {};
          if (displayNameChanged) updateData.followedDisplayName = afterData.displayName;
          if (photoUrlChanged) updateData.followedPhotoUrl = afterData.photoUrl;
          if (nicknameChanged) updateData.followedNickname = afterData.nickname;
          if (isPremiumChanged) updateData.followedIsPremium = afterIsPremium;
          if (cityChanged) updateData.followedCity = afterData.city;
          followedBatch.update(doc.ref, updateData);
        });
        batchPromises.push(followedBatch.commit());
        console.log(`Updating ${followsAsFollowedSnapshot.size} follows (as followed)`);
      }
    }

    // 5. POSTS güncelle (ownerDisplayName, ownerPhotoUrl varsa)
    if (displayNameChanged || photoUrlChanged) {
      const postsSnapshot = await db.collection('posts')
        .where('ownerId', '==', userId)
        .get();

      if (!postsSnapshot.empty) {
        const postsBatch = db.batch();
        postsSnapshot.docs.forEach(doc => {
          const updateData = {};
          if (displayNameChanged) updateData.ownerDisplayName = afterData.displayName;
          if (photoUrlChanged) updateData.ownerPhotoUrl = afterData.photoUrl;
          postsBatch.update(doc.ref, updateData);
        });
        batchPromises.push(postsBatch.commit());
        console.log(`Updating ${postsSnapshot.size} posts`);
      }
    }

    // Tüm batch işlemlerini çalıştır
    await Promise.all(batchPromises);
    
    console.log(`✅ Successfully cascaded user ${userId} profile updates`);
    return null;

  } catch (error) {
    console.error('Error cascading user profile updates:', error);
    throw error;
  }
});

/**
 * Kullanıcı silindiğinde tüm ilgili verileri temizle (opsiyonel)
 */
exports.onUserDelete = onDocumentDeleted('users/{userId}', async (event) => {
  const userId = event.params.userId;
  console.log(`User ${userId} deleted. Cleaning up related data...`);

  const batchPromises = [];

  try {
    // Comments sil
    const commentsSnapshot = await db.collection('comments')
      .where('authorId', '==', userId)
      .get();
    if (!commentsSnapshot.empty) {
      const commentsBatch = db.batch();
      commentsSnapshot.docs.forEach(doc => commentsBatch.delete(doc.ref));
      batchPromises.push(commentsBatch.commit());
    }

    // Likes sil
    const likesSnapshot = await db.collection('likes')
      .where('userId', '==', userId)
      .get();
    if (!likesSnapshot.empty) {
      const likesBatch = db.batch();
      likesSnapshot.docs.forEach(doc => likesBatch.delete(doc.ref));
      batchPromises.push(likesBatch.commit());
    }

    // Follows sil
    const followsSnapshot = await db.collection('follows')
      .where('followerId', '==', userId)
      .get();
    const followedSnapshot = await db.collection('follows')
      .where('followedId', '==', userId)
      .get();
    
    if (!followsSnapshot.empty || !followedSnapshot.empty) {
      const followsBatch = db.batch();
      followsSnapshot.docs.forEach(doc => followsBatch.delete(doc.ref));
      followedSnapshot.docs.forEach(doc => followsBatch.delete(doc.ref));
      batchPromises.push(followsBatch.commit());
    }

    await Promise.all(batchPromises);
    console.log(`✅ Successfully cleaned up user ${userId} data`);
    return null;

  } catch (error) {
    console.error('Error cleaning up user data:', error);
    throw error;
  }
});
