package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.example.skamav2.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Used for retrieving information regarding cryptocurrency values from internet.
 */
public class LogicActualValueCryptoDetail extends AsyncTask<Void, Void, Void> {
    enum tabList{GENERAL_TAB, HISTORY_TAB}; /* specifies tab which user wants to display (two tabs in settings - general and history tab) */

    /* variables assigned in constructor - START */
    private String cryptoId; /* id of selected cryptocurrency */
    private tabList tabChoice; /* to identify tab which was chosen by user - general or history */
    private Activity appActivity; /* reference to Activity of the application */
    private FragmentAlertsCrypto callFragment; /* reference to instance - used when working with alerts; report data */
    private String historyDateFrom; /* from which date history should be retrieved */
    private String historyDateTo; /* to which date history should be retrieved */
    private boolean justValueCheck; /* true when used with alerts */
    private boolean noInternet; /* true when internet connection is not available */
    private boolean goBackFragment; /* false when user should be moved to previous fragment on connection lost */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* number of source, from which information should be retrieved - default is 0 */
    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private double actualPrice; /* actual price of cryptocurrency in currency given by user */
    private ArrayList<HistoryDataPiece> historyData; /* ArrayList which stores historical data regarding given cryptocurrency */
    private HistoryDataPiece latestPrice; /* used when working with history tab; retrieve actual price for simulation purposes */
    /* important variables assigned during parsing process - END */

    /* constants which define part of web pages addresses, from which desired information could be retrieved - START */
    private final String PAGE_PREF0_FIRST = "https://api.coingecko.com/api/v3/simple/price?ids=ID_PLACEHOLDER&vs_currencies=CURRENCY_PLACEHOLDER&include_24hr_vol=false"; /* get actual value of given cryptocurrency */
    private final String PAGE_PREF0_SECOND = "https://api.coingecko.com/api/v3/coins/ID_PLACEHOLDER/market_chart?vs_currency=CURRENCY_PLACEHOLDER&days=1"; /* get values for last 24 hours ; for GENERAL_TAB */
    private final String PAGE_PREF0_THIRD = "https://api.coingecko.com/api/v3/coins/ID_PLACEHOLDER/market_chart/range?vs_currency=CURRENCY_PLACEHOLDER&from=FROM_PLACEHOLDER&to=TO_PLACEHOLDER"; /* get value for one given day ; for HISTORY_TAB */
    /* constants which define part of web pages addresses, from which desired information could be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains info regarding cryptocurrency values), which will be later parsed */
    /* other variables & objects used during parsing process - END */

    /* references to objects, which are in activity regarding cryptocurrency details - START */
    private TextView actualValueTv; /* reference to crypto_detail_frag1_act_val_tv2_id; TextView object which displays actual value of cryptocurrency */
    private LineChart dataChart; /* reference to actual_value_crypto_detail_frag1_chart_id or actual_value_crypto_detail_frag2_chart_id; object with chart */
    /* references to objects, which are in activity regarding cryptocurrency details - END */

    private double lastSelectedPrice; /* price - last selected point from chart */
    private String lastSelectedDate; /* date - last selected point from chart */
    private String lastSelectedTime; /* time - last selected point from chart */

    /**
     * Constructor expects use with general tab.
     * @param appContext application context
     * @param tabChoice retrieved data depends on chosen tab
     * @param cryptoId id of cryptocurrency selected by user
     */
    public LogicActualValueCryptoDetail(Context appContext, tabList tabChoice, String cryptoId){
        this.appActivity = (Activity) appContext;
        this.tabChoice = tabChoice;
        this.cryptoId = cryptoId;
        this.justValueCheck = false;
        this.noInternet = false;
        this.actualPrice = -1;
    }

    /**
     * Constructor expects use with history tab.
     * @param appContext application context
     * @param tabChoice retrieved data depends on chosen tab
     * @param cryptoId id of cryptocurrency selected by user
     * @param historyDateFrom date from which history chart should be displayed (expected format: day.month.year) - used just for history tab
     * @param historyDateTo date to which history chart should be displayed (expected format: day.month.year) - used just for history tab
     */
    public LogicActualValueCryptoDetail(Context appContext, tabList tabChoice, String cryptoId, String historyDateFrom, String historyDateTo){
        this.appActivity = (Activity) appContext;
        this.tabChoice = tabChoice;
        this.cryptoId = cryptoId;
        this.historyDateFrom = historyDateFrom;
        this.historyDateTo = historyDateTo;
        this.justValueCheck = false;
        this.noInternet = false;
        this.actualPrice = -1;
    }


