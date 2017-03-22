package com.example.consoledownloader.lib;

import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.io.InputStream;

public class RateLimitedInputStream extends InputStream{
    private final InputStream in;
    private final RateLimiter rateLimiter;

    public RateLimitedInputStream(InputStream in, RateLimiter rateLimiter) {
        this.in = in;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public int read() throws IOException {
        this.rateLimiter.acquire();
        return this.in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        double rate = this.rateLimiter.getRate();
        int permits = rate < len ? (int) rate : len;
        this.rateLimiter.acquire(permits);

        return this.in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return this.in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return this.in.available();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }

    @Override
    public synchronized void mark(int readLimit) {
        in.mark(readLimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.in.reset();
    }

    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }
}
