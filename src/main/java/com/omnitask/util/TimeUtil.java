package com.omnitask.util;

import java.time.LocalTime;

public class TimeUtil {

    private static final LocalTime START_WORK = LocalTime.of(8, 0);
    private static final LocalTime BREAK_START = LocalTime.of(11, 30);
    private static final LocalTime BREAK_END = LocalTime.of(13, 30);
    private static final LocalTime END_WORK = LocalTime.of(16, 0);

    public static String getCurrentWorkPeriod(LocalTime time) {
        if (time.isBefore(START_WORK)) {
            return "Early Morning (Check-In Open)";
        } else if (time.isBefore(BREAK_START)) {
            return "Morning Work Session";
        } else if (time.isBefore(BREAK_END)) {
            return "Lunch Break";
        } else if (time.isBefore(END_WORK)) {
            return "Afternoon Work Session";
        } else {
            return "Overtime / After Hours";
        }
    }
}