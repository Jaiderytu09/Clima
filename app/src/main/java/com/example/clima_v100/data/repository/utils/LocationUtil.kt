package com.example.clima_v100.data.repository.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.clima_v100.data.local.dto.LocationInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility class for location-related operations.
 * Handles retrieving the current location of the device and reverse geocoding.
 */
class LocationUtil(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    companion object {
        private const val TAG = "LocationUtil"
    }

    /**
     * Gets the current location of the device.
     * Uses FusedLocationProviderClient with ACCESS_COARSE_LOCATION for approximate location.
     * Note: Permission checks must be performed by the caller before invoking this method.
     * @return Pair of (latitude, longitude) or null if location cannot be obtained
     * @throws SecurityException if location permissions are not granted
     * @throws Exception if location retrieval fails
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            } catch (securityException: SecurityException) {
                continuation.resumeWithException(securityException)
            }
        }
    }

    /**
     * Gets location information (city, region, country) based on latitude and longitude.
     * Uses Android's Geocoder with reverse geocoding.
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return LocationInfo containing city, region, country, and coordinates, or null if it cannot be determined
     */
    suspend fun getCityFromCoordinates(latitude: Double, longitude: Double): LocationInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to geocode coordinates: lat=$latitude, lon=$longitude")

                val geocoder = Geocoder(context, Locale.ENGLISH)
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses.isNullOrEmpty()) {
                    Log.d(TAG, "No address found for coordinates: $latitude, $longitude")
                    return@withContext null
                }

                val address = addresses.first()

                // Extract location information with fallbacks
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val region = address.adminArea ?: ""
                val country = address.countryName ?: ""

                if (city.isEmpty()) {
                    Log.w(TAG, "Could not determine city name from coordinates")
                    return@withContext null
                }

                if (region.isEmpty()) {
                    Log.w(TAG, "Region name not found for coordinates: $latitude, $longitude")
                    return@withContext null
                }

                if (country.isEmpty()) {
                    Log.w(TAG, "Country name not found for coordinates: $latitude, $longitude")
                    return@withContext null
                }

                val locationInfo = LocationInfo(
                    city = city,
                    region = region,
                    country = country,
                    latitude = latitude,
                    longitude = longitude
                )

                // Log for debugging
                Log.d(TAG, "Geocoded location - City: $city, Region: $region, Country: $country")

                return@withContext locationInfo

            } catch (securityException: SecurityException) {
                // Handle missing permissions
                Log.e(
                    TAG,
                    "SecurityException: Missing location permissions for geocoding",
                    securityException
                )
                return@withContext null
            } catch (exception: Exception) {
                // Handle other errors (no internet, geocoder not available, etc.)
                Log.e(
                    TAG,
                    "Error geocoding coordinates ($latitude, $longitude): ${exception.message}",
                    exception
                )
                return@withContext null
            }
        }
    }

    /**
     * Gets the current location and geocodes it to get city, region, and country information.
     * Combines getCurrentLocation() and getCityFromCoordinates().
     * @return LocationInfo if successful, null if location or geocoding fails
     * @throws Exception if location retrieval fails with permission issues
     */
    suspend fun getCurrentLocationInfo(): LocationInfo? {
        return try {
            val location = getCurrentLocation()
            if (location != null) {
                getCityFromCoordinates(location.first, location.second)
            } else {
                Log.w(TAG, "Could not get current location")
                null
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error getting current location info: ${exception.message}", exception)
            throw exception
        }
    }
}
