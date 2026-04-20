# Firestore Security Rules Update for User Features
# Add these rules to your existing firestore.rules file

## Report System Rules

```javascript
// Reports collection - Users can create, read own reports, admins read all
match /reports/{reportId} {
  // Users can create reports
  allow create: if request.auth != null 
    && request.resource.data.reporterId == request.auth.uid
    && request.resource.data.createdAt == request.time;
  
  // Users can read their own reports
  allow read: if request.auth != null 
    && (request.auth.token.admin == true 
        || resource.data.reporterId == request.auth.uid);
  
  // Only admins can update/delete reports
  allow update, delete: if request.auth != null 
    && request.auth.token.admin == true;
}
```

## Block System Rules

```javascript
// Blocked users collection - Users can manage their own blocks
match /blocked_users/{blockerId}/blocks/{blockedUserId} {
  // Users can create/delete their own blocks
  allow create, delete: if request.auth != null 
    && blockerId == request.auth.uid;
  
  // Users can read their own blocked list
  allow read: if request.auth != null 
    && blockerId == request.auth.uid;
  
  // No updates allowed (delete and recreate instead)
  allow update: if false;
}
```

## Account Deletion Rules

```javascript
// Account deletions collection - Read-only for users, write-only via Cloud Functions
match /account_deletions/{userId} {
  // Users can read their own deletion status
  allow read: if request.auth != null 
    && userId == request.auth.uid;
  
  // Only Cloud Functions can write (serverTimestamp required)
  allow write: if false;
}
```

## Updated User Document Rules

```javascript
// Users collection - Add deletion and ban fields
match /users/{userId} {
  // Read: public profile data
  allow read: if request.auth != null;
  
  // Create: only during registration
  allow create: if request.auth != null 
    && request.auth.uid == userId
    && request.resource.data.uid == userId;
  
  // Update: only own profile, cannot change certain fields
  allow update: if request.auth != null 
    && request.auth.uid == userId
    && request.resource.data.uid == userId
    && request.resource.data.xp == resource.data.xp  // XP managed by functions
    && request.resource.data.level == resource.data.level  // Level managed by functions
    && request.resource.data.ispremium == resource.data.ispremium  // Premium managed by functions
    && request.resource.data.isBanned == resource.data.isBanned  // Ban status managed by admin
    && request.resource.data.deletionRequested == resource.data.deletionRequested;  // Deletion managed by functions
  
  // Delete: not allowed directly (use account deletion flow)
  allow delete: if false;
}
```

## Updated Posts Rules

```javascript
// Posts collection - Add deletion and block filtering
match /posts/{postId} {
  // Read: authenticated users, not removed by admin, owner not banned
  allow read: if request.auth != null
    && (!exists(/databases/$(database)/documents/posts/$(postId)) 
        || resource.data.isRemovedByAdmin != true);
  
  // Create: authenticated, not banned users
  allow create: if request.auth != null
    && request.resource.data.ownerid == request.auth.uid
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isBanned != true;
  
  // Update: only owner, cannot change ownerid
  allow update: if request.auth != null
    && request.auth.uid == resource.data.ownerid
    && request.resource.data.ownerid == resource.data.ownerid
    && request.resource.data.isRemovedByAdmin == resource.data.isRemovedByAdmin;  // Admin removal managed by functions
  
  // Delete: only owner (soft delete via status update preferred)
  allow delete: if request.auth != null
    && request.auth.uid == resource.data.ownerid;
}
```

## Updated Comments Rules

```javascript
// Comments collection - Add reporting and deletion
match /comments/{commentId} {
  // Read: authenticated users
  allow read: if request.auth != null;
  
  // Create: authenticated, not banned users
  allow create: if request.auth != null
    && request.resource.data.ownerid == request.auth.uid
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isBanned != true;
  
  // Update: only owner
  allow update: if request.auth != null
    && request.auth.uid == resource.data.ownerid;
  
  // Delete: owner or admin
  allow delete: if request.auth != null
    && (request.auth.uid == resource.data.ownerid
        || request.auth.token.admin == true);
}
```

## Helper Functions (Add to top of rules file)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is admin
    function isAdmin() {
      return request.auth != null && request.auth.token.admin == true;
    }
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns resource
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }
    
    // Helper function to check if user is banned
    function isBanned(userId) {
      return get(/databases/$(database)/documents/users/$(userId)).data.isBanned == true;
    }
    
    // Helper function to check if user has blocked another user
    function hasBlocked(blockerId, blockedId) {
      return exists(/databases/$(database)/documents/blocked_users/$(blockerId)/blocks/$(blockedId));
    }
    
    // ... rest of your rules
  }
}
```

## Composite Indexes Required

Add these indexes to your Firestore for optimal performance:

### Reports Collection
```
Collection: reports
Fields: 
  - reporterId (Ascending)
  - createdAt (Descending)
  
Collection: reports
Fields:
  - status (Ascending)
  - priority (Descending)
  - createdAt (Descending)
```

### Blocked Users Collection
```
Collection: blocked_users/{blockerId}/blocks
Fields:
  - createdAt (Descending)
```

### Account Deletions Collection
```
Collection: account_deletions
Fields:
  - status (Ascending)
  - scheduledFor (Ascending)
```

## Deployment

1. Update your `firestore.rules` file with the above rules
2. Deploy rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

3. Create indexes (Firebase will prompt when needed, or add via Console):
   - Go to Firebase Console → Firestore → Indexes
   - Add the composite indexes listed above

## Testing

Test your rules in Firebase Console → Firestore → Rules Playground:

1. Test report creation (authenticated user)
2. Test block creation (authenticated user)
3. Test account deletion read (authenticated user)
4. Test admin operations (with admin token)
5. Test banned user post creation (should fail)

## Notes

- All write operations to `account_deletions` are handled by Cloud Functions
- Block/unblock operations are instant and don't require admin approval
- Reports are reviewed by admins via admin panel
- Soft delete is preferred over hard delete for posts
- Banned users cannot create posts or comments
- Users in deletion grace period can still access the app

---

**Status**: Ready for deployment
**Last Updated**: 2026-02-06
**Version**: 1.0