    /**
     * Used when checking actual crypto value for reminders.
     * @param appContext application context
     * @param cryptoId id of cryptocurrency selected by user
     * @param callFragment reference to calling fragment
     */
    public LogicActualValueCryptoDetail(Context appContext, String cryptoId, FragmentAlertsCrypto callFragment){
        this.appActivity = (Activity) appContext;
        this.cryptoId = cryptoId;
        this.justValueCheck = true;
        this.callFragment = callFragment;
        this.noInternet = false;
        this.actualPrice = -1;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadPreferences();
        if(!justValueCheck)
        acquireRefViewsInfo();

        historyData = new ArrayList<>();

        if(!justValueCheck){
            parsingPd = new ProgressDialog(appActivity);
            parsingPd.setMessage(appActivity.getResources().getString(R.string.logic_retrieving));
            parsingPd.setCancelable(false);
            parsingPd.show();
        }
    }

    /**
     * Method is used to perform backgrounded tasks and cannot operate with UI elements (by Android OS definition).
     * Here is used for downloading web page source, which is parsed afterwards.
     * @param voids not used here
     * @return not used
     */
    @Override
    protected Void doInBackground(Void... voids) {
        parseInfo();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(noInternet && !justValueCheck){ /* lost internet connection - display alert */
            parsingPd.dismiss();

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(appActivity);
            alertBuilder.setTitle(appActivity.getResources().getString(R.string.no_internet_alert_title));
            alertBuilder.setMessage(appActivity.getResources().getString(R.string.no_internet_alert_mes));
            alertBuilder.setPositiveButton(appActivity.getResources().getString(R.string.no_internet_alert_ok),null);
            alertBuilder.show();

            /* go back to previous fragment */
            if(!goBackFragment)
            appActivity.onBackPressed();
            return;
        }

        if(justValueCheck){
            callFragment.reportAlert(actualPrice, cryptoId);
            return;
        }
        parsingPd.dismiss();

        if(historyData != null && historyData.size() != 0){
            showRetrievedInfo();
        }else{
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(appActivity);
            alertBuilder.setTitle(appActivity.getResources().getString(R.string.actual_value_stocks_fill_title));
            alertBuilder.setMessage(appActivity.getResources().getString(R.string.actual_value_stocks_data_mes));
            alertBuilder.setPositiveButton(appActivity.getResources().getString(R.string.actual_value_stocks_fill_ok),null);
            alertBuilder.show();
        }
    }

    /**
     * Parses code of web page, which is used for getting information regarding cryptocurrency values.
     * Parsing process provides pieces of information, which are then stored in respective variables (actualPrice and historyData).
     */
    private void parseInfo(){
        switch(sourceNumPref){
            case 0:
                if(tabChoice == tabList.GENERAL_TAB){ /* for general tab retrieve actual price + 24 hour history data */
                    String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                    String actualPricePrepLink = PAGE_PREF0_FIRST.replace("ID_PLACEHOLDER", cryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]);
                    retrievePageDOM(actualPricePrepLink);
                    if(pageDOM == null){
                        noInternet = true;
                        return;
                    }
                    retrieveInfo0first();

                    String lastDayHistoryPrepLink = PAGE_PREF0_SECOND.replace("ID_PLACEHOLDER", cryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]);
                    retrievePageDOM(lastDayHistoryPrepLink);
                    if(pageDOM == null){
                        noInternet = true;
                        return;
                    }
                    retrieveInfo0second();
                }else if(tabChoice == tabList.HISTORY_TAB){ /* for history tab retrieve values for days between user given dates */
                    goBackFragment = true;

                    /* retrieve also actual price for simulation purposes - START */
                    String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                    String actualPricePrepLink = PAGE_PREF0_FIRST.replace("ID_PLACEHOLDER", cryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]);
                    retrievePageDOM(actualPricePrepLink);
                    if(pageDOM == null){
                        noInternet = true;
                        return;
                    }
                    retrieveInfo0first();
                    /* retrieve also actual price for simulation purposes - END */

                    String[] splitHistoryDateFrom = historyDateFrom.split("\\.");
                    String historyDateFromDay = splitHistoryDateFrom[0];
                    String historyDateFromMonth = splitHistoryDateFrom[1];
                    String historyDateFromYear = splitHistoryDateFrom[2];

                    String[] splitHistoryDateTo = historyDateTo.split("\\.");
                    String historyDateToDay = splitHistoryDateTo[0];
                    String historyDateToMonth = splitHistoryDateTo[1];
                    String historyDateToYear = splitHistoryDateTo[2];

