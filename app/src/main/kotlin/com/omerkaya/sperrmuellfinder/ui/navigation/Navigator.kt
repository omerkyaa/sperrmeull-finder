package com.omerkaya.sperrmuellfinder.ui.navigation

import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController

/**
 * Navigation helper extensions for NavController.
 * Provides type-safe navigation functions using AppDestinations.
 */

// Main navigation functions
fun NavController.navigateToHome() {
    navigate(AppDestinations.home()) {
        popUpTo(AppDestinations.home()) { inclusive = true }
    }
}

fun NavController.navigateToNotifications() {
    navigate(AppDestinations.notifications())
}

fun NavController.navigateToSearch() {
    navigate(AppDestinations.search())
}

fun NavController.navigateToCamera() {
    navigate(AppDestinations.camera())
}

fun NavController.navigateToProfile() {
    navigate(AppDestinations.profile())
}

fun NavController.navigateToPremium() {
    navigate(AppDestinations.premium())
}

fun NavController.navigateToModernPaywall() {
    navigate(AppDestinations.modernPaywall())
}

fun NavController.navigateToRevenueCatPaywall() {
    navigate(AppDestinations.revenueCatPaywall())
}

fun NavController.navigateToCustomerCenter() {
    navigate(AppDestinations.customerCenter())
}

fun NavController.navigateToSettings() {
    navigate(AppDestinations.settings())
}

fun NavController.navigateToEditProfile() {
    navigate(AppDestinations.editProfile())
}

fun NavController.navigateToBlockedUsers() {
    navigate(AppDestinations.blockedUsers())
}

fun NavController.navigateToDeleteAccount() {
    navigate(AppDestinations.deleteAccount())
}

fun NavController.navigateToAdminDashboard() {
    navigate(AppDestinations.adminDashboard())
}

// Navigation functions with arguments
fun NavController.navigateToLikes(postId: String) {
    navigate(AppDestinations.likes(postId))
}

fun NavController.navigateToComments(postId: String) {
    navigate(AppDestinations.comments(postId))
}

fun NavController.navigateToPostDetail(postId: String) {
    navigate(AppDestinations.postDetail(postId))
}

fun NavController.navigateToUserProfile(userId: String) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId != null && userId == currentUserId) {
        navigate(AppDestinations.profile())
    } else {
        navigate(AppDestinations.userProfile(userId))
    }
}

fun NavController.navigateToFollowers(userId: String) {
    navigate(AppDestinations.followers(userId))
}

fun NavController.navigateToFollowing(userId: String) {
    navigate(AppDestinations.following(userId))
}

// Utility navigation functions
fun NavController.navigateBack() {
    popBackStack()
}

fun NavController.navigateBackToHome() {
    popBackStack(AppDestinations.home(), inclusive = false)
}
