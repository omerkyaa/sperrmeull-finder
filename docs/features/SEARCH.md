# 🔍 Search Module - SperrmüllFinder

## Overview

The Search module provides comprehensive search functionality for posts and users with NO PREMIUM RESTRICTIONS. All users (Basic and Premium) have full access to all search features.

**Key Changes:**
- ✅ All Premium restrictions removed
- ✅ Full filter access for all users
- ✅ Comprehensive logging for debugging
- ✅ Optimized Firestore queries to minimize index requirements

---

## 🎯 Features

### Available to ALL Users

1. **Text Search**
   - Search in post descriptions
   - Search in categories (German and English)
   - Search in city names
   - Multi-word search support

2. **Category Filtering**
   - 10 popular categories available
   - Multi-select support
   - German UI with English backend queries

3. **City Filtering**
   - Major German cities
   - Case-insensitive matching
   - Single city selection

4. **Radius Control**
   - Range: 500m to 50km
   - Slider with 19 steps
   - Default: 50km for all users

5. **Time Range Filtering**
   - Last 24 hours
   - Last 48 hours
   - Last 72 hours
   - All time (default)

6. **Sorting Options**
   - Newest first (default)
   - Oldest first
   - Most liked
   - Most commented
   - Most viewed
   - Expiring soon
   - Nearest (client-side distance calculation)

7. **User Search**
   - Search by username
   - Search by display name
   - Prefix matching support

---

## 🗄️ Firestore Indexes Required

### Minimal Index Strategy

The search module uses a **minimal server-side filtering** approach to reduce index requirements. Most filtering is done client-side.

### Required Indexes

#### 1. Basic Post Search (Status + Created At)
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "created_at", "order": "DESCENDING"}
  ]
}
```

#### 2. Post Search with Time Filter
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "created_at", "order": "ASCENDING"}
  ]
}
```

#### 3. Sort by Likes
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "likes_count", "order": "DESCENDING"}
  ]
}
```

#### 4. Sort by Comments
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "comments_count", "order": "DESCENDING"}
  ]
}
```

