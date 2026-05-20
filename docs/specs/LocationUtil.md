# LocationUtil - Device Location and Geocoding Operations

## Overview

`LocationUtil` is a utility class for all location-related operations in the weather application. It
handles device location retrieval via FusedLocationProviderClient and reverse geocoding to determine
city, region, and country information.

**Location:** `data/repository/utils/LocationUtil.kt`

**Pattern:** Kotlin `class` with instance-based dependency injection (via Hilt)

---

## Responsibilities

1. ✅ **Device Location Retrieval** - Get current device coordinates
2. ✅ **Reverse Geocoding** - Convert coordinates to city/region/country
3. ✅ **Location Convenience Methods** - Combined operations for ease of use
4. ✅ **Error Handling** - Manage permissions, network issues, and unavailability

---

## Dependencies

### Google Play Services Location

```gradle
implementation("com.google.android.gms:play-services-location:21.1.0")
```

### Android Permissions

The app requires location permissions in `AndroidManifest.xml`:

```xml

<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /><uses-permission
android:name="android.permission.ACCESS_FINE_LOCATION" />
```

**Note:** Runtime permissions must be requested at app startup (handled by Activity)

---

## API Methods

### `getCurrentLocation(): Pair<Double, Double>?`

Retrieves the device's current geographic coordinates using FusedLocationProviderClient.

**Returns:**

- `Pair(latitude, longitude)` if location obtained successfully
- `null` if location cannot be determined

**Example:**

```kotlin
val location = locationUtil.getCurrentLocation()
if (location != null) {
    val (latitude, longitude) = location
    println("Device location: $latitude°N, $longitude°E")
    // Result: "Device location: 40.7128°N, -74.0060°E" (New York)
} else {
    println("Location unavailable")
}
```

**Error Handling:**

- **SecurityException**: Thrown if location permissions not granted
    - Logged: "SecurityException: Missing location permissions"
    - App should show fallback error screen

- **Location unavailable**: Returns `null`
    - Occurs when GPS/network is disabled
    - Logged: "No location available"

**Implementation Details:**

```
FusedLocationProviderClient.getCurrentLocation(
    Priority.PRIORITY_BALANCED_POWER_ACCURACY,  // Power-efficient but accurate enough
    null                                         // No timeout enforcement
)
```

**⚠️ IMPORTANT:** Permission checks must be performed by the caller (Activity/ViewModel) before
invoking this method.

**Use cases:**

- Getting user's current city for weather display
- Used internally by `getCurrentLocationInfo()`
- Starting point for location-based features

---

### `getCityFromCoordinates(latitude: Double, longitude: Double): LocationInfo?`

Converts geographic coordinates to city, region, and country using Android's Geocoder API.

**Parameters:**

- `latitude`: Latitude coordinate (decimal degrees)
- `longitude`: Longitude coordinate (decimal degrees)

**Returns:**

- `LocationInfo` DTO containing city, region, country, latitude, longitude
- `null` if unable to determine location

**LocationInfo DTO:**

```kotlin
data class LocationInfo(
    val city: String,           // City name (e.g., "New York")
    val region: String,         // State/Province (e.g., "NY")
    val country: String,        // Country name (e.g., "United States")
    val latitude: Double,       // Original latitude
    val longitude: Double       // Original longitude
)
```

**Example:**

```kotlin
val locationInfo = locationUtil.getCityFromCoordinates(40.7128, -74.0060)
if (locationInfo != null) {
    println("${locationInfo.city}, ${locationInfo.region}, ${locationInfo.country}")
    // Output: "New York, NY, United States"
} else {
    println("Geocoding failed")
}
```

**Reverse Geocoding Logic:**

```
Android Geocoder → getFromLocation(lat, lon, 1)
                ↓
        Returns first Address
                ↓
        Extracts: locality → subAdminArea → adminArea (for city)
                  adminArea (for region)
                  countryName (for country)
```

**Error Handling:**

- **SecurityException**: Permission denied
    - Logged: "SecurityException: Missing location permissions for geocoding"
    - Returns `null`

- **No address found**: Invalid coordinates or remote area
    - Logged: "No address found for coordinates: $lat, $lon"
    - Returns `null`

- **Geocoder unavailable**:
    - Logged: "Error geocoding coordinates: ${exception.message}"
    - Returns `null` gracefully

- **Network errors**: No internet connection
    - Geocoder cached previously (if available)
    - Returns `null` if no cache

**Fallback Chain for City Name:**

1. `address.locality` - Primary (e.g., Manhattan)
2. `address.subAdminArea` - Secondary (e.g., Kings County)
3. `address.adminArea` - Tertiary (e.g., New York State)

**Locale:** Always uses `Locale.ENGLISH` for consistency across all devices.

**Performance:**

- Fully blocking operation (IO-bound)
- Wrapped with `withContext(Dispatchers.IO)` in calling code
- Allows timeouts and cancellation via coroutine scope

**Use cases:**

- Converting device coordinates to city name for weather lookup
- Determining user's current location
- Used internally by `getCurrentLocationInfo()`

---

### `getCurrentLocationInfo(): LocationInfo?`

**Convenience method** that combines `getCurrentLocation()` and `getCityFromCoordinates()`.

Returns complete location information (city, region, country, coordinates) in a single call.

