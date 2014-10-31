package it.e_gueli.smsas.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import fi.iki.elonen.SimpleWebServer;
import it.e_gueli.smsas.R;
import it.e_gueli.smsas.sftp.InputStreamWithAvailable;
import it.e_gueli.smsas.sftp.SftpManager;
import it.e_gueli.smsas.ui.MyActivity_;

/**
 * Created by ris8 on 26/10/14.
 */
@EService
public class PlayerService extends Service {
    // TODO cpu-lock and wifi-lock
    // TODO notifications
    // TODO audio focus
    // TODO audio becoming noisy

    // TODO link PlayerService with an activity to prompt for sftp-related user interaction

    private static final String TAG = PlayerService.class.getSimpleName();

    private MediaPlayer mMediaPlayer;
    private Handler mHandler = new Handler();
    private static final int NOTIFICATION_ID = 665455242; // any number is OK as long it's not 0

    @Bean
    SftpManager sftpManager;

    SimpleWebServer server;

    //region Binder stuff

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

    //endregion


    @Override
    public void onCreate() {
        super.onCreate();

        server = new SimpleWebServer("127.0.0.1", 8080, new File("/sdcard/Music"), false);
        try {
            server.start();
        }
        catch (IOException e) {
            // TODO listen on random port, and let orphan NanoHTTPD thread close themselves.
            throw new RuntimeException(e);
        }
    }

    public void connectAndPlay(String songPath) {
        buildStreaming(songPath);

        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMediaPlayer.start();
                    }
                });
            }
        });

        createNotification(songPath);
    }


    private void createNotification(String songName) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                MyActivity_.intent(getApplicationContext()).get(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification();
        notification.tickerText = "Song playing"; // TODO pass a Song object so we get the title
        notification.icon = android.R.drawable.ic_media_play;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
                "Playing: " + songName, pi);
        startForeground(NOTIFICATION_ID, notification);
    }


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

            server.setHttpStream(new BufferedInputStream(stream, 131072));

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
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (server != null) {
            server.stop();
            server = null;
        }

        super.onDestroy();
    }

}
