# Firestore Indexes - Complete Production Setup

## 📋 Overview

This document describes **all 33 composite indexes** required for SperrmuellFinder's complete functionality.

## 🎯 Index Categories

### 1️⃣ Posts Collection (17 indexes)
The most critical collection with extensive filtering and sorting needs.

#### Basic Status + Sorting
- `(status ASC, created_at DESC)` - Active posts feed, newest first
- `(status ASC, created_at ASC)` - Active posts feed, oldest first  
- `(status ASC, likes_count DESC)` - Most liked active posts
- `(status ASC, comments_count DESC)` - Most commented active posts
- `(status ASC, views_count DESC)` - Most viewed active posts
- `(status ASC, expires_at ASC)` - Expiring soon

#### Location-Based Queries
- `(status ASC, city ASC, created_at DESC)` - Posts in specific city
- `(status ASC, city ASC, likes_count DESC)` - Popular posts in city

#### User-Specific Queries
- `(status ASC, ownerid ASC, created_at DESC)` - User's own posts

#### Category Filtering
- `(status ASC, category_en ASC, created_at DESC)` - Category filtered feed
- `(status ASC, category_en ASC, likes_count DESC)` - Popular in category

#### Advanced Multi-Filter Queries
- `(city ASC, category_en ASC, created_at DESC)` - City + category feed
- `(city ASC, category_en ASC, likes_count DESC)` - Popular in city + category
- `(city ASC, category_en ASC, comments_count DESC)` - Most discussed in city + category
- `(city ASC, category_en ASC, views_count DESC)` - Most viewed in city + category
- `(city ASC, category_en ASC, expires_at ASC)` - Expiring items in city + category

#### Premium/Search Queries
- `(status ASC, city ASC, category_en ASC, created_at ASC)` - Full filter combo

### 2️⃣ Comments Collection (1 index)
- `(postId ASC, created_at DESC)` - Comments for a specific post, newest first

### 3️⃣ Likes Collection (1 index)
- `(postId ASC, createdAt DESC)` - Likes for a specific post, newest first

### 4️⃣ Follows Collection (3 indexes)
Social graph queries for followers/following functionality.

- `(followerId ASC, isActive ASC, created_at DESC)` - Who user is following
- `(followedId ASC, isActive ASC, created_at DESC)` - User's followers
- `(followerId ASC, followedId ASC, isActive ASC)` - Check if specific follow exists

### 5️⃣ Notifications Collection (2 indexes)
- `(userId ASC, created_at DESC)` - User's notifications, newest first
- `(userId ASC, isRead ASC)` - Filter read/unread notifications

### 6️⃣ Blocks Collection (1 index)
- `(blockerId ASC, createdAt DESC)` - User's blocked list

### 7️⃣ Reports Collection (5 indexes)
Admin moderation and report management.

- `(status ASC, created_at DESC)` - Reports by status
- `(priority ASC, status ASC, created_at DESC)` - Priority + status filter
- `(priority ASC, status ASC, type ASC, created_at DESC)` - Full report filter
- `(reporterId ASC, createdAt ASC)` - Reports by specific user
- `(status ASC, priority DESC, createdAt DESC)` - Admin dashboard view

### 8️⃣ Moderation Queue Collection (1 index)
- `(status ASC, priority DESC, createdAt DESC)` - Pending moderation items

### 9️⃣ Account Deletions Collection (1 index)
- `(status ASC, scheduledFor ASC)` - Scheduled deletion management

## 📊 Total Index Count

| Collection | Indexes | Purpose |
|------------|---------|---------|
| posts | 17 | Main feed, search, filters, sorting |
| comments | 1 | Post comments |
| likes | 1 | Post likes |
| follows | 3 | Social graph |
| notifications | 2 | User notifications |
| blocks | 1 | Blocked users |
| reports | 5 | Admin moderation |
| moderation_queue | 1 | Admin queue |
| account_deletions | 1 | GDPR compliance |
| **TOTAL** | **33** | **Complete app** |

## 🚀 Deployment

Indexes deployed via Firebase CLI:
```bash
firebase deploy --only firestore:indexes
```

**Status**: ✅ Successfully deployed on 2026-02-15

## ⏱️ Build Time

- **First deployment**: 10-30 minutes (depending on data volume)
- **Subsequent updates**: 5-15 minutes per new index
- **Check status**: [Firebase Console - Indexes](https://console.firebase.google.com/project/sperrmuellfinder-bb585/firestore/indexes)

## 🔍 Index Status Verification

All indexes must show **"Enabled"** status before queries work:
1. Go to Firebase Console → Firestore → Indexes
2. Wait for all 33 indexes to show green "Enabled" badge
3. Test app functionality once all indexes are ready

## 📱 App Features Enabled by Indexes

### ✅ Basic User Features
- ✅ Home feed (newest, popular, trending)
- ✅ Post detail (likes, comments)
- ✅ User profile (own posts, archive)
- ✅ Follow/unfollow users
- ✅ Notifications (all types)
- ✅ Block users

### ✅ Premium Features
- ✅ Advanced search (city + category + sort)
- ✅ Multiple filter combinations
- ✅ Extended search radius
- ✅ Category-specific feeds

### ✅ Admin Features
- ✅ Reports dashboard (filter by status/priority/type)
- ✅ Moderation queue (priority sorting)
- ✅ User management
- ✅ Account deletion management

## 🛠️ Maintenance

### Adding New Indexes
If you add new query combinations:
1. Firebase will provide index URL in error logs
2. Add index definition to `firestore.indexes.json`
3. Deploy: `firebase deploy --only firestore:indexes`
4. Wait 10-30 minutes for build

### Removing Unused Indexes
To clean up unused indexes:
```bash
firebase deploy --only firestore:indexes --force
```
⚠️ This will delete indexes not in `firestore.indexes.json`

## 📝 Related Files

- `firestore.indexes.json` - Index definitions (this file)
- `firestore.rules` - Security rules
- `data/src/main/kotlin/.../paging/*PagingSource.kt` - Query implementations
- `data/src/main/kotlin/.../repository/*RepositoryImpl.kt` - Data layer

## 🎯 Performance Impact

### Query Speed
- **Without index**: ❌ Error (`FAILED_PRECONDITION`)
- **With index**: ✅ <100ms for most queries
- **Pagination**: ✅ Efficient with `startAfter` cursors

### Storage Cost
- **Index overhead**: ~30-50% of document size
- **33 indexes**: Acceptable for production app
- **Optimization**: Sparse indexes used where applicable

## 🔒 Security

All indexes respect Firestore security rules:
- User-specific data requires authentication
- Admin indexes require custom claims verification
- Public data (posts, comments) indexed for all users

## ✨ Best Practices Followed

1. ✅ **Ascending/Descending optimization**: Most common sort order indexed first
2. ✅ **Equality filters first**: `status`, `city`, `category_en` before sorting
3. ✅ **Pagination support**: All indexes support `startAfter` cursors
4. ✅ **Client-side filtering**: Minimize unnecessary indexes
5. ✅ **Documentation**: Every index has clear purpose

## 📞 Support

If you encounter `FAILED_PRECONDITION` errors:
1. Check Firebase Console → Indexes tab
2. Verify all 33 indexes show "Enabled"
3. Wait 10-30 minutes if any show "Building"
4. Check error logs for specific index URL
5. Add missing index to `firestore.indexes.json` if needed

---

**Last Updated**: 2026-02-15  
**Status**: ✅ Production Ready  
**Total Indexes**: 33  
**Build Status**: All Deployed
