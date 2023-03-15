package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.example.skamav2.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Used for retrieving information regarding stocks values from internet.
 */
public class LogicActualValueStocksDetail extends AsyncTask<Void, Void, Void> {
    enum tabList{GENERAL_TAB, HISTORY_TAB}; /* specifies tab which user wants to display (two tabs in settings - general and history tab) */

    /* variables assigned in constructor - START */
    private String stockSym; /* symbol of selected stock */
    private LogicActualValueStocksDetail.tabList tabChoice; /* to identify tab which was chosen by user - general or history */
    private Activity appActivity; /* reference to Activity of the application */
    private ActualValueStocksDetailFrag callFragment; /* reference to fragment which executed the task (needed for info return) */
    private FragmentAlertsStocks callFragmentAlerts; /* reference to instance - used when working with alerts; report data */
    private FragmentActualValueStocks callFragmentCheck; /* reference to fragment which typically requests just check for stock existence */
    private String historyDateFrom; /* from which date history should be retrieved */
    private String historyDateTo; /* to which date history should be retrieved */
    private boolean justCheckExist; /* when true, then just stock existence chceck is performed */
    private boolean justValueCheck; /* true when used with alerts */
    private boolean noInternet; /* true when internet connection is not available */
    private boolean goBackFragment; /* false when user should be moved to previous fragment on connection lost */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* number of source, from which information should be retrieved - default is 0 */
    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private double actualPrice; /* actual price of stock in currency given by user */
    private ArrayList<LogicActualValueStocksDetail.HistoryDataPiece> historyData; /* ArrayList which stores historical data regarding given stock */
    private LogicActualValueStocksDetail.HistoryDataPiece latestPrice; /* used when working with history tab; retrieve actual price for simulation purposes */
    private boolean stockExists; /* true if stock with given symbol exists, else false */
    private double eurCzkConversionRate; /* conversion rate eur -> czk */
    private double eurUsdConversionRate; /* conversion rate eur -> usd */
    /* important variables assigned during parsing process - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    /* other variables & objects used during parsing process - END */

    private final String CONVERT_PAGE = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"; /* page, which will be used for converting values provided by YahooFinance api (in USD) to another currency */

    /**
     * Constructor expects use with general tab.
     * @param appContext application context
     * @param callFragment reference to calling fragment
     * @param tabChoice retrieved data depends on chosen tab
     * @param stockSym symbol of stock selected by user
     */
    public LogicActualValueStocksDetail(Context appContext, ActualValueStocksDetailFrag callFragment, LogicActualValueStocksDetail.tabList tabChoice, String stockSym){
        this.appActivity = (Activity) appContext;
        this.callFragment = callFragment;
        this.tabChoice = tabChoice;
        this.stockSym = stockSym;

        this.justCheckExist = false;
        this.justValueCheck = false;
        this.noInternet = false;
        this.eurCzkConversionRate = -1;
        this.eurUsdConversionRate = -1;
        this.actualPrice = -1;
    }

    /**
     * Constructor expects use with history tab.
     * @param appContext application context
     * @param callFragment reference to calling fragment
     * @param tabChoice retrieved data depends on chosen tab
     * @param stockSym symbol of stock selected by user
     * @param historyDateFrom date from which history chart should be displayed (expected format: day.month.year) - used just for history tab
     * @param historyDateTo date to which history chart should be displayed (expected format: day.month.year) - used just for history tab
     */
    public LogicActualValueStocksDetail(Context appContext, ActualValueStocksDetailFrag callFragment, LogicActualValueStocksDetail.tabList tabChoice, String stockSym, String historyDateFrom, String historyDateTo){
        this.appActivity = (Activity) appContext;
        this.callFragment = callFragment;
        this.tabChoice = tabChoice;
        this.stockSym = stockSym;
        this.historyDateFrom = historyDateFrom;
        this.historyDateTo = historyDateTo;

        this.justCheckExist = false;
        this.justValueCheck = false;
        this.noInternet = false;
        this.eurCzkConversionRate = -1;
        this.eurUsdConversionRate = -1;
        this.actualPrice = -1;
    }

