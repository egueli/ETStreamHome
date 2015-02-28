package it.e_gueli.smsas.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import fi.iki.elonen.SimpleWebServer;
import it.e_gueli.smsas.sftp.InputStreamWithAvailable;
import it.e_gueli.smsas.sftp.SftpManager;
import it.e_gueli.smsas.ui.PlayerActivity_;
import it.e_gueli.smsas.ui.SearchActivity_;

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

    private State state;

    public static final String ACTION_STATE_CHANGED = "it.e_gueli.smsas.PLAYER_STATE_CHANGED";

    private static final String ACTION_PLAY = "it.e_gueli.smsas.PLAY";
    private static final String EXTRA_PATH = "path";

    @SystemService
    WifiManager wifiManager;

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

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        state = new State();
        state.listenToMediaPlayer(mMediaPlayer);

        server = new SimpleWebServer("127.0.0.1", 8080, new File("/sdcard/Music"), false);
        try {
            server.start();
        }
        catch (IOException e) {
            // TODO listen on random port, and let orphan NanoHTTPD thread close themselves.
            throw new RuntimeException(e);
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void connectAndPlay(String songPath) {
        // can't do the stuff here, but start the service instead. This will prevent the service to
        // be killed when it is unbound.
        // Same issue as http://stackoverflow.com/q/22895001
        PlayerService_.intent(this)
                .action(ACTION_PLAY)
                .extra(EXTRA_PATH, songPath)
                .start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // TODO service restarted, resume playback
            return START_NOT_STICKY;
        }
        else {
            String songPath = intent.getStringExtra(EXTRA_PATH);
            buildStreaming(songPath);

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            state.setPlayerState(PlayerState.PLAYING);
                            mMediaPlayer.start();
                        }
                    });
                    state.onPrepared(mediaPlayer);
                }
            });

            createNotification(songPath);
            return START_STICKY;
        }
    }

    private void createNotification(String songName) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                PlayerActivity_.intent(getApplicationContext()).get(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = (new Notification.Builder(getApplicationContext()))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Playing from SFTP")
                .setContentText(songName)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }


    @Background
    void buildStreaming(String songPath) {
        try {
            state.setPlayerState(PlayerState.LOADING);

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
                private WifiManager.WifiLock wifiLock;

                @Override
                public void init(int op, String src, String dest, long max) {
                    Log.d(TAG, String.format("init() op=%d src=%s dest=%s max=%d", op, src, dest, max));
                    wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "it.e_gueli.smsas");
                    wifiLock.acquire();
                }

                @Override
                public boolean count(long count) {
//                    Log.d(TAG, String.format("count() count=%d", count));
                    return true;
                }

                @Override
                public void end() {
                    wifiLock.release();
                    wifiLock = null;
                    Log.d(TAG, "end()");
                }

                @Override
                protected void finalize() throws Throwable {
                    if (wifiLock != null) {
                        wifiLock.release();
                        wifiLock = null;
                    }
                    super.finalize();
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

    public PlayerState getPlayerState() {
        return state.playerState;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
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

    public enum PlayerState {
        IDLE,
        LOADING,
        PLAYING,
        PAUSED
    }

    public class State implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener {
        private PlayerState playerState = PlayerState.IDLE;

        private void setPlayerState(PlayerState playerState) {
            if (playerState == this.playerState)
                return;

            this.playerState = playerState;
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));

        }

        private void listenToMediaPlayer(MediaPlayer player) {
            player.setOnBufferingUpdateListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnInfoListener(this);
            //player.setOnPreparedListener(this); NOT added; PlayerService handles this
            player.setOnSeekCompleteListener(this);
        }

        @Override
        public void onBufferingUpdate(MediaPlayer player, int i) {
            Log.d(TAG, String.format("onBufferingUpdate: %2d%%", i));
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));
        }

        @Override
        public void onCompletion(MediaPlayer player) {
            Log.d(TAG, "onCompletion()");
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));

        }

        @Override
        public boolean onError(MediaPlayer player, int what, int extra) {
            String whatStr;
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    whatStr = "server died";
                    break;
                default:
                    whatStr = "unknown cause " + what;
            }
            String extraStr;
            switch (extra) {
                case MediaPlayer.MEDIA_ERROR_IO:
                    extraStr = "I/O error";
                    break;
                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    extraStr = "Malformed";
                    break;
                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    extraStr = "Timed out";
                    break;
                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    extraStr = "Unsupported";
                    break;
                default:
                    extraStr = "unknown detail " + extra;
            }

            Log.e(TAG, "onError: what=" + what + " extra=" + extra);
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));

            return false;
        }

        @Override
        public boolean onInfo(MediaPlayer player, int i, int i2) {
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));

            return false;
        }

        public void onPrepared(MediaPlayer player) {
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));
        }

        @Override
        public void onSeekComplete(MediaPlayer player) {
            sendBroadcast(new Intent(ACTION_STATE_CHANGED));
        }
    }
}
