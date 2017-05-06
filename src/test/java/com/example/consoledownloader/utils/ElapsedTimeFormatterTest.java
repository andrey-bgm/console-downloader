package com.example.consoledownloader.utils;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ElapsedTimeFormatterTest {
    @Test
    public void from32Seconds() throws Exception {
        Duration duration = Duration.ofSeconds(32);
        assertThat(new ElapsedTimeFormatter().format(duration)).isEqualTo("32s");
    }

    @Test
    public void from2Minutes3Seconds() throws Exception {
        Duration duration = Duration.ofSeconds(123);
        assertThat(new ElapsedTimeFormatter().format(duration)).isEqualTo("2m 3s");
    }

    @Test
    public void from26Hours35Minutes42Seconds() throws Exception {
        Duration duration = Duration.ofHours(26).plusMinutes(35).plusSeconds(42);
        assertThat(new ElapsedTimeFormatter().format(duration)).isEqualTo("26h 35m 42s");
    }
}
