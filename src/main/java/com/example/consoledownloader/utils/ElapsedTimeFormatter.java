package com.example.consoledownloader.utils;

import java.time.Duration;

public class ElapsedTimeFormatter {
    public String format(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() - hours * 60;
        long seconds = duration.getSeconds() - hours * 3600 - minutes * 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }

        return String.format("%ds", seconds);
    }
}
