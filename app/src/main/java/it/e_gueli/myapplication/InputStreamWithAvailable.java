package it.e_gueli.myapplication;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ris8 on 21/10/14.
 */
public class InputStreamWithAvailable extends InputStream {
    private final long size;
    private long available;
    private InputStream stream;
    public InputStreamWithAvailable(InputStream stream, long size) {
        this.stream = stream;
        this.available = size;
        this.size = size;
    }

    @Override
    public int available() throws IOException {
        return (int)(Math.min(available, Integer.MAX_VALUE));
    }

    @Override
    public int read() throws IOException {
        available--;
        onProgress();
        return stream.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int count = stream.read(buffer);
        available -= count;
        onProgress();
        return count;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int count = stream.read(buffer, byteOffset, byteCount);
        available -= count;
        onProgress();
        return count;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        long count = super.skip(byteCount);
        available -= count;
        onProgress();
        return count;
    }

    private static final int PROGRESS_DELTA = 10240;
    private long lastProgress = 0;
    private void onProgress() {
        long progress = size - available;
        if ((progress - lastProgress) > PROGRESS_DELTA) {
            Log.d("InputStream", "progress: " + (progress / 1024) + "k");
            lastProgress = progress;
        }

    }
}