                    /* convert date to correspond with unix timestamp */
                    Date startDate = new Date(Integer.parseInt(historyDateFromYear) - 1900, Integer.parseInt(historyDateFromMonth) - 1, Integer.parseInt(historyDateFromDay));

                    Date endDate = new Date(Integer.parseInt(historyDateToYear) - 1900, Integer.parseInt(historyDateToMonth) - 1, Integer.parseInt(historyDateToDay));
                    endDate = new Date(endDate.getTime() + TimeUnit.DAYS.toMillis(1)); /* increment by one day */

                    long startDateUnix = startDate.getTime() / 1000;
                    long endDateUnix = endDate.getTime() / 1000;

                    String pageToRetrieve = PAGE_PREF0_THIRD.replace("ID_PLACEHOLDER", cryptoId).replace("FROM_PLACEHOLDER", String.valueOf(startDateUnix)).replace("TO_PLACEHOLDER", String.valueOf(endDateUnix)).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]);
                    retrievePageDOM(pageToRetrieve);
                    if(pageDOM == null){
                        noInternet = true;
                        return;
                    }
                    retrieveInfo0first(); /* to get actual price for simulation */
                    retrieveInfo0third();
                }else{ /* for reminders */
                    String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                    String actualPricePrepLink = PAGE_PREF0_FIRST.replace("ID_PLACEHOLDER", cryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]);
                    retrievePageDOM(actualPricePrepLink);
                    if(pageDOM == null){
                        noInternet = true;
                        return;
                    }
                    retrieveInfo0first();
                }
                break;

            case 1:
                retrieveInfo1();
                break;

            case 2:
                retrieveInfo2();
                break;
        }
    }

    /**
     * Downloads source code of web page, which contains information regarding cryptocurrency values and build DOM from it.
     * @param pageLink website link which will be used to retrieve needed information
     */
    private void retrievePageDOM(String pageLink){
        try{
            pageDOM = Jsoup.connect(pageLink).timeout(12000).ignoreContentType(true).get();
        } catch (Exception e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page which contains data regarding cryptocurrency values.
     * This method is created for use with link located in the variable "" (first link).
     */
    private void retrieveInfo0first(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONObject jsonData = jsonToParse.getJSONObject(cryptoId);

            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            actualPrice = jsonData.getDouble(currencyArray[currencyPref].toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void retrieveInfo0second(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONArray jsonData = jsonToParse.getJSONArray("prices"); /* key = unix timestamp, which tells to which date the given price is valid; value = price of cryptocurrency */

            for(int i = 0; i < jsonData.length(); i++){ /* iterate through array and get wanted data */
                String timeValuePiece = jsonData.get(i).toString();
                String[] timeValuePieceSplit = timeValuePiece.split(",");

                String timeString = timeValuePieceSplit[0].substring(1); /* remove [ at the beginning */
                String valueString = timeValuePieceSplit[1].substring(0, timeValuePieceSplit[1].length() - 1); /* remove ] at the end */

                historyData.add(new HistoryDataPiece(Long.parseLong(timeString), Double.parseDouble(valueString)));
            }

            /* add actual value */
            historyData.add(new HistoryDataPiece(System.currentTimeMillis(), actualPrice));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void retrieveInfo0third(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONArray jsonData = jsonToParse.getJSONArray("prices"); /* key = unix timestamp, which tells to which date the given price is valid; value = price of cryptocurrency */
            for(int i = 0; i < jsonData.length(); i++){ /* iterate through array and get wanted data */
                String timeValuePiece = jsonData.get(i).toString();
                String[] timeValuePieceSplit = timeValuePiece.split(",");

                String timeString = timeValuePieceSplit[0].substring(1); /* remove [ at the beginning */
                String valueString = timeValuePieceSplit[1].substring(0, timeValuePieceSplit[1].length() - 1); /* remove ] at the end */

                historyData.add(new HistoryDataPiece(Long.parseLong(timeString), Double.parseDouble(valueString)));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns String array which contains data for simulation purposes. On 0 index is actual price of the crypto and on 1 index is actual time.
     * @return String array with desired data
     */
    public String[] getLatestPriceReadable(){
        String[] latestPrice = new String[2];
        latestPrice[0] = String.valueOf(actualPrice);
        latestPrice[1] = dataChart.getXAxis().getValueFormatter().getFormattedValue(System.currentTimeMillis(), null);

        return latestPrice;
    }

    /**
     * Parses downloaded DOM of the web page which contains data regarding cryptocurrency values.
     * This method is created for use with second link.
     */
    private void retrieveInfo1(){

    }

    /**
     * Parses downloaded DOM of the web page which contains data regarding cryptocurrency values.
     * This method is created for use with third link.
     */
    private void retrieveInfo2(){

    }

    /**
     * Gets references to some objects which are later used to display retrieved data (regarding cryptocurrency values).
     */
    private void acquireRefViewsInfo(){
        switch(tabChoice){
            case GENERAL_TAB: /* general (first) tab */
                actualValueTv = appActivity.findViewById(R.id.crypto_detail_frag1_act_val_tv2_id);
                dataChart = appActivity.findViewById(R.id.crypto_detail_frag1_chart_id);
                break;

            case HISTORY_TAB: /* history (second) tab */
                dataChart = appActivity.findViewById(R.id.crypto_detail_frag2_chart_id);
                break;

            default:  /* unknown tab */
        }
    }

    /**
     * Shows retrieved information (regarding cryptocurrency values) to user.
     */
    private void showRetrievedInfo(){
        if(tabChoice == tabList.GENERAL_TAB){ /* for general tab display actual price + chart with 24 hour history data */
            dataChart.setTouchEnabled(true);
            dataChart.setDrawMarkerViews(true);
            ActualValueMarker customMarkerView = new ActualValueMarker(appActivity.getApplicationContext(), R.layout.actual_value_item);
            dataChart.setMarkerView(customMarkerView);

            /* hide legend */
            dataChart.getLegend().setEnabled(false);
            dataChart.getDescription().setEnabled(false);

            /* limit list of visible x value labels (solves text overlapping) - START */
            dataChart.getXAxis().setGranularityEnabled(true);
            dataChart.getXAxis().setGranularity(1.0f);
            dataChart.getXAxis().setLabelCount(3);
            /* limit list of visible x value labels (solves text overlapping) - END */

            /* display actual value of cryptocurrency - START */
            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            DecimalFormat decFor = new DecimalFormat("#.####");
            DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
            decForSym.setDecimalSeparator('.');
            decFor.setDecimalFormatSymbols(decForSym);
            actualValueTv.setText(" " + decFor.format(actualPrice) + " " + currencyArray[currencyPref]);

            /* display actual value of cryptocurrency - END */

            /* display chart with 24 hour history - START */
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i = 0; i < historyData.size(); i++){
                HistoryDataPiece onePiece = historyData.get(i);
                entries.add(new Entry(onePiece.getTime(), (float) onePiece.getValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Customized values");
            LineData data = new LineData(dataSet);
            dataChart.setData(data);

            IAxisValueFormatter dateFormatter = new DateFormatter();
            XAxis xAxis = dataChart.getXAxis();
            xAxis.setValueFormatter(dateFormatter);
            dataChart.invalidate();
            /* display chart with 24 hour history - END */
        }else if(tabChoice == tabList.HISTORY_TAB){ /* for history tab display just chart with history data for days between user given dates */
            dataChart.setTouchEnabled(true);
            dataChart.setDrawMarkerViews(true);
            ActualValueMarker customMarkerView = new ActualValueMarker(appActivity.getApplicationContext(), R.layout.actual_value_item);
            dataChart.setMarkerView(customMarkerView);

            /* hide legend */
            dataChart.getLegend().setEnabled(false);
            dataChart.getDescription().setEnabled(false);

            /* limit list of visible x value labels (solves text overlapping) - START */
            dataChart.getXAxis().setGranularityEnabled(true);
            dataChart.getXAxis().setGranularity(1.0f);
            dataChart.getXAxis().setLabelCount(3);
            /* limit list of visible x value labels (solves text overlapping) - END */

            /* display chart with history - START */
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i = 0; i < historyData.size(); i++){
                HistoryDataPiece onePiece = historyData.get(i);
                entries.add(new Entry(onePiece.getTime(), (float) onePiece.getValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Customized values");
            LineData data = new LineData(dataSet);
            dataChart.setData(data);

            IAxisValueFormatter dateFormatter = new DateFormatter();
            XAxis xAxis = dataChart.getXAxis();
            xAxis.setValueFormatter(dateFormatter);
            dataChart.invalidate();
            /* display chart with history - END */
        }
    }

    /**
     * Loads preferences regarding cryptocurrency values. In this case: preferred ordinal currency (eur, usd...) + preferred source number.
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        sourceNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_crypto_actual_pref), 0); /* preferred source number */
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */

        sourceNumPref = 0;
    }

    /**
     * Getter for lastSelectedPrice. Returns price represented by last selected point from the chart.
     * @return price of the last selected point
     */
    public double getLastSelectedPrice(){
        return lastSelectedPrice;
    }

    /**
     * Getter for lastSelectedDate. Returns date represented by last selected point from the chart.
     * @return date of the last selected point
     */
    public String getLastSelectedDate(){
        return lastSelectedDate;
    }

    /**
     * Getter for lastSelectedTime. Returns time represented by last selected point from the chart.
     * @return time of the last selected point
     */
    public String getLastSelectedTime(){
        return lastSelectedTime;
    }

    /**
     * Inner data class, carries data regarding cryptocurrency price at some point in history.
     */
    public static class HistoryDataPiece{
        private long time; /* time to which is value valid (unix timestamp) */
        private double value; /* value of cryptocurrency in user selected currency (eur, usd...) */

        /**
         * Constructor takes just cryptocurrency value and time, to which is given value valid (unix timestamp).
         * @param time time to which given value is valid (unix timestamp)
         * @param value value of cryptocurrency
         */
        public HistoryDataPiece(long time, double value){
            this.time = time;
            this.value = value;
        }

        /**
         * Getter for time.
         * @return time to which is value valid (unix timestamp)
         */
        public long getTime() {
            return time;
        }

        /**
         * Getter for cryptocurrency value (in user selected currency - eur, usd...).
         * @return value of cryptocurrency
         */
        public double getValue() {
            return value;
        }
    }

    /**
     * Inner class which helps with converting of unix timestamp to human readable date (and time). Converted value is finally displayed in chart. Used for 24 hour history.
     */
    public class DateFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Date date = new Date((long) value);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            String yearCut = String.valueOf(year).substring(2);

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int min = cal.get(Calendar.MINUTE);
            int sec = cal.get(Calendar.SECOND);

            /* edit hour & minute & sec to always have two digits - START */
            String hourEdit = String.valueOf(hour);
            String minEdit = String.valueOf(min);
            String secEdit = String.valueOf(sec);

            if(hourEdit.length() == 1){
                hourEdit = "0" + hourEdit;
            }

            if(minEdit.length() == 1){
                minEdit = "0" + minEdit;
            }

            if(secEdit.length() == 1){
                secEdit = "0" + secEdit;
            }
            /* edit hour & minute & sec to always have two digits - END */

            String dateEdit = day + "." + month + "." + yearCut + " " + hourEdit + ":" + minEdit + ":" + secEdit;

            return dateEdit;
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    public class ActualValueMarker extends MarkerView{
        private TextView actualPriceTv;
        private TextView actualTimeTv;

        public ActualValueMarker(Context context, int layoutResource) {
            super(context, layoutResource);
            this.actualPriceTv = findViewById(R.id.actual_value_item_date_tv2_id);
            this.actualTimeTv = findViewById(R.id.actual_value_item_price_tv2_id);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            Date date = new Date((long) e.getX());

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;

            int year = cal.get(Calendar.YEAR);
            String yearCut = String.valueOf(year).substring(2);

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int min = cal.get(Calendar.MINUTE);
            int sec = cal.get(Calendar.SECOND);

            /* edit hour & minute & sec to always have two digits - START */
            String hourEdit = String.valueOf(hour);
            String minEdit = String.valueOf(min);
            String secEdit = String.valueOf(sec);

            if(hourEdit.length() == 1){
                hourEdit = "0" + hourEdit;
            }

            if(minEdit.length() == 1){
                minEdit = "0" + minEdit;
            }

            if(secEdit.length() == 1){
                secEdit = "0" + secEdit;
            }
            /* edit hour & minute & sec to always have two digits - END */

            String dateEdit = day + "." + month + "." + yearCut + " " + hourEdit + ":" + minEdit + ":" + secEdit;

            lastSelectedDate = day + "." + month + "." + yearCut;
            lastSelectedTime = hourEdit + ":" + minEdit + ":" + secEdit;

            actualPriceTv.setText(dateEdit);
            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            DecimalFormat decFor = new DecimalFormat("#.####");
            DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
            decForSym.setDecimalSeparator('.');
            decFor.setDecimalFormatSymbols(decForSym);
            actualTimeTv.setText(String.valueOf(decFor.format(e.getY())) + " " + currencyArray[currencyPref]);

            String[] splitPrice = actualTimeTv.getText().toString().split(" ");
            lastSelectedPrice = Double.valueOf(splitPrice[0]);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth()), -getHeight());
        }
    }
}
