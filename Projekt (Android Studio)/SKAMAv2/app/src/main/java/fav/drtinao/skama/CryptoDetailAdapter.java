package fav.drtinao.skama;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.skamav2.R;

/**
 * This adapter provides switching between tabs which are present in actual value section of the app. There are two tabs - one for latest values (24 hours) and one for history.
 */
public class CryptoDetailAdapter extends FragmentPagerAdapter {
    private Activity appActivity; /* used just for access to getResources */
    private int tabCount; /* number of tabs */
    private String selectedCryptoId; /* id of cryptocurrency, which was selected */
    private String selectedCryptoName; /* name of cryptocurrency, which was selected */
    private String selectedCryptoSymbol; /* symbol of cryptocurrency which was selected */

    /**
     * Constructor is used for initialization of basic values.
     * @param appActivity used just for access to getResources
     * @param fm instance of FragmentManager
     * @param tabCount number of tabs
     * @param selectedCryptoId cryptocurrency id
     * @param selectedCryptoName cryptocurrency name
     * @param selectedCryptoSymbol cryptocurrency symbol
     */
    public CryptoDetailAdapter(Activity appActivity, FragmentManager fm, int tabCount, String selectedCryptoId, String selectedCryptoName, String selectedCryptoSymbol) {
        super(fm);
        this.appActivity = appActivity;
        this.tabCount = tabCount;
        /* assign variables regarding to selected crypto - START */
        this.selectedCryptoId = selectedCryptoId;
        this.selectedCryptoName = selectedCryptoName;
        this.selectedCryptoSymbol = selectedCryptoSymbol;
        /* assign variables regarding to selected crypto - END */
    }

    @Override
    public Fragment getItem(int i) {
        ActualValueCryptoDetailFrag actualValueCryptoDetailFrag = new ActualValueCryptoDetailFrag();
        Bundle dataBundle = new Bundle();

        switch(i){
            case 0: /* general info */
                dataBundle.putInt(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_tab), 1);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id), selectedCryptoId);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name), selectedCryptoName);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_symbol), selectedCryptoSymbol);
                actualValueCryptoDetailFrag.setArguments(dataBundle);
                break;

            case 1: /* history info */
                dataBundle.putInt(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_tab), 2);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id), selectedCryptoId);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name), selectedCryptoName);
                dataBundle.putString(appActivity.getResources().getString(R.string.actual_value_crypto_detail_sharedpref_symbol), selectedCryptoSymbol);
                actualValueCryptoDetailFrag.setArguments(dataBundle);
                break;
        }

        return  actualValueCryptoDetailFrag;
    }

    @Override
    public int getCount() {
        return tabCount;
    }
}