#### 5. Sort by Views
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "views_count", "order": "DESCENDING"}
  ]
}
```

#### 6. Sort by Expiring Soon
```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "status", "order": "ASCENDING"},
    {"fieldPath": "expires_at", "order": "ASCENDING"}
  ]
}
```

#### 7. User Search (Username)
```json
{
  "collectionGroup": "users",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "usernameLower", "order": "ASCENDING"}
  ]
}
```

#### 8. User Search (Display Name)
```json
{
  "collectionGroup": "users",
  "queryScope": "COLLECTION",
  "fields": [
    {"fieldPath": "displayNameLower", "order": "ASCENDING"}
  ]
}
```

### Creating Indexes

**Method 1: Firebase Console**
1. Go to https://console.firebase.google.com/
2. Select your project
3. Navigate to Firestore Database → Indexes
4. Click "Create Index"
5. Add the fields as specified above

**Method 2: Automatic Creation**
When you run a query that requires an index, Firestore will show an error with a link to automatically create it. Click the link and wait for the index to build.

**Method 3: Firebase CLI**
```bash
firebase deploy --only firestore:indexes
```

---

## 📊 Query Strategy

### Server-Side Filtering (Minimal)
- `status == "active"` (always applied)
- `created_at` ordering (for most sorts)
- Time range filter (when selected)

### Client-Side Filtering (Comprehensive)
- Text search in description, categories, city
- Category matching (German/English)
- City matching (case-insensitive)
- Radius filtering (Haversine distance calculation)
- Distance-based sorting

**Why this approach?**
- Reduces Firestore index requirements
- Allows flexible filtering without complex composite indexes
- Maintains good performance with pagination
- Easier to maintain and extend

---

## 🐛 Debugging

### Comprehensive Logging

The search module includes extensive logging for debugging:

```kotlin
// SearchPostsPagingSource logs:
logger.d(TAG, "=== SEARCH QUERY DEBUG ===")
logger.d(TAG, "Query: '$query'")
logger.d(TAG, "City: $city")
logger.d(TAG, "Categories: $categories")
logger.d(TAG, "Radius: ${radiusMeters}m (NO RESTRICTIONS)")
logger.d(TAG, "Time Range: $timeRange")
logger.d(TAG, "Sort By: $sortBy")
logger.d(TAG, "User Location: ($lat, $lng)")
logger.d(TAG, "Firestore returned X documents")
logger.d(TAG, "Text search filtered: X → Y posts")
logger.d(TAG, "City filter applied: X → Y posts")
logger.d(TAG, "Category filter applied: X → Y posts")
logger.d(TAG, "Radius filter applied: X → Y posts")
logger.d(TAG, "=== SEARCH RESULT ===")
logger.d(TAG, "Final result count: X")
logger.d(TAG, "Has next page: true/false")
```

### Common Issues

#### 1. No Posts Showing

**Check logs for:**
```
Firestore returned 0 documents
```

**Possible causes:**
- No posts with `status = "active"` in database
- Time range filter too restrictive
- Missing Firestore index

**Solution:**
1. Check Firestore Console for active posts
2. Try "All time" filter
3. Check logcat for index error messages

#### 2. Index Missing Error

**Log will show:**
```
⚠️ FIRESTORE INDEX MISSING ⚠️
Index creation URL: https://console.firebase.google.com/...
```

**Solution:**
1. Click the URL in the logs
2. Wait for index to build (can take several minutes)
3. Retry the search

#### 3. Location Not Set

**Log will show:**
```
User Location: not set
```

**Solution:**
- Ensure location permissions are granted
- Check LocationManager is providing location
- Default location (Berlin) is used as fallback

---

## 🧪 Testing

### Test Scenarios

#### 1. Basic Search
```
Query: "möbel"
Expected: Posts with "Möbel" category or "möbel" in description
```

#### 2. City Filter
```
Query: ""
City: "Berlin"
Expected: Posts from Berlin
```

#### 3. Category Filter
```
Query: ""
Categories: ["furniture", "electronics"]
Expected: Posts with these categories
```

#### 4. Radius Filter
```
Query: ""
Radius: 5000m (5km)
Expected: Posts within 5km of user location
```

#### 5. Time Range Filter
```
Query: ""
Time Range: Last 24 hours
Expected: Posts created in last 24 hours
```

#### 6. Sort Options
```
Sort: Most Liked
Expected: Posts ordered by likes_count DESC
```

#### 7. User Search
```
Query: "john"
Expected: Users with username or display name starting with "john"
```

### Manual Testing Checklist

- [ ] Search with empty query shows all active posts
- [ ] Text search filters results correctly
- [ ] Category chips work (can select/deselect)
- [ ] City chips work (can select/deselect)
- [ ] Radius slider adjusts from 500m to 50km
- [ ] Time range radio buttons work
- [ ] All sort options work correctly
- [ ] User search returns matching users
- [ ] Pagination loads more results
- [ ] No Premium restrictions (all features available)
- [ ] German/English localization works
- [ ] Loading states show correctly
- [ ] Empty states show when no results
- [ ] Error states show on failures

---

## 📱 UI Components

### SearchScreen
- Main search interface
- Tabs for Posts/Users/All
- Search bar with filter and sort buttons

### SearchBar
- Text input with debouncing (500ms)
- Clear button
- Filter button (with active filter count badge)
- Sort button

### SearchTabs
- Posts tab (always enabled)
- Users tab (always enabled)
- All tab (always enabled - NO PREMIUM RESTRICTION)

### FilterBottomSheet
- Category selection (multi-select)
- City selection (single-select)
- Radius slider (500m - 50km)
- Time range selection
- Apply/Clear buttons

### SortBottomSheet
- 7 sort options
- Radio button selection
- Visual indicator for active sort

---

## 🌐 Localization

All strings are localized in German (default) and English (fallback).

### Key String Resources

```xml
<!-- Search -->
<string name="search_title">Suchen / Search</string>
<string name="search_hint">Suche nach Gegenständen... / Search for items...</string>
<string name="search_radius_all_users">Radius: %1$d km</string>

