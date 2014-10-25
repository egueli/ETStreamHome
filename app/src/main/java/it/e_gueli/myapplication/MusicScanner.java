package it.e_gueli.myapplication;

import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.e_gueli.myapplication.songdb.DatabaseHelper;
import it.e_gueli.myapplication.songdb.Song;
import it.e_gueli.myapplication.songdb.WordMatch;

/**
 * Created by ris8 on 22/10/14.
 */
@EBean
public class MusicScanner {
    private static final String TAG = MusicScanner.class.getSimpleName();

    interface ProgressListener {
        void onNewDir(String path);
    }

    @OrmLiteDao(helper = DatabaseHelper.class, model = Song.class)
    Dao<Song, Integer> songDao;

    @OrmLiteDao(helper = DatabaseHelper.class, model = WordMatch.class)
    Dao<WordMatch, Integer> wordDao;

    private PreparedQuery<Song> songInDbQuery;
    SelectArg fullPathArg = new SelectArg();

    private ProgressListener progressListener;

    @AfterInject
    void prepareQueries() {
        try {
            songInDbQuery = songDao.queryBuilder().where().eq("fullPath", fullPathArg).prepare();
        }
        catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    void setProgressListener(ProgressListener listener) {
        progressListener = listener;
    }

    void doMusicScan(ChannelSftp channelSftp) throws SftpException, SQLException {
        scanRecursive(channelSftp, "./Musica");
    }

    Pattern wordPattern = Pattern.compile("([a-z']+)");

    void scanRecursive(ChannelSftp channel, String path) throws SftpException, SQLException {
        if (progressListener != null) progressListener.onNewDir(path);

        List<ChannelSftp.LsEntry> entries = channel.ls(path);
        for (ChannelSftp.LsEntry entry : entries) {
            if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename()))
                continue;

            SftpATTRS attrs = entry.getAttrs();
            if (attrs.isDir()) {
                scanRecursive(channel, path + File.separator + entry.getFilename());
            }
            else {
                String name = entry.getFilename().toLowerCase();
                int extensionPos = name.indexOf(".mp3");
                if (extensionPos == -1)
                    continue;

                String base = name.substring(0, extensionPos);

                Song song = new Song();
                String fullPath = path + File.separator + name;

                fullPathArg.setValue(fullPath);
                if (songDao.query(songInDbQuery).isEmpty()) {
                    Log.v(TAG, "adding to db:  " + fullPath);
                    song.setFullPath(fullPath);
                    song.setName(base);
                    songDao.create(song);

                    String wordsSource = path.toLowerCase() + File.separator + base;
                    Matcher matcher = wordPattern.matcher(wordsSource);
                    for (; matcher.find(); ) {
                        WordMatch word = new WordMatch();
                        String wordStr = matcher.group(1);
                        word.setWord(wordStr);
                        word.setSong(song);
                        wordDao.create(word);
                    }
                }
                else {
                    Log.v(TAG, "already in db: " + fullPath);
                }
            }
        }
    }

}
