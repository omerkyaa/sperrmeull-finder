package com.omerkaya.sperrmuellfinder.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import com.omerkaya.sperrmuellfinder.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private fun hasNetworkStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if device is currently connected to internet
     */
    fun isConnected(): Boolean {
        return try {
            if (!hasNetworkStatePermission()) return false
            val manager = connectivityManager ?: return false
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            logger.e(Logger.TAG_NETWORK, "Error checking network connectivity", e)
            false
        }
    }

    /**
     * Check if device is connected to WiFi
     */
    fun isConnectedToWiFi(): Boolean {
        return try {
            if (!hasNetworkStatePermission()) return false
            val manager = connectivityManager ?: return false
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            logger.e(Logger.TAG_NETWORK, "Error checking WiFi connectivity", e)
            false
        }
    }

    /**
     * Check if device is connected to mobile data
     */
    fun isConnectedToMobileData(): Boolean {
        return try {
            if (!hasNetworkStatePermission()) return false
            val manager = connectivityManager ?: return false
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            logger.e(Logger.TAG_NETWORK, "Error checking mobile data connectivity", e)
            false
        }
    }

    /**
     * Get current network type
     */
    fun getCurrentNetworkType(): NetworkType {
        return try {
            if (!hasNetworkStatePermission()) return NetworkType.NONE
            val manager = connectivityManager ?: return NetworkType.NONE
            val network = manager.activeNetwork ?: return NetworkType.NONE
            val capabilities = manager.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            when {
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkType.NONE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } catch (e: Exception) {
            logger.e(Logger.TAG_NETWORK, "Error getting network type", e)
            NetworkType.NONE
        }
    }

    /**
     * Check if network is metered (limited data usage)
     */
    fun isNetworkMetered(): Boolean {
        return try {
            if (!hasNetworkStatePermission()) return false
            val manager = connectivityManager ?: return false
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } catch (e: Exception) {
            logger.e(Logger.TAG_NETWORK, "Error checking if network is metered", e)
            false
        }
    }

    /**
     * Flow that emits network connectivity changes
     */
    fun networkConnectivityFlow(): Flow<NetworkState> = callbackFlow {
        if (!hasNetworkStatePermission() || connectivityManager == null) {
            trySend(
                NetworkState(
                    isConnected = false,
                    networkType = NetworkType.NONE,
                    isMetered = false
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val networkType = getCurrentNetworkType()
                val isMetered = isNetworkMetered()
                
                val networkState = NetworkState(
                    isConnected = true,
                    networkType = networkType,
                    isMetered = isMetered
                )
                
                logger.d(Logger.TAG_NETWORK, "Network available: $networkState")
                trySend(networkState)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                val networkState = NetworkState(
                    isConnected = false,
                    networkType = NetworkType.NONE,
                    isMetered = false
                )
                
                logger.d(Logger.TAG_NETWORK, "Network lost: $networkState")
                trySend(networkState)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (isConnected) {
                    val networkType = getCurrentNetworkType()
                    val isMetered = isNetworkMetered()
                    
                    val networkState = NetworkState(
                        isConnected = true,
                        networkType = networkType,
                        isMetered = isMetered
                    )
                    
                    logger.d(Logger.TAG_NETWORK, "Network capabilities changed: $networkState")
                    trySend(networkState)
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Send initial state
        val initialState = NetworkState(
            isConnected = isConnected(),
            networkType = getCurrentNetworkType(),
            isMetered = isNetworkMetered()
        )
        trySend(initialState)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            logger.d(Logger.TAG_NETWORK, "Network callback unregistered")
        }
    }.distinctUntilChanged().flowOn(ioDispatcher)

    /**
     * Flow that emits only connectivity status (boolean)
     */
    fun isConnectedFlow(): Flow<Boolean> = callbackFlow {
        if (!hasNetworkStatePermission() || connectivityManager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Send initial state
        trySend(isConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged().flowOn(ioDispatcher)

    /**
     * Check if it's safe to perform heavy network operations
     * (connected to WiFi or unlimited mobile data)
     */
    fun isSafeForHeavyOperations(): Boolean {
        return isConnected() && (isConnectedToWiFi() || !isNetworkMetered())
    }

    /**
     * Get network info for analytics
     */
    fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = isConnected(),
            networkType = getCurrentNetworkType(),
            isMetered = isNetworkMetered(),
            isWiFi = isConnectedToWiFi(),
            isMobile = isConnectedToMobileData()
        )
    }

    enum class NetworkType {
        NONE,
        WIFI,
        MOBILE,
        ETHERNET,
        OTHER
    }

    data class NetworkState(
        val isConnected: Boolean,
        val networkType: NetworkType,
        val isMetered: Boolean
    )

    data class NetworkInfo(
        val isConnected: Boolean,
        val networkType: NetworkType,
        val isMetered: Boolean,
        val isWiFi: Boolean,
        val isMobile: Boolean
    )
}
