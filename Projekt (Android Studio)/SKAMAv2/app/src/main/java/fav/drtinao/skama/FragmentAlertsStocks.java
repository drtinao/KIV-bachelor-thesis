package fav.drtinao.skama;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

/**
 * Is responsible for displaying part of the application, which allows user to manage existing stocks alerts.
 */
public class FragmentAlertsStocks extends Fragment {
    private ListView alertsStocksLV; /* reference to alerts_stocks_lv; ListView object which will be used for displaying active alerts */
    private View createdLayoutView; /* reference to inflated layout, needed for working with elements defined in the layout */
    private ArrayList<AlertStockData> alertDataList; /* ArrayList with AlertStockData objects; each one represents one alert regarding to stocks */
    StocksAlertsAdapter stocksAlertsAdapter; /* data adapter */
    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private boolean alertDisplay; /* true if alert informing about lost connection is displayed */

    /**
     * Is called by Android OS when layout should be inflated, here alerts_stocks_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.alert_crypto_menu);
        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.alerts_stocks_fragment, container, false);
        createdLayoutView = createdLayout;

        /* get reference to ListView with alerts */
        alertsStocksLV = createdLayoutView.findViewById(R.id.alerts_stocks_lv);

        loadCreatedAlerts();
        showCreatedAlerts();

