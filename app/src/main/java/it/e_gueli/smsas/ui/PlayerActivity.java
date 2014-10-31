package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.MediaController;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import it.e_gueli.smsas.R;
import it.e_gueli.smsas.player.PlayerService;
import it.e_gueli.smsas.player.PlayerService_;

/**
 * Created by presentation on 31/10/14.
 */
@EActivity(R.layout.player)
public class PlayerActivity extends Activity implements MediaController.MediaPlayerControl {

    PlayerService playerService;

    ServiceConnection playerServiceConnection;

    @ViewById
    MediaController mediaController;

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

    private void onPlayerServiceConnected() {
        mediaController.setMediaPlayer(this);
    }

    @Override
    protected void onStop() {
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
        
    }

    @Override
    public void pause() {

    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int i) {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
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
        return 0;
    }
}
