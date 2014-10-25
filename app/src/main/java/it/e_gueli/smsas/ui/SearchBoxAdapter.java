package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.e_gueli.smsas.songdb.DatabaseHelper;
import it.e_gueli.smsas.songdb.Song;
import it.e_gueli.smsas.songdb.WordMatch;

/**
 * Created by ris8 on 23/10/14.
 */
@EBean
public class SearchBoxAdapter extends BaseAdapter implements Filterable {
    private static final String TAG = SearchBoxAdapter.class.getSimpleName();

    @OrmLiteDao(helper = DatabaseHelper.class, model = Song.class)
    RuntimeExceptionDao<Song, Integer> songDao;

    @OrmLiteDao(helper = DatabaseHelper.class, model = WordMatch.class)
    RuntimeExceptionDao<WordMatch, Integer> wordDao;

    @RootContext
    Activity context;

    private List<Song> songs;

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        TextView item = (TextView) row.findViewById(android.R.id.text1);
        item.setText(songs.get(position).getName());
        return row;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            @Trace
            protected FilterResults performFiltering(CharSequence constraint) {
                try {
                    List<Song> songsList;
                    if (constraint != null) {
                        List<WordMatch> results = wordDao.query(wordDao.queryBuilder()
                                .limit(20L)
                                .where()
                                .like("word", constraint.toString() + "%")
                                .prepare());

                        Set<Song> songsSet = new HashSet<Song>();
                        for (WordMatch match : results) {
                            songsSet.add(match.getSong());
                        }
                        songsList = new ArrayList<Song>(songsSet.size());
                        for (Song song : songsSet) {
                            songDao.refresh(song);
                            songsList.add(song);
                        }
                    }
                    else {
                        songsList = songDao.queryForAll();
                    }

                    Collections.sort(songsList, new Comparator<Song>() {
                        @Override
                        public int compare(
                                Song song1,
                                Song song2 /* woo-hoo, when I feel heavy-metal... */) {
                            return song1.getName().compareTo(song2.getName());
                        }
                    });
                    FilterResults out = new FilterResults();
                    out.count = songsList.size();
                    out.values = songsList;
                    return out;
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                songs = (List<Song>)filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}

