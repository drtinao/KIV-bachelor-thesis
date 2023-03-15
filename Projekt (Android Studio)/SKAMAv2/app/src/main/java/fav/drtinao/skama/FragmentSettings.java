package fav.drtinao.skama;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.skamav2.R;

/**
 * Displays settings Fragment, which is basically part of the application in which can user set his preferences and so on.
 */
public class FragmentSettings extends Fragment {
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEd;

    /* variables used for detecting user interaction with Spinners; when user tapped at least once, then true, else false - START */
    private boolean cryptoNewsSpTap;
    private boolean cryptoWalletBtcTap;
    private boolean cryptoWalletLtcTap;
    private boolean stocksNewsTap;
    private boolean generalCurrencyTap;
    /* variables used for detecting user interaction with Spinners; when user tapped at least once, then true, else false - END */

    /**
     * Is called by Android OS when layout should be inflated, here settings_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.other_settings_menu);

        setHasOptionsMenu(true);
        Bundle dataBundle = getArguments();
        int position = dataBundle.getInt(getResources().getString(R.string.settings_sharedpref_tab), -1);

        View createdLayout = null;

        cryptoNewsSpTap = false;
        cryptoWalletBtcTap = false;
        cryptoWalletLtcTap = false;
        stocksNewsTap = false;
        generalCurrencyTap = false;

        switch(position){
            case 2:
                createdLayout = inflater.inflate(R.layout.fragment_settings_frag2, container, false);
                prepCryptoActualPrefSp(createdLayout);
                prepCryptoNewsPrefSp(createdLayout);
                prepCryptoWalletBtcPrefSp(createdLayout);
                prepCryptoWalletLtcPrefSp(createdLayout);
                break;

            case 3:
                createdLayout = inflater.inflate(R.layout.fragment_settings_frag3, container, false);
                prepStocksNewsPrefSp(createdLayout);
                break;

            default:
                createdLayout = inflater.inflate(R.layout.fragment_settings_frag1, container, false);
                prepGeneralCurrencyPrefSp(createdLayout);
    }

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        sharedPrefEd = sharedPref.edit(); /* modify preferences */
    }

    /**
     * Prepares Spinner which allows user to pick preffered ordinal currency (eur, usd...) - the given currency will be later used in various situations (eg. if user wants to know actual value of some cryptocurrency,
     * the value will be displayed in currency specified by user).
     * @param view needed for interaction with UI elements
     */
    private void prepGeneralCurrencyPrefSp(View view){
        Spinner generalCurrencyPrefSpinner = view.findViewById(R.id.settings_general_currency_pref_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.settings_general_currency_pref_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        generalCurrencyPrefSpinner.setAdapter(dataAdapter);

        generalCurrencyPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_currency_pref), 0)); /* first option will be default */
        generalCurrencyPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                if(generalCurrencyTap){
                    Toast.makeText(getActivity(), getResources().getString(R.string.settings_pref_cur_selected), Toast.LENGTH_SHORT).show();
                }else{
                    generalCurrencyTap = false;
                }
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_currency_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        generalCurrencyPrefSpinner.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                generalCurrencyTap = true;
                return false;
            }
        });
    }

    /**
     * Prepares Spinner which allows user to pick source from which actual values of cryptocurrencies will be retrieved.
     * @param view needed for interaction with UI elements
     */
    private void prepCryptoActualPrefSp(View view){
        Spinner cryptoActualPrefSpinner = view.findViewById(R.id.settings_crypto_news_pref_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.settings_crypto_news_pref_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        cryptoActualPrefSpinner.setAdapter(dataAdapter);

        cryptoActualPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_crypto_actual_pref), 0)); /* first option will be default */
        cryptoActualPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_crypto_actual_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Prepares Spinner which allows user to pick source from which news regarding cryptocurrencies will be retrieved.
     * @param view needed for interaction with UI elements
     */
    private void prepCryptoNewsPrefSp(View view){
        Spinner cryptoNewsPrefSpinner = view.findViewById(R.id.settings_crypto_news_pref_spinner_id);

        cryptoNewsPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_crypto_news_pref), 0)); /* first option will be default */
        cryptoNewsPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                if(cryptoNewsSpTap){
                    Toast.makeText(view.getContext(), getResources().getString(R.string.settings_crypto_pref_news_selected), Toast.LENGTH_SHORT).show();
                }else{
                    cryptoNewsSpTap = false;
                }
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_crypto_news_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        cryptoNewsPrefSpinner.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cryptoNewsSpTap = true;
                return false;
            }
        });
    }

    /**
     * Prepares Spinner which allows user to pick source from which actual amount stored in bitcoin wallet will be retrieved.
     * @param view needed for interaction with UI elements
     */
    private void prepCryptoWalletBtcPrefSp(View view){
        Spinner cryptoWalletBtcPrefSpinner = view.findViewById(R.id.settings_crypto_wallet_btc_pref_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.settings_crypto_wallet_btc_pref_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        cryptoWalletBtcPrefSpinner.setAdapter(dataAdapter);

        cryptoWalletBtcPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_crypto_wallet_btc_pref), 0)); /* first option will be default */
        cryptoWalletBtcPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                if(cryptoWalletBtcTap){
                    Toast.makeText(view.getContext(), getResources().getString(R.string.settings_crypto_pref_btc_selected), Toast.LENGTH_SHORT).show();
                }else{
                    cryptoWalletBtcTap = false;
                }
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_crypto_wallet_btc_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        cryptoWalletBtcPrefSpinner.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cryptoWalletBtcTap = true;
                return false;
            }
        });
    }

    /**
     * Prepares Spinner which allows user to pick source from which actual amount stored in litecoin wallet will be retrieved.
     * @param view needed for interaction with UI elements
     */
    private void prepCryptoWalletLtcPrefSp(View view){
        Spinner cryptoWalletLtcPrefSpinner = view.findViewById(R.id.settings_crypto_wallet_ltc_pref_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.settings_crypto_wallet_ltc_pref_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        cryptoWalletLtcPrefSpinner.setAdapter(dataAdapter);

        cryptoWalletLtcPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_crypto_wallet_ltc_pref), 0)); /* first option will be default */
        cryptoWalletLtcPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                if(cryptoWalletLtcTap){
                    Toast.makeText(view.getContext(), getResources().getString(R.string.settings_crypto_pref_ltc_selected), Toast.LENGTH_SHORT).show();
                }else{
                    cryptoWalletLtcTap = false;
                }
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_crypto_wallet_ltc_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        cryptoWalletLtcPrefSpinner.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cryptoWalletLtcTap = true;
                return false;
            }
        });
    }

    /**
     * Prepares Spinner which allows user to pick source from which news regarding stocks will be retrieved.
     * @param view needed for interaction with UI elements
     */
    private void prepStocksNewsPrefSp(View view){
        Spinner stocksNewsPrefSpinner = view.findViewById(R.id.settings_stocks_news_pref_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.settings_stocks_news_pref_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        stocksNewsPrefSpinner.setAdapter(dataAdapter);

        stocksNewsPrefSpinner.setSelection(sharedPref.getInt(getResources().getString(R.string.settings_sharedpref_stocks_news_pref), 0)); /* first option will be default */
        stocksNewsPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                if(stocksNewsTap){
                    Toast.makeText(view.getContext(), getResources().getString(R.string.settings_stocks_pref_news_selected), Toast.LENGTH_SHORT).show();
                }else{
                    stocksNewsTap = false;
                }
                sharedPrefEd.putInt(getResources().getString(R.string.settings_sharedpref_stocks_news_pref), parent.getSelectedItemPosition());
                sharedPrefEd.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        stocksNewsPrefSpinner.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                stocksNewsTap = true;
                return false;
            }
        });
    }
}
