package com.example.consoledownloader.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DefaultLinkDownloader implements LinkDownloader {
    @Override
    public InputStream download(String link) throws IOException {
        URL url = new URL(link);
        return url.openStream();
    }
}
