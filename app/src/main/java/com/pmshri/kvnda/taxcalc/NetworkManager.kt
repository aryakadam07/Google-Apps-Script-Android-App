package com.pmshri.kvnda.taxcalc

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

/**
 * Manages network connectivity detection and state changes.
 * Supports active callback notifications for automatic reconnects when network returns.
 */
class NetworkManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Synchronously checks if an active and validated Internet connection is currently available.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Registers a listener that fires callbacks on network state transitions.
     */
    fun registerNetworkListener(
        onAvailable: () -> Unit,
        onLost: () -> Unit
    ) {
        val cm = connectivityManager ?: return
        unregisterNetworkListener()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post { onAvailable() }
            }

            override fun onLost(network: Network) {
                mainHandler.post { onLost() }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        } catch (_: Exception) {
            // Fallback gracefully if registering network callback fails
        }
    }

    /**
     * Safely unregisters the callback to prevent memory leaks.
     */
    fun unregisterNetworkListener() {
        val cm = connectivityManager ?: return
        networkCallback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            networkCallback = null
        }
    }
}
