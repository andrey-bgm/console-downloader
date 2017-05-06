package com.example.consoledownloader.argsparser;

public interface ArgsParser {
    Options parse(String... args) throws ArgsParseException;

    default String usage() {
        String newLine = System.lineSeparator();
        return "Usage: console-downloader -f FILE [OPTIONS]" + newLine +
            newLine +
            "The options:" + newLine +
            "  -f, --link-file=FILE                a file with links for download" + newLine +
            "  -o, --output-dir=DIRECTORY          an output directory for downloaded files" + newLine +
            "  -n, --thread-number=NUMBER          the number of threads" + newLine +
            "  -l, --speed-limit                   the download speed limit in bytes, kilobytes (10k)" + newLine +
            "                                      or megabytes (10m)" + newLine +
            "  -v, --verbose                       turn on the verbose mode" + newLine +
            "  -h, --help                          show help" + newLine +
            newLine +
            "The example of the link file:" + newLine +
            "   http://example.com/file1.txt file1.txt" + newLine +
            "   http://example.com/file1.txt file1_copy.txt" + newLine +
            "   http://example.com/file2.txt file2.txt" + newLine;
    }

    String LINK_FILE_SHORT = "-f";
    String LINK_FILE_LONG = "--link-file";
    boolean LINK_FILE_REQUIRED = true;

    String OUTPUT_DIR_SHORT = "-o";
    String OUTPUT_DIR_LONG = "--output-dir";
    boolean OUTPUT_DIR_REQUIRED = false;

    String THREAD_NUMBER_SHORT = "-n";
    String THREAD_NUMBER_LONG = "--thread-number";
    boolean THREAD_NUMBER_REQUIRED = false;

    String SPEED_LIMIT_SHORT = "-l";
    String SPEED_LIMIT_LONG = "--speed-limit";
    boolean SPEED_LIMIT_REQUIRED = false;

    String VERBOSE_SHORT = "-v";
    String VERBOSE_LONG = "--verbose";

    String HELP_SHORT = "-h";
    String HELP_LONG = "--help";
}
