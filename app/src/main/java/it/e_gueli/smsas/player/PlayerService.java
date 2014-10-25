package it.e_gueli.smsas.player;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.androidannotations.annotations.EService;

/**
 * Created by ris8 on 26/10/14.
 */
@EService
public class PlayerService extends Service {
    // TODO handle media player
    // TODO cpu-lock and wifi-lock
    // TODO notifications
    // TODO audio focus
    // TODO audio becoming noisy


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
