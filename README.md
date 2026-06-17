# 🌍 GPS Location Spoofer - No Developer Mode Required

## ✨ Features

✅ **Works WITHOUT Developer Mode Enabled**  
✅ Spoof GPS location in real-time  
✅ Support for Multiple Location Providers (GPS, Network, Passive)  
✅ Hide Mock Location Detection  
✅ Automatic Mock Provider Setup  
✅ Foreground Service (continuous spoofing)  
✅ Save & Load Previous Coordinates  

---

## 🔧 Technical Improvements

### Previous Version Issues:
- ❌ Required Developer Mode to be enabled
- ❌ Manual mock location permission setup needed
- ❌ Limited compatibility with older Android versions

### New Version Solutions:
- ✅ **Automatic Provider Enablement** - No manual setup required
- ✅ **Reflection-based Injection** - Works even if standard methods fail
- ✅ **Multi-version Support** - Android 7.0+ compatibility
- ✅ **Improved Error Handling** - Fallback mechanisms

---

## 📱 Installation

### Requirements:
- Android 7.0+ (API 24+)
- Minimum 50MB free storage
- Location permissions granted

### Setup Steps:

1. **Clone Repository**
   ```bash
   git clone https://github.com/krishnaagri/gps-location.git
   cd gps-location
   ```

2. **Build APK**
   ```bash
   ./gradlew build
   ```

3. **Install APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Grant Permissions (One-time)**
   ```bash
   adb shell pm grant com.spoofgps android.permission.ACCESS_FINE_LOCATION
   adb shell pm grant com.spoofgps android.permission.ACCESS_COARSE_LOCATION
   ```

5. **Launch App** ✅
   - No need to enable Developer Mode!
   - No need to enable Mock Locations in settings!
   - Click "SET LOCATION" and enjoy!

---

## 📋 How It Works

```
User enters coordinates
         ↓
  LocationService starts
         ↓
  Automatically enables mock providers
         ↓
  Injects location every 2 seconds
         ↓
  Apps receive spoofed coordinates ✓
```

---

## 🎯 Key Code Changes

### LocationService.kt
```kotlin
private fun enableMockProviders() {
    for (provider in providers) {
        locationManager.addTestProvider(...) // Auto-enable
        locationManager.setTestProviderEnabled(provider, true)
    }
}

private fun injectViaReflection(...) {
    // Fallback method using reflection
    val method = locationManager.javaClass.getMethod(...)
    method.invoke(locationManager, provider, location)
}
```

### AndroidManifest.xml
```xml
<!-- Removed -->  
<!-- <uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" /> -->

<!-- Added -->
<uses-permission android:name="com.android.permission.MOCK_LOCATION" />
```

---

## 🔐 Security Notes

⚠️ **Disclaimer:** This app is for testing and educational purposes only.

- ✓ No data collection
- ✓ Open source (transparency)
- ✓ Works locally (no server needed)
- ⚠️ May violate app ToS that forbid spoofing
- ⚠️ Not for malicious purposes

---

## 🐛 Troubleshooting

### Location not changing?
1. Ensure permissions are granted
2. Check logcat: `adb logcat | grep LocationService`
3. Restart the app
4. Try different coordinates

### App crashes on startup?
1. Update to latest Android version
2. Clear app cache: `adb shell pm clear com.spoofgps`
3. Reinstall APK

### Works on some apps but not others?
- Some apps detect mock locations and ignore them
- Try enabling "Hide Mock Detection" toggle
- Different apps have different location APIs

---

## 📚 Coordinates for Testing

| Location | Latitude | Longitude |
|----------|----------|-----------|
| New Delhi, India | 28.7041 | 77.1025 |
| Mumbai, India | 19.0760 | 72.8777 |
| New York, USA | 40.7128 | -74.0060 |
| London, UK | 51.5074 | -0.1278 |
| Tokyo, Japan | 35.6762 | 139.6503 |

---

## 📝 License

Open Source - Free to use and modify

---

## 👨‍💻 Author

**krishnaagri** - [@krishnaagri](https://github.com/krishnaagri)

---

## 🌟 Star this repo if it helped you!