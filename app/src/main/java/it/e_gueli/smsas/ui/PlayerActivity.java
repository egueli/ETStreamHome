package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.view.View;
import android.widget.MediaController;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SupposeUiThread;
import org.androidannotations.annotations.ViewById;

import it.e_gueli.smsas.R;
import it.e_gueli.smsas.player.PlayerService;
import it.e_gueli.smsas.player.PlayerService_;

/**
 * Created by presentation on 31/10/14.
 */
@EActivity(R.layout.activity_nowplaying)
public class PlayerActivity extends Activity implements MediaController.MediaPlayerControl {

    PlayerService playerService;

    ServiceConnection playerServiceConnection;

    MediaController mediaController;

    MediaPlayer mediaPlayer;

    @ViewById(R.id.audioView)
    View anchorView;

    @Override
    protected void onStart() {
        super.onStart();

        connectToPlayerService();
    }

    private void connectToPlayerService() {
        playerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playerService = ((PlayerService.LocalBinder) iBinder).getService();
                onPlayerServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        bindService(PlayerService_.intent(this).get(), playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @SupposeUiThread
    void onPlayerServiceConnected() {
        mediaPlayer = playerService.getMediaPlayer();
        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(anchorView);
        mediaController.setEnabled(true);
        mediaController.show(3000);
        //mediaController.setAnchorView(anchorView);
    }

    @Override
    protected void onStop() {
        mediaController.hide();
        disconnectFromPlayerService();

        super.onStop();
    }

    private void disconnectFromPlayerService() {
        if (playerServiceConnection != null) {
            unbindService(playerServiceConnection);
            playerService = null;
            playerServiceConnection = null;
        }
    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        mediaPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
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
    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }
}
