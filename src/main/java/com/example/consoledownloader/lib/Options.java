package com.example.consoledownloader.lib;

import com.google.common.base.Strings;

public class Options {

    private final String linksFile;
    private final String outputDir;
    private final int threads;
    private final long limit;
    private final boolean verbose;
    private final boolean help;

    private Options(Builder builder) {
        this.linksFile = Strings.nullToEmpty(builder.linksFile);
        this.outputDir = Strings.nullToEmpty(builder.outputDir);
        this.threads = builder.threads == 0 ? 1 : builder.threads;
        this.limit = builder.limit;
        this.verbose = builder.verbose;
        this.help = builder.help;
    }

    public int getThreads() {
        return threads;
    }

    public long getLimit() {
        return limit;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getLinksFile() {
        return linksFile;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isHelpNeeded() {
        return help;
    }

    public static class Builder {
        private String linksFile;
        private String outputDir;
        private int threads;
        private long limit;
        private boolean verbose;
        private boolean help;

        public Builder(String linksFile) {
            this.linksFile = linksFile;
        }

        public Builder outputDir(String outputFolder) {
            this.outputDir = outputFolder;
            return this;
        }

        public Builder threads(int threads) {
            this.threads = threads;
            return this;
        }

        public Builder limit(long limit) {
            this.limit = limit;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Options build() {
            return new Options(this);
        }

        public Builder help(boolean help) {
            this.help = help;
            return this;
        }
    }
}
