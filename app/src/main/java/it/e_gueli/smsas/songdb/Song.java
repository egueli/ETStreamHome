package it.e_gueli.smsas.songdb;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ris8 on 22/10/14.
 */
@DatabaseTable(tableName = "songs")
public class Song {
    @DatabaseField(generatedId = true)
    int index;

    @DatabaseField(unique = true, canBeNull = false)
    String fullPath;

    /** The string to show to the user. Duplicates are OK.*/
    @DatabaseField(canBeNull = false)
    String name;

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;

        Song song = (Song) o;

        if (index != song.index) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return index;
    }
}
