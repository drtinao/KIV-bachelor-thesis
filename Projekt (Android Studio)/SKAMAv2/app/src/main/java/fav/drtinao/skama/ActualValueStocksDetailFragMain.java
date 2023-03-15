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
 * Takes care of switching tabs in part of the application, which displays stocks values.
 */
public class ActualValueStocksDetailFragMain extends Fragment {
    private TabLayout actualValueStocksDetailTl;
    private ViewPager actualValueStocksDetailVp;
    private StocksDetailAdapter stocksDetailAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.actual_value_stocks_detail_frag_main, container, false);
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
        String selectedStockSym = dataBundle.getString(getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol));

        actualValueStocksDetailTl = view.findViewById(R.id.actual_value_stocks_detail_frag_main_tl_id);
        actualValueStocksDetailVp = view.findViewById(R.id.actual_value_stocks_detail_frag_main_vp_id);

        stocksDetailAdapter = new StocksDetailAdapter(getActivity(), getChildFragmentManager(), actualValueStocksDetailTl.getTabCount(), selectedStockSym);
        actualValueStocksDetailVp.setAdapter(stocksDetailAdapter);

        actualValueStocksDetailVp.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(actualValueStocksDetailTl));

        actualValueStocksDetailTl.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                actualValueStocksDetailVp.setCurrentItem(tab.getPosition());
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
