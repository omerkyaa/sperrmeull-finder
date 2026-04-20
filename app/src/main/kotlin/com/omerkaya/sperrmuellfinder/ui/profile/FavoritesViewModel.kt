package com.omerkaya.sperrmuellfinder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📌 FAVORITES VIEW MODEL - SperrmüllFinder
 * Rules.md compliant - MVVM pattern with StateFlow
 * 
 * Features:
 * - Load user favorites with real-time updates
 * - Professional error handling
 * - Clean Architecture compliance
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserId = getCurrentUserId()
                if (currentUserId.isNotBlank()) {
                    combine(
                        firestoreRepository.getUserFavorites(currentUserId),
                        userRepository.getCurrentUser()
                    ) { favorites, user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            favorites = favorites,
                            isPremium = user?.isPremium ?: false,
                            error = null
                        )
                    }.collect { }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun getCurrentUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}

/**
 * UI State for Favorites Screen
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<Post> = emptyList(),
    val error: String? = null,
    val isPremium: Boolean = false
)