**Returns:**

- `LocationInfo` if location successfully obtained and geocoded
- `null` if location cannot be obtained or geocoding fails

**Example:**

```kotlin
val locationInfo = locationUtil.getCurrentLocationInfo()
if (locationInfo != null) {
    println("Current location: ${locationInfo.city}, ${locationInfo.country}")
    println("Coordinates: ${locationInfo.latitude}°, ${locationInfo.longitude}°")
    // Output:
    // Current location: New York, United States
    // Coordinates: 40.7128°, -74.0060°
} else {
    println("Unable to determine location")
}
```

**Error Handling:**

- Combines errors from both methods
- Returns `null` on any failure
- Individual errors logged for debugging

**Use cases:**

- **PRIMARY USE CASE**: ViewModel initialization
- Getting all location data at app startup
- Recommended over calling individual methods separately

---

## Dependency Injection Setup (Hilt)

### LocationModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationUtil(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): LocationUtil = LocationUtil(context, fusedLocationClient)

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}
```

### ViewModel Injection

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val locationUtil: LocationUtil,
    private val repository: IRepositoryRegistroClima
) : ViewModel() {
    // LocationUtil automatically provided by Hilt
}
```

---

## Usage Flow in ViewModel

```kotlin
class WeatherViewModel @Inject constructor(
    private val locationUtil: LocationUtil,
    private val repository: IRepositoryRegistroClima
) : ViewModel() {

    private val _weather = MutableStateFlow<WeatherRecordDto?>(null)
    val weather: StateFlow<WeatherRecordDto?> = _weather.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        getCurrentLocationAndWeather()
    }

    private fun getCurrentLocationAndWeather() {
        viewModelScope.launch {
            try {
                // ✅ Step 1: Get current location info
                val locationInfo = locationUtil.getCurrentLocationInfo()
                    ?: run {
                        _error.value = "The location couldn't be obtained to get weather info"
                        return@launch
                    }

                // ✅ Step 2: Pass to repository for weather fetch
                val today = TimeUtil.getCurrentUtcDate()
                val weather = repository.obtainOrFetchHistoricalWeather(
                    date = today,
                    city = locationInfo.city,
                    region = locationInfo.region,
                    country = locationInfo.country
                )

                if (weather != null) {
                    _weather.value = weather
                } else {
                    _error.value = "The weather couldn't be obtained"
                }
            } catch (exception: Exception) {
                _error.value = "Error: ${exception.message}"
                Log.e("WeatherViewModel", exception.message ?: "Unknown error", exception)
            }
        }
    }
}
```

---

## Error Scenarios and Handling

### Scenario 1: Permissions Not Granted

```
SecurityException thrown
↓
ViewModel catches exception
↓
UI shows: "The location couldn't be obtained to get weather info"
↓
User needs to grant permissions in app settings
```

### Scenario 2: Location Services Disabled (GPS/Network)

```
getCurrentLocation() returns null
↓
ViewModel receives null
↓
UI shows: "The location couldn't be obtained to get weather info"
↓
User needs to enable location services
```

### Scenario 3: Geocoding Fails (Remote Area)

```
getCityFromCoordinates() returns null
↓
getCurrentLocationInfo() returns null
↓
ViewModel receives null
↓
UI shows: "The location couldn't be obtained to get weather info"
↓
App cannot determine city name from coordinates
```

### Scenario 4: Network Unavailable During Geocoding

```
Geocoder attempts API call
↓
Network error
↓
getCityFromCoordinates() returns null
↓
UI shows: "The location couldn't be obtained to get weather info"
```

---

## Logging Examples

```
// Successful location
D/LocationUtil: Geocoded location - City: New York, Country: United States, Admin: NY

// Permission error
E/LocationUtil: SecurityException: Missing location permissions for geocoding

// No address found
D/LocationUtil: No address found for coordinates: 0.0000, 0.0000

// Geocoding error
E/LocationUtil: Error geocoding coordinates (40.7128, -74.0060): Network error
```

---

## Best Practices

1. **Always request permissions before using**: Location operations require runtime permissions
2. **Handle all null returns**: Gracefully degrade when location unavailable
3. **Use getCurrentLocationInfo()**: Prefer combined method over individual calls
4. **One-time call at startup**: Get location once in ViewModel init, reuse the result
5. **Show error screens**: Implement fallback UI for error scenarios
6. **Log comprehensively**: Helps debugging permission and network issues

---

## Future Enhancements

1. **[Future] Background Location Updates** - Location change detection
2. **[Future] Location History** - Track user's location over time
3. **[Future] Multiple Saved Locations** - Remember favorite cities
4. **[Future] Latitude/Longitude Caching** - Avoid multiple geocoding calls
5. **[Future] Location Permissions UI** - In-app permission dialog flow

---

## Post Scriptum

- ✅ Uses FusedLocationProviderClient (recommended by Google)
- ✅ ACCESS_COARSE_LOCATION sufficient for city-level accuracy
- ✅ Reverse geocoding with Android Geocoder (no API key needed)
- ✅ English locale ensures consistency across devices
- ✅ Comprehensive error handling with null safety
- ✅ Ready for Hilt dependency injection
- ✅ Fully asynchronous with proper IO dispatcher handling

