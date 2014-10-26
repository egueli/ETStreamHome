package it.e_gueli.smsas.player;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.MediaController;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.UiThread;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Vector;

import fi.iki.elonen.SimpleWebServer;
import it.e_gueli.smsas.sftp.InputStreamWithAvailable;
import it.e_gueli.smsas.sftp.SftpManager;

/**
 * Created by ris8 on 26/10/14.
 */
@EService
public class PlayerService extends Service {
    // TODO cpu-lock and wifi-lock
    // TODO notifications
    // TODO audio focus
    // TODO audio becoming noisy

    private static final String TAG = PlayerService.class.getSimpleName();

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    private final LocalBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private MediaController mMediaController;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler = new Handler();

    public void connectAndPlay(String songPath) {
        buildStreaming(songPath);

        mMediaPlayer = new MediaPlayer();
        mMediaController = new MediaController(this);
        //mMediaController.setMediaPlayer(this);
        //mMediaController.setAnchorView(audioView);

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
          //              mMediaController.show(10000);
                        mMediaPlayer.start();
                    }
                });
            }
        });
    }

    @Bean
    SftpManager sftpManager;

    SimpleWebServer server;


    @Background
    void buildStreaming(String songPath) {
        try {
            ChannelSftp channel = sftpManager.connectAndGetSftpChannel();

            String dir = "";
            String file = songPath;
            int lastSlash = songPath.lastIndexOf(File.separator);
            if (lastSlash != -1) {
                dir = songPath.substring(0, lastSlash);
                file = songPath.substring(lastSlash + 1);
            }
            channel.cd(dir);
            Vector<ChannelSftp.LsEntry> entries = (Vector<ChannelSftp.LsEntry>) channel.ls(".");
            ChannelSftp.LsEntry songEntry = null;
            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getFilename().toLowerCase().contains(file)) {
                    songEntry = entry;
                    break;
                }
            }
            long size = songEntry.getAttrs().getSize();

            InputStream stream = new InputStreamWithAvailable(channel.get(songEntry.getFilename(), new SftpProgressMonitor() {
                @Override
                public void init(int op, String src, String dest, long max) {
                    Log.d(TAG, String.format("init() op=%d src=%s dest=%s max=%d", op, src, dest, max));
                }

                @Override
                public boolean count(long count) {
//                    Log.d(TAG, String.format("count() count=%d", count));
                    return true;
                }

                @Override
                public void end() {
                    Log.d(TAG, "end()");
                }
            }), size);

            if (server == null)
                server = new SimpleWebServer("127.0.0.1", 8080, new File("/sdcard/Music"), false);

            server.setHttpStream(new BufferedInputStream(stream, 131072));

            if (!server.isAlive())
                server.start();

            startPlaying();
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @UiThread
    void startPlaying() {
        try {
            mMediaPlayer.setDataSource("http://127.0.0.1:8080/stream.mp3");
            mMediaPlayer.prepare();
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public void onDestroy() {
        if (mMediaController != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }


        super.onDestroy();
    }

}
