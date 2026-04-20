package com.omerkaya.sperrmuellfinder.navigation

import com.omerkaya.sperrmuellfinder.core.navigation.BottomNavDestination
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BottomNavDestination crash fixes.
 * 
 * CRASH FIX: Tests ensure that navigation never returns null and handles
 * all edge cases that could cause NPE in BottomNavigationBar.
 */
class BottomNavDestinationTest {

    @Test
    fun `fromRoute with null returns Home`() {
        // CRASH FIX: Null route should return Home, not null
        val result = BottomNavDestination.fromRoute(null)
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `fromRoute with empty string returns Home`() {
        // CRASH FIX: Empty route should return Home, not null
        val result = BottomNavDestination.fromRoute("")
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `fromRoute with blank string returns Home`() {
        // CRASH FIX: Blank route should return Home, not null
        val result = BottomNavDestination.fromRoute("   ")
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `fromRoute with invalid route returns Home`() {
        // CRASH FIX: Invalid route should return Home, not null
        val result = BottomNavDestination.fromRoute("invalid_route")
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `fromRoute with valid routes returns correct destinations`() {
        // Test all valid routes
        assertEquals(BottomNavDestination.Home, BottomNavDestination.fromRoute("home"))
        assertEquals(BottomNavDestination.Search, BottomNavDestination.fromRoute("search"))
        assertEquals(BottomNavDestination.Map, BottomNavDestination.fromRoute("map"))
        assertEquals(BottomNavDestination.Camera, BottomNavDestination.fromRoute("camera"))
        assertEquals(BottomNavDestination.Profile, BottomNavDestination.fromRoute("profile"))
    }

    @Test
    fun `destinations list is never empty`() {
        // CRASH FIX: Destinations list should never be empty
        assertTrue("Destinations list should not be empty", BottomNavDestination.destinations.isNotEmpty())
        assertTrue("Destinations list should contain Home", BottomNavDestination.destinations.contains(BottomNavDestination.Home))
    }

    @Test
    fun `getSafeDestination with null returns Home`() {
        // CRASH FIX: Null destination should return Home
        val result = BottomNavDestination.getSafeDestination(null, isPremium = false)
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `getSafeDestination with premium destination and basic user returns Home`() {
        // CRASH FIX: Premium destination for basic user should return Home
        val result = BottomNavDestination.getSafeDestination(BottomNavDestination.Search, isPremium = false)
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `getSafeDestination with premium destination and premium user returns same destination`() {
        // Premium user should access premium destinations
        val result = BottomNavDestination.getSafeDestination(BottomNavDestination.Search, isPremium = true)
        assertEquals(BottomNavDestination.Search, result)
    }

    @Test
    fun `getSafeDestination with non-premium destination returns same destination`() {
        // Non-premium destinations should be accessible to all users
        val result = BottomNavDestination.getSafeDestination(BottomNavDestination.Home, isPremium = false)
        assertEquals(BottomNavDestination.Home, result)
    }

    @Test
    fun `getAvailableDestinations for basic user excludes premium destinations`() {
        // CRASH FIX: Basic users should only see non-premium destinations
        val result = BottomNavDestination.getAvailableDestinations(isPremium = false)
        
        assertTrue("Available destinations should not be empty", result.isNotEmpty())
        assertTrue("Available destinations should contain Home", result.contains(BottomNavDestination.Home))
        assertFalse("Available destinations should not contain Search for basic user", result.contains(BottomNavDestination.Search))
    }

    @Test
    fun `getAvailableDestinations for premium user includes all destinations`() {
        // Premium users should see all destinations
        val result = BottomNavDestination.getAvailableDestinations(isPremium = true)
        
        assertEquals("Premium user should see all destinations", BottomNavDestination.destinations.size, result.size)
        assertTrue("Available destinations should contain Home", result.contains(BottomNavDestination.Home))
        assertTrue("Available destinations should contain Search for premium user", result.contains(BottomNavDestination.Search))
    }

    @Test
    fun `all destinations have valid properties`() {
        // CRASH FIX: Ensure all destinations have valid properties to prevent NPE
        BottomNavDestination.destinations.forEach { destination ->
            assertNotNull("Destination route should not be null", destination.route)
            assertTrue("Destination route should not be empty", destination.route.isNotEmpty())
            assertTrue("Destination iconRes should be non-negative", destination.iconRes >= 0)
            assertTrue("Destination titleResId should be positive", destination.titleResId > 0)
            assertNotNull("Destination requiresPremium should not be null", destination.requiresPremium)
            assertNotNull("Destination showPremiumIndicator should not be null", destination.showPremiumIndicator)
        }
    }

    @Test
    fun `requiresPremium property is correctly set`() {
        // Verify premium requirements are correctly set
        assertFalse("Home should not require premium", BottomNavDestination.Home.requiresPremium)
        assertTrue("Search should require premium", BottomNavDestination.Search.requiresPremium)
        assertFalse("Map should not require premium", BottomNavDestination.Map.requiresPremium)
        assertFalse("Camera should not require premium", BottomNavDestination.Camera.requiresPremium)
        assertFalse("Profile should not require premium", BottomNavDestination.Profile.requiresPremium)
    }

    @Test
    fun `showPremiumIndicator property is correctly set`() {
        // Verify premium indicator settings
        assertFalse("Home should not show premium indicator", BottomNavDestination.Home.showPremiumIndicator)
        assertTrue("Search should show premium indicator", BottomNavDestination.Search.showPremiumIndicator)
        assertFalse("Map should not show premium indicator", BottomNavDestination.Map.showPremiumIndicator)
        assertFalse("Camera should not show premium indicator", BottomNavDestination.Camera.showPremiumIndicator)
        assertFalse("Profile should not show premium indicator", BottomNavDestination.Profile.showPremiumIndicator)
    }
}
