package edu.stanford.slac.elog_plus.utility;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtilities {
    static public String toUTCString(LocalTime localTime) {
        // Convert the local time to a ZonedDateTime in the local time zone
        ZonedDateTime localZonedDateTime = localTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault());

        // Convert to the UTC time zone
        ZonedDateTime utcZonedDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));

        // Format the UTC time to a string in "HH:mm" format
        return utcZonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }


    static public LocalTime fromUTCString(String utcString) {
        // Parse the time string to a LocalTime object
        LocalTime utcTime = LocalTime.parse(utcString);

        // Convert to a ZonedDateTime in the UTC time zone
        ZonedDateTime utcZonedDateTime = utcTime.atDate(LocalDate.now(ZoneId.of("UTC"))).atZone(ZoneId.of("UTC"));

        // Convert to the local time zone
        ZonedDateTime localZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.systemDefault());

        // Get the local date (you can also get the local time or other details if needed)
        return localZonedDateTime.toLocalTime();
    }
}
