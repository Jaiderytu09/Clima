package com.example.clima_v100.data.repository.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for time-related operations.
 * Handles UTC timestamp generation and timezone conversions.
 */
class TimeUtil {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

        /**
         * Gets the current UTC timestamp in YYYY-MM-DD format.
         * @return Current date in YYYY-MM-DD format (UTC)
         */
        fun getCurrentUtcDate(): String {
            val utcFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
                .withZone(ZoneId.of("UTC"))
            return Instant.now().atZone(ZoneId.of("UTC")).format(utcFormatter)
        }

        /**
         * Gets the current UTC timestamp in YYYY-MM-DD HH:mm:ss format.
         * @return Current datetime in YYYY-MM-DD HH:mm:ss format (UTC)
         */
        fun getCurrentUtcDateTime(): String {
            val utcFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT)
                .withZone(ZoneId.of("UTC"))
            return Instant.now().atZone(ZoneId.of("UTC")).format(utcFormatter)
        }

        /**
         * Extracts the date part (YYYY-MM-DD) from a UTC timestamp.
         * @param utcDateTime UTC datetime string (format: YYYY-MM-DD HH:mm:ss or similar)
         * @return Date string in YYYY-MM-DD format
         */
        fun extractDateFromUtcDateTime(utcDateTime: String): String {
            return utcDateTime.substring(0, 10)
        }

        /**
         * Converts a UTC date/time to a local timezone based on the location.
         * @param utcDateTime UTC datetime string (format: YYYY-MM-DD HH:mm:ss)
         * @param timezone Timezone ID (e.g., "America/New_York", "Europe/London")
         * @return Converted datetime in the specified timezone
         */
        fun convertUtcToLocalTimeZone(utcDateTime: String, timezone: String): String {
            return try {
                val formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT)
                val parsedDateTime = LocalDateTime.parse(utcDateTime, formatter)
                val utcZonedDateTime = parsedDateTime.atZone(ZoneId.of("UTC"))
                val localZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.of(timezone))
                localZonedDateTime.format(formatter)
            } catch (exception: Exception) {
                // In case of parsing error, return the original datetime
                utcDateTime
            }
        }

        /**
         * Validates if a given date string is in the past or today (not future).
         * @param dateString Date string in YYYY-MM-DD format
         * @return True if the date is today or in the past, False if it's in the future
         */
        fun isValidHistoricalDate(dateString: String): Boolean {
            return try {
                val providedDate = LocalDateTime.parse(
                    "$dateString 00:00:00",
                    DateTimeFormatter.ofPattern(DATETIME_FORMAT)
                )
                    .toLocalDate()
                val todayUtc = Instant.now().atZone(ZoneId.of("UTC")).toLocalDate()
                !providedDate.isAfter(todayUtc)
            } catch (exception: Exception) {
                false
            }
        }
    }
}