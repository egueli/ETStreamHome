package it.e_gueli.myapplication;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Vector;

import fi.iki.elonen.SimpleWebServer;
import it.e_gueli.myapplication.sftp.SftpManager;
import it.e_gueli.myapplication.sftp.StoredOrAskedUserInfo;
import it.e_gueli.myapplication.songdb.DatabaseHelper;
import it.e_gueli.myapplication.songdb.Song;
import it.e_gueli.myapplication.ui.SettingsActivity;
import it.e_gueli.myapplication.ui.SettingsActivity_;

@EActivity(R.layout.activity_my)
@OptionsMenu(R.menu.my)
public class MyActivity extends Activity implements MediaController.MediaPlayerControl {

    private static final String TAG = MyActivity.class.getSimpleName();

    private MediaController mMediaController;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler = new Handler();

    @Bean
    StoredOrAskedUserInfo userInfo;

    @ViewById
    LinearLayout audioView;

    @ViewById
    AutoCompleteTextView searchBox;

    @Bean
    SearchBoxAdapter searchBoxAdapter;

    @ViewById
    TextView statusText;

    @Bean
    MusicScanner musicScanner;

    @Bean
    SftpManager sftpManager;

    @OrmLiteDao(helper = DatabaseHelper.class, model = Song.class)
    RuntimeExceptionDao<Song, Integer> songDao;

    SimpleWebServer server;

    @AfterViews
    @UiThread
    void setupSearchBox() {
        searchBox.setAdapter(searchBoxAdapter);
        searchBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = songDao.queryForId((int)id);
                connectAndPlay(song.getFullPath());
            }
        });
    }

    @OptionsItem(R.id.action_settings)
    void showSettings() {
        SettingsActivity_.intent(this).start();
    }

    @Click
    void connectAndScan() {
        statusText.setText("Connecting...");
        doMusicScan();
    }

    @Background // because it involves network
    void doMusicScan() {
        try {
            ChannelSftp channelSftp = sftpManager.connectAndGetSftpChannel();
            musicScanner.setProgressListener(new MusicScanner.ProgressListener() {
                @Override
                public void onNewDir(String path) {
                    setStatus(path);
                }
            });
            musicScanner.doMusicScan(channelSftp);
            setStatus("finished!");
        }
        catch(Exception e) {
            setStatus(e.toString());
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }


    @UiThread
    void setStatus(String message) {
        statusText.setText(message);
    }

    //@AfterInject
    void connectAndPlay(String songPath) {
        buildStreaming(songPath);

        mMediaPlayer = new MediaPlayer();
        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(audioView);

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMediaController.show(10000);
                        mMediaPlayer.start();
                    }
                });
            }
        });
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
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.stop();
        mMediaPlayer.release();
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

            if (server == null)
                server = new SimpleWebServer("0.0.0.0", 8080, new File("/sdcard/Music"), false);

            server.setHttpStream(new BufferedInputStream(stream, 131072));

            if (!server.isAlive())
                server.start();

            startPlaying();
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        int percentage = (mMediaPlayer.getCurrentPosition() * 100) / mMediaPlayer.getDuration();

        return percentage;
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void pause() {
        if(mMediaPlayer.isPlaying())
            mMediaPlayer.pause();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //mMediaController.show();

        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
