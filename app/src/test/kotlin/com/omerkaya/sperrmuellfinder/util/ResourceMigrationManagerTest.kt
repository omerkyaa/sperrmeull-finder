package com.omerkaya.sperrmuellfinder.util

import android.content.Context
import android.content.SharedPreferences
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.PreferencesManager
import com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationManager
import com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationResult
import com.omerkaya.sperrmuellfinder.core.util.ResourceValidationResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ResourceMigrationManager crash fixes.
 * 
 * CRASH FIX: Tests ensure that resource ID migration prevents
 * "No package ID 6a found for resource ID 0x6a0b000f" errors.
 */
class ResourceMigrationManagerTest {

    private lateinit var context: Context
    private lateinit var logger: Logger
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var resourceMigrationManager: ResourceMigrationManager
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        resourceMigrationManager = ResourceMigrationManager(context, logger, preferencesManager)
    }

    @Test
    fun `performResourceMigration with no previous version runs migration`() = runTest {
        // Setup: No previous migration
        every { mockPrefs.getInt("migration_version", 0) } returns 0
        every { mockPrefs.all } returns emptyMap()

        val result = resourceMigrationManager.performResourceMigration()

        assertTrue("Migration should succeed", result is ResourceMigrationResult.Success)
        verify { mockEditor.putInt("migration_version", 1) }
    }

    @Test
    fun `performResourceMigration with current version returns already up to date`() = runTest {
        // Setup: Already migrated
        every { mockPrefs.getInt("migration_version", 0) } returns 1

        val result = resourceMigrationManager.performResourceMigration()

        assertEquals("Should be already up to date", ResourceMigrationResult.AlreadyUpToDate, result)
    }

    @Test
    fun `isLikelyResourceId detects problematic resource IDs`() {
        // Use reflection to access private method for testing
        val method = ResourceMigrationManager::class.java.getDeclaredMethod("isLikelyResourceId", Int::class.java)
        method.isAccessible = true

        // Test problematic resource IDs
        assertTrue("0x6a0b000f should be detected as resource ID", 
            method.invoke(resourceMigrationManager, 0x6a0b000f) as Boolean)
        assertTrue("0x7f020001 should be detected as resource ID", 
            method.invoke(resourceMigrationManager, 0x7f020001) as Boolean)
        assertTrue("0x01040001 should be detected as resource ID", 
            method.invoke(resourceMigrationManager, 0x01040001) as Boolean)

        // Test normal values
        assertFalse("Small positive number should not be detected as resource ID", 
            method.invoke(resourceMigrationManager, 42) as Boolean)
        assertFalse("Negative number should not be detected as resource ID", 
            method.invoke(resourceMigrationManager, -1) as Boolean)
        assertFalse("Zero should not be detected as resource ID", 
            method.invoke(resourceMigrationManager, 0) as Boolean)
    }

    @Test
    fun `containsProblematicResourcePattern detects hex patterns`() {
        // Use reflection to access private method for testing
        val method = ResourceMigrationManager::class.java.getDeclaredMethod("containsProblematicResourcePattern", String::class.java)
        method.isAccessible = true

        // Test problematic patterns
        assertTrue("Should detect 0x6a pattern", 
            method.invoke(resourceMigrationManager, "resource_0x6a0b000f") as Boolean)
        assertTrue("Should detect 0x7f pattern", 
            method.invoke(resourceMigrationManager, "0x7f020001_value") as Boolean)

        // Test normal strings
        assertFalse("Normal string should not be detected", 
            method.invoke(resourceMigrationManager, "normal_string_value") as Boolean)
        assertFalse("Empty string should not be detected", 
            method.invoke(resourceMigrationManager, "") as Boolean)
    }

    @Test
    fun `cleanIntegerResourceIds removes problematic values`() = runTest {
        // Setup: SharedPreferences with problematic resource IDs
        val problematicData = mapOf(
            "normal_key" to "normal_value",
            "resource_id_key" to 0x6a0b000f, // Problematic resource ID
            "another_normal_key" to 42,
            "string_with_resource" to "value_0x6a123456",
            "safe_int" to 100
        )
        
        every { mockPrefs.all } returns problematicData
        every { mockEditor.remove(any()) } returns mockEditor

        // Use reflection to access private method
        val method = ResourceMigrationManager::class.java.getDeclaredMethod("cleanIntegerResourceIds", SharedPreferences::class.java, String::class.java)
        method.isAccessible = true

        val cleanedCount = method.invoke(resourceMigrationManager, mockPrefs, "test_prefs") as Int

        // Should clean the problematic resource ID and string with resource pattern
        assertEquals("Should clean 2 problematic entries", 2, cleanedCount)
        verify { mockEditor.remove("resource_id_key") }
        verify { mockEditor.remove("string_with_resource") }
        verify(exactly = 0) { mockEditor.remove("normal_key") }
        verify(exactly = 0) { mockEditor.remove("safe_int") }
    }

    @Test
    fun `validateNoResourceIds returns clean when no issues found`() = runTest {
        // Setup: Clean SharedPreferences
        every { mockPrefs.all } returns mapOf(
            "clean_key" to "clean_value",
            "safe_int" to 42
        )

        val result = resourceMigrationManager.validateNoResourceIds()

        assertEquals("Validation should return clean", ResourceValidationResult.Clean, result)
    }

    @Test
    fun `validateNoResourceIds returns issues when problems found`() = runTest {
        // Setup: SharedPreferences with issues
        every { mockPrefs.all } returns mapOf(
            "clean_key" to "clean_value",
            "problematic_resource" to 0x6a0b000f
        )

        val result = resourceMigrationManager.validateNoResourceIds()

        assertTrue("Validation should find issues", result is ResourceValidationResult.IssuesFound)
        val issues = (result as ResourceValidationResult.IssuesFound).issues
        assertTrue("Should report the problematic resource", 
            issues.any { it.contains("0x6a0b000f") })
    }

    @Test
    fun `getMigrationStatus returns correct status`() {
        every { mockPrefs.getInt("migration_version", 0) } returns 1
        every { mockPrefs.getLong("last_migration_time", 0) } returns 123456789L

        val status = resourceMigrationManager.getMigrationStatus()

        assertEquals("Current version should be 1", 1, status.currentVersion)
        assertEquals("Required version should be 1", 1, status.requiredVersion)
        assertTrue("Should be up to date", status.isUpToDate)
        assertEquals("Last migration time should match", 123456789L, status.lastMigrationTime)
    }

    @Test
    fun `forceMigration resets version and runs migration`() = runTest {
        // Setup: Previously migrated
        every { mockPrefs.getInt("migration_version", 0) } returnsMany listOf(1, 0) // First call returns 1, then 0 after reset
        every { mockPrefs.all } returns emptyMap()

        val result = resourceMigrationManager.forceMigration()

        assertTrue("Force migration should succeed", result is ResourceMigrationResult.Success)
        verify { mockEditor.putInt("migration_version", 0) } // Reset
        verify { mockEditor.putInt("migration_version", 1) } // Set after migration
    }

    @Test
    fun `migration handles exceptions gracefully`() = runTest {
        // Setup: Exception during SharedPreferences access
        every { mockPrefs.getInt("migration_version", 0) } returns 0
        every { context.getSharedPreferences(any(), any()) } throws RuntimeException("Test exception")

        val result = resourceMigrationManager.performResourceMigration()

        assertTrue("Migration should handle exception", result is ResourceMigrationResult.Failed)
        val exception = (result as ResourceMigrationResult.Failed).exception
        assertEquals("Should preserve original exception", "Test exception", exception.message)
    }
}
