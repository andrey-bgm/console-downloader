package com.example.consoledownloader.downloader;

public class DownloaderLogRecord {
    private final Type type;
    private final String message;

    public static DownloaderLogRecord create(Type type, String message) {
        return new DownloaderLogRecord(type, message);
    }

    public DownloaderLogRecord(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public enum Type {
        DOWNLOAD_SUCCESS("OK"), DOWNLOAD_FAIL("FAIL"), SYSTEM_ERROR("ERROR");
        private final String prettyString;

        Type(String prettyString) {
            this.prettyString = prettyString;
        }

        public String toPretty() {
            return prettyString;
        }
    }
}
