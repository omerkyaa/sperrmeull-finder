package com.omerkaya.sperrmuellfinder.ui.premium

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.premium.PostExtensionProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumStatus
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumType
import com.omerkaya.sperrmuellfinder.domain.model.premium.PurchaseResult
import com.omerkaya.sperrmuellfinder.domain.model.premium.RestoreResult
import com.omerkaya.sperrmuellfinder.domain.usecase.premium.PremiumGatingUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.premium.PurchaseUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.premium.RestorePurchasesUseCase
import com.omerkaya.sperrmuellfinder.domain.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Premium screens including Paywall, Purchase flow, and Premium status.
 * 
 * According to rules.md:
 * - All premium logic goes through PremiumManager
 * - Purchase flows use RevenueCat
 * - Analytics events are logged
 * - UI shows localized messages
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumManager: PremiumManager,
    private val premiumRepository: PremiumRepository,
    private val purchaseUseCase: PurchaseUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val premiumGatingUseCase: PremiumGatingUseCase,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = Channel<PremiumEvent>()
    val events = _events.receiveAsFlow()
    private var firebasePremiumRegistration: ListenerRegistration? = null

    init {
        observePremiumStatus()
        observeFirebasePremiumShadow()
        loadProducts()
    }

    /**
     * Observe premium status changes in real-time
     */
    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.premiumEntitlement.collect { entitlement ->
                val hasRevenueCatSignal = _uiState.value.hasRevenueCatSignal || hasRevenueCatSignal(entitlement)
                val firebaseFallbackPremium = _uiState.value.firebaseIsPremium
                val effectiveIsPremium = if (hasRevenueCatSignal) {
                    entitlement.isAccessible()
                } else {
                    entitlement.isAccessible() || firebaseFallbackPremium
                }
                val effectivePlanType = if (hasRevenueCatSignal) {
                    entitlement.type
                } else {
                    entitlement.type ?: _uiState.value.firebasePremiumType
                }
                _uiState.value = _uiState.value.copy(
                    premiumEntitlement = entitlement,
                    hasRevenueCatSignal = hasRevenueCatSignal,
                    isPremium = effectiveIsPremium,
                    isInTrial = entitlement.isInTrial,
                    isInGracePeriod = entitlement.isInGracePeriod,
                    displayPlanEndDate = calculateDisplayPlanEndDate(
                        planType = effectivePlanType,
                        latestPurchaseDate = entitlement.latestPurchaseDate,
                        originalPurchaseDate = entitlement.originalPurchaseDate,
                        expirationDate = entitlement.expirationDate ?: _uiState.value.firebasePremiumUntil
                    ),
                    autoRenewEnabled = if (effectiveIsPremium) entitlement.willRenew || !hasRevenueCatSignal else false
                )
                
                logger.d(Logger.TAG_PREMIUM, "Premium status updated: isPremium=${entitlement.isAccessible()}")
            }
        }
    }

    private fun observeFirebasePremiumShadow() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firebasePremiumRegistration?.remove()
        firebasePremiumRegistration = firestore
            .collection("users_private")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(Logger.TAG_PREMIUM, "Firebase premium shadow listener failed", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val firebaseIsPremium = snapshot.getBoolean("ispremium")
                    ?: snapshot.getBoolean("isPremium")
                    ?: false
                val premiumTypeRaw = snapshot.getString("premiumType")
                val premiumUntil = snapshot.getDate("premiumuntil")

                val entitlement = _uiState.value.premiumEntitlement
                val hasRevenueCatSignal = _uiState.value.hasRevenueCatSignal || (entitlement?.let { hasRevenueCatSignal(it) } == true)
                val entitlementPremium = entitlement?.isAccessible() == true
                val effectiveIsPremium = if (hasRevenueCatSignal) entitlementPremium else entitlementPremium || firebaseIsPremium
                val effectivePlanType = if (hasRevenueCatSignal) entitlement?.type else entitlement?.type ?: mapPremiumType(premiumTypeRaw)
                _uiState.value = _uiState.value.copy(
                    firebaseIsPremium = firebaseIsPremium,
                    firebasePremiumType = mapPremiumType(premiumTypeRaw),
                    firebasePremiumUntil = premiumUntil,
                    isPremium = effectiveIsPremium,
                    displayPlanEndDate = calculateDisplayPlanEndDate(
                        planType = effectivePlanType,
                        latestPurchaseDate = entitlement?.latestPurchaseDate,
                        originalPurchaseDate = entitlement?.originalPurchaseDate,
                        expirationDate = entitlement?.expirationDate ?: premiumUntil
                    ),
                    autoRenewEnabled = if (effectiveIsPremium) {
                        if (hasRevenueCatSignal) entitlement?.willRenew == true else true
                    } else {
                        false
                    }
                )
            }
    }

    private fun mapPremiumType(raw: String?): PremiumType? {
        if (raw.isNullOrBlank()) return null
        return runCatching { PremiumType.valueOf(raw) }.getOrNull()
    }

    /**
     * Load all available products from RevenueCat
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load premium products
                val premiumResult = premiumRepository.getPremiumProducts()
                val extensionResult = premiumRepository.getPostExtensionProducts()
                
                val premiumProducts = if (premiumResult is Result.Success) premiumResult.data else emptyList()
                val extensionProducts = if (extensionResult is Result.Success) extensionResult.data else emptyList()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    premiumProducts = premiumProducts,
                    postExtensionProducts = extensionProducts,
                    hasRevenueCatSignal = true,
                    error = null
                )
                
                logger.i(Logger.TAG_PREMIUM, "Products loaded: ${premiumProducts.size} premium, ${extensionProducts.size} extensions")
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_PREMIUM, "Error loading products", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load products"
                )
            }
        }
    }

    /**
     * Purchase a premium product
     */
    fun purchasePremium(activity: Activity, product: PremiumProduct) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, purchaseError = null)
            
            logger.d(Logger.TAG_PREMIUM, "Initiating premium purchase: ${product.id}")
            
            val result = purchaseUseCase(activity, product)
            
            when (result) {
                is PurchaseResult.Success -> {
                    logger.i(Logger.TAG_PREMIUM, "Premium purchase successful: ${product.id}")
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        showSuccessAnimation = true
                    )
                    _events.send(PremiumEvent.PurchaseSuccess(product.title))
                }
                
                is PurchaseResult.UserCancelled -> {
                    logger.w(Logger.TAG_PREMIUM, "Premium purchase cancelled: ${product.id}")
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                    _events.send(PremiumEvent.PurchaseCancelled)
                }
                
                is PurchaseResult.Error -> {
                    logger.e(Logger.TAG_PREMIUM, "Premium purchase failed: ${product.id}, Error: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        purchaseError = result.message
                    )
                    _events.send(PremiumEvent.PurchaseError(result.message))
                }
                
                is PurchaseResult.NetworkError -> {
                    logger.e(Logger.TAG_PREMIUM, "Network error during premium purchase: ${product.id}")
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                    _events.send(PremiumEvent.NetworkError)
                }
                
                is PurchaseResult.PaymentPending -> {
                    logger.i(Logger.TAG_PREMIUM, "Premium purchase pending: ${product.id}")
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                    _events.send(PremiumEvent.PaymentPending)
                }
                
                is PurchaseResult.ProductAlreadyOwned,
                is PurchaseResult.AlreadyOwned -> {
                    logger.w(Logger.TAG_PREMIUM, "Premium product already owned: ${product.id}")
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                    _events.send(PremiumEvent.AlreadyOwned)
                }
                
                is PurchaseResult.ProductNotAvailable -> {
                    logger.e(Logger.TAG_PREMIUM, "Premium product not available: ${product.id}")
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                    _events.send(PremiumEvent.ProductNotAvailable)
                }
            }
        }
    }

    /**
     * Purchase an XP product
     */
    /**
     * Purchase a post extension product
     */
    fun purchasePostExtension(activity: Activity, product: PostExtensionProduct) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, purchaseError = null)
            
            logger.d(Logger.TAG_PREMIUM, "Initiating post extension purchase: ${product.id}")
            
            val result = purchaseUseCase(activity, product)
            
            when (result) {
                is PurchaseResult.Success -> {
                    logger.i(Logger.TAG_PREMIUM, "Post extension purchase successful: ${product.id}")
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        showSuccessAnimation = true
                    )
                    _events.send(PremiumEvent.PostExtensionSuccess(product.extensionHours))
                }
                
                else -> handlePurchaseResult(result, "Post Extension")
            }
        }
    }

    /**
     * Restore previous purchases
     */
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, restoreError = null)
            
            logger.d(Logger.TAG_PREMIUM, "Initiating restore purchases")
            
            val result = restorePurchasesUseCase()
            
            when (result) {
                is RestoreResult.Success -> {
                    logger.i(Logger.TAG_PREMIUM, "Restore purchases successful: ${result.restoredCount} items")
                    _uiState.value = _uiState.value.copy(isRestoring = false)
                    _events.send(PremiumEvent.RestoreSuccess(result.restoredCount))
                }
                
                is RestoreResult.NoRestorablePurchases -> {
                    logger.i(Logger.TAG_PREMIUM, "No restorable purchases found")
                    _uiState.value = _uiState.value.copy(isRestoring = false)
                    _events.send(PremiumEvent.NoRestorablePurchases)
                }
                
                is RestoreResult.Error -> {
                    logger.e(Logger.TAG_PREMIUM, "Restore purchases failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isRestoring = false,
                        restoreError = result.message
                    )
                    _events.send(PremiumEvent.RestoreError(result.message))
                }
                
                is RestoreResult.NetworkError -> {
                    logger.e(Logger.TAG_PREMIUM, "Network error during restore purchases")
                    _uiState.value = _uiState.value.copy(isRestoring = false)
                    _events.send(PremiumEvent.NetworkError)
                }
            }
        }
    }

    /**
     * Dismiss success animation
     */
    fun dismissSuccessAnimation() {
        _uiState.value = _uiState.value.copy(showSuccessAnimation = false)
    }

    /**
     * Clear purchase error
     */
    fun clearPurchaseError() {
        _uiState.value = _uiState.value.copy(purchaseError = null)
    }

    /**
     * Clear restore error
     */
    fun clearRestoreError() {
        _uiState.value = _uiState.value.copy(restoreError = null)
    }

    /**
     * Get premium features summary
     */
    fun getPremiumFeaturesSummary(): List<PremiumFeatureItem> {
        return listOf(
            PremiumFeatureItem(
                titleResId = R.string.premium_unlimited_radius,
                descriptionResId = R.string.premium_unlimited_radius_desc,
                isAvailable = premiumManager.hasUnlimitedRadius(),
                icon = "🗺️"
            ),
            PremiumFeatureItem(
                titleResId = R.string.feature_early_access,
                descriptionResId = R.string.feature_early_access_desc,
                isAvailable = premiumManager.hasEarlyAccess(),
                icon = "⚡"
            ),
            PremiumFeatureItem(
                titleResId = R.string.premium_feature_premium_markers,
                descriptionResId = R.string.premium_feature_premium_markers_desc,
                isAvailable = _uiState.value.isPremium,
                icon = "📍"
            ),
            PremiumFeatureItem(
                titleResId = R.string.feature_archive_access,
                descriptionResId = R.string.feature_archive_access_desc,
                isAvailable = _uiState.value.isPremium,
                icon = "📁"
            ),
            PremiumFeatureItem(
                titleResId = R.string.premium_feature_unlimited_search,
                descriptionResId = R.string.premium_feature_unlimited_search_desc,
                isAvailable = _uiState.value.isPremium,
                icon = "🔎"
            ),
            PremiumFeatureItem(
                titleResId = R.string.premium_feature_advanced_filters,
                descriptionResId = R.string.premium_feature_advanced_filters_desc,
                isAvailable = _uiState.value.isPremium,
                icon = "🎛️"
            )
        )
    }

    /**
     * Handle common purchase result patterns
     */
    private suspend fun handlePurchaseResult(result: PurchaseResult, productType: String) {
        when (result) {
            is PurchaseResult.UserCancelled -> {
                _uiState.value = _uiState.value.copy(isPurchasing = false)
                _events.send(PremiumEvent.PurchaseCancelled)
            }
            
            is PurchaseResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isPurchasing = false,
                    purchaseError = result.message
                )
                _events.send(PremiumEvent.PurchaseError(result.message))
            }
            
            is PurchaseResult.NetworkError -> {
                _uiState.value = _uiState.value.copy(isPurchasing = false)
                _events.send(PremiumEvent.NetworkError)
            }
            
            is PurchaseResult.PaymentPending -> {
                _uiState.value = _uiState.value.copy(isPurchasing = false)
                _events.send(PremiumEvent.PaymentPending)
            }
            
            is PurchaseResult.ProductAlreadyOwned,
            is PurchaseResult.AlreadyOwned -> {
                _uiState.value = _uiState.value.copy(isPurchasing = false)
                _events.send(PremiumEvent.AlreadyOwned)
            }
            
            is PurchaseResult.ProductNotAvailable -> {
                _uiState.value = _uiState.value.copy(isPurchasing = false)
                _events.send(PremiumEvent.ProductNotAvailable)
            }
            
            else -> {
                logger.w(Logger.TAG_PREMIUM, "Unhandled purchase result: $result")
            }
        }
    }

    override fun onCleared() {
        firebasePremiumRegistration?.remove()
        super.onCleared()
    }

    private fun hasRevenueCatSignal(entitlement: PremiumEntitlement): Boolean {
        return entitlement.status != PremiumStatus.NEVER_PURCHASED ||
            entitlement.type != null ||
            entitlement.latestPurchaseDate != null ||
            entitlement.originalPurchaseDate != null ||
            entitlement.expirationDate != null ||
            entitlement.unsubscribeDetectedAt != null ||
            entitlement.billingIssueDetectedAt != null
    }

    private fun calculateDisplayPlanEndDate(
        planType: PremiumType?,
        latestPurchaseDate: Date?,
        originalPurchaseDate: Date?,
        expirationDate: Date?
    ): Date? {
        if (planType == null) return expirationDate

        val anchorPurchaseDate = latestPurchaseDate ?: originalPurchaseDate
        if (anchorPurchaseDate == null) {
            if (expirationDate != null) return expirationDate
            return addPeriodToDate(Date(), planType)
        }

        return addPeriodToDate(anchorPurchaseDate, planType)
    }

    private fun addPeriodToDate(anchor: Date, planType: PremiumType): Date {
        val calendar = Calendar.getInstance().apply { time = anchor }
        when (planType) {
            PremiumType.PREMIUM_WEEK -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            PremiumType.PREMIUM_MONTH, PremiumType.PREMIUM_PLUS_MONTH -> calendar.add(Calendar.MONTH, 1)
            PremiumType.PREMIUM_YEAR -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.time
    }
}

