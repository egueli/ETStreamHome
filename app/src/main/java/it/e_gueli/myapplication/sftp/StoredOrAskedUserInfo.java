package it.e_gueli.myapplication.sftp;

import android.app.Activity;
import android.util.Log;

import com.jcraft.jsch.UserInfo;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import it.e_gueli.myapplication.ui.Prompter;

/**
 * Created by ris8 on 20/10/14.
 */
@EBean
public class StoredOrAskedUserInfo implements UserInfo {
    private static final String TAG = StoredOrAskedUserInfo.class.getSimpleName();

    @RootContext
    Activity activity;

    @Pref
    SftpPrefs_ sftpPrefs;

    @Bean
    Prompter prompter;

    String userInputPassword = null;

    @Override
    public String getPassphrase() {
        return null;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return true;
    }


    @Override
    public boolean promptPassword(String message) {
        if (sftpPrefs.sshPassword().exists())
            return true;
        else {
            userInputPassword = prompter.showTextInputDialogBlocking(message, true);
            return (userInputPassword != null);
        }
    }


    @Override
    public String getPassword() {
        if (userInputPassword != null)
            return userInputPassword;
        else if (sftpPrefs.sshPassword().exists())
            return sftpPrefs.sshPassword().get();
        else {
            Log.w(TAG, "no password set, returning null");
            return null;
        }
    }

    /**
     * To be called after the SSH connection is made. If the password was entered by the user, this
     * method will store it in the sharedprefs.
     */
    public void confirmPasswordRight() {
        if (userInputPassword != null)
            sftpPrefs.sshPassword().put(userInputPassword);
    }

    /**
     * To be called when the connection was unsuccessful.
     */
    public void clearPassword() {
        sftpPrefs.sshPassword().remove();
    }

    @Override
    public boolean promptYesNo(String message) {
        return prompter.showYesNoDialogBlocking(message);
    }


    @Override
    public void showMessage(String message) {
        prompter.showMessageDialogBlocking(message);
    }

}
