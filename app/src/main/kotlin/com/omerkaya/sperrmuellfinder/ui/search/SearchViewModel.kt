package com.omerkaya.sperrmuellfinder.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.Category
import com.omerkaya.sperrmuellfinder.domain.model.GermanCities
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.core.model.PostLocation
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.model.SearchSuggestion
import com.omerkaya.sperrmuellfinder.domain.model.SearchType
import com.omerkaya.sperrmuellfinder.domain.model.TimeRange
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.search.GetSearchSuggestionsUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.search.SearchPostsUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.search.SearchUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.TogglePostLikeUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.DeletePostUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetBlockedUsersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.domain.manager.LocationManager
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Search Screen.
 * Search is premium-gated: basic users can view the UI but interactions open paywall.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val premiumManager: PremiumManager,
    private val locationManager: LocationManager,
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val BASIC_RADIUS_METERS = 1500
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = Channel<SearchEvent>()
    val events = _events.receiveAsFlow()

    // Search query with debouncing
    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(FirestoreConstants.SAVED_STATE_SEARCH_QUERY) ?: ""
    )

    // Current search filters (Basic defaults to 1.5km until entitlement is loaded).
    private val _searchFilters = MutableStateFlow(
        SearchFilters(
            query = _searchQuery.value,
            radiusMeters = BASIC_RADIUS_METERS
        )
    )

    // User location for search
    private val _userLocation = MutableStateFlow<PostLocation?>(null)

    // Blocked user IDs for filtering search results
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())

    // Search results with pagination - simplified approach per rules.md
    private val _searchResults = MutableStateFlow<Flow<PagingData<Post>>>(emptyFlow())
    val searchResults: Flow<PagingData<Post>> = _searchResults
        .flatMapLatest { it }
        .cachedIn(viewModelScope)
    
    // User search results
    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults.asStateFlow()
    
    // Current search type (posts, users, etc.)
    private val _searchType = MutableStateFlow(SearchType.POSTS)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()

    private val _likeStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likeStatusMap: StateFlow<Map<String, Boolean>> = _likeStatusMap.asStateFlow()

    private val _likeLoadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likeLoadingMap: StateFlow<Map<String, Boolean>> = _likeLoadingMap.asStateFlow()

    private val _likeCountDeltaMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likeCountDeltaMap: StateFlow<Map<String, Int>> = _likeCountDeltaMap.asStateFlow()

    init {
        logger.d(Logger.TAG_DEFAULT, "SearchViewModel initialized")

        // Observe real device location from LocationManager, fall back to Berlin only when null
        viewModelScope.launch {
            locationManager.currentLocation.collect { location ->
                _userLocation.value = location ?: PostLocation.defaultLocation()
            }
        }

        // Observe current user and entitlement for search gating.
        viewModelScope.launch {
            combine(
                getCurrentUserUseCase(),
                premiumManager.isPremium
            ) { user, isPremium ->
                val updatedRadius = if (isPremium) {
                    _searchFilters.value.radiusMeters.coerceAtLeast(20000)
                } else {
                    BASIC_RADIUS_METERS
                }
                _searchFilters.value = _searchFilters.value.copy(
                    radiusMeters = updatedRadius
                )
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    currentUserId = user?.uid,
                    isPremium = isPremium,
                    isLoading = false,
                    showPaywall = false
                )
                
                logger.d(Logger.TAG_DEFAULT, "User status - Premium: $isPremium, User: ${user?.displayName}")

                updateSearchResults()
            }.collect { }
        }
        
        // Observe location changes for search results
        viewModelScope.launch {
            _userLocation.collect { location ->
                if (location != null) {
                    logger.d(Logger.TAG_DEFAULT, "Location updated, updating search results")
                    updateSearchResults()
                }
            }
        }
        
        // Track blocked user IDs for filtering user search results
        viewModelScope.launch {
            getBlockedUsersUseCase()
                .collect { blocked ->
                    _blockedUserIds.value = blocked.map { it.blockedUserId }.toSet()
                    if (_searchFilters.value.query.isNotBlank() && _searchType.value != SearchType.USERS) {
                        updateSearchResults()
                    }
                }
        }
        
        // Load search suggestions for empty query
        loadSearchSuggestions("")
    }

    /**
     * Sets user location for search
     */
    fun setUserLocation(location: PostLocation) {
        logger.d(Logger.TAG_DEFAULT, "Setting search location: ${location.latitude}, ${location.longitude}")
        _userLocation.value = location
    }

    /**
     * Updates search results based on current filters and user status
     */
    private fun updateSearchResults() {
        viewModelScope.launch {
            try {
                // Refresh location only if we don't already have a value.
                if (_userLocation.value == null) {
                    val currentLocationResult = locationManager.getCurrentLocation()
                    if (currentLocationResult is com.omerkaya.sperrmuellfinder.core.util.Result.Success) {
                        _userLocation.value = currentLocationResult.data
                    }
                }
                
                val filters = _searchFilters.value
                val userLocation = _userLocation.value
                // Update search filters with user location
                val updatedFilters = filters.copy(userLocation = userLocation)
                _searchFilters.value = updatedFilters
                
                // Update UI state with current filters
                _uiState.value = _uiState.value.copy(
                    searchFilters = updatedFilters
                )
                
                if (updatedFilters.query.isNotBlank()) {
                    logger.d(Logger.TAG_DEFAULT, "Updating search results with query: '${updatedFilters.query}'")
                    
                    // Execute search with updated filters
                    val searchFlow = searchPostsUseCase.invoke(
                        filters = updatedFilters,
                        userLocation = userLocation ?: PostLocation.defaultLocation(),
                        isPremium = _uiState.value.isPremium
                    )
                    
                    _searchResults.value = searchFlow
                    logger.d(Logger.TAG_DEFAULT, "Search results updated successfully")
                } else {
                    logger.d(Logger.TAG_DEFAULT, "Search results cleared - Query: '${updatedFilters.query}'")
                    _searchResults.value = emptyFlow()
                }
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error updating search results", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Search failed",
                    isSearching = false
                )
            }
        }
    }

    // Debounce job for search
    private var searchJob: kotlinx.coroutines.Job? = null
    private var usersSearchJob: Job? = null
    
    /**
     * Updates search query with debounce
     */
    fun updateSearchQuery(query: String) {
        logger.d(Logger.TAG_DEFAULT, "Updating search query: '$query'")
        
        // Update query immediately for UI responsiveness
        _searchQuery.value = query
        savedStateHandle[FirestoreConstants.SAVED_STATE_SEARCH_QUERY] = query
        
        // Update search filters
        _searchFilters.value = _searchFilters.value.copy(query = query)
        
        // Update UI state immediately
        _uiState.value = _uiState.value.copy(
            searchFilters = _searchFilters.value,
            showSuggestions = query.isEmpty(),
            showResults = query.isNotEmpty()
        )
        
        // Cancel previous search job
        searchJob?.cancel()
        
        if (query.isEmpty()) {
            // Clear results when query is empty
            clearSearchResults()
            loadSearchSuggestions(query)
        } else {
            // Load suggestions immediately
            loadSearchSuggestions(query)
            
            // Debounce search execution
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(500) // 500ms debounce
                performSearch()
            }
        }
    }

    /**
     * Clears search results
     */
    private fun clearSearchResults() {
        _searchResults.value = emptyFlow<PagingData<Post>>().cachedIn(viewModelScope)
        _userSearchResults.value = emptyList()
        usersSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showResults = false,
            isSearching = false
        )
    }
    
    /**
     * Performs search with current filters.
     * Basic users can search; only filters and sort are premium-gated.
     */
    fun performSearch() {
        val query = _searchFilters.value.query
        if (query.isBlank()) {
            logger.w(Logger.TAG_DEFAULT, "Search blocked - empty query")
            return
        }
        
        logger.i(Logger.TAG_DEFAULT, "Performing search: '$query'")
        
        _uiState.value = _uiState.value.copy(
            isSearching = true,
            showSuggestions = false,
            showResults = true,
            error = null
        )
        
        // Update search results based on current search type
        when (_searchType.value) {
            SearchType.POSTS -> {
                updateSearchResults()
            }
            SearchType.USERS -> {
                searchUsers(query)
            }
            SearchType.ALL -> {
                // "ALL" is disabled in UI. Fallback to posts only for safety.
                updateSearchResults()
            }
        }
        
        viewModelScope.launch {
            // Simulate search completion for UI state
            kotlinx.coroutines.delay(500)
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }
    

    /**
     * Applies category filter.
     */
    fun applyCategoryFilter(categories: List<String>) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying category filter: $categories")
        
        _searchFilters.value = _searchFilters.value.copy(categories = categories)
        _uiState.value = _uiState.value.copy(showFilters = false)
        
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Applies city filter.
     */
    fun applyCityFilter(city: String?) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying city filter: $city")
        
        _searchFilters.value = _searchFilters.value.copy(city = city)
        _uiState.value = _uiState.value.copy(showFilters = false)
        
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Applies radius filter.
     */
    fun applyRadiusFilter(radiusMeters: Int) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying radius filter: ${radiusMeters}m")
        
        _searchFilters.value = _searchFilters.value.copy(radiusMeters = radiusMeters)
        _uiState.value = _uiState.value.copy(showFilters = false)
        
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Applies time range filter.
     */
    fun applyTimeRangeFilter(timeRange: TimeRange) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying time range filter: $timeRange")
        
        _searchFilters.value = _searchFilters.value.copy(timeRange = timeRange)
        _uiState.value = _uiState.value.copy(showFilters = false)
        
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Applies sort filter.
     */
    fun applySortFilter(sortBy: com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying sort filter: $sortBy")
        
        _searchFilters.value = _searchFilters.value.copy(sortBy = sortBy)
        _uiState.value = _uiState.value.copy(showSort = false)
        
        refreshActiveSearchForCurrentTab()
    }


    /**
     * Shows filter bottom sheet.
     */
    fun showFilters() {
        if (!_uiState.value.isPremium) {
            logger.d(Logger.TAG_DEFAULT, "Premium gate hit: filter sheet")
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Showing filters")
        _uiState.value = _uiState.value.copy(showFilters = true)
    }

    /**
     * Shows sort bottom sheet.
     */
    fun showSort() {
        if (!_uiState.value.isPremium) {
            logger.d(Logger.TAG_DEFAULT, "Premium gate hit: sort sheet")
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Showing sort options")
        _uiState.value = _uiState.value.copy(showSort = true)
    }

    /**
     * Hides filter bottom sheet
     */
    fun hideFilters() {
        _uiState.value = _uiState.value.copy(showFilters = false)
    }

    /**
     * Hides sort bottom sheet
     */
    fun hideSort() {
        _uiState.value = _uiState.value.copy(showSort = false)
    }
    
    
    
    /**
     * Applies complete filter set.
     */
    fun applyFilters(filters: SearchFilters) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Applying filters: $filters")
        
        _searchFilters.value = filters
        _uiState.value = _uiState.value.copy(
            searchFilters = filters,
            showFilters = false
        )
        
        refreshActiveSearchForCurrentTab()
    }
    
    /**
     * Updates sort by option.
     */
    fun updateSortBy(sortBy: PostSortBy) {
        if (!_uiState.value.isPremium) {
            showPaywall()
            return
        }
        logger.d(Logger.TAG_DEFAULT, "Updating sort by: $sortBy")
        
        _searchFilters.value = _searchFilters.value.copy(sortBy = sortBy)
        _uiState.value = _uiState.value.copy(
            searchFilters = _searchFilters.value,
            showSort = false
        )
        
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Clears all filters
     */
    fun clearFilters() {
        logger.d(Logger.TAG_DEFAULT, "Clearing all filters")
        val clearedFilters = SearchFilters(query = _searchFilters.value.query)
        _searchFilters.value = clearedFilters
        
        // Update UI state
        _uiState.value = _uiState.value.copy(
            searchFilters = clearedFilters
        )
        
        // Trigger search with cleared filters
        refreshActiveSearchForCurrentTab()
    }

    /**
     * Shows paywall for premium features.
     */
    fun showPaywall() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showPaywall = true,
                showFilters = false,
                showSort = false
            )
            _events.send(SearchEvent.ShowPaywall)
        }
    }

    /**
     * Hides paywall.
     */
    fun hidePaywall() {
        _uiState.value = _uiState.value.copy(showPaywall = false)
    }

    /**
     * Handles suggestion selection.
     * Available to all users; filters are premium-gated separately.
     */
    fun onSuggestionSelected(suggestion: SearchSuggestion) {
        logger.d(Logger.TAG_DEFAULT, "Suggestion selected: ${suggestion.text}")
        
        updateSearchQuery(suggestion.text)
        performSearch()
    }

    /**
     * Handles post click
     */
    fun onPostClick(postId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Post clicked: $postId")
            _events.send(SearchEvent.NavigateToPostDetail(postId))
        }
    }

    /**
     * Location click behavior for search results:
     * Basic user can open map only inside 1.5km, otherwise paywall.
     */
    fun onPostLocationClick(post: Post) {
        val postLocation = post.location ?: return
        viewModelScope.launch {
            val isPremium = premiumManager.isPremium.value
            if (isPremium) {
                _events.send(SearchEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
                return@launch
            }

            val resolvedUserLocation = _userLocation.value
                ?: locationManager.currentLocation.value
                ?: when (val locationResult = locationManager.getCurrentLocation()) {
                    is Result.Success -> locationResult.data
                    else -> null
                }

            val distanceMeters = when {
                resolvedUserLocation != null ->
                    locationManager.calculateDistance(resolvedUserLocation, postLocation)
                post.distanceFromUser != null -> post.distanceFromUser
                else -> null
            }

            if (distanceMeters != null && distanceMeters > BASIC_RADIUS_METERS) {
                _events.send(SearchEvent.ShowPaywall)
            } else {
                _events.send(SearchEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
            }
        }
    }

    /**
     * Handles user click
     */
    fun onUserClick(userId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "User clicked: $userId")
            _events.send(SearchEvent.NavigateToUserProfile(userId))
        }
    }
    
    /**
     * Changes search type (posts, users, etc.)
     */
    fun setSearchType(type: SearchType) {
        logger.d(Logger.TAG_DEFAULT, "Setting search type: $type")
        _searchType.value = type
        
        // Clear previous results when switching types
        when (type) {
            SearchType.POSTS -> {
                usersSearchJob?.cancel()
                _userSearchResults.value = emptyList()
                if (_searchFilters.value.query.isNotBlank()) {
                    updateSearchResults()
                }
            }
            SearchType.USERS -> {
                _searchResults.value = emptyFlow()
                searchUsers(_searchFilters.value.query)
            }
            SearchType.ALL -> {
                // ALL tab removed from UI; keep safe fallback.
                usersSearchJob?.cancel()
                if (_searchFilters.value.query.isNotBlank()) {
                    updateSearchResults()
                }
            }
        }
    }
    
    /**
     * Searches for users with current query
     */
    fun searchUsers(query: String = _searchFilters.value.query) {
        logger.d(Logger.TAG_DEFAULT, "Searching users with query: '$query'")

        usersSearchJob?.cancel()
        usersSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            searchUsersUseCase.observe(query).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _userSearchResults.value = applyUserFilters(result.data)
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_DEFAULT, "User search failed", result.exception)
                        _userSearchResults.value = emptyList()
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = result.exception.message
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isSearching = true)
                    }
                }
            }
        }
    }
    
    /**
     * Clears user search results
     */
    fun clearUserSearchResults() {
        _userSearchResults.value = emptyList()
    }

    fun onPostLike(postId: String, isCurrentlyLiked: Boolean, currentLikesCount: Int) {
        val currentUserId = _uiState.value.currentUserId
        if (currentUserId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "Authentication required")
            return
        }
        if (_likeLoadingMap.value[postId] == true) return

        viewModelScope.launch {
            _likeLoadingMap.value = _likeLoadingMap.value + (postId to true)

            // Optimistic update
            val targetLiked = !isCurrentlyLiked
            _likeStatusMap.value = _likeStatusMap.value + (postId to targetLiked)
            val optimisticCount = if (targetLiked) currentLikesCount + 1 else maxOf(0, currentLikesCount - 1)
            _likeCountDeltaMap.value = _likeCountDeltaMap.value + (postId to (optimisticCount - currentLikesCount))

            when (val result = togglePostLikeUseCase(postId, currentUserId)) {
                is Result.Success -> {
                    val actualLiked = result.data
                    _likeStatusMap.value = _likeStatusMap.value + (postId to actualLiked)
                    val finalCount = if (actualLiked) currentLikesCount + 1 else maxOf(0, currentLikesCount - 1)
                    _likeCountDeltaMap.value = _likeCountDeltaMap.value + (postId to (finalCount - currentLikesCount))
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is Result.Error -> {
                    // Revert optimistic state on failure
                    _likeStatusMap.value = _likeStatusMap.value + (postId to isCurrentlyLiked)
                    _likeCountDeltaMap.value = _likeCountDeltaMap.value + (postId to 0)
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message ?: "Failed to update like"
                    )
                }
                is Result.Loading -> Unit
            }

            _likeLoadingMap.value = _likeLoadingMap.value + (postId to false)
        }
    }

    /**
     * Loads search suggestions
     */
    private fun loadSearchSuggestions(query: String) {
        viewModelScope.launch {
            when (val result = getSearchSuggestionsUseCase(query)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        suggestions = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to load suggestions", result.exception)
                    _uiState.value = _uiState.value.copy(
                        suggestions = emptyList(),
                        error = result.exception.message
                    )
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Clears error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun refreshActiveSearchForCurrentTab() {
        if (_searchFilters.value.query.isBlank()) return
        when (_searchType.value) {
            SearchType.USERS -> searchUsers(_searchFilters.value.query)
            else -> updateSearchResults()
        }
    }

    private fun applyUserFilters(users: List<User>): List<User> {
        val filters = _searchFilters.value
        val currentUserId = _uiState.value.currentUserId
        val blockedIds = _blockedUserIds.value

        var filtered = users

        // Remove current user and blocked users from results
        filtered = filtered.filter { user ->
            user.uid != currentUserId && user.uid !in blockedIds
        }

        val cityFilter = filters.city?.trim()?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
        if (cityFilter != null) {
            filtered = filtered.filter { user ->
                val userCity = user.city?.trim()?.lowercase(Locale.ROOT) ?: return@filter false
                userCity.contains(cityFilter) || cityFilter.contains(userCity)
            }
        }

        return filtered
    }

    /**
     * Deletes a post owned by the current user.
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = deletePostUseCase(postId)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Post deleted from search: $postId")
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to delete post: $postId", result.exception)
                    _uiState.value = _uiState.value.copy(error = result.exception?.message)
                }
                else -> Unit
            }
        }
    }

}

/**
 * UI State for Search Screen
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val currentUserId: String? = null,
    val isPremium: Boolean = false,
    val showPaywall: Boolean = false,
    val showSuggestions: Boolean = false,
    val showResults: Boolean = false,
    val showFilters: Boolean = false,
    val showSort: Boolean = false,
    val suggestions: List<SearchSuggestion> = emptyList(),
    val searchFilters: SearchFilters = SearchFilters(),
    val availableCategories: List<Category> = Category.POPULAR_CATEGORIES,
    val availableCities: List<String> = GermanCities.MAJOR_CITIES
)

/**
 * Events for Search Screen
 */
sealed class SearchEvent {
    data object ShowPaywall : SearchEvent()
    data class NavigateToPostDetail(val postId: String) : SearchEvent()
    data class NavigateToUserProfile(val userId: String) : SearchEvent()
    data class NavigateToMapLocation(val latitude: Double, val longitude: Double) : SearchEvent()
}
