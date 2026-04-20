# Firestore Setup Guide - SperrmüllFinder

This document provides instructions for setting up Firestore indexes and security rules for the SperrmüllFinder application.

## Required Firestore Indexes

The application requires several composite indexes for optimal performance. These are defined in `firestore.indexes.json`.

### Notifications Indexes

1. **Notifications by User and Created Date**
   - Collection: `notifications`
   - Fields: `userId` (ASC), `created_at` (DESC)
   - Purpose: Fetch user notifications ordered by newest first

2. **Notifications by User and Read Status**
   - Collection: `notifications` 
   - Fields: `userId` (ASC), `isRead` (ASC)
   - Purpose: Query unread notifications for a user

### Posts Indexes

3. **Posts by City, Category and Date**
   - Collection: `posts`
   - Fields: `city` (ASC), `category_en` (ASC), `created_at` (DESC)
   - Purpose: Search posts by location and category, sorted by newest

4. **Posts by City, Category and Likes**
   - Collection: `posts`
   - Fields: `city` (ASC), `category_en` (ASC), `likes_count` (DESC)
   - Purpose: Search posts sorted by most liked

5. **Posts by City, Category and Comments**
   - Collection: `posts`
   - Fields: `city` (ASC), `category_en` (ASC), `comments_count` (DESC)
   - Purpose: Search posts sorted by most commented

6. **Posts by City, Category and Views**
   - Collection: `posts`
   - Fields: `city` (ASC), `category_en` (ASC), `views_count` (DESC)
   - Purpose: Search posts sorted by most viewed

7. **Posts by City, Category and Expiration**
   - Collection: `posts`
   - Fields: `city` (ASC), `category_en` (ASC), `expires_at` (ASC)
   - Purpose: Search posts sorted by expiring soon

### Users Indexes

8. **Users by Display Name**
   - Collection: `users`
   - Fields: `displayname` (ASC)
   - Purpose: User search by username

## Deployment Instructions

### Prerequisites

1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```bash
   firebase login
   ```

3. Initialize Firebase in project directory:
   ```bash
   firebase init
   ```

### Deploy Indexes

1. Deploy Firestore indexes:
   ```bash
   firebase deploy --only firestore:indexes
   ```

2. Deploy Firestore rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

3. Deploy Storage rules:
   ```bash
   firebase deploy --only storage
   ```

4. Deploy all at once:
   ```bash
   firebase deploy
   ```

### Verify Deployment

1. Check Firebase Console:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project: `sperrmuellfinder-bb585`
   - Navigate to Firestore Database > Indexes
   - Verify all indexes are created and in "Ready" state

2. Test the application:
   - Open notifications screen
   - Verify no index errors in logcat
   - Test search functionality
   - Verify all queries work correctly

## Index Creation Status

After deployment, indexes may take several minutes to build. Monitor the status in Firebase Console.

### Expected Index Build Times

- Small datasets (< 1000 documents): 1-5 minutes
- Medium datasets (1000-10000 documents): 5-15 minutes  
- Large datasets (> 10000 documents): 15+ minutes

## Troubleshooting

### Common Issues

1. **Index Creation Failed**
   - Check Firebase Console for error messages
   - Verify field names match exactly
   - Ensure proper permissions

2. **Query Still Requires Index**
   - Wait for index to finish building
   - Check index status in Firebase Console
   - Verify query matches index definition exactly

3. **Permission Denied Errors**
   - Check Firestore security rules
   - Verify user authentication
   - Test rules in Firebase Console simulator

### Support

If you encounter issues:

1. Check Firebase Console logs
2. Review Firestore documentation
3. Test queries in Firebase Console
4. Contact development team

## Security Rules

The application uses comprehensive security rules defined in `firestore.rules`:

- Users can only access their own data
- Posts have public read access
- Notifications are private to each user
- XP transactions and purchases are read-only for users
- Admin operations require special permissions

## Performance Considerations

- Indexes are optimized for common query patterns
- Client-side sorting is used as fallback when indexes are unavailable
- Query limits prevent excessive data transfer
- Real-time listeners are scoped to minimize bandwidth

---

*Last updated: October 2024*
*Project: SperrmüllFinder*
*Version: 1.0.0*
