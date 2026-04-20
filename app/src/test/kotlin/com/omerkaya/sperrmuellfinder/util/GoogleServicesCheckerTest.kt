package com.omerkaya.sperrmuellfinder.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.omerkaya.sperrmuellfinder.core.util.GoogleServicesChecker
import com.omerkaya.sperrmuellfinder.core.util.GoogleServicesStatus
import com.omerkaya.sperrmuellfinder.core.util.Logger
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GoogleServicesChecker crash fixes.
 * 
 * CRASH FIX: Tests ensure that Google Services errors are handled gracefully
 * and never cause SecurityExceptions or crashes.
 */
class GoogleServicesCheckerTest {

    private lateinit var context: Context
    private lateinit var logger: Logger
    private lateinit var googleServicesChecker: GoogleServicesChecker
    private lateinit var mockGoogleApiAvailability: GoogleApiAvailability

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        mockGoogleApiAvailability = mockk(relaxed = true)

        // Mock static GoogleApiAvailability.getInstance()
        mockkStatic(GoogleApiAvailability::class)
        every { GoogleApiAvailability.getInstance() } returns mockGoogleApiAvailability

        googleServicesChecker = GoogleServicesChecker(context, logger)
    }

    @Test
    fun `checkGoogleServicesAvailability returns Available when services are available`() {
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return Available status", result is GoogleServicesStatus.Available)
    }

    @Test
    fun `checkGoogleServicesAvailability returns NotInstalled when services missing`() {
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        every { mockGoogleApiAvailability.isUserResolvableError(ConnectionResult.SERVICE_MISSING) } returns true

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return NotInstalled status", result is GoogleServicesStatus.NotInstalled)
        val notInstalled = result as GoogleServicesStatus.NotInstalled
        assertTrue("Should be user resolvable", notInstalled.isResolvable)
    }

    @Test
    fun `checkGoogleServicesAvailability returns UpdateRequired when update needed`() {
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
        every { mockGoogleApiAvailability.isUserResolvableError(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) } returns true

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return UpdateRequired status", result is GoogleServicesStatus.UpdateRequired)
        val updateRequired = result as GoogleServicesStatus.UpdateRequired
        assertTrue("Should be user resolvable", updateRequired.isResolvable)
    }

    @Test
    fun `checkGoogleServicesAvailability returns Disabled when services disabled`() {
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_DISABLED
        every { mockGoogleApiAvailability.isUserResolvableError(ConnectionResult.SERVICE_DISABLED) } returns true

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return Disabled status", result is GoogleServicesStatus.Disabled)
    }

    @Test
    fun `checkGoogleServicesAvailability returns Invalid when services invalid`() {
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_INVALID
        every { mockGoogleApiAvailability.isUserResolvableError(ConnectionResult.SERVICE_INVALID) } returns false

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return Invalid status", result is GoogleServicesStatus.Invalid)
        val invalid = result as GoogleServicesStatus.Invalid
        assertFalse("Should not be user resolvable", invalid.isResolvable)
    }

    @Test
    fun `checkGoogleServicesAvailability handles SecurityException gracefully`() {
        // CRASH FIX: SecurityException should be caught and handled
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } throws SecurityException("Unknown calling package name")

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return SecurityError status", result is GoogleServicesStatus.SecurityError)
        val securityError = result as GoogleServicesStatus.SecurityError
        assertTrue("Should contain security error message", securityError.message.contains("Security error"))
        assertEquals("Should preserve original exception", "Unknown calling package name", securityError.exception.message)
    }

    @Test
    fun `checkGoogleServicesAvailability handles general exceptions gracefully`() {
        // CRASH FIX: Any exception should be caught and handled
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } throws RuntimeException("Unexpected error")

        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return UnknownError status", result is GoogleServicesStatus.UnknownError)
        val unknownError = result as GoogleServicesStatus.UnknownError
        assertTrue("Should contain unknown error message", unknownError.message.contains("Unknown error"))
        assertEquals("Should preserve original exception", "Unexpected error", unknownError.exception.message)
    }

    @Test
    fun `isGoogleServicesAvailable returns true only when Available`() {
        // Test Available case
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS
        assertTrue("Should return true for Available", googleServicesChecker.isGoogleServicesAvailable())

        // Test non-Available case
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        assertFalse("Should return false for NotInstalled", googleServicesChecker.isGoogleServicesAvailable())
    }

    @Test
    fun `canUseGoogleServicesSafely returns false for SecurityError`() {
        // CRASH FIX: SecurityError should make services unusable
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } throws SecurityException("Security error")

        assertFalse("Should return false for SecurityError", googleServicesChecker.canUseGoogleServicesSafely())
    }

    @Test
    fun `canUseGoogleServicesSafely returns true only for Available`() {
        // Test Available case
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS
        assertTrue("Should return true for Available", googleServicesChecker.canUseGoogleServicesSafely())

        // Test other cases
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        assertFalse("Should return false for NotInstalled", googleServicesChecker.canUseGoogleServicesSafely())
    }

    @Test
    fun `getStatusMessage returns appropriate messages`() {
        // Test Available
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS
        val availableMessage = googleServicesChecker.getStatusMessage()
        assertTrue("Should contain available message", availableMessage.contains("verfügbar"))

        // Test NotInstalled
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        every { mockGoogleApiAvailability.isUserResolvableError(any()) } returns true
        val notInstalledMessage = googleServicesChecker.getStatusMessage()
        assertTrue("Should contain not installed message", notInstalledMessage.contains("nicht installiert"))

        // Test SecurityError
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } throws SecurityException("Security error")
        val securityMessage = googleServicesChecker.getStatusMessage()
        assertTrue("Should contain security error message", securityMessage.contains("Sicherheitsfehler"))
    }

    @Test
    fun `caching works correctly`() {
        // First call
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS
        val result1 = googleServicesChecker.checkGoogleServicesAvailability()

        // Second call within cache validity (should use cached result)
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        val result2 = googleServicesChecker.checkGoogleServicesAvailability()

        // Both results should be the same (cached)
        assertEquals("Second call should return cached result", result1::class, result2::class)
        assertTrue("Both should be Available", result1 is GoogleServicesStatus.Available)
        assertTrue("Both should be Available", result2 is GoogleServicesStatus.Available)

        // Verify GoogleApiAvailability was called only once
        verify(exactly = 1) { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) }
    }

    @Test
    fun `clearCache forces fresh check`() {
        // First call
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS
        googleServicesChecker.checkGoogleServicesAvailability()

        // Clear cache
        googleServicesChecker.clearCache()

        // Second call after cache clear (should make fresh call)
        every { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING
        val result = googleServicesChecker.checkGoogleServicesAvailability()

        assertTrue("Should return fresh result", result is GoogleServicesStatus.NotInstalled)
        verify(exactly = 2) { mockGoogleApiAvailability.isGooglePlayServicesAvailable(context) }
    }

    @Test
    fun `performDiagnostic handles package manager exceptions`() {
        // Setup: PackageManager throws exception
        every { context.packageManager } throws RuntimeException("PackageManager error")

        val diagnostic = googleServicesChecker.performDiagnostic()

        assertNotNull("Diagnostic should not be null", diagnostic)
        assertFalse("Google Services package should be marked as not installed", diagnostic.googleServicesPackage.isInstalled)
        assertEquals("Should have fallback package name", "com.google.android.gms", diagnostic.googleServicesPackage.packageName)
    }
}