    /**
     * When this constructor is used, then just check for stock existence is expected to be performed.
     * @param stockSym symbol of stock
     * @param callFragment reference to calling fragment
     * @param appContext application context
     */
    public LogicActualValueStocksDetail(String stockSym, FragmentActualValueStocks callFragment, Context appContext){
        this.appActivity = (Activity) appContext;
        this.stockSym = stockSym;
        this.callFragmentCheck = callFragment;

        this.justCheckExist = true;
        this.justValueCheck = false;
        this.noInternet = false;
        this.eurCzkConversionRate = -1;
        this.eurUsdConversionRate = -1;
        this.actualPrice = -1;
    }

    /**
     * Used when checking actual stock value for reminders.
     * @param appContext application context
     * @param stockSym symbol of stock
     * @param callFragment reference to calling fragment
     */
    public LogicActualValueStocksDetail(Context appContext, String stockSym, FragmentAlertsStocks callFragment){
        this.appActivity = (Activity) appContext;
        this.stockSym = stockSym;
        this.callFragmentAlerts = callFragment;

        this.justCheckExist = false;
        this.justValueCheck = true;
        this.noInternet = false;
        this.eurCzkConversionRate = -1;
        this.eurUsdConversionRate = -1;
        this.actualPrice = -1;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(justCheckExist){ /* nothing to do in this method when just existence check is expected */
            return;
        }

        loadPreferences();

        historyData = new ArrayList<>();
        stockExists = true;

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
        if(justCheckExist){
            performExistenceCheck();
        }else{
            parseInfo();
        }
        return null;
    }

