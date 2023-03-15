package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import com.example.skamav2.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
 * Class is used for retrieving information regarding available cryptocurrencies from internet. Used mainly in converter and actual state part of the application.
 */
public class LogicCryptoList extends AsyncTask<Void, Void, Void> {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private FragmentConverterCrypto callFragmentConverterCrypto; /* reference to fragment which executed the task (needed for info return) - not null if called by FragmentConverterCrypto */
    private FragmentActualValueCrypto callFragmentActualValueCrypto; /* reference to fragment which executed the task (needed for info return) - not null if called by FragmentActualValueCrypto */
    private boolean noInternet; /* true when internet connection is not available */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* cryptocurrency data source number - default source num is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private ArrayList<CryptoSupport> cryptoList; /* cryptocurrency list (contains id, name, symbol) */
    /* important variables assigned during parsing process - END */

    /* constants which define web pages, from which information regarding cryptocurrencies can be retrieved - START */
    private final String PAGE_PREF0 = "https://api.coingecko.com/api/v3/coins/list";
    /* constants which define web pages, from which information regarding cryptocurrencies can be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains cryptocurrency list), which will be later parsed */
    /* other variables & objects used during parsing process - END */

    /**
     * Constructor takes application context, which is needed for working with UI elements and reference to calling fragment (for data return). For use with FragmentConverterCrypto fragment.
     * @param appContext application context
     * @param callFragment reference to calling fragment
     */
    public LogicCryptoList(Context appContext, FragmentConverterCrypto callFragment) {
        this.appActivity = (Activity) appContext;
        this.callFragmentConverterCrypto = callFragment;
    }

    /**
     * Constructor takes application context, which is needed for working with UI elements and reference to calling fragment (for data return). For use with ActualValueCryptoDetailFrag fragment.
     * @param appContext application context
     * @param callFragment reference to calling fragment
     */
    public LogicCryptoList(Context appContext, FragmentActualValueCrypto callFragment){
        this.appActivity = (Activity) appContext;
        this.callFragmentActualValueCrypto = callFragment;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadPreferences();

        cryptoList = new ArrayList<>();

        parsingPd = new ProgressDialog(appActivity);
        parsingPd.setMessage(appActivity.getResources().getString(R.string.logic_retrieving));
        parsingPd.setCancelable(false);
        parsingPd.show();
    }

    /**
     * Method is used to perform backgrounded tasks and cannot operate with UI elements (by Android OS definition).
     * Here is used for downloading web page source, which is parsed afterwards.
     * @param voids not used
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
        if(noInternet){ /* lost internet connection - display alert */
            parsingPd.dismiss();

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(appActivity);
            alertBuilder.setTitle(appActivity.getResources().getString(R.string.no_internet_alert_title));
            alertBuilder.setMessage(appActivity.getResources().getString(R.string.no_internet_alert_mes));
            alertBuilder.setPositiveButton(appActivity.getResources().getString(R.string.no_internet_alert_ok),null);
            alertBuilder.show();

            ((AppCompatActivity)appActivity).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentWalletCrypto()).addToBackStack(null).commit();
            return;
        }

        parsingPd.dismiss();
        if(callFragmentConverterCrypto != null){ /* if executed by FragmentConverterCrypto */
            callFragmentConverterCrypto.setCryptoList(cryptoList); /* return retrieved data */
        }else{ /* if executed by FragmentActualValueCrypto */
            callFragmentActualValueCrypto.setCryptoList(cryptoList); /* return retrieved data */
        }
    }

    /**
     * Parses code of web page which is used for getting information about available cryptocurrencies.
     * Parsing process provides us namely with: cryptocurrency name, cryptocurrency symbol and cryptocurrency id (given by website).
     */
    private void parseInfo(){
        retrievePageDOM();
        if(pageDOM == null){
            noInternet = true;
            return;
        }

        switch(sourceNumPref){
            case 0:
                retrieveInfo0();
                break;
        }
    }

    /**
     * Downloads source code of wanted page and builds DOM from it (contains information regarding available cryptocurrencies).
     */
    private void retrievePageDOM(){
        try{
            switch(sourceNumPref){
                case 0:
                    pageDOM = Jsoup.connect(PAGE_PREF0).timeout(12000).ignoreContentType(true).get();
                    break;
            }
        } catch (Exception e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page which contains information regarding available cryptocurrencies.
     */
    private void retrieveInfo0(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONArray jsonToParse = new JSONArray(pureJson);

            for(int i = 0; i < jsonToParse.length(); i++){ /* go through cryptocurrency list items (each contains id, name and symbol) */
                JSONObject oneCryptoItem = jsonToParse.getJSONObject(i);
                String cryptoId = oneCryptoItem.getString("id");
                String cryptoSymbol = oneCryptoItem.getString("symbol");
                String cryptoName =  oneCryptoItem.getString("name");;

                cryptoList.add(new LogicCryptoList.CryptoSupport(cryptoId, cryptoSymbol, cryptoName));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads preferences regarding cryptocurrencies. In this case just number of source, from which user wants to get cryptocurrency values.
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        sourceNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_crypto_actual_pref), 0);
        sourceNumPref = 0;
    }

    /**
     * Inner data class, carries data regarding cryptocurrency (cryptocurrency name, symbol and id).
     */
    public static class CryptoSupport{
        private String idCrypto; /* cryptocurrency id (given by website; needed for retrieving price and other information) */
        private String symbolCrypto; /* cryptocurrency symbol (btc...) */
        private String nameCrypto; /* cryptocurrency full name (bitcoin...) */

        /**
         * Constructor is used just for value initialization.
         * @param idCrypto cryptocurrency id - given by website; needed for retrieving price and other information
         * @param symbolCrypto cryptocurrency symbol (btc...)
         * @param nameCrypto cryptocurrency full name (bitcoin...)
         */
        public CryptoSupport(String idCrypto, String symbolCrypto, String nameCrypto){
            this.idCrypto = idCrypto;
            this.symbolCrypto = symbolCrypto;
            this.nameCrypto = nameCrypto;
        }

        /**
         * Getter for cryptocurrency id.
         * @return cryptocurrency id
         */
        public String getIdCrypto() {
            return idCrypto;
        }

        /**
         * Getter for cryptocurrency symbol.
         * @return cryptocurrency symbol
         */
        public String getSymbolCrypto() {
            return symbolCrypto;
        }

        /**
         * Getter for cryptocurrency name.
         * @return cryptocurrency name
         */
        public String getNameCrypto() {
            return nameCrypto;
        }

    }
}
