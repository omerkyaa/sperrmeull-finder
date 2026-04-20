package com.omerkaya.sperrmuellfinder.core.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Library Manager for handling native library loading issues.
 * 
 * CRASH FIX: Provides defensive mechanisms for native library loading failures
 * that can occur in emulators or when dependencies have missing native components.
 */
@Singleton
class NativeLibraryManager @Inject constructor(
    private val logger: Logger
) {
    
    private val loadedLibraries = mutableSetOf<String>()
    private val failedLibraries = mutableSetOf<String>()
    
    /**
     * Safely load a native library with comprehensive error handling.
     * 
     * @param libraryName Name of the library (without "lib" prefix or ".so" suffix)
     * @param isRequired Whether the library is required for app functionality
     * @return true if loaded successfully, false otherwise
     */
    fun loadLibrarySafely(libraryName: String, isRequired: Boolean = false): Boolean {
        // Check if already loaded
        if (loadedLibraries.contains(libraryName)) {
            logger.d(Logger.TAG_DEFAULT, "Library '$libraryName' already loaded")
            return true
        }
        
        // Check if previously failed
        if (failedLibraries.contains(libraryName)) {
            logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' previously failed to load")
            return false
        }
        
        return try {
            System.loadLibrary(libraryName)
            loadedLibraries.add(libraryName)
            logger.i(Logger.TAG_DEFAULT, "Successfully loaded native library: $libraryName")
            true
            
        } catch (e: UnsatisfiedLinkError) {
            failedLibraries.add(libraryName)
            
            val errorLevel = if (isRequired) "ERROR" else "WARNING"
            val message = "Failed to load native library '$libraryName': ${e.message}"
            
            if (isRequired) {
                logger.e(Logger.TAG_DEFAULT, "$errorLevel: $message", e)
            } else {
                logger.w(Logger.TAG_DEFAULT, "$errorLevel: $message - continuing without it", e)
            }
            
            // Analyze the error for common causes
            analyzeLibraryError(libraryName, e)
            
            false
            
        } catch (e: SecurityException) {
            failedLibraries.add(libraryName)
            logger.e(Logger.TAG_DEFAULT, "Security error loading library '$libraryName'", e)
            false
            
        } catch (e: Exception) {
            failedLibraries.add(libraryName)
            logger.e(Logger.TAG_DEFAULT, "Unexpected error loading library '$libraryName'", e)
            false
        }
    }
    
    /**
     * Check if a library is available without attempting to load it.
     */
    fun isLibraryAvailable(libraryName: String): Boolean {
        return loadedLibraries.contains(libraryName)
    }
    
    /**
     * Get list of successfully loaded libraries.
     */
    fun getLoadedLibraries(): Set<String> {
        return loadedLibraries.toSet()
    }
    
    /**
     * Get list of libraries that failed to load.
     */
    fun getFailedLibraries(): Set<String> {
        return failedLibraries.toSet()
    }
    
    /**
     * Analyze library loading errors and provide helpful information.
     */
    private fun analyzeLibraryError(libraryName: String, error: UnsatisfiedLinkError) {
        val errorMessage = error.message ?: ""
        
        when {
            errorMessage.contains("dlopen failed") -> {
                logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' - dlopen failed (common in emulator)")
            }
            errorMessage.contains("not found") -> {
                logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' - file not found (missing in APK or wrong ABI)")
            }
            errorMessage.contains("No implementation found") -> {
                logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' - no implementation (JNI method missing)")
            }
            errorMessage.contains("magtsync") -> {
                logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' - appears to be from a dependency, safe to ignore")
            }
            else -> {
                logger.w(Logger.TAG_DEFAULT, "Library '$libraryName' - unknown error: $errorMessage")
            }
        }
        
        // Provide solutions
        logger.i(Logger.TAG_DEFAULT, "Solutions for '$libraryName' loading issue:")
        logger.i(Logger.TAG_DEFAULT, "1. Check if library is needed (may be optional dependency)")
        logger.i(Logger.TAG_DEFAULT, "2. Verify correct ABI in build.gradle (arm64-v8a, armeabi-v7a, x86_64)")
        logger.i(Logger.TAG_DEFAULT, "3. Check jniLibs folder contains the library")
        logger.i(Logger.TAG_DEFAULT, "4. If from third-party dependency, update or exclude it")
    }
    
    /**
     * Initialize common libraries that might be needed.
     * This is called during app startup to preload and check libraries.
     */
    fun initializeCommonLibraries() {
        logger.d(Logger.TAG_DEFAULT, "Checking common native libraries...")
        
        // List of libraries that might be used by dependencies
        val commonLibraries = listOf(
            "magtsync",    // Sometimes used by Google services or other SDKs
            "c++_shared",  // Common C++ runtime
            "ssl",         // OpenSSL
            "crypto"       // Cryptography
        )
        
        commonLibraries.forEach { library ->
            loadLibrarySafely(library, isRequired = false)
        }
        
        logger.i(Logger.TAG_DEFAULT, "Native library check complete. Loaded: ${loadedLibraries.size}, Failed: ${failedLibraries.size}")
        
        if (failedLibraries.isNotEmpty()) {
            logger.w(Logger.TAG_DEFAULT, "Failed libraries (app continues normally): ${failedLibraries.joinToString()}")
        }
    }
    
    /**
     * Clear all tracking data (for testing purposes).
     */
    fun clearTracking() {
        loadedLibraries.clear()
        failedLibraries.clear()
        logger.d(Logger.TAG_DEFAULT, "Native library tracking cleared")
    }
}
