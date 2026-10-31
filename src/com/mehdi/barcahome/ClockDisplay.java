package com.mehdi.barcahome;
import java.text.SimpleDateFormat;
import java.util.*;

/** Display-only offset. Never writes device time, timezone or Android settings. */
public final class ClockDisplay {
    private ClockDisplay() {}
    public static String format(long epochMillis, TimeZone zone, int offsetMinutes) {
        SimpleDateFormat format=new SimpleDateFormat("HH:mm",Locale.FRANCE);
        format.setTimeZone(zone);
        return format.format(new Date(epochMillis + offsetMinutes * 60_000L));
    }
}
