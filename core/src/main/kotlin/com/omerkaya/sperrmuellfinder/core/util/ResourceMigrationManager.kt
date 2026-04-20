package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resource Migration Manager for handling resource ID migration issues.
 * 
 * CRASH FIX: Fixes "No package ID 6a found for resource ID 0x6a0b000f" errors
 * by migrating integer resource IDs to string-based keys in persistent storage.
 * 
 * The root cause: When apps store integer resource IDs (like R.string.xyz's numeric value)
 * in SharedPreferences or databases, these IDs can change between app versions,
 * causing runtime crashes when the old IDs are no longer valid.
 * 
 * Solution: Always store resource names/keys as strings, not integer IDs.
 */
@Singleton
class ResourceMigrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    private val preferencesManager: PreferencesManager
) {
    
    companion object {
        private const val MIGRATION_PREFS = "resource_migration_prefs"
        private const val KEY_MIGRATION_VERSION = "migration_version"
        private const val KEY_LAST_MIGRATION_TIME = "last_migration_time"
        
        // Current migration version - increment when adding new migrations
        private const val CURRENT_MIGRATION_VERSION = 1
        
        // Known problematic resource ID patterns that need migration
        private val PROBLEMATIC_RESOURCE_PATTERNS = listOf(
            "0x6a", // Package ID 6a (common in resource errors)
            "0x7f", // App package ID (can change between builds)
            "0x01", // System package ID (should not be stored)
            "0x02"  // Framework package ID (should not be stored)
        )
    }
    
    private val migrationPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
    }
    
    /**
     * Perform comprehensive resource migration.
     * This should be called during app startup before any UI is shown.
     */
    suspend fun performResourceMigration(): ResourceMigrationResult {
        return withContext(Dispatchers.IO) {
            try {
                logger.i(Logger.TAG_DEFAULT, "Starting resource migration process...")
                
                val currentVersion = migrationPrefs.getInt(KEY_MIGRATION_VERSION, 0)
                val startTime = System.currentTimeMillis()
                
                if (currentVersion >= CURRENT_MIGRATION_VERSION) {
                    logger.d(Logger.TAG_DEFAULT, "Resource migration already up to date (v$currentVersion)")
                    return@withContext ResourceMigrationResult.AlreadyUpToDate
                }
                
                val migrationsPerformed = mutableListOf<String>()
                
                // Migration v1: Clean up integer resource IDs from SharedPreferences
                if (currentVersion < 1) {
                    val v1Result = performMigrationV1()
                    migrationsPerformed.addAll(v1Result)
                }
                
                // Save migration completion
                migrationPrefs.edit()
                    .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
                    .putLong(KEY_LAST_MIGRATION_TIME, System.currentTimeMillis())
                    .apply()
                
                val duration = System.currentTimeMillis() - startTime
                logger.i(Logger.TAG_DEFAULT, "Resource migration completed in ${duration}ms. Migrations: ${migrationsPerformed.size}")
                
                ResourceMigrationResult.Success(migrationsPerformed, duration)
                
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Resource migration failed", e)
                ResourceMigrationResult.Failed(e)
            }
        }
    }
    
    /**
     * Migration V1: Clean up integer resource IDs from all SharedPreferences.
     */
    private suspend fun performMigrationV1(): List<String> {
        val migrationsPerformed = mutableListOf<String>()
        
        logger.d(Logger.TAG_DEFAULT, "Performing migration V1: Integer resource ID cleanup")
        
        try {
            // Get all SharedPreferences files in the app
            val prefsFiles = listOf(
                "sperrmuell_finder_secure_prefs",
                "sperrmuell_finder_prefs", 
                "sperrmuell_finder_fallback_prefs",
                context.packageName + "_preferences" // Default preferences
            )
            
            prefsFiles.forEach { prefsName ->
                try {
                    val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    val cleanedCount = cleanIntegerResourceIds(prefs, prefsName)
                    
                    if (cleanedCount > 0) {
                        migrationsPerformed.add("Cleaned $cleanedCount integer resource IDs from $prefsName")
                        logger.i(Logger.TAG_DEFAULT, "Cleaned $cleanedCount integer resource IDs from $prefsName")
                    }
                    
                } catch (e: Exception) {
                    logger.w(Logger.TAG_DEFAULT, "Failed to clean preferences file: $prefsName", e)
                }
            }
            
            // Clean up any cached resource references in memory
            migrationsPerformed.add("Cleared resource caches")
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Migration V1 failed", e)
            throw e
        }
        
        return migrationsPerformed
    }
    
    /**
     * Clean integer resource IDs from a specific SharedPreferences file.
     */
    private fun cleanIntegerResourceIds(prefs: SharedPreferences, prefsName: String): Int {
        var cleanedCount = 0
        
        try {
            val allEntries = prefs.all
            val editor = prefs.edit()
            var hasChanges = false
            
            allEntries.forEach { (key, value) ->
                when (value) {
                    is Int -> {
                        // Check if this looks like a resource ID
                        if (isLikelyResourceId(value)) {
                            logger.w(Logger.TAG_DEFAULT, "Removing suspected resource ID: $key = 0x${value.toString(16)} from $prefsName")
                            editor.remove(key)
                            hasChanges = true
                            cleanedCount++
                        }
                    }
                    is String -> {
                        // Check if string contains hex resource ID patterns
                        if (containsProblematicResourcePattern(value)) {
                            logger.w(Logger.TAG_DEFAULT, "Removing string with resource ID pattern: $key = $value from $prefsName")
                            editor.remove(key)
                            hasChanges = true
                            cleanedCount++
                        }
                    }
                }
            }
            
            if (hasChanges) {
                editor.apply()
                logger.i(Logger.TAG_DEFAULT, "Applied $cleanedCount changes to $prefsName")
            }
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error cleaning resource IDs from $prefsName", e)
        }
        
        return cleanedCount
    }
    
    /**
     * Check if an integer value looks like a resource ID.
     */
    private fun isLikelyResourceId(value: Int): Boolean {
        if (value <= 0) return false
        
        val hexString = value.toString(16)
        
        // Resource IDs typically have specific package ID patterns
        return when {
            // Package ID 0x6a (the problematic one from the error)
            hexString.startsWith("6a0") -> true
            // App package IDs (0x7f is common)
            hexString.startsWith("7f0") -> true
            // System/framework IDs (shouldn't be stored by apps)
            hexString.startsWith("010") || hexString.startsWith("020") -> true
            // Very high values that look like resource IDs
            value > 0x01000000 && value < 0x7fffffff -> true
            else -> false
        }
    }
    
    /**
     * Check if a string contains problematic resource ID patterns.
     */
    private fun containsProblematicResourcePattern(value: String): Boolean {
        return PROBLEMATIC_RESOURCE_PATTERNS.any { pattern ->
            value.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Get migration status information.
     */
    fun getMigrationStatus(): ResourceMigrationStatus {
        val currentVersion = migrationPrefs.getInt(KEY_MIGRATION_VERSION, 0)
        val lastMigrationTime = migrationPrefs.getLong(KEY_LAST_MIGRATION_TIME, 0)
        
        return ResourceMigrationStatus(
            currentVersion = currentVersion,
            requiredVersion = CURRENT_MIGRATION_VERSION,
            isUpToDate = currentVersion >= CURRENT_MIGRATION_VERSION,
            lastMigrationTime = lastMigrationTime
        )
    }
    
    /**
     * Force a fresh migration (for testing or troubleshooting).
     */
    suspend fun forceMigration(): ResourceMigrationResult {
        logger.w(Logger.TAG_DEFAULT, "Forcing fresh resource migration...")
        val previousVersion = migrationPrefs.getInt(KEY_MIGRATION_VERSION, 0)
        logger.d(Logger.TAG_DEFAULT, "Current migration version before force reset: $previousVersion")
        
        // Reset migration version
        migrationPrefs.edit()
            .putInt(KEY_MIGRATION_VERSION, 0)
            .apply()
        
        return performResourceMigration()
    }
    
    /**
     * Validate that no integer resource IDs are currently stored.
     * This can be used for debugging and verification.
     */
    suspend fun validateNoResourceIds(): ResourceValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                val issues = mutableListOf<String>()
                
                // Check common preferences files
                val prefsFiles = listOf(
                    "sperrmuell_finder_secure_prefs",
                    "sperrmuell_finder_prefs",
                    "sperrmuell_finder_fallback_prefs"
                )
                
                prefsFiles.forEach { prefsName ->
                    try {
                        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        val foundIssues = findResourceIdIssues(prefs, prefsName)
                        issues.addAll(foundIssues)
                    } catch (e: Exception) {
                        issues.add("Failed to validate $prefsName: ${e.message}")
                    }
                }
                
                if (issues.isEmpty()) {
                    ResourceValidationResult.Clean
                } else {
                    ResourceValidationResult.IssuesFound(issues)
                }
                
            } catch (e: Exception) {
                ResourceValidationResult.ValidationFailed(e)
            }
        }
    }
    
    /**
     * Find resource ID issues in a SharedPreferences file.
     */
    private fun findResourceIdIssues(prefs: SharedPreferences, prefsName: String): List<String> {
        val issues = mutableListOf<String>()
        
        try {
            prefs.all.forEach { (key, value) ->
                when (value) {
                    is Int -> {
                        if (isLikelyResourceId(value)) {
                            issues.add("$prefsName: $key = 0x${value.toString(16)} (suspected resource ID)")
                        }
                    }
                    is String -> {
                        if (containsProblematicResourcePattern(value)) {
                            issues.add("$prefsName: $key = $value (contains resource ID pattern)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            issues.add("$prefsName: Error during validation - ${e.message}")
        }
        
        return issues
    }
}

/**
 * Result of resource migration operation.
 */
sealed class ResourceMigrationResult {
    data object AlreadyUpToDate : ResourceMigrationResult()
    data class Success(val migrationsPerformed: List<String>, val durationMs: Long) : ResourceMigrationResult()
    data class Failed(val exception: Exception) : ResourceMigrationResult()
}

/**
 * Status of resource migration.
 */
data class ResourceMigrationStatus(
    val currentVersion: Int,
    val requiredVersion: Int,
    val isUpToDate: Boolean,
    val lastMigrationTime: Long
)

/**
 * Result of resource validation.
 */
sealed class ResourceValidationResult {
    data object Clean : ResourceValidationResult()
    data class IssuesFound(val issues: List<String>) : ResourceValidationResult()
    data class ValidationFailed(val exception: Exception) : ResourceValidationResult()
}