    /**
     * Checks if stock symbol provided by user is valid or not.
     */
    private void performExistenceCheck(){
        try {
            Stock stockInfo = YahooFinance.get(stockSym);

            if(stockInfo == null || stockInfo.getQuote().getPrice() == null){ /* null if selected stock does not exist */
                stockExists = false;
            }else{ /* stock exists */
                stockExists = true;
            }
        } catch (IOException e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Parses code of web page, which is used for getting information regarding stocks values.
     * Parsing process provides pieces of information, which are then stored in respective variables (actualPrice and historyData).
     */
    private void parseInfo(){
        switch(sourceNumPref){
            case 0:
                if(tabChoice == LogicActualValueStocksDetail.tabList.GENERAL_TAB){ /* for general tab retrieve actual price + 24 hour history data */
                    retrieveInfo0first();
                    retrieveInfo0second();
                }else if(tabChoice == LogicActualValueStocksDetail.tabList.HISTORY_TAB){ /* for history tab retrieve values for days between user given dates */
                    retrieveInfo0third();
                }else{ /* for reminders */
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
     * Retrieves actual price of selected stock. Uses YahooFinance api to achieve the goal.
     */
    private void retrieveInfo0first(){
        try {
            Stock stockInfo = YahooFinance.get(stockSym);

            if(stockInfo == null || stockInfo.getQuote().getPrice() == null){ /* null if selected stock does not exist */
                stockExists = false;
                return;
            }

            BigDecimal actualPriceNum = stockInfo.getQuote().getPrice();

            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            actualPrice = convertToCurrency(actualPriceNum.doubleValue(), currencyArray[currencyPref]); /* convert to double */
        } catch (IOException e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Retrieves one month history of selected stock. Uses YahooFinance api to achieve the goal.
     */
    private void retrieveInfo0second(){
        Calendar fromHistory = Calendar.getInstance();
        Calendar toHistory = Calendar.getInstance();

        fromHistory.add(Calendar.MONTH, -1); /* one month ago */

        try {
            Stock stockInfo = YahooFinance.get(stockSym);
            if(stockInfo == null || stockInfo.getQuote().getPrice() == null){ /* null if selected stock does not exist */
                stockExists = false;
                return;
            }

            List<HistoricalQuote> stockHistory = stockInfo.getHistory(fromHistory, toHistory, Interval.DAILY);
            for(int i = 0; i < stockHistory.size(); i++){
                HistoricalQuote stockInfoPiece = stockHistory.get(i);

                Long time = stockInfoPiece.getDate().getTimeInMillis();
                Double value = (stockInfoPiece.getHigh().doubleValue() + stockInfoPiece.getLow().doubleValue()) / 2; /* get average */

                String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                historyData.add(new HistoryDataPiece(time, convertToCurrency(value, currencyArray[currencyPref])));
            }

            /* add actual value */
            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            historyData.add(new HistoryDataPiece(System.currentTimeMillis(), convertToCurrency(YahooFinance.get(stockSym).getQuote().getPrice().doubleValue(), currencyArray[currencyPref])));
        } catch (IOException e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Retrieves history for selected dates. Uses YahooFinance api to achieve the goal.
     */
    private void retrieveInfo0third() {
        noInternet = true;
        goBackFragment = true;

        String[] splitHistoryDateFrom = historyDateFrom.split("\\.");
        String historyDateFromDay = splitHistoryDateFrom[0];
        String historyDateFromMonth = splitHistoryDateFrom[1];
        String historyDateFromYear = splitHistoryDateFrom[2];

        String[] splitHistoryDateTo = historyDateTo.split("\\.");
        String historyDateToDay = splitHistoryDateTo[0];
        String historyDateToMonth = splitHistoryDateTo[1];
        String historyDateToYear = splitHistoryDateTo[2];

        Calendar fromHistoryDate = Calendar.getInstance();
        fromHistoryDate.set(Integer.valueOf(historyDateFromYear), Integer.valueOf(historyDateFromMonth) - 1, Integer.valueOf(historyDateFromDay));

        Calendar toHistoryDate = Calendar.getInstance();
        toHistoryDate.set(Integer.valueOf(historyDateToYear), Integer.valueOf(historyDateToMonth) - 1, Integer.valueOf(historyDateToDay));
        toHistoryDate.add(Calendar.DATE, 1); /* add one day */

        try {
            Stock stockInfo = YahooFinance.get(stockSym);
            noInternet = false;
            Calendar today = Calendar.getInstance();

            if(fromHistoryDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && fromHistoryDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)){
                BigDecimal actualPriceNum = stockInfo.getQuote().getPrice();

                String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                HistoryDataPiece toAdd = new HistoryDataPiece(System.currentTimeMillis(), convertToCurrency(actualPriceNum.doubleValue(), currencyArray[currencyPref]));

                historyData.add(toAdd);

                latestPrice = toAdd;

                return;
            }

            List<HistoricalQuote> stockHistory = stockInfo.getHistory(fromHistoryDate, toHistoryDate, Interval.DAILY);

            for(int i = 0; i < stockHistory.size(); i++){
                HistoricalQuote stockInfoPiece = stockHistory.get(i);
                if(stockInfoPiece == null || stockInfoPiece.getHigh() == null || stockInfoPiece.getLow() == null){ /* probably null if stocks did not existed in selected day */
                    continue;
                }

                Long time = stockInfoPiece.getDate().getTimeInMillis();
                Double value = (stockInfoPiece.getHigh().doubleValue() + stockInfoPiece.getLow().doubleValue()) / 2; /* get average */

                String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                historyData.add(new HistoryDataPiece(time, convertToCurrency(value, currencyArray[currencyPref])));

                /* retrieve actual value */
                latestPrice = new HistoryDataPiece(System.currentTimeMillis(), convertToCurrency(YahooFinance.get(stockSym).getQuote().getPrice().doubleValue(), currencyArray[currencyPref]));
            }
        } catch (IOException e) {
            stockExists = false;
            e.printStackTrace();
        }
    }

    private void retrieveInfo1(){

    }

    private void retrieveInfo2(){

    }

    /**
     * Converts amount given by YahooFinance api (in USD) to another currency using online service.
     * @param originalAmount original amount provided by YahooFinance api
     * @param toCurrency to which currency amount should be converted (CZK / EUR / USD)
     * @return converted price in desired currency
     */
    private double convertToCurrency(double originalAmount, String toCurrency){
        if(toCurrency.equals("USD")){ /* nothing to convert */
            return originalAmount;
        }

        Document pageDOM = null;

        if(eurCzkConversionRate == -1 || eurUsdConversionRate == -1){
            try{
                pageDOM = Jsoup.connect(CONVERT_PAGE).timeout(12000).ignoreContentType(true).get();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        if(pageDOM != null){
            String pageString = pageDOM.toString();
            String[] splitCzk = pageString.split("currency=\"CZK\" rate=\"");
            splitCzk = splitCzk[1].split("\"");

            eurCzkConversionRate = Double.valueOf(splitCzk[0]);

            String[] splitUsd = pageString.split("currency=\"USD\" rate=\"");
            splitUsd = splitUsd[1].split("\"");
            eurUsdConversionRate = Double.valueOf(splitUsd[0]);
        }

        /* convert amount from usd to eur to match with eur table - START */
        if(eurUsdConversionRate > 1){
            originalAmount = originalAmount / eurUsdConversionRate;
        }else{
            originalAmount = originalAmount * eurUsdConversionRate;
        }
        /* convert amount from usd to eur to match with eur table - END */

        if(toCurrency.equals("EUR")){
            return originalAmount;
        }else{ /* CZK */
            originalAmount = originalAmount * eurCzkConversionRate;
            return originalAmount;
        }
    }

    /**
     * Returns latest price of the stock which is provided by the server.
     * @return latest price
     */
    public LogicActualValueStocksDetail.HistoryDataPiece getLatestPrice(){
        if(latestPrice == null){ /* display one month history */
            return historyData.get(historyData.size() - 1);
        }else{ /* display history between user selected dates */
            return latestPrice;
        }
    }

    /**
     * Loads preferences regarding stocks values. In this case: preferred ordinal currency (eur, usd...) + preferred source number.
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        sourceNumPref = sharedPref.getInt("stocks_actual_pref_num", 0); /* preferred source number */
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */

        sourceNumPref = 0;
    }

    /**
     * Inner data class, carries data regarding stock price at some point in history.
     */
    public static class HistoryDataPiece{
        private long time; /* time to which is value valid (unix timestamp) */
        private double value; /* value of stock in user selected currency (eur, usd...) */

        /**
         * Constructor takes just stock value and time, to which is given value valid (unix timestamp).
         * @param time time to which given value is valid (unix timestamp)
         * @param value value of stock
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
         * Getter for stock value (in user selected currency - eur, usd...).
         * @return value of stock
         */
        public double getValue() {
            return value;
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(noInternet){ /* lost internet connection - display alert */
            if(parsingPd != null)
            parsingPd.dismiss();

            if(!justValueCheck){
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(appActivity);
                alertBuilder.setTitle(appActivity.getResources().getString(R.string.no_internet_alert_title));
                alertBuilder.setMessage(appActivity.getResources().getString(R.string.no_internet_alert_mes));
                alertBuilder.setPositiveButton(appActivity.getResources().getString(R.string.no_internet_alert_ok),null);
                alertBuilder.show();
            }else{
                callFragmentAlerts.reportAlert(actualPrice, stockSym);
            }

            /* go back to previous fragment */
            if(parsingPd != null && !goBackFragment)
            appActivity.onBackPressed();
            return;
        }

        if(justCheckExist){
            callFragmentCheck.setStockExistence(stockExists);
            return;
        }

        if(justValueCheck){
            callFragmentAlerts.reportAlert(actualPrice, stockSym);
            return;
        }

        parsingPd.dismiss();

        /* if stock does not exist, return null */
        if(!stockExists){
            callFragment.setHistoryData(null);
            return;
        }

        if(tabChoice == tabList.GENERAL_TAB){ /* for general tab return actual price + one month history */
            callFragment.setActualPrice(actualPrice);
            callFragment.setHistoryData(historyData);
        }else if(tabChoice == tabList.HISTORY_TAB){ /* for history tab return just history (corresponding with user given dates) */
            callFragment.setHistoryData(historyData);
        }
    }
}
