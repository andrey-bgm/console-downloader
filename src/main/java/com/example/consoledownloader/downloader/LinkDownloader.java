package com.example.consoledownloader.downloader;

import java.io.IOException;
import java.io.InputStream;

public interface LinkDownloader {
    InputStream download(String link) throws IOException;
}
