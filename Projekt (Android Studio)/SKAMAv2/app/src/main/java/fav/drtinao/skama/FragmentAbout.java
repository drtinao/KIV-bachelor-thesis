package fav.drtinao.skama;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.skamav2.R;

/**
 * Responsible for displaying "About" part of the application.
 */
public class FragmentAbout extends Fragment {
    /**
     * Is called by Android OS when layout should be inflated, here fragment_about.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View createdLayout = inflater.inflate(R.layout.fragment_about, container, false);
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(getResources().getString(R.string.other_about_menu));
        return createdLayout;
    }

    /**
     * This fragment is not supposed to have menu in ActionBar - the method is used just for clearing menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}
