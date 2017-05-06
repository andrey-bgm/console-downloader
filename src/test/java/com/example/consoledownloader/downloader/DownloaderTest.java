package com.example.consoledownloader.downloader;

import com.example.consoledownloader.argsparser.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class DownloaderTest {

    private static final String LINK_PREFIX = "http://example.com/";
    private static final String ROOT_DIR_PREFIX = "consoledowloader";
    private static final String DOWNLOAD_DIR_NAME = "download";
    private static final String LINKS_FILE_NAME = "links";
    private static final int TIMEOUT = 10 * 1000;

    private Path rootDirPath;
    private Path downloadDirPath;
    private Path linksFilePath;
    private List<Source> sources;
    private List<LinkDescription> links;
    private LinkDescription wrongLink;

    private Downloader downloader;

    @Before
    public void setUp() throws Exception {
        sources = Arrays.asList(
            new Source("src_file1.txt","hello world\n"),
            new Source("src_file2.txt","line1: word1 word2 123\nline2: word3 word4 word5\n"),
            new Source("src_file3.txt","another file's content\n")
        );
        links = Arrays.asList(
            new LinkDescription("file1.txt", sources.get(0)),
            new LinkDescription("file2.txt", sources.get(1)),
            new LinkDescription("file3.txt", sources.get(2)),
            new LinkDescription("file2_copy1.txt", sources.get(1)),
            new LinkDescription("file3_copy1.txt", sources.get(2)),
            new LinkDescription("file3_copy2.txt", sources.get(2))
        );
        wrongLink = new LinkDescription("file0.txt", new Source("file0.txt", ""));

        rootDirPath = Files.createTempDirectory(ROOT_DIR_PREFIX);
        downloadDirPath = Paths.get(this.rootDirPath.toString(), DOWNLOAD_DIR_NAME);
        linksFilePath = Paths.get(this.rootDirPath.toString(), LINKS_FILE_NAME);

        downloader = new Downloader(link -> getFileContentByLink(sources, link));
    }

    @After
    public void deleteTempDirectory() throws Exception {
        Files.walkFileTree(rootDirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.deleteIfExists(dir);

                    return FileVisitResult.CONTINUE;
                }

                throw exc;
            }
        });
    }

    @Test
    public void downloadEmptyLinks() throws Exception {
        assertSuccessfulDownload(Collections.emptyList(), 1);
    }

    @Test
    public void downloadOneLink() throws Exception {
        LinkDescription linkDescription = links.get(0);
        assertSuccessfulDownload(Collections.singletonList(linkDescription), 1);
    }

    @Test
    public void downloadAllLinks() throws Exception {
        assertSuccessfulDownload(links, 2);
    }

    @Test
    public void downloadOneLinkWhenTargetFileAlreadyExists() throws Exception {
        LinkDescription linkDescription = links.get(0);

        Files.createDirectories(downloadDirPath);
        Path filePath = Paths.get(downloadDirPath.toString(), linkDescription.fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()))) {
            writer.write("preexisted file's content");
            writer.newLine();
            writer.flush();
        }

        assertSuccessfulDownload(Collections.singletonList(linkDescription), 1);
    }

    @Test(timeout = TIMEOUT)
    public void downloadWithOneMegabyteSpeedLimit() throws Exception {
        final int limit = 1024 * 1024;
        final int byteCount = limit * 3;
        final long expectedSeconds = 3L;

        assertSuccessDownloadWithSpeedLimit(limit, byteCount, expectedSeconds);
    }

    @Test(timeout = TIMEOUT)
    public void downloadWithTooSmallSpeedLimit() throws Exception {
        assertSuccessDownloadWithSpeedLimit(1, 10, 0);
    }

    @Test
    public void failedDownloadBecauseThereIsNoLinkFile() throws Exception {
        assertFailedDownloadWithOneError(DownloaderLogRecord.Type.SYSTEM_ERROR);
    }

    @Test
    public void failedDownloadBecauseThereIsFileNamedTargetDir() throws Exception {
        Files.createFile(downloadDirPath);
        assertFailedDownloadWithOneError(DownloaderLogRecord.Type.SYSTEM_ERROR);
    }

    @Test
    public void failedDownloadBecauseWrongLink() throws Exception {
        writeLinksToFile(Collections.singletonList(wrongLink));
        assertFailedDownloadWithOneError(DownloaderLogRecord.Type.DOWNLOAD_FAIL);
    }

    private void assertSuccessfulDownload(List<LinkDescription> links, int threadCount) throws Exception {
        writeLinksToFile(links);

        Options options = makeDefaultOptions().threads(threadCount).build();
        Downloader.Result downloadResult = downloader.download(options);

        assertSuccessfulDownloadResult(links, downloadResult);
    }

    private void assertSuccessDownloadWithSpeedLimit(int limit, int byteCount, long expectedSeconds) throws Exception {
        List<Source> sources = Collections.singletonList(
            new Source("src.txt", new byte[byteCount])
        );
        List<LinkDescription> links = Collections.singletonList(
            new LinkDescription("target.txt", sources.get(0))
        );
        writeLinksToFile(links);

        Downloader downloader = new Downloader(link -> getFileContentByLink(sources, link));

        Options options = makeDefaultOptions()
            .threads(1)
            .limit(limit)
            .build();

        Instant startTime = Instant.now();
        Downloader.Result downloadResult = downloader.download(options);
        Duration duration = Duration.between(startTime, Instant.now());
        assertThat(duration.getSeconds()).isBetween(expectedSeconds, expectedSeconds + 1);

        assertSuccessfulDownloadResult(links, downloadResult);
    }

    private void assertSuccessfulDownloadResult(List<LinkDescription> links,
                                                Downloader.Result downloadResult) throws Exception {
        long expectedByteCount = calcUniqueLinksSize(links);

        assertThat(Files.isDirectory(downloadDirPath)).isTrue();
        assertThat(downloadResult.getByteCount()).isEqualTo(expectedByteCount);
        assertDownloadedFilesExist(links);

        Map<DownloaderLogRecord.Type, Integer> expectedRecordTypes = Collections.singletonMap(
            DownloaderLogRecord.Type.DOWNLOAD_SUCCESS, links.size());
        assertLog(downloadResult.getLog(), expectedRecordTypes);
    }

    private long calcUniqueLinksSize(List<LinkDescription> links) {
        return links.stream()
            .map(LinkDescription::getSrc)
            .distinct()
            .mapToLong(Source::calcSize)
            .sum();
    }

    private void assertFailedDownloadWithOneError(DownloaderLogRecord.Type logRecordType) {
        Options options = makeDefaultOptions().build();
        Downloader.Result downloadResult = downloader.download(options);
        assertThat(downloadResult.getByteCount()).isEqualTo(0L);

        Map<DownloaderLogRecord.Type, Integer> expectedRecordTypes = Collections.singletonMap(logRecordType, 1);
        assertLog(downloadResult.getLog(), expectedRecordTypes);
    }

    private void assertDownloadedFilesExist(List<LinkDescription> links) throws Exception {
        links.forEach(description -> {
            Path filePath = Paths.get(downloadDirPath.toString(), description.fileName);
            assertThat(Files.isRegularFile(filePath))
                .as(String.format("The file exists %s", description.fileName))
                .isTrue();

            if (Files.isRegularFile(filePath)) {
                try {
                    assertThat(new FileInputStream(filePath.toString()))
                        .as(String.format("The file's content %s", description.fileName))
                        .hasSameContentAs(new ByteArrayInputStream(description.src.content));
                } catch (FileNotFoundException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    private void assertLog(List<DownloaderLogRecord> actualLog,
                           Map<DownloaderLogRecord.Type, Integer> expectedRecordTypes) {
        Arrays.stream(DownloaderLogRecord.Type.values()).forEach(type -> {
            long expectedCount = expectedRecordTypes.getOrDefault(type, 0);
            long actualCount = actualLog.stream()
                .filter(record -> record.getType() == type)
                .count();
            assertThat(actualCount).as(String.format("Records count with the type %s", type))
                .isEqualTo(expectedCount);
        });
    }

    private Options.Builder makeDefaultOptions() {
        return new Options.Builder(this.linksFilePath.toString())
            .outputDir(this.downloadDirPath.toString());
    }

    private void writeLinksToFile(List<LinkDescription> links) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(this.linksFilePath)){
            links.stream()
                .map(LinkDescription::makeLinkString)
                .forEach(line -> {
                    try {
                        writer.write(line);
                        writer.newLine();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static class Source {
        final String link;
        final byte[] content;

        Source(String name, String content) {
            this(name, content.getBytes());
        }

        Source(String name, byte[] content) {
            this.link = LINK_PREFIX + name;
            this.content = content;
        }

        long calcSize() {
            return content.length;
        }
    }
    
    private static class LinkDescription {
        final String fileName;
        final Source src;

        LinkDescription(String fileName, Source src) {
            this.fileName = fileName;
            this.src = src;
        }

        String makeLinkString() {
            return src.link + " " + fileName;
        }

        Source getSrc() {
            return src;
        }
    }

    private static InputStream getFileContentByLink(List<Source> sources, String link) throws IOException {
        return sources.stream()
            .filter(src -> src.link.equals(link))
            .findFirst()
            .map(src -> new ByteArrayInputStream(src.content))
            .orElseThrow(() -> new IOException("The link %s cannot be downloaded: there's no such source"));
    }
}
