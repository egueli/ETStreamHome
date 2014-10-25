package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.InputType;

import org.androidannotations.annotations.EActivity;

import java.util.Map;

import it.e_gueli.smsas.R;

/**
 * Created by ris8 on 25/10/14.
 */
@EActivity
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onStart() {
            super.onStart();
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            for (Map.Entry<String, ?> pref : prefs.getAll().entrySet()) {
                setSummary(pref.getKey());
            }
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onStop();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            setSummary(s);
        }

        protected void setSummary(String prefKey) {
            Preference pre = getPreferenceManager().findPreference(prefKey);
            if (pre instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference)pre;
                if ((etp.getEditText().getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == 0) {
                    etp.setSummary(etp.getText());
                }
            }
        }
    }

}