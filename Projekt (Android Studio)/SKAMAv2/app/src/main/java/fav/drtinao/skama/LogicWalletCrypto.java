package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Contains logic for part of the application which displays information about wallet (bitcoin + litecoin supported).
 */
public class LogicWalletCrypto extends AsyncTask<Void, Void, Void> {
    enum walletType{BITCOIN, LITECOIN}; /* specifies type of cryptocurrency that wallet contains */

    /* variables assigned in constructor - START */
    private String walletAddr; /* address of wallet (assigned in constructor; given by user) */
    private walletType type; /* type of wallet (assigned in constructor; given by user) */
    private Activity appActivity; /* reference to Activity of the application */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int btcNumPref; /* bitcoin wallet source number - default source num is 0 */
    private int ltcNumPref; /* litecoin wallet source number - default source num is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private double balance; /* actual wallet balance */
    private double totalReceived; /* total amount which was received (sum of all incoming transactions) */
    private int countTransactions; /* total count of transactions */
    private double totalSent; /* total amount which was send from the wallet (sum of all outcoming transactions) */
    /* important variables assigned during parsing process - END */

    /* constants which define part of web pages addresses, from which can info about wallet content be retrieved - START */
    private final String BTC_PAGE_PREF0 = "https://insight.bitpay.com/api/addr/"; /* api */
    private final String BTC_PAGE_PREF1 = "https://api.blockcypher.com/v1/btc/main/addrs/"; /* api */
    private final String BTC_PAGE_PREF2 = "https://chain.api.btc.com/v3/address/"; /* api */
    private final String BTC_PAGE_PREF0_LINK = "https://insight.bitpay.com/address/"; /* link for BTC_PAGE_PREF0 (human readable web page with transaction history) */
    private final String BTC_PAGE_PREF1_LINK = "https://live.blockcypher.com/btc/address/"; /* link for BTC_PAGE_PREF1 (human readable web page with transaction history) */
    private final String BTC_PAGE_PREF2_LINK = "https://btc.com/"; /* link for BTC_PAGE_PREF2 (human readable web page with transaction history) */

    private final String LTC_PAGE_PREF0 = "https://api.blockcypher.com/v1/ltc/main/addrs/"; /* api */
    private final String LTC_PAGE_PREF1 = "https://ltc-chain.api.btc.com/v3/address/"; /* api */
    private final String LTC_PAGE_PREF0_LINK = "https://live.blockcypher.com/ltc/address/"; /* link for LTC_PAGE_PREF0 (human readable web page with transaction history) */
    private final String LTC_PAGE_PREF1_LINK = "https://ltc-chain.api.btc.com/v3/address/"; /* link for LTC_PAGE_PREF1 (human readable web page with transaction history) */
    /* constants which define part of web pages addresses, from which can info about wallet content be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains info regarding wallet), which will be later parsed */
    private boolean walletExists; /* true if wallet with given address exists, else false */
    private boolean secondTryRetrieveDOM; /* sometimes occurs error when retrieving data in short time interval - probably web crawler protection; try one more time... */
    private boolean failApi; /* true when too much requests, try later - api fault, not app... */
    private final double SATOSHI_CONVERSION_RATIO = 100000000;
    /* other variables & objects used during parsing process - END */

    /* references to objects, which are in activity regarding wallet part of the application - START */
    private View walletCryptoDivider; /* reference to wallet_crypto_divider_id; just divider placed between items in the activity */
    private TextView walletCryptoInfoPrint; /* reference to wallet_crypto_infoprint_tv_id; TV object used as title for information about wallet */

    private TextView walletCryptoInfoAddr1; /* reference to wallet_crypto_infoaddr_tv1_id; title for wallet address */
    private TextView walletCryptoInfoAddr2; /* reference to wallet_crypto_infoaddr_tv2_id; contains retrieved wallet address */

    private TextView walletCryptoInfoBalance1; /* reference to wallet_crypto_infobalance_tv1_id; title for balance */
    private TextView walletCryptoInfoBalance2; /* reference to wallet_crypto_infobalance_tv2_id; contains retrieved balance */

    private TextView walletCryptoInfoCountTran1; /* reference to wallet_crypto_infocounttran_tv1_id; title for transaction count */
    private TextView walletCryptoInfoCountTran2; /* reference to wallet_crypto_infocounttran_tv2_id; contains retrieved transaction count */

