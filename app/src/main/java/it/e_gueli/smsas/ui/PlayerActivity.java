package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.SupposeUiThread;
import org.androidannotations.annotations.ViewById;

import it.e_gueli.smsas.R;
import it.e_gueli.smsas.player.PlayerService;
import it.e_gueli.smsas.player.PlayerService_;

/**
 * Created by presentation on 31/10/14.
 */
@EActivity(R.layout.activity_nowplaying)
public class PlayerActivity extends Activity {

    PlayerService playerService;

    ServiceConnection playerServiceConnection;

    MediaPlayer mediaPlayer;

    @ViewById(R.id.playerStatus)
    TextView playerStatus;

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
        onPlayerStateChange();
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

    @Receiver(actions = PlayerService.ACTION_STATE_CHANGED)
    void onPlayerStateChange() {
        playerStatus.setText(playerService.getPlayerState().name());
    }
}
