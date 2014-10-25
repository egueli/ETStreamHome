package it.e_gueli.myapplication.songdb;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ris8 on 22/10/14.
 */
@DatabaseTable(tableName = "words")
public class WordMatch {

    @DatabaseField(generatedId = true)
    Integer id;

    @DatabaseField(index = true)
    String word;

    @DatabaseField(canBeNull = false, foreign = true)
    Song song;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