    private TextView walletCryptoInfoTotalRecv1; /* reference to wallet_crypto_infototalrecv_tv1_id; title for total received amount */
    private TextView walletCryptoInfoTotalRecv2; /* reference to wallet_crypto_infototalrecv_tv2_id; contains retrieved total received amount */

    private TextView walletCryptoInfoTotalSent1; /* reference to wallet_crypto_infototalsent_tv1_id; title for total sent amount */
    private TextView walletCryptoInfoTotalSent2; /* reference to wallet_crypto_infototalsent_tv2_id; contains retrieved total sent amount */

    private TextView walletCryptoInfoMore1; /* reference to wallet_crypto_infomore_tv1_id; title for more info about wallet */
    private TextView walletCryptoInfoMore2; /* reference to wallet_crypto_infomore_tv2_id; contains wallet link */

    private Button walletCryptoAddAddrBtn; /* reference to wallet_crypto_add_addr_btn_id; button shows up when wallet address entered by user is valid and is not saved yed */
    /* references to objects, which are in activity regarding wallet part of the application - END */

    /**
     * Constructor takes address of wallet and type of cryptocurrency, which wallet contains.
     * These values are needed to get proper information about wallet content.
     * @param walletAddr address of wallet
     * @param type type of crypto, that wallet contains (bitcoin, litecoin and so on)
     * @param appContext application context (often needed when working with UI elements - ProgressDialog for example)
     */
    public LogicWalletCrypto(String walletAddr, walletType type, Context appContext){
        this.walletAddr = walletAddr;
        this.type = type;
        this.appActivity = (Activity) appContext;
    }

    /**
     * Method is called before connection with server is established and is used to get references to objects in activity and to inform user, that web page content will be parsed.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadPreferences();
        acquireRefViewsInfo();

        walletExists = true;
        secondTryRetrieveDOM = false;
        failApi = false;
        parsingPd = new ProgressDialog(appActivity);
        parsingPd.setMessage(appActivity.getResources().getString(R.string.logic_retrieving));
        parsingPd.setCancelable(false);
        parsingPd.show();
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

    /**
     * Method is called when process of parsing ends and is used to dismiss message, which tells user that the application is busy.
     * Information about wallet is shown if everything went ok (given address was valid & page parsing was successful).
     * @param aVoid not used in this case
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        parsingPd.dismiss();

        if(failApi){
            hideViewsInfo();
            Toast.makeText(appActivity, appActivity.getResources().getString(R.string.logic_wallet_crypto_too_much_requests), Toast.LENGTH_LONG).show();
            return;
        }

        if(walletExists){
            showRetrievedInfo();
        }else{
            hideViewsInfo();
            Toast.makeText(appActivity, appActivity.getResources().getString(R.string.logic_wallet_crypto_not_exist), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Parses code of web page which is used for getting information about wallet content.
     * Parsing process gives us information, which are later stored in respective variables (like balance etc.).
     */
    private void parseInfo(){
        retrievePageDOM();

        if(!walletExists){
            return;
        }

        switch(type){
            case BITCOIN:
                switch(btcNumPref){
                    case 0:
                        retrieveBasicInfoBTC0();
                        break;

                    case 1:
                        retrieveBasicInfoBTC1();
                        break;

                    case 2:
                        retrieveBasicInfoBTC2();
                        break;
                }
                break;

            case LITECOIN:
                switch(ltcNumPref){
                    case 0:
                        retrieveBasicInfoLTC0();
                        break;

                    case 1:
                        retrieveBasicInfoLTC1();
                        break;
                }
                break;
        }
    }

