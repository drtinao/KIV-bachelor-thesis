package fav.drtinao.skama;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.skamav2.R;

/**
 * This adapter provides switching between tabs which are present in actual value section of the app. There are two tabs - one for latest values (one month) and one for history.
 */
public class StocksDetailAdapter extends FragmentPagerAdapter {
    private Activity appActivity; /* reference to Activity of the application */
    private int tabCount; /* number of tabs */
    private String selectedStockSym; /* symbol of stock, which was selected */

    /**
     * Constructor is used for initialization of basic values.
     * @param appActivity reference to Activity of the application
     * @param fm instance of FragmentManager
     * @param tabCount number of tabs
     * @param selectedStockSym symbol of the selected stock
     */
    public StocksDetailAdapter(Activity appActivity, FragmentManager fm, int tabCount, String selectedStockSym){
        super(fm);
        this.appActivity = appActivity;
        this.tabCount = tabCount;
        this.selectedStockSym = selectedStockSym;
    }

    @Override
    public Fragment getItem(int i) {
        ActualValueStocksDetailFrag actualValueStocksDetailFrag = new ActualValueStocksDetailFrag();
        Bundle dataBundle = new Bundle();

        switch(i){
            case 0: /* general info */
                dataBundle.putInt(appActivity.getResources().getString(R.string.actual_value_stocks_detail_sharedpref_tab), 1);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol), selectedStockSym);
                actualValueStocksDetailFrag.setArguments(dataBundle);
                break;

            case 1: /*history info */
                dataBundle.putInt(appActivity.getResources().getString(R.string.actual_value_stocks_detail_sharedpref_tab), 2);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol), selectedStockSym);
                actualValueStocksDetailFrag.setArguments(dataBundle);
                break;
        }

        return actualValueStocksDetailFrag;
    }

    @Override
    public int getCount() {
        return tabCount;
    }
}
