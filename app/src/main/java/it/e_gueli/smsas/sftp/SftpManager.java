package it.e_gueli.smsas.sftp;

import android.content.Context;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;

/**
 * Created by ris8 on 23/10/14.
 */
@EBean
public class SftpManager {

    @RootContext
    Context context;

    @Bean
    StoredOrAskedUserInfo storedOrAskedUserInfo;

    // If you see an error like "symbol not found" here, try changing something in build.gradle
    // and do Sync now, then Make module 'app'.
    @Pref
    SftpPrefs_ prefs;

    /*
    TODO public key authentication
    - the app creates an own private/public key pair.
    - the app sends the public key to Pastebin
    - the app invites the user to log in to his SSH server and paste a command like
      curl http://pastebin.com/abcdefgh >> .ssh/authorized_keys
     */

    public ChannelSftp connectAndGetSftpChannel() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(context.getFilesDir().getAbsolutePath() + File.separator + "jsch_known_hosts");

        String username = prefs.sshUserName().getOr(null);
        String host = prefs.sshServerAddress().getOr(null);
        String port = prefs.sshServerPort().getOr(null);
        if (username == null || host == null || port == null)
            throw new IllegalStateException("Please enter your server settings first");

        Session session = jsch.getSession(username, host, Integer.parseInt(port));

        UserInfo info = storedOrAskedUserInfo;
        session.setUserInfo(info);
        try {
            session.connect(5000);
            storedOrAskedUserInfo.confirmPasswordRight();
        }
        catch (JSchException je) {
            if ("auth fail".equals(je.getMessage().toLowerCase())) { // undocumented!
                storedOrAskedUserInfo.clearPassword();
            }
            throw je;
        }

        ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
        channel.connect(5000);
        return channel;
    }
}
