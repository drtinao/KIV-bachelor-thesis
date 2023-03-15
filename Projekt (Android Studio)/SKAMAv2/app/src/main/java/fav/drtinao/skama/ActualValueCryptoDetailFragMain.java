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
 * Is responsible for displaying part of the app, which shows actual cryptocurrency values and history.
 */
public class ActualValueCryptoDetailFragMain extends Fragment {
    private TabLayout actualValueCryptoDetailTl;
    private ViewPager actualValueCryptoDetailVp;
    public CryptoDetailAdapter cryptoDetailAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.actual_value_crypto_detail_frag_main, container, false);
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
        Bundle dataBundle = getArguments();
        String selectedCryptoId = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id));
        String selectedCryptoName = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name));
        String selectedCryptoSymbol = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_symbol));

        actualValueCryptoDetailTl = view.findViewById(R.id.actual_value_crypto_detail_frag_main_tl_id);
        actualValueCryptoDetailVp = view.findViewById(R.id.actual_value_crypto_detail_frag_main_vp_id);

        cryptoDetailAdapter = new CryptoDetailAdapter(getActivity(), getChildFragmentManager(), actualValueCryptoDetailTl.getTabCount(), selectedCryptoId, selectedCryptoName, selectedCryptoSymbol);
        actualValueCryptoDetailVp.setAdapter(cryptoDetailAdapter);

        actualValueCryptoDetailVp.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(actualValueCryptoDetailTl));

        actualValueCryptoDetailTl.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                actualValueCryptoDetailVp.setCurrentItem(tab.getPosition());
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
