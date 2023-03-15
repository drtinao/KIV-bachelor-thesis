package fav.drtinao.skama;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.skamav2.R;

/**
 * Responsible for switching tabs inside settings part of the application.
 */
public class SettingsAdapter extends FragmentPagerAdapter {
    private Activity appActivity; /* reference to Activity of the application */
    private int tabCount; /* number of tabs */

    public SettingsAdapter(Activity appActivity, FragmentManager fm, int tabCount) {
        super(fm);
        this.appActivity = appActivity;
        this.tabCount = tabCount;
    }

    @Override
    public Fragment getItem(int i) {
        FragmentSettings fragmentSettings = new FragmentSettings();
        Bundle dataBundle = new Bundle();

        switch(i){
            case 0: /* general settings */
                dataBundle.putInt(appActivity.getResources().getString(R.string.settings_sharedpref_tab), 1);
                fragmentSettings.setArguments(dataBundle);
                break;

            case 1: /* crypto settings */
                dataBundle.putInt(appActivity.getResources().getString(R.string.settings_sharedpref_tab), 2);
                fragmentSettings.setArguments(dataBundle);
                break;

            case 2: /* stocks settings */
                dataBundle.putInt(appActivity.getResources().getString(R.string.settings_sharedpref_tab), 3);
                fragmentSettings.setArguments(dataBundle);
                break;
        }

        return fragmentSettings;
    }

    @Override
    public int getCount() {
        return tabCount;
    }
}