/**
 * UI State for Premium screens
 */
data class PremiumUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
    val purchaseError: String? = null,
    val restoreError: String? = null,
    val showSuccessAnimation: Boolean = false,
    
    // Premium status
    val premiumEntitlement: PremiumEntitlement? = null,
    val hasRevenueCatSignal: Boolean = false,
    val isPremium: Boolean = false,
    val isInTrial: Boolean = false,
    val isInGracePeriod: Boolean = false,
    val displayPlanEndDate: Date? = null,
    val autoRenewEnabled: Boolean = false,
    val firebaseIsPremium: Boolean = false,
    val firebasePremiumType: PremiumType? = null,
    val firebasePremiumUntil: Date? = null,
    
    // Products
    val premiumProducts: List<PremiumProduct> = emptyList(),
    val postExtensionProducts: List<PostExtensionProduct> = emptyList()
)

/**
 * Premium events for UI feedback
 */
sealed class PremiumEvent {
    data class PurchaseSuccess(val productName: String) : PremiumEvent()
    data class PostExtensionSuccess(val hours: Int) : PremiumEvent()
    data object PurchaseCancelled : PremiumEvent()
    data class PurchaseError(val message: String) : PremiumEvent()
    data object NetworkError : PremiumEvent()
    data object PaymentPending : PremiumEvent()
    data object AlreadyOwned : PremiumEvent()
    data object ProductNotAvailable : PremiumEvent()
    
    data class RestoreSuccess(val restoredCount: Int) : PremiumEvent()
    data object NoRestorablePurchases : PremiumEvent()
    data class RestoreError(val message: String) : PremiumEvent()
}

/**
 * Premium feature item for UI display
 */
data class PremiumFeatureItem(
    val titleResId: Int,
    val descriptionResId: Int,
    val isAvailable: Boolean,
    val icon: String
)
