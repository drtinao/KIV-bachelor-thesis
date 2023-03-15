package fav.drtinao.skama;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.skamav2.R;

/**
 * Takes care of tab switching in settings part of the application.
 */
public class FragmentSettingsMain extends Fragment {
    private TabLayout fragmentSettingsMainTl;
    private ViewPager fragmentSettingsMainVp;
    public SettingsAdapter settingsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_settings_main, container, false);
    }

    /**
     * This fragment is not supposed to have menu in ActionBar - the method is used just for clearing menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fragmentSettingsMainTl = view.findViewById(R.id.fragment_settings_main_tl_id);
        fragmentSettingsMainVp = view.findViewById(R.id.fragment_settings_main_vp_id);

        settingsAdapter = new SettingsAdapter(getActivity(), getChildFragmentManager(), fragmentSettingsMainTl.getTabCount());
        fragmentSettingsMainVp.setAdapter(settingsAdapter);

        fragmentSettingsMainVp.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(fragmentSettingsMainTl));

        fragmentSettingsMainTl.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                fragmentSettingsMainVp.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
}
