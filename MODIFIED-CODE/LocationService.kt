package com.spoofgps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var latitude = 0.0
    private var longitude = 0.0
    private var accuracy = 5f
    private var hideMode = false
    private var updateTimer: Timer? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_spoof"
        private const val TAG = "LocationService"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
        longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
        accuracy = intent?.getFloatExtra("accuracy", 5f) ?: 5f
        hideMode = intent?.getBooleanExtra("hide", false) ?: false

        Log.d(TAG, "Location: $latitude, $longitude, Accuracy: $accuracy, Hide: $hideMode")

        startForeground(NOTIFICATION_ID, createNotification())

        GlobalScope.launch(Dispatchers.Default) {
            injectMockLocation()
        }

        return START_STICKY
    }

    private fun injectMockLocation() {
        Handler(Looper.getMainLooper()).post {
            enableMockProviders()
            
            updateTimer = timer(initialDelay = 0, period = 2000) {
                try {
                    val providers = arrayOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )

                    for (provider in providers) {
                        try {
                            if (!locationManager.isProviderEnabled(provider)) {
                                Log.d(TAG, "Provider $provider not enabled, enabling...")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    addTestProvider(provider)
                                }
                            }

                            val mockLoc = Location(provider).apply {
                                this.latitude = this@LocationService.latitude
                                this.longitude = this@LocationService.longitude
                                this.accuracy = this@LocationService.accuracy
                                this.time = System.currentTimeMillis()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                    this.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                                }
                                if (hideMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    isFromMockProvider = false
                                }
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    locationManager.setTestProviderLocation(provider, mockLoc)
                                    Log.d(TAG, "✓ Injected $provider: $latitude, $longitude")
                                } catch (e: Exception) {
                                    Log.d(TAG, "Provider $provider error: ${e.message}")
                                    injectViaReflection(provider, mockLoc)
                                }
                            } else {
                                injectViaReflection(provider, mockLoc)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error with $provider", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Injection error", e)
                }
            }
        }
    }

    private fun enableMockProviders() {
        try {
            val providers = arrayOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            for (provider in providers) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        locationManager.addTestProvider(
                            provider,
                            false,
                            false,
                            false,
                            false,
                            true,
                            true,
                            true,
                            android.location.Criteria.POWER_LOW,
                            android.location.Criteria.ACCURACY_FINE
                        )
                        locationManager.setTestProviderEnabled(provider, true)
                        Log.d(TAG, "✓ Enabled mock provider: $provider")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Provider $provider already added or error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling mock providers", e)
        }
    }

    private fun addTestProvider(provider: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                locationManager.addTestProvider(
                    provider,
                    false, false, false, false, true, true, true,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE
                )
                locationManager.setTestProviderEnabled(provider, true)
            }
        } catch (e: Exception) {
            Log.d(TAG, "addTestProvider error: ${e.message}")
        }
    }

    private fun injectViaReflection(provider: String, location: Location) {
        try {
            val method = locationManager.javaClass.getMethod(
                "setTestProviderLocation",
                String::class.java,
                Location::class.java
            )
            method.invoke(locationManager, provider, location)
            Log.d(TAG, "✓ Reflection injection: $provider")
        } catch (e: Exception) {
            Log.d(TAG, "Reflection failed for $provider: ${e.message}")
        }
    }

    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌍 Location Spoof Active")
            .setContentText("Lat: $latitude | Lon: $longitude")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Spoofing",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer?.cancel()
        
        try {
            val providers = arrayOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                locationManager.removeTestProvider(provider)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error removing test providers: ${e.message}")
        }
        
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}