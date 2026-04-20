package com.omerkaya.sperrmuellfinder.domain.repository

import androidx.paging.PagingData
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.AdminLog
import com.omerkaya.sperrmuellfinder.domain.model.AdminRole
import com.omerkaya.sperrmuellfinder.domain.model.ModerationPriority
import com.omerkaya.sperrmuellfinder.domain.model.ModerationQueueEntry
import com.omerkaya.sperrmuellfinder.domain.model.Report
import com.omerkaya.sperrmuellfinder.domain.model.ReportStatus
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for admin operations
 * Rules.md compliant - Clean Architecture domain layer
 */
interface AdminRepository {
    
    // ========================================
    // ADMIN ROLE MANAGEMENT
    // ========================================
    
    /**
     * Check if a user is an admin
     */
    suspend fun isAdmin(userId: String): Result<Boolean>
    
    /**
     * Get admin role for a specific user
     */
    suspend fun getAdminRole(userId: String): Result<AdminRole?>
    
    /**
     * Get current user's admin role
     */
    suspend fun getCurrentAdminRole(): Result<AdminRole?>
    
    /**
     * Observe current user's admin role changes
     */
    fun observeAdminRole(): Flow<AdminRole?>
    
    /**
     * Set admin role for a user (super_admin only)
     */
    suspend fun setAdminRole(userId: String, role: AdminRole): Result<Unit>
    
    /**
     * Remove admin role from a user (super_admin only)
     */
    suspend fun removeAdminRole(userId: String): Result<Unit>
    
    // ========================================
    // REPORT MANAGEMENT
    // ========================================
    
    /**
     * Get reports with filtering and pagination
     */
    fun getReports(
        status: ReportStatus? = null,
        priority: ModerationPriority? = null,
        type: ReportTargetType? = null
    ): Flow<PagingData<Report>>
    
    /**
     * Get single report by ID
     */
    suspend fun getReport(reportId: String): Result<Report>
    
    /**
     * Assign report to admin
     */
    suspend fun assignReport(reportId: String, adminId: String): Result<Unit>
    
    /**
     * Dismiss report
     */
    suspend fun dismissReport(reportId: String, reason: String): Result<Unit>
    
    /**
     * Warn user based on report
     */
    suspend fun warnUser(reportId: String, reason: String): Result<Unit>
    
    /**
     * Delete reported content
     */
    suspend fun deleteReportedContent(reportId: String, reason: String): Result<Unit>
    
    // ========================================
    // USER MANAGEMENT - 3-Tier Ban System
    // ========================================
    
    /**
     * Search users
     */
    fun searchUsers(query: String): Flow<PagingData<User>>
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * Soft Ban - Temporary suspension (3 days, 1 week, 1 month)
     * User document updated with ban info
     * Firebase Auth stays active (app-level control)
     */
    suspend fun softBanUser(
        reportId: String?,
        userId: String,
        durationDays: Int, // 3, 7, or 30
        reason: String
    ): Result<Unit>
    
    /**
     * Hard Ban - Permanent ban
     * Firebase Auth disabled - user cannot login at all
     * User document kept for records
     */
    suspend fun hardBanUser(
        reportId: String?,
        userId: String,
        reason: String
    ): Result<Unit>
    
    /**
     * Delete Account - Complete removal
     * Firebase Auth deleted
     * Firestore user doc deleted
     * All user data removed (posts, comments, likes, etc.)
     * IRREVERSIBLE - Super admin only
     */
    suspend fun deleteUserAccount(
        reportId: String?,
        userId: String,
        reason: String
    ): Result<Unit>
    
    /**
     * Unban user (for soft/hard bans only)
     * Cannot restore deleted accounts
     */
    suspend fun unbanUser(userId: String, reason: String): Result<Unit>
    
    // ========================================
    // PREMIUM MANAGEMENT
    // ========================================
    
    /**
     * Grant premium to user
     */
    suspend fun grantPremium(
        userId: String,
        durationDays: Int,
        reason: String
    ): Result<Unit>
    
    /**
     * Revoke premium from user
     */
    suspend fun revokePremium(userId: String, reason: String): Result<Unit>
    
    // ========================================
    // CONTENT DELETION
    // ========================================
    
    /**
     * Delete post
     */
    suspend fun deletePost(postId: String, reason: String, reportId: String?): Result<Unit>
    
    /**
     * Delete comment
     */
    suspend fun deleteComment(commentId: String, reason: String, reportId: String?): Result<Unit>
    
    // ========================================
    // NOTIFICATIONS
    // ========================================
    
    /**
     * Send broadcast notification
     */
    suspend fun sendBroadcastNotification(
        targetType: String, // "all", "premium", "specific", "city"
        targetIds: List<String>?,
        title: String,
        body: String,
        deeplink: String?
    ): Result<Int> // Returns count of notifications sent
    
    // ========================================
    // ANALYTICS & LOGS
    // ========================================
    
    /**
     * Get admin logs with pagination
     */
    fun getAdminLogs(
        adminId: String? = null,
        action: String? = null
    ): Flow<PagingData<AdminLog>>
    
    /**
     * Get dashboard stats
     */
    suspend fun getDashboardStats(): Result<DashboardStats>
    
    /**
     * Get moderation queue
     */
    fun getModerationQueue(): Flow<PagingData<ModerationQueueEntry>>
}

/**
 * Dashboard statistics
 */
data class DashboardStats(
    val totalUsers: Int,
    val activeUsers: Int, // Last 7 days
    val premiumUsers: Int,
    val totalPosts: Int,
    val activePosts: Int,
    val pendingReports: Int,
    val reportsToday: Int,
    val bannedUsers: Int
)