<!-- Filters -->
<string name="search_filter_categories">Kategorien / Categories</string>
<string name="search_filter_city">Stadt / City</string>
<string name="search_filter_radius">Suchradius / Search Radius</string>
<string name="search_filter_time_range">Zeitraum / Time Range</string>

<!-- Time Range -->
<string name="time_range_24h">Letzte 24 Stunden / Last 24 hours</string>
<string name="time_range_48h">Letzte 48 Stunden / Last 48 hours</string>
<string name="time_range_72h">Letzte 72 Stunden / Last 72 hours</string>
<string name="time_range_all">Alle Zeit / All time</string>

<!-- Sort -->
<string name="sort_newest">Neueste zuerst / Newest first</string>
<string name="sort_oldest">Älteste zuerst / Oldest first</string>
<string name="sort_most_liked">Meiste Likes / Most liked</string>
<string name="sort_most_commented">Meiste Kommentare / Most commented</string>
<string name="sort_most_viewed">Meiste Aufrufe / Most viewed</string>
<string name="sort_expiring_soon">Läuft bald ab / Expiring soon</string>
<string name="sort_nearest">Nächste zuerst / Nearest first</string>
```

---

## 🔧 Architecture

### Data Flow

```
User Input → SearchViewModel → SearchUseCase → SearchRepository → SearchPostsPagingSource → Firestore
                                                                                         ↓
                                                                                    Client-side filtering
                                                                                         ↓
                                                                                    Paging 3 UI
```

### Key Classes

1. **SearchViewModel**
   - Manages search state
   - Handles filter updates
   - Coordinates search execution
   - NO PREMIUM GATING

2. **SearchPostsPagingSource**
   - Executes Firestore queries
   - Applies client-side filters
   - Calculates distances
   - Comprehensive logging

3. **SearchRepositoryImpl**
   - Creates PagingSource instances
   - Provides user search
   - Handles search suggestions

4. **SearchBar / FilterBottomSheet / SortBottomSheet**
   - UI components
   - NO PREMIUM RESTRICTIONS
   - All features enabled for everyone

---

## 📈 Performance

### Optimization Strategies

1. **Pagination**: 20 items per page
2. **Debouncing**: 500ms delay on text input
3. **Client-side filtering**: Reduces index requirements
4. **Distance calculation**: Haversine formula (efficient)
5. **Image loading**: Landscapist-Glide with caching

### Expected Performance

- **First page load**: <500ms
- **Subsequent pages**: <200ms
- **Text search**: <100ms (client-side)
- **Filter application**: <100ms (client-side)

---

## 🚀 Deployment Checklist

Before deploying search functionality:

- [ ] All Firestore indexes created and built
- [ ] Test with empty database (shows empty state)
- [ ] Test with 100+ posts (pagination works)
- [ ] Test all filter combinations
- [ ] Test all sort options
- [ ] Test user search
- [ ] Verify German/English strings
- [ ] Check logs for errors
- [ ] Verify NO Premium restrictions
- [ ] Test on different screen sizes
- [ ] Test with poor network conditions

---

## 📞 Support

If you encounter issues:

1. Check logcat for detailed error messages
2. Look for "SearchPostsPagingSource" or "SearchViewModel" tags
3. Check for index creation URLs in logs
4. Verify Firestore Security Rules allow reading posts
5. Ensure location permissions are granted

---

## 🔄 Changelog

### Version 1.0 (Current)
- ✅ Removed all Premium restrictions
- ✅ Added comprehensive logging
- ✅ Optimized Firestore queries
- ✅ Implemented client-side filtering
- ✅ Added German/English localization
- ✅ Fixed "no posts showing" issue
- ✅ All features available to all users

---

**Last Updated**: 2025-11-10
**Module Status**: ✅ Production Ready
**Premium Restrictions**: ❌ None - All features free for everyone

