package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
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
import it.e_gueli.smsas.R;
import it.e_gueli.smsas.player.PlayerService;
import it.e_gueli.smsas.player.PlayerService_;
import it.e_gueli.smsas.sftp.InputStreamWithAvailable;
import it.e_gueli.smsas.sftp.SftpManager;
import it.e_gueli.smsas.sftp.StoredOrAskedUserInfo;
import it.e_gueli.smsas.songdb.DatabaseHelper;
import it.e_gueli.smsas.songdb.MusicScanner;
import it.e_gueli.smsas.songdb.Song;

@EActivity(R.layout.activity_my)
@OptionsMenu(R.menu.my)
public class MyActivity extends Activity {

    private static final String TAG = MyActivity.class.getSimpleName();



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

    private PlayerService playerService;
    private ServiceConnection playerServiceConnection;

    @AfterViews
    @UiThread
    void setupSearchBox() {
        searchBox.setEnabled(false); // will be enabled when the player service is bound
        searchBox.setAdapter(searchBoxAdapter);
        searchBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = songDao.queryForId((int)id);
                playerService.connectAndPlay(song.getFullPath());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        playerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playerService = ((PlayerService.LocalBinder) iBinder).getService();
                searchBox.setEnabled(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                searchBox.setEnabled(false);
            }
        };
        bindService(PlayerService_.intent(this).get(), playerServiceConnection, Context.BIND_AUTO_CREATE);
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


    @Override
    protected void onStop() {
        if (playerServiceConnection != null) {
            unbindService(playerServiceConnection);
            playerService = null;
            playerServiceConnection = null;
        }

        super.onStop();
    }


//    @Override
//    public boolean canPause() {
//        return true;
//    }
//
//    @Override
//    public boolean canSeekBackward() {
//        return false;
//    }
//
//    @Override
//    public boolean canSeekForward() {
//        return false;
//    }
//
//    @Override
//    public int getBufferPercentage() {
//        int percentage = (mMediaPlayer.getCurrentPosition() * 100) / mMediaPlayer.getDuration();
//
//        return percentage;
//    }
//
//    @Override
//    public int getCurrentPosition() {
//        return mMediaPlayer.getCurrentPosition();
//    }
//
//    @Override
//    public int getDuration() {
//        return mMediaPlayer.getDuration();
//    }
//
//    @Override
//    public boolean isPlaying() {
//        return mMediaPlayer.isPlaying();
//    }
//
//    @Override
//    public void pause() {
//        if(mMediaPlayer.isPlaying())
//            mMediaPlayer.pause();
//    }
//
//    @Override
//    public void seekTo(int pos) {
//        mMediaPlayer.seekTo(pos);
//    }
//
//    @Override
//    public void start() {
//        mMediaPlayer.start();
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        //mMediaController.show();
//
//        return false;
//    }
//
//    @Override
//    public int getAudioSessionId() {
//        return 0;
//    }
}
