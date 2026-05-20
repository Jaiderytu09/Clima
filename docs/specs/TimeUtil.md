# TimeUtil - Time Utility Operations

## Overview

`TimeUtil` is a utility class for all time-related operations in the weather application. It handles
UTC timestamp generation, timezone conversions, and historical date validation for weather records.

**Location:** `data/repository/utils/TimeUtil.kt`

**Pattern:** Kotlin `class` with `companion object` (static methods)

---

## Responsibilities

1. ✅ **UTC Timestamp Generation** - Get current date/time in UTC
2. ✅ **Timezone Conversion** - Convert UTC time to local timezones
3. ✅ **Date Extraction** - Extract date portion from datetime strings
4. ✅ **Historical Date Validation** - Ensure dates are not in the future

---

## API Methods

### `getCurrentUtcDate(): String`

Returns the current date in UTC timezone.

**Format:** `YYYY-MM-DD`

**Example:**

```kotlin
val today = TimeUtil.getCurrentUtcDate()
// Result: "2026-04-27"
```

**Use cases:**

- Getting today's date for weather record lookup
- Creating new weather records for current date
- Validation reference point for historical dates

---

### `getCurrentUtcDateTime(): String`

Returns the current date and time in UTC timezone.

**Format:** `YYYY-MM-DD HH:mm:ss`

**Example:**

```kotlin
val now = TimeUtil.getCurrentUtcDateTime()
// Result: "2026-04-27 14:30:45"
```

**Use cases:**

- Logging timestamps with millisecond precision
- Recording exact moment of API fetch
- Audit trails

---

### `extractDateFromUtcDateTime(utcDateTime: String): String`

Extracts the date portion from a UTC datetime string.

**Parameters:**

- `utcDateTime`: DateTime string in format `YYYY-MM-DD HH:mm:ss` or `YYYY-MM-DD`

**Returns:** Date in `YYYY-MM-DD` format

**Example:**

```kotlin
val dateOnly = TimeUtil.extractDateFromUtcDateTime("2026-04-27 14:30:45")
// Result: "2026-04-27"
```

**Use cases:**

- Processing API responses (WeatherAPI returns both date and time)
- Standardizing dates for database storage
- Converting timestamps to database date format

---

### `convertUtcToLocalTimeZone(utcDateTime: String, timezone: String): String`

Converts a UTC datetime to any local timezone.

**Parameters:**

- `utcDateTime`: UTC datetime string in format `YYYY-MM-DD HH:mm:ss`
- `timezone`: Timezone ID (e.g., `"America/New_York"`, `"Europe/London"`, `"Asia/Tokyo"`)

**Returns:** Converted datetime in the specified timezone

**Example:**

```kotlin
val nyTime = TimeUtil.convertUtcToLocalTimeZone(
    "2026-04-27 14:30:45",
    "America/New_York"
)
// Result: "2026-04-27 10:30:45" (EDT = UTC-4)

val tokyoTime = TimeUtil.convertUtcToLocalTimeZone(
    "2026-04-27 14:30:45",
    "Asia/Tokyo"
)
// Result: "2026-04-27 23:30:45" (JST = UTC+9)
```

**Error Handling:**