        return createdLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAlerts();
    }

    /**
     * ActionBar will have button, through which can user add alert.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.history_nav, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Is called when button / icon from ActionBar is tapped. Just button for adding alert present.
     * @param item MenuItem object which represents tapped item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentActualValueStocks()).addToBackStack(null).commit();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads data regarding to user created alerts from SharedPreferences, name "stocks_alerts".
     * Each reminder is saved in format: ";;;stock_sym;;;BELOW/ABOVE;;;target_price".
     */
    private void loadCreatedAlerts(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */

        String createdStocksAlerts = sharedPref.getString(getResources().getString(R.string.sharedpref_stocks_alerts), "");
        String[] createdStocksAlertsSplit = createdStocksAlerts.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items */

        alertDataList = new ArrayList<>();

        /* start from 1 - first is empty */
        for(int i = 1; i < createdStocksAlertsSplit.length; i += 3){
            String symbol = createdStocksAlertsSplit[i];
            String type = createdStocksAlertsSplit[i + 1];
            String targetPrice = createdStocksAlertsSplit[i + 2];

            double targetPriceDouble = Double.valueOf(targetPrice);

            AlertStockData savedAlert = new AlertStockData(symbol, targetPriceDouble, type);
            alertDataList.add(savedAlert);
        }
    }

    /**
     * Used for reporting data from LogicActualValueStocksDetail instances.
     * @param actualValue actual value of the selected stock
     * @param stockSymbol symbol of the checked stock
     */
    public void reportAlert(double actualValue, String stockSymbol){
        if(actualValue == -1 && !alertDisplay){ /* probably connection lost */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getActivity().getResources().getString(R.string.no_internet_alert_title));
            alertBuilder.setMessage(getActivity().getResources().getString(R.string.no_internet_alert_mes));
            alertBuilder.setPositiveButton(getActivity().getResources().getString(R.string.no_internet_alert_ok),null);
            alertBuilder.show();
            alertDisplay = true;
        }

        if(actualValue == -1){
            return;
        }

        DecimalFormat decFor = new DecimalFormat("#.####");
        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
        decForSym.setDecimalSeparator('.');
        decFor.setDecimalFormatSymbols(decForSym);

        /* get matching crypto */
        FragmentAlertsStocks.AlertStockData match = null;
        for(int i = 0; i < alertDataList.size(); i++){
            if(alertDataList.get(i).getStockSymbol().equals(stockSymbol)){ /* ok, got match */
                match = alertDataList.get(i);
            }
        }

        /* check if condition is filled */
        if(match.getTypeAlert().equals(getActivity().getResources().getString(R.string.sharedpref_alert_type_below)) && actualValue < match.getTargetPrice()){
            String[] curArr = getResources().getStringArray(R.array.settings_general_currency_pref_list);
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
            String mesToSet = getResources().getString(R.string.alerts_stocks_mes1) + " " + match.getStockSymbol() + " " + getResources().getString(R.string.alerts_stocks_up_mes2) + " " + decFor.format(match.getTargetPrice()) + " " + curArr[currencyPref] + getResources().getString(R.string.alerts_stocks_mes3) + getResources().getString(R.string.alerts_stocks_mes4) + " " + decFor.format(actualValue) + " " + curArr[currencyPref] + getResources().getString(R.string.alerts_stocks_mes3);

            alertBuilder.setMessage(mesToSet);
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
            alertBuilder.show();

            stocksAlertsAdapter.removeStockAlert(match.getStockSymbol());
        }else if(match.getTypeAlert().equals(getActivity().getResources().getString(R.string.sharedpref_alert_type_above)) && actualValue > match.getTargetPrice()){
            String[] curArr = getResources().getStringArray(R.array.settings_general_currency_pref_list);
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
            String mesToSet = getResources().getString(R.string.alerts_stocks_mes1) + " " + match.getStockSymbol() + " " + getResources().getString(R.string.alerts_stocks_up_mes2) + " " + decFor.format(match.getTargetPrice()) + " " + curArr[currencyPref] + getResources().getString(R.string.alerts_stocks_mes3) + getResources().getString(R.string.alerts_stocks_mes4) + " " + decFor.format(actualValue) + " " + curArr[currencyPref] + getResources().getString(R.string.alerts_stocks_mes3);

            alertBuilder.setMessage(mesToSet);
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
            alertBuilder.show();

            stocksAlertsAdapter.removeStockAlert(match.getStockSymbol());
        }
    }

    /**
     * Checks if some alerts regarding to stocks should be displayed and if so, then displays them.
     */
    private void checkAlerts(){
        /* retrieve actual value for all watched cryptos and then compare values */
        for(int i = 0; i < alertDataList.size(); i++){
            LogicActualValueStocksDetail cryptoDetail = new LogicActualValueStocksDetail(getActivity(), alertDataList.get(i).getStockSymbol(), this);
            cryptoDetail.execute();
        }
    }

    /**
     * Represents data from ArrayList alertDataList to user. The ArrayList contains information regarding created alerts.
     */
    private void showCreatedAlerts(){
        if(alertDataList.size() == 0){ /* no alerts saved yet */
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.alerts_stocks_no_saved_toast), Toast.LENGTH_LONG).show();
            return;
        }

        stocksAlertsAdapter = new StocksAlertsAdapter(getActivity(), alertDataList, false);
        alertsStocksLV.setAdapter(stocksAlertsAdapter);
        stocksAlertsAdapter.notifyDataSetChanged();
    }

    /**
     * Inner data class, is used for carrying data regarding to stock alert.
     */
    public static class AlertStockData{
        private String stockSymbol; /* symbol of the stock (or cryptocurrency name) */
        private double targetPrice; /* target price (given by user) */
        private String typeAlert; /* will be BELOW or ABOVE (user selection) */

        /**
         * Constructor is used for initialization of basic values regarding to the stock alert.
         * @param stockSymbol symbol of the stock
         * @param targetPrice price at which user wants to be reminded
         * @param typeAlert could be "BELOW" or "ABOVE"
         */
        public AlertStockData(String stockSymbol, double targetPrice, String typeAlert) {
            this.stockSymbol = stockSymbol;
            this.targetPrice = targetPrice;
            this.typeAlert = typeAlert;
        }

        /**
         * Getter for stock symbol (or cryptocurrency name).
         * @return stock symbol
         */
        public String getStockSymbol() {
            return stockSymbol;
        }

        /**
         * Getter for target price (given by user).
         * @return target price
         */
        public double getTargetPrice() {
            return targetPrice;
        }

        /**
         * Getter for type of the alert ("BELOW" or "ABOVE")
         * @return type of the alert
         */
        public String getTypeAlert() {
            return typeAlert;
        }
    }
}
