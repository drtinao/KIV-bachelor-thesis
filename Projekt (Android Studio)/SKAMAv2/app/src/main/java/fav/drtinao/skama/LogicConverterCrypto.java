package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.EditText;

import com.example.skamav2.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Contains logic for retrieving actual values of cryptocurrencies from internet and converting value of one cryptocurrency to value of another.
 */
public class LogicConverterCrypto extends AsyncTask<Void, Void, Void> {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private double amountToConvert;
    private String fromCryptoId;
    private String toCryptoId;
    private boolean noInternet; /* true when internet connection is not available */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* cryptocurrency data source number - default source num is 0 */
    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private double fromActualPrice; /* actual price of cryptocurrency from which amount should be converted (in user preferred currency) */
    private double toActualPrice; /* actual price of cryptocurrency to which amount should be converted (in user preferred currency) */
    /* important variables assigned during parsing process - END */

    /* constants which define part of web pages addresses, from which desired information could be retrieved - START */
    private final String PAGE_PREF0 = "https://api.coingecko.com/api/v3/simple/price?ids=ID_PLACEHOLDER&vs_currencies=CURRENCY_PLACEHOLDER&include_24hr_vol=false"; /* get actual value of given cryptocurrency */
    /* constants which define part of web pages addresses, from which desired information could be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains info regarding cryptocurrency values), which will be later parsed */
    /* other variables & objects used during parsing process - END */

    /**
     * Constructor takes basic information which are necessary for retrieving converted amount.
     * @param appContext application context
     * @param amountToConvert amount which should be converted from one cryptocurrency to another one
     * @param fromCryptoId id of cryptocurrency from which amount should be converted (given by website)
     * @param toCryptoId id of cryptocurrency to which amount should be converted (given by wbsite)
     */
    public LogicConverterCrypto(Context appContext, double amountToConvert, String fromCryptoId, String toCryptoId) {
        this.appActivity = (Activity) appContext;
        this.amountToConvert = amountToConvert;
        this.fromCryptoId = fromCryptoId;
        this.toCryptoId = toCryptoId;
        this.noInternet = false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadPreferences();

        /* init values - START */
        fromActualPrice = 0;
        toActualPrice = 0;
        /* init values - END */

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

            return;
        }
        parsingPd.dismiss();

        showRetrievedInfo();
    }

    /**
     * Parses code of web pages which are used for retrieving information about actual cryptocurrency values.
     * In this case parsing process provides us with actual value of two cryptocurrencies selected by user (user selects
     * from which cryptocurrency should given value be converted & cryptocurrency to which value should be converted).
     */
    private void parseInfo(){
        switch(sourceNumPref){
            case 0:
                String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
                String pageToRetFrom = PAGE_PREF0.replace("ID_PLACEHOLDER", fromCryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]); /* retrieve page which contains information about value of cryptocurrency from which user given value should be converted */
                retrievePageDOM(pageToRetFrom);
                if(pageDOM == null){
                    noInternet = true;
                    return;
                }
                retrieveInfo0First();

                String pageToRetTo = PAGE_PREF0.replace("ID_PLACEHOLDER", toCryptoId).replace("CURRENCY_PLACEHOLDER", currencyArray[currencyPref]); /* retrieve page which contains information about value of cryptocurrency to which user given value should be converted */
                retrievePageDOM(pageToRetTo);
                if(pageDOM == null){
                    noInternet = true;
                    return;
                }
                retrieveInfo0Second();
                break;
        }
    }

    /**
     * Downloads source code of wanted page and builds DOM from it (contains information regarding actual value of cryptocurrency).
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
     * Parses downloaded DOM of the web page which contains information regarding actual value of cryptocurrency from which user given amount should be converted.
     * This method is created for use with first link.
     */
    private void retrieveInfo0First(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONObject jsonData = jsonToParse.getJSONObject(fromCryptoId);

            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            fromActualPrice = jsonData.getDouble(currencyArray[currencyPref].toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page which contains information regarding actual value of cryptocurrency to which user given amount should be converted.
     * This method is created for use with first link.
     */
    private void retrieveInfo0Second(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONObject jsonData = jsonToParse.getJSONObject(toCryptoId);

            String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
            toActualPrice = jsonData.getDouble(currencyArray[currencyPref].toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts user given amount from one cryptocurrency to another and displays result to user.
     */
    private void showRetrievedInfo(){
        EditText conCryptoConAmountET = appActivity.findViewById(R.id.converter_crypto_converted_amount_et_id);
        double conversionRatio = fromActualPrice / toActualPrice; /* calculate conversion ratio of given cryptocurrencies */
        double result = amountToConvert * conversionRatio; /* too many decimal places, reduce with DecimalFormat */

        DecimalFormat decFor = new DecimalFormat("#.####");
        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
        decForSym.setDecimalSeparator('.');
        decFor.setDecimalFormatSymbols(decForSym);
        result = Double.parseDouble(decFor.format(result));

        conCryptoConAmountET.setText(String.valueOf(result));
    }

    /**
     * Loads preferences regarding cryptocurrency values. In this case: preferred ordinal currency (eur, usd...) + preferred source number.
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        sourceNumPref = sharedPref.getInt("crypto_converter_pref_num", 0); /* preferred source number */
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */

        sourceNumPref = 0;
        currencyPref = 0;
    }
}
