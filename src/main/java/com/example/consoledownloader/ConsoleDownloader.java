package com.example.consoledownloader;

import com.example.consoledownloader.argsparser.ArgsParseException;
import com.example.consoledownloader.argsparser.ArgsParser;
import com.example.consoledownloader.argsparser.ArgsParserJCommander;
import com.example.consoledownloader.downloader.DefaultLinkDownloader;
import com.example.consoledownloader.downloader.Downloader;
import com.example.consoledownloader.lib.DownloaderLogRecord;
import com.example.consoledownloader.lib.ElapsedTimeFormatter;
import com.example.consoledownloader.lib.Options;

import java.time.Duration;
import java.time.Instant;

public class ConsoleDownloader {
    public static void main(String... args) {
        Options options;

        try {
            options = createArgsParser().parse(args);
        } catch (ArgsParseException e) {
            System.out.printf("Invalid options: %s%n", e.getMessage());
            System.out.printf("Try 'console-downloader %s' for more information%n", ArgsParser.HELP_LONG);
            return;
        }

        if (options.isHelpNeeded()) {
            System.out.println(createArgsParser().usage());
            return;
        }

        Instant startTime = Instant.now();

        Downloader downloader = new Downloader(new DefaultLinkDownloader());
        Downloader.Result downloadResult = downloader.download(options);

        Duration duration = Duration.between(startTime, Instant.now());

        System.out.printf("Time elapsed: %s | Downloaded: %d bytes%n",
            new ElapsedTimeFormatter().format(duration),
            downloadResult.getByteCount());
        downloadResult.getLog().stream()
            .filter(logRecord -> options.isVerbose() || logRecord.getType() == DownloaderLogRecord.Type.SYSTEM_ERROR)
            .map(logRecord -> String.format("%s %s", logRecord.getType().toPretty(), logRecord.getMessage()))
            .forEach(System.out::println);
    }

    private static ArgsParser createArgsParser() {
        return new ArgsParserJCommander();
    }
}