    /**
     * Downloads source code of wanted page and builds DOM from it (contains information about cryptocurrency wallet).
     */
    private void retrievePageDOM(){
        try {
            if(type == walletType.BITCOIN){
                switch(btcNumPref){
                    case 0:
                        pageDOM = Jsoup.connect(BTC_PAGE_PREF0 + walletAddr).timeout(12000).ignoreContentType(true).get();
                        break;

                    case 1:
                        pageDOM = Jsoup.connect(BTC_PAGE_PREF1 + walletAddr).timeout(12000).ignoreContentType(true).get();
                        break;

                    case 2:
                        pageDOM = Jsoup.connect(BTC_PAGE_PREF2 + walletAddr).timeout(12000).ignoreContentType(true).get();
                        break;
                }

            }else if(type == walletType.LITECOIN){
                switch(ltcNumPref){
                    case 0:
                        pageDOM = Jsoup.connect(LTC_PAGE_PREF0 + walletAddr).timeout(12000).ignoreContentType(true).get();
                        break;

                    case 1:
                        pageDOM = Jsoup.connect(LTC_PAGE_PREF1 + walletAddr).timeout(12000).ignoreContentType(true).get();
                        break;
                }
            }
        } catch (Exception e) {
            if(!secondTryRetrieveDOM){
                secondTryRetrieveDOM = true;
                retrievePageDOM();
            }else{
                walletExists = false;
            }
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page regarding BTC wallet and gets basic information regarding wallet (balance, number of transactions, total received amount, total sent amount
     * and actual amount present in wallet).
     */
    private void retrieveBasicInfoBTC0(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        if(pureJson.contains("Invalid address:")){
            walletExists = false;
        }

        try {
            JSONObject jsonToParse = new JSONObject(pureJson);

            countTransactions = jsonToParse.getInt("txApperances");
            totalReceived = jsonToParse.getDouble("totalReceived");
            totalSent = jsonToParse.getDouble("totalSent");
            balance = jsonToParse.getDouble("balance");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page regarding BTC wallet and gets basic information regarding wallet (balance, number of transactions, total received amount, total sent amount
     * and actual amount present in wallet).
     */
    private void retrieveBasicInfoBTC1(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        if(pureJson.contains("\"error\": \"Error: ")){
            walletExists = false;
        }

        try {
            JSONObject jsonToParse = new JSONObject(pureJson);

            countTransactions = jsonToParse.getInt("n_tx");
            totalReceived = jsonToParse.getDouble("total_received") / SATOSHI_CONVERSION_RATIO;
            totalSent = jsonToParse.getDouble("total_sent") / SATOSHI_CONVERSION_RATIO;
            balance = jsonToParse.getDouble("balance") / SATOSHI_CONVERSION_RATIO;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page regarding BTC wallet and gets basic information regarding wallet (balance, number of transactions, total received amount, total sent amount
     * and actual amount present in wallet).
     */
    private void retrieveBasicInfoBTC2(){
        if(pageDOM.toString().contains("abuse the API")){
            failApi = true;
            return;
        }

        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        if(pureJson.contains("\"data\":null") && pureJson.contains("\"err_no\"")){
            walletExists = false;
        }

        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONObject jsonData = jsonToParse.getJSONObject("data");

            countTransactions = jsonData.getInt("tx_count");
            totalReceived = jsonData.getDouble("received") / SATOSHI_CONVERSION_RATIO;
            totalSent = jsonData.getDouble("sent") / SATOSHI_CONVERSION_RATIO;
            if(!jsonData.isNull("banlance")){
                balance = jsonData.getDouble("banlance") / SATOSHI_CONVERSION_RATIO;
            }else{
                balance = jsonData.getDouble("balance") / SATOSHI_CONVERSION_RATIO;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page regarding LTC wallet and gets basic information regarding wallet (balance, number of transactions, total received amount, total sent amount
     * and actual amount present in wallet).
     */
    private void retrieveBasicInfoLTC0(){
        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        if(pureJson.contains("\"error\": \"Error: ")){
            walletExists = false;
        }

        try {
            JSONObject jsonToParse = new JSONObject(pureJson);

            countTransactions = jsonToParse.getInt("n_tx");
            totalReceived = jsonToParse.getDouble("total_received") / SATOSHI_CONVERSION_RATIO;
            totalSent = jsonToParse.getDouble("total_sent") / SATOSHI_CONVERSION_RATIO;
            balance = jsonToParse.getDouble("balance") / SATOSHI_CONVERSION_RATIO;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page regarding LTC wallet and gets basic information regarding wallet (balance, number of transactions, total received amount, total sent amount
     * and actual amount present in wallet).
     */
    private void retrieveBasicInfoLTC1(){
        if(pageDOM.toString().contains("abuse the API")){
            failApi = true;
            return;
        }

        /* jsoup library added html tags to json -> side effect, remove it */
        String[] jsonSplit1 = pageDOM.toString().split("<body>");
        String[] jsonSplit2 = jsonSplit1[1].split("</body>");

        String pureJson = jsonSplit2[0].trim();
        if(pureJson.contains("\"data\":null") && pureJson.contains("\"err_no\"")){
            walletExists = false;
        }

        try {
            JSONObject jsonToParse = new JSONObject(pureJson);
            JSONObject jsonData = jsonToParse.getJSONObject("data");

            countTransactions = jsonData.getInt("tx_count");
            totalReceived = jsonData.getDouble("received") / SATOSHI_CONVERSION_RATIO;
            totalSent = jsonData.getDouble("sent") / SATOSHI_CONVERSION_RATIO;
            if(!jsonData.isNull("banlance")){
                balance = jsonData.getDouble("banlance") / SATOSHI_CONVERSION_RATIO;
            }else{
                balance = jsonData.getDouble("balance") / SATOSHI_CONVERSION_RATIO;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method gets references to some objects presented in the activity, which contains Views regarding wallet.
     * Reference to these objects is needed because some elements of UI need to be hidden / visible (according to situation)
     * and also some changes regarding text within TextViews are often needed (update TV content with values retrieved from web page parsing).
     */
    private void acquireRefViewsInfo(){
        walletCryptoDivider = appActivity.findViewById(R.id.wallet_crypto_divider_id);
        walletCryptoInfoPrint = appActivity.findViewById(R.id.wallet_crypto_infoprint_tv_id);
        walletCryptoInfoAddr1 = appActivity.findViewById(R.id.wallet_crypto_infoaddr_tv1_id);
        walletCryptoInfoAddr2 = appActivity.findViewById(R.id.wallet_crypto_infoaddr_tv2_id);
        walletCryptoInfoBalance1 = appActivity.findViewById(R.id.wallet_crypto_infobalance_tv1_id);
        walletCryptoInfoBalance2 = appActivity.findViewById(R.id.wallet_crypto_infobalance_tv2_id);
        walletCryptoInfoCountTran1 = appActivity.findViewById(R.id.wallet_crypto_infocounttran_tv1_id);
        walletCryptoInfoCountTran2 = appActivity.findViewById(R.id.wallet_crypto_infocounttran_tv2_id);
        walletCryptoInfoTotalRecv1 = appActivity.findViewById(R.id.wallet_crypto_infototalrecv_tv1_id);
        walletCryptoInfoTotalRecv2 = appActivity.findViewById(R.id.wallet_crypto_infototalrecv_tv2_id);
        walletCryptoInfoTotalSent1 = appActivity.findViewById(R.id.wallet_crypto_infototalsent_tv1_id);
        walletCryptoInfoTotalSent2 = appActivity.findViewById(R.id.wallet_crypto_infototalsent_tv2_id);
        walletCryptoInfoMore1 = appActivity.findViewById(R.id.wallet_crypto_infomore_tv1_id);
        walletCryptoInfoMore2 = appActivity.findViewById(R.id.wallet_crypto_infomore_tv2_id);
        walletCryptoAddAddrBtn = appActivity.findViewById(R.id.wallet_crypto_add_addr_btn_id);
    }

    /**
     * Hides UI elements which contains information regarding the wallet. These elements should be typically hidden when
     * there is no information to show (ie. wallet address not given or is invalid).
     */
    private void hideViewsInfo(){
        walletCryptoDivider.setVisibility(View.GONE);
        walletCryptoInfoPrint.setVisibility(View.GONE);
        walletCryptoInfoAddr1.setVisibility(View.GONE);
        walletCryptoInfoAddr2.setVisibility(View.GONE);
        walletCryptoInfoBalance1.setVisibility(View.GONE);
        walletCryptoInfoBalance2.setVisibility(View.GONE);
        walletCryptoInfoCountTran1.setVisibility(View.GONE);
        walletCryptoInfoCountTran2.setVisibility(View.GONE);
        walletCryptoInfoTotalRecv1.setVisibility(View.GONE);
        walletCryptoInfoTotalRecv2.setVisibility(View.GONE);
        walletCryptoInfoTotalSent1.setVisibility(View.GONE);
        walletCryptoInfoTotalSent2.setVisibility(View.GONE);
        walletCryptoInfoMore1.setVisibility(View.GONE);
        walletCryptoInfoMore2.setVisibility(View.GONE);
        walletCryptoAddAddrBtn.setVisibility(View.GONE);
    }

    /**
     * Shows information about the wallet to user using various TextView objects.
     */
    private void showRetrievedInfo(){
        String currency;
        String link;

        switch(type){
            case BITCOIN:
                currency = " BTC";
                switch(btcNumPref){
                    case 0:
                        link = BTC_PAGE_PREF0_LINK;
                        break;

                    case 1:
                        link = BTC_PAGE_PREF1_LINK;
                        break;

                    case 2:
                        link = BTC_PAGE_PREF2_LINK;
                        break;

                    default: /* use first source as default */
                        link = BTC_PAGE_PREF0_LINK;
                }
                break;

            case LITECOIN:
                currency = " LTC";
                switch(ltcNumPref){
                    case 0:
                        link = LTC_PAGE_PREF0_LINK;
                        break;

                    case 1:
                        link = LTC_PAGE_PREF1_LINK;
                        break;

                    default: /* use first source as default */
                        link = LTC_PAGE_PREF0_LINK;
                }
                break;

            default:  /* unknown currency */
                currency = "";
                link = "";
        }

        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setMaximumFractionDigits(350);

        walletCryptoInfoAddr2.setText(walletAddr);
        walletCryptoInfoBalance2.setText(df.format(balance) + currency);
        walletCryptoInfoCountTran2.setText(String.valueOf(countTransactions));
        walletCryptoInfoTotalRecv2.setText(df.format(totalReceived) + currency);
        walletCryptoInfoTotalSent2.setText(df.format(totalSent) + currency);

        /* create link */
        String linkWallet = "<a href=" + link + walletAddr + ">";
        linkWallet += "info";
        linkWallet += "</a>";

        walletCryptoInfoMore2.setMovementMethod(LinkMovementMethod.getInstance());
        walletCryptoInfoMore2.setText(Html.fromHtml(linkWallet));

        walletCryptoDivider.setVisibility(View.VISIBLE);
        walletCryptoInfoPrint.setVisibility(View.VISIBLE);
        walletCryptoInfoAddr1.setVisibility(View.VISIBLE);
        walletCryptoInfoAddr2.setVisibility(View.VISIBLE);
        walletCryptoInfoBalance1.setVisibility(View.VISIBLE);
        walletCryptoInfoBalance2.setVisibility(View.VISIBLE);
        walletCryptoInfoCountTran1.setVisibility(View.VISIBLE);
        walletCryptoInfoCountTran2.setVisibility(View.VISIBLE);
        walletCryptoInfoTotalRecv1.setVisibility(View.VISIBLE);
        walletCryptoInfoTotalRecv2.setVisibility(View.VISIBLE);
        walletCryptoInfoTotalSent1.setVisibility(View.VISIBLE);
        walletCryptoInfoTotalSent2.setVisibility(View.VISIBLE);
        walletCryptoInfoMore1.setVisibility(View.VISIBLE);
        walletCryptoInfoMore2.setVisibility(View.VISIBLE);

        if(!isWalletSaved()){ /* if wallet´s address was not saved before, then show button which allows user to save the address */
            walletCryptoAddAddrBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Loads preferences regarding wallet (sources, from which wallet data should be retrieved).
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);

        btcNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_crypto_wallet_btc_pref), 0);
        ltcNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_crypto_wallet_ltc_pref), 0);
    }

    /**
     * Checks whether the wallet address was saved before or not. Each wallet is saved in SharedPreferences - format of each wallet is: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added". Need to check wallet type and address.
     * @return true if address already saved, else false
     */
    private boolean isWalletSaved(){
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);

        /* build String which will represent current wallet and then the representation in SharedPreferences - START */
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String selectedWallet = itemSeparator; /* wallets are presented in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added" - check if type and address matches */

        String[] walletTypes = appActivity.getResources().getStringArray(R.array.wallet_crypto_support_list);

        String walletType = "";
        if(type == LogicWalletCrypto.walletType.BITCOIN){
            walletType = walletTypes[0]; /* Bitcoin (BTC) */
        }else if(type == LogicWalletCrypto.walletType.LITECOIN){
            walletType = walletTypes[1]; /* Litecoin (LTC) */
        }else{ /* default - use bitcoin wallet */
            walletType = walletTypes[0]; /* Bitcoin (BTC) */
        }

        selectedWallet += walletType + itemSeparator;
        selectedWallet += walletAddr + itemSeparator;
        /* build String which will represent current wallet and then the representation in SharedPreferences - END */

        /* check if the wallet is already saved or not - START */
        String savedWalletsSharedPref = sharedPref.getString(appActivity.getResources().getString(R.string.sharedpref_crypto_wallets_list), ""); /* wallets are presented in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added" */
        if(!savedWalletsSharedPref.contains(selectedWallet)){
            return false;
        }else{
            return true;
        }
        /* check if the wallet is already saved or not - END */
    }
}
