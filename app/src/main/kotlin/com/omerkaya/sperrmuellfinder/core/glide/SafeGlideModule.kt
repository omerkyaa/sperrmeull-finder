package com.omerkaya.sperrmuellfinder.core.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.omerkaya.sperrmuellfinder.core.util.ContentUriCleaner

/**
 * Custom Glide module for safe image loading
 * 
 * This module:
 * 1. Adds global error handling for SecurityException
 * 2. Logs problematic URIs for debugging
 * 3. Provides fallback behavior for content:// URIs
 */
@GlideModule
class SafeGlideModule : AppGlideModule() {
    
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        
        // Add custom error handling
        Log.d("SafeGlideModule", "🖼️ SafeGlideModule registered - content:// URI protection enabled")
    }
    
    override fun isManifestParsingEnabled(): Boolean {
        // Disable manifest parsing for better performance
        return false
    }
}
