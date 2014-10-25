package it.e_gueli.myapplication.sftp;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(SharedPref.Scope.APPLICATION_DEFAULT)
interface SftpPrefs {
    String sshUserName();

    String sshPassword();

    String sshServerAddress();

    /**
     * It should be int, but EditTextPreference writes as string although its inputType is numeric.
     * @return
     */
    String sshServerPort();
}