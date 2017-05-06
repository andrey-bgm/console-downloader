package com.example.consoledownloader.downloader;

import com.example.consoledownloader.argsparser.Options;
import com.example.consoledownloader.utils.RateLimitedInputStream;
import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Downloader {
    private static final String LINK_LINE_DELIMITER = " ";
    private final LinkDownloader linkDownloader;

    public Downloader(LinkDownloader linkDownloader) {
        this.linkDownloader = linkDownloader;
    }

    public Result download(Options options) {
        boolean outputDirIsNeeded = !options.getOutputDir().isEmpty();
        if (outputDirIsNeeded) {
            try {
                Files.createDirectories(Paths.get(options.getOutputDir()));
            } catch (IOException e) {
                return makeFailedResult("Cannot create the output directory: %s", e);
            }
        }

        Map<String, List<String>> linksMap;
        try {
            linksMap = readLinksToMap(options);
        } catch (IOException e) {
            return makeFailedResult("Cannot read the file with links: %s", e);
        }

        return download(linksMap, options);
    }

    private Result download(Map<String, List<String>> linksMap, Options options) {
        ExecutorService pool = Executors.newFixedThreadPool(options.getThreadNumber());
        List<Callable<Result>> tasks = transformLinksMapToDownloadTasks(linksMap, options);

        Result result;

        try {
            result = pool.invokeAll(tasks).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        DownloaderLogRecord logRecord = DownloaderLogRecord.create(
                            DownloaderLogRecord.Type.SYSTEM_ERROR, e.getMessage());
                        return new Result(0L, Collections.singletonList(logRecord));
                    }
                })
                .collect(Collector.of(Result::new, Result::addResult, Result::addResult));
        } catch (InterruptedException e) {
            result = makeFailedResult("Download process failed: %s", e);
        }

        pool.shutdown();

        return result;
    }

    private List<Callable<Result>> transformLinksMapToDownloadTasks(Map<String, List<String>> linksMap,
                                                                    Options options) {
        Function<InputStream, InputStream> inputWrapper = makeInputWrapper(options);

        return linksMap.keySet().stream()
            .collect(
                ArrayList::new,
                (tasks, link) -> {
                    List<String> fileNames = linksMap.get(link);
                    Callable<Result> task = makeDownloadTask(link, fileNames, options, inputWrapper);
                    tasks.add(task);
                },
                ArrayList::addAll);
    }

    private Function<InputStream, InputStream> makeInputWrapper(Options options) {
        if (options.getLimit() > 0) {
            RateLimiter rateLimiter = RateLimiter.create(options.getLimit());

            return in -> new RateLimitedInputStream(in, rateLimiter);
        } else {
            return Function.identity();
        }
    }

    private Callable<Result> makeDownloadTask(String link, List<String> fileNames, Options options,
                                              Function<InputStream, InputStream> inputWrapper) {
        return () -> {
            String firstFileName = fileNames.get(0);
            Path firstPath = makeFilePath(firstFileName, options);

            DownloadLinkResult downloadResult = downloadLink(link, firstFileName, firstPath, inputWrapper);

            if (!downloadResult.success) {
                return new Result(0L, downloadResult.log);
            }

            List<DownloaderLogRecord> copyLog = copyRestFilesFromFirst(fileNames, options,
                firstFileName, firstPath);

            List<DownloaderLogRecord> log = Stream.concat(downloadResult.log.stream(), copyLog.stream())
                .collect(Collectors.toList());

            return new Result(downloadResult.byteCount, log);
        };
    }

    private DownloadLinkResult downloadLink(String link, String dest, Path path,
                                            Function<InputStream, InputStream> inputWrapper) {
        try (InputStream input = inputWrapper.apply(this.linkDownloader.download(link))) {
            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            long byteCount = Files.size(path);
            DownloaderLogRecord logRecord = DownloaderLogRecord.create(DownloaderLogRecord.Type.DOWNLOAD_SUCCESS,
                String.format("%s", dest));

            return new DownloadLinkResult(true, byteCount, logRecord);
        } catch (IOException e) {
            DownloaderLogRecord logRecord = DownloaderLogRecord.create(DownloaderLogRecord.Type.DOWNLOAD_FAIL,
                String.format("%s: %s", dest, e.getMessage()));

            return new DownloadLinkResult(false, 0L, logRecord);
        }
    }

    private ArrayList<DownloaderLogRecord> copyRestFilesFromFirst(List<String> fileNames, Options options,
                                                                  String firstFileName, Path firstPath) {
        ArrayList<DownloaderLogRecord> log = new ArrayList<>();

        for (int i = 1; i < fileNames.size(); i++) {
            String dest = fileNames.get(i);
            Path destPath = makeFilePath(dest, options);

            try {
                Files.copy(firstPath, destPath);
                log.add(DownloaderLogRecord.create(DownloaderLogRecord.Type.DOWNLOAD_SUCCESS,
                    String.format("%s copy from %s", dest, firstFileName)));
            } catch (IOException e) {
                log.add(DownloaderLogRecord.create(DownloaderLogRecord.Type.DOWNLOAD_FAIL,
                    String.format("%s copy from %s: %s", dest, firstFileName, e.getMessage())));
            }
        }

        return log;
    }

    private static Path makeFilePath(String fileName, Options options) {
        return Paths.get(options.getOutputDir(), fileName);
    }

    private Result makeFailedResult(String msgTemplate, Exception e) {
        DownloaderLogRecord logRecord = DownloaderLogRecord.create(
            DownloaderLogRecord.Type.SYSTEM_ERROR,
            String.format(msgTemplate, e.getMessage()));

        return new Result(0, Collections.singletonList(logRecord));
    }

    private Map<String, List<String>> readLinksToMap(Options options) throws IOException {
        try (Stream<String> readerStream = Files.lines(Paths.get(options.getLinksFile()))) {
            return readerStream
                .map(line -> {
                    String[] parts = line.split(LINK_LINE_DELIMITER);
                    return new LinkDescription(parts[0], parts[1]);
                })
                .collect(Collectors.groupingBy(
                    LinkDescription::getLink,
                    Collectors.mapping(LinkDescription::getDest, Collectors.toList())));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static class Result {
        private long byteCount;
        private final List<DownloaderLogRecord> log;

        private Result() {
            this(0L, new ArrayList<>());
        }

        private Result(long byteCount, List<DownloaderLogRecord> log) {
            this.byteCount = byteCount;
            this.log = log;
        }

        public long getByteCount() {
            return byteCount;
        }

        public List<DownloaderLogRecord> getLog() {
            return log;
        }

        private Result addResult(Result another) {
            this.byteCount += another.byteCount;
            this.log.addAll(another.log);

            return this;
        }
    }

    private static class LinkDescription {
        final String link;
        final String dest;

        LinkDescription(String link, String dest) {
            this.link = link;
            this.dest = dest;
        }

        String getLink() {
            return link;
        }

        String getDest() {
            return dest;
        }
    }

    static class DownloadLinkResult {
        final long byteCount;
        final List<DownloaderLogRecord> log;
        final boolean success;

        DownloadLinkResult(boolean success, long byteCount, DownloaderLogRecord logRecord) {
            this(success, byteCount, Collections.singletonList(logRecord));
        }

        DownloadLinkResult(boolean success, long byteCount, List<DownloaderLogRecord> log) {
            this.success = success;
            this.byteCount = byteCount;
            this.log = log;
        }
    }
}
