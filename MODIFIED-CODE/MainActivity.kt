package com.spoofgps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 100
    private lateinit var prefs: SharedPreferences

    private lateinit var latInput: EditText
    private lateinit var longInput: EditText
    private lateinit var accuracyInput: EditText
    private lateinit var setButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusView: TextView
    private lateinit var hideSwitch: Switch

    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("spoofgps", Context.MODE_PRIVATE)

        // Initialize UI
        latInput = findViewById(R.id.latInput)
        longInput = findViewById(R.id.longInput)
        accuracyInput = findViewById(R.id.accuracyInput)
        setButton = findViewById(R.id.setButton)
        stopButton = findViewById(R.id.stopButton)
        statusView = findViewById(R.id.statusView)
        hideSwitch = findViewById(R.id.hideSwitch)

        // Load saved values
        latInput.setText(prefs.getString("lat", "28.7041"))
        longInput.setText(prefs.getString("long", "77.1025"))
        accuracyInput.setText(prefs.getFloat("accuracy", 5f).toString())

        // Request Permissions
        requestPermissions()

        // UI Listeners
        setButton.setOnClickListener { onSetLocation() }
        stopButton.setOnClickListener { onStopLocation() }

        // Check status
        checkMockLocationStatus()
        updateServiceStatus()
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_NETWORK_STATE
        )

        val notGranted = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted, PERMISSION_CODE)
        }
    }

    private fun onSetLocation() {
        val lat = latInput.text.toString().toDoubleOrNull()
        val long = longInput.text.toString().toDoubleOrNull()
        val accuracy = accuracyInput.text.toString().toFloatOrNull() ?: 5f

        if (lat == null || long == null) {
            Toast.makeText(this, "Invalid coordinates!", Toast.LENGTH_SHORT).show()
            return
        }

        if (lat !in -90.0..90.0 || long !in -180.0..180.0) {
            Toast.makeText(this, "Coordinates out of range!", Toast.LENGTH_SHORT).show()
            return
        }

        // Save
        prefs.edit().apply {
            putString("lat", lat.toString())
            putString("long", long.toString())
            putFloat("accuracy", accuracy)
            apply()
        }

        // Start Service
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("latitude", lat)
            putExtra("longitude", long)
            putExtra("accuracy", accuracy)
            putExtra("hide", hideSwitch.isChecked)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        serviceRunning = true
        updateServiceStatus()
        Toast.makeText(this, "✓ Location Spoofing Started!", Toast.LENGTH_SHORT).show()
    }

    private fun onStopLocation() {
        stopService(Intent(this, LocationService::class.java))
        serviceRunning = false
        updateServiceStatus()
        Toast.makeText(this, "✗ Location Spoofing Stopped!", Toast.LENGTH_SHORT).show()
    }

    private fun updateServiceStatus() {
        GlobalScope.launch(Dispatchers.Main) {
            if (serviceRunning) {
                statusView.text = "✓ ACTIVE\nLat: ${latInput.text}\nLong: ${longInput.text}"
                statusView.setBackgroundColor(getColor(android.R.color.holo_green_light))
                setButton.isEnabled = false
            } else {
                statusView.text = "✗ INACTIVE"
                statusView.setBackgroundColor(getColor(android.R.color.holo_red_light))
                setButton.isEnabled = true
            }
        }
    }

    private fun checkMockLocationStatus() {
        val status = buildString {
            append("🌍 Ready to Spoof\n")
            append("No Developer Mode Needed!\n")
            append("Auto-enabling providers...")
        }

        statusView.text = status
        statusView.setBackgroundColor(getColor(android.R.color.holo_blue_light))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty()) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "✓ Permissions Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "✗ Permissions Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}