package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import android.content.Intent
import com.omerkaya.sperrmuellfinder.core.navigation.DeepLinks

/**
 * Utility class for sharing posts and handling share functionality.
 * Provides methods to create share intents and format share content.
 */
object ShareUtils {

    /**
     * Creates and launches a share intent for a post.
     * 
     * @param context Android context
     * @param postId The ID of the post to share
     * @param postDescription Description of the post
     * @param postCity City where the post is located
     * @param appStoreLink Link to the app in Play Store
     * @param shareTitle Title for the share chooser
     */
    fun sharePost(
        context: Context,
        postId: String,
        postDescription: String,
        postCity: String,
        appStoreLink: String,
        shareTitle: String
    ) {
        val shareableLink = DeepLinks.createShareablePostLink(postId)
        val shareText = DeepLinks.createPostShareText(
            postDescription = postDescription,
            postCity = postCity,
            shareableLink = shareableLink,
            appStoreLink = appStoreLink
        )
        
        val shareIntent = createShareIntent(
            text = shareText,
            subject = "SperrmüllFinder - $postCity"
        )
        
        val chooserIntent = Intent.createChooser(shareIntent, shareTitle)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            // Log error but don't crash the app
            Logger().e(Logger.TAG_DEFAULT, "Failed to launch share intent", e)
        }
    }

    /**
     * Creates a basic share intent with text content.
     * 
     * @param text The text content to share
     * @param subject Optional subject for the share
     * @return Configured share intent
     */
    private fun createShareIntent(
        text: String,
        subject: String? = null
    ): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
    }

    /**
     * Creates a share intent with both text and image.
     * This can be used for future enhancements to share post images.
     * 
     * @param text The text content to share
     * @param imageUri URI of the image to share
     * @param subject Optional subject for the share
     * @return Configured share intent
     */
    fun createShareIntentWithImage(
        text: String,
        imageUri: android.net.Uri,
        subject: String? = null
    ): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Formats share text for different platforms.
     * Can be extended to customize text for specific platforms like WhatsApp, Instagram, etc.
     * 
     * @param postId The ID of the post to format
     * @param postDescription Description of the post
     * @param postCity City where the post is located
     * @param platform Target platform (optional)
     * @return Formatted share text
     */
    fun formatShareText(
        postId: String,
        postDescription: String,
        postCity: String,
        platform: SharePlatform = SharePlatform.GENERIC
    ): String = when (platform) {
        SharePlatform.WHATSAPP -> {
            // WhatsApp specific formatting with emojis
            "🔍 *SperrmüllFinder Fund!*\n\n" +
            "📍 $postCity\n" +
            "📝 ${postDescription.take(100)}${if (postDescription.length > 100) "..." else ""}\n\n" +
            "🔗 Schau es dir an: ${DeepLinks.createShareablePostLink(postId)}\n\n" +
            "📱 App herunterladen: https://play.google.com/store/apps/details?id=com.omerkaya.sperrmuellfinder"
        }
        SharePlatform.INSTAGRAM -> {
            // Instagram specific formatting (shorter text)
            "Found on SperrmüllFinder in $postCity! 🔍\n" +
            "${DeepLinks.createShareablePostLink(postId)}"
        }
        SharePlatform.FACEBOOK -> {
            // Facebook specific formatting
            "SperrmüllFinder: $postDescription\n\n" +
            "Ort: $postCity\n" +
            "Details: ${DeepLinks.createShareablePostLink(postId)}\n\n" +
            "App herunterladen: https://play.google.com/store/apps/details?id=com.omerkaya.sperrmuellfinder"
        }
        SharePlatform.TWITTER -> {
            // Twitter specific formatting (character limit)
            val shortDesc = if (postDescription.length > 60) {
                postDescription.take(57) + "..."
            } else postDescription
            "#SperrmüllFinder Fund in $postCity: $shortDesc\n${DeepLinks.createShareablePostLink(postId)}"
        }
        SharePlatform.GENERIC -> {
            // Generic formatting for all other platforms
            DeepLinks.createPostShareText(
                postDescription = postDescription,
                postCity = postCity,
                shareableLink = DeepLinks.createShareablePostLink(postId),
                appStoreLink = "https://play.google.com/store/apps/details?id=com.omerkaya.sperrmuellfinder"
            )
        }
        else -> {
            // Fallback for any unexpected enum values
            DeepLinks.createPostShareText(
                postDescription = postDescription,
                postCity = postCity,
                shareableLink = DeepLinks.createShareablePostLink(postId),
                appStoreLink = "https://play.google.com/store/apps/details?id=com.omerkaya.sperrmuellfinder"
            )
        }
    }
}

/**
 * Enum for different share platforms to customize content.
 */
enum class SharePlatform {
    GENERIC,
    WHATSAPP,
    INSTAGRAM,
    FACEBOOK,
    TWITTER
}