- Returns original datetime if timezone parsing fails
- Logs error for debugging
- Graceful degradation (doesn't crash)

**Common Timezone IDs:**

```
UTC: "UTC"
US Eastern: "America/New_York"
US Central: "America/Chicago"
US Mountain: "America/Denver"
US Pacific: "America/Los_Angeles"
Europe London: "Europe/London"
Europe Paris: "Europe/Paris"
Europe Tokyo: "Asia/Tokyo"
Australia Sydney: "Australia/Sydney"
```

**Use cases:**

- Displaying weather times in user's local timezone
- Adjusting weather data for user's location
- Future timezone-aware features

---

### `isValidHistoricalDate(dateString: String): Boolean`

Validates whether a given date is in the past or today (not in the future).

**Parameters:**

- `dateString`: Date string in format `YYYY-MM-DD`

**Returns:**

- `true` if date is today or in the past ✅
- `false` if date is in the future ❌

**Example:**

```kotlin
val today = "2026-04-27"
TimeUtil.isValidHistoricalDate(today)  // Result: true ✅

val tomorrow = "2026-04-28"
TimeUtil.isValidHistoricalDate(tomorrow)  // Result: false ❌

val yesterday = "2026-04-26"
TimeUtil.isValidHistoricalDate(yesterday)  // Result: true ✅
```

**Error Handling:**

- Returns `false` if date format is invalid (logs error)
- Returns `false` if parsing fails
- Safe null handling

**Use cases:**

- **CRITICAL:** Validating historical weather queries
- Preventing API calls for future dates
- Validating user-provided dates in UI
- Database integrity checks

**⚠️ IMPORTANT CONSTRAINT:**
All methods in repository that fetch historical weather use this validation:

- `fetchHistoricalWeatherFromApi()` throws `IllegalArgumentException` if date is future
- `obtainOrFetchHistoricalWeather()` throws `IllegalArgumentException` if date is future
- This ensures the app CANNOT request weather for future dates from WeatherAPI

---

## Implementation Details

### Dependency: java.time API

- Uses `java.time.LocalDateTime`, `java.time.ZoneId`, `java.time.Instant`
- Requires core library desugaring for API levels below 26
- Build configuration enables desugaring automatically

### Thread Safety

- All methods are thread-safe (no mutable state)
- Can be called from any coroutine context

### Coroutine Usage

- All methods are synchronous (no suspend keyword)
- Safe to call from `viewModelScope` or `lifecycleScope`
- No blocking IO operations

---

## Example: Complete Usage Flow

```kotlin
// Repository Method Example
class RepositoryRegistroClima : IRepositoryRegistroClima {

    override suspend fun obtainOrFetchHistoricalWeather(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto? {
        // ✅ Step 1: Validate date is not future
        if (!TimeUtil.isValidHistoricalDate(date)) {
            throw IllegalArgumentException("Cannot fetch weather for future dates")
        }

        // ✅ Step 2: Query database or API
        // ... repository logic ...

        // ✅ Step 3: Use date in API call
        val apiResponse = weatherApiService.getHistoricalWeather(
            apiKey = BuildConfig.API_KEY,
            query = city,
            date = date  // Now guaranteed to be valid
        )

        // ✅ Step 4: Extract date from API response
        val forecastDay = apiResponse.forecast.forecastday.first()
        val databaseDate = TimeUtil.extractDateFromUtcDateTime(forecastDay.date)

        return WeatherRecordDto(
            date = databaseDate,
            // ... other fields ...
        )
    }
}
```

---

## Future Enhancements

1. **[Future] Daylight Saving Time Handling** - Auto-adjust for DST changes
2. **[Future] Time Range Queries** - Get date range for weather reports
3. **[Future] Recurring Dates** - Support for weekly/monthly patterns
4. **[Future] Locale-Aware Formatting** - Return dates in user's locale format

---

## Testing

### Unit Tests Coverage

```kotlin
// Valid dates
TimeUtil.isValidHistoricalDate("2026-04-26")  // ✅ Today-1
TimeUtil.isValidHistoricalDate("2026-04-27")  // ✅ Today
TimeUtil.isValidHistoricalDate("2020-01-01")  // ✅ Old date

// Invalid dates
TimeUtil.isValidHistoricalDate("2026-04-28")  // ❌ Tomorrow
TimeUtil.isValidHistoricalDate("2030-01-01")  // ❌ Future
TimeUtil.isValidHistoricalDate("invalid")     // ❌ Bad format

// Timezone conversions
val utc = "2026-04-27 14:30:00"
TimeUtil.convertUtcToLocalTimeZone(utc, "America/New_York")
// Expected: "2026-04-27 10:30:00"
```

---

## Post Scriptum

- ✅ All methods use UTC as reference for consistency
- ✅ Future date validation is **mandatory** for all historical weather operations
- ✅ Java 8 time API ensures timezone safety
- ✅ No external time libraries needed (built into Java)
- ✅ Ready for internationalization

