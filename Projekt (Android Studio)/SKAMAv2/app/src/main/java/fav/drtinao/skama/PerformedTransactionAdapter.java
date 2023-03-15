package fav.drtinao.skama;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Is basically bridge between ListView, which will be used for displaying performed transactions and ArrayList, which contains transaction data.
 */
public class PerformedTransactionAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<FragmentHistoryCrypto.PieceTransactionData> ownedCryptoList; /* owned cryptocurrencies */
    private String selectedCryptoId;
    /* variables assigned in constructor - END */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */

    private SharedPreferences sharedPref;

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList which contains information regarding to transactions performed with the cryptocurrency.
     * @param appActivity Activity of the application
     * @param ownedCryptoList ArrayList with PieceTransactionData instances - each represents one transaction performed with the cryptocurrency
     */
    public PerformedTransactionAdapter(Activity appActivity, ArrayList<FragmentHistoryCrypto.PieceTransactionData> ownedCryptoList, String selectedCryptoId){
        this.appActivity = appActivity;
        this.ownedCryptoList = ownedCryptoList;
        this.selectedCryptoId = selectedCryptoId;

        sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Returns number of items which are available in the ListView object. Number of performed transactions with the specific cryptocurrency is returned in this case.
     * @return transaction count
     */
    @Override
    public int getCount() {
        return ownedCryptoList.size();
    }

    @Override
    public Object getItem(int position) {
        return ownedCryptoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Method is called for every item passed to the adapter - sets layout for each item.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        /* load String, which contains saved transactions */
        ViewHolder viewHolder = new ViewHolder();

        if(convertView == null){ /* if layout not yet inflated, inflate it */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.transaction_item_lv, parent, false);

            viewHolder.itemType = convertView.findViewById(R.id.transaction_item_type_tv_id);
            viewHolder.itemAmount = convertView.findViewById(R.id.transaction_item_amount_tv_id);
            viewHolder.itemPrice = convertView.findViewById(R.id.transaction_item_price_tv_id);
            viewHolder.itemTimestamp = convertView.findViewById(R.id.transaction_item_date_tv_id);
            viewHolder.itemRemove = convertView.findViewById(R.id.transaction_item_remove_id);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FragmentHistoryCrypto.PieceTransactionData transaction = ownedCryptoList.get(position);

        String typeString = "";
        String typeToDisplay = "";
        if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY){
            typeString = appActivity.getResources().getString(R.string.sharedpref_transaction_type_buy);
            typeToDisplay = appActivity.getResources().getString(R.string.transaction_item_buy);
            viewHolder.itemType.setTextColor(Color.GREEN);
        }else if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL){
            typeString = appActivity.getResources().getString(R.string.sharedpref_transaction_type_sell);
            typeToDisplay = appActivity.getResources().getString(R.string.transaction_item_sell);
            viewHolder.itemType.setTextColor(Color.RED);
        }

        DecimalFormat decFor = new DecimalFormat("#.####");
        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
        decForSym.setDecimalSeparator('.');
        decFor.setDecimalFormatSymbols(decForSym);

        final String unformAmount = String.valueOf(transaction.getAmount());
        final String unformPrice = String.valueOf(transaction.getPrice());

        final String amountString = String.valueOf(decFor.format(transaction.getAmount()));
        final String priceString = String.valueOf(decFor.format(transaction.getPrice()));

        viewHolder.itemType.setText(typeToDisplay);
        viewHolder.itemAmount.setText(appActivity.getResources().getString(R.string.transaction_item_amount) + " " + amountString);
        viewHolder.itemPrice.setText(appActivity.getResources().getString(R.string.transaction_item_price) + " " + priceString);

        String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
        viewHolder.itemPrice.append(" " + currencyArray[currencyPref]);

        viewHolder.itemTimestamp.setText(convertTimestampToReadable(transaction.getTimestamp()));
        final String finalTypeString = typeString;
        viewHolder.itemRemove.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                removeCryptoTransaction(finalTypeString, unformAmount, unformPrice, String.valueOf(transaction.getTimestamp()));
                Toast.makeText(appActivity, appActivity.getResources().getString(R.string.performed_transaction_removed_alert), Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    public void removeCryptoTransaction(String type, String amount, String price, String timestamp){
        /* load String, which contains information regarding performed transactions */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        if(amount.replace(",", ".").contains(".")){
            String[] splittedAmount = amount.split("\\.");
            if(Integer.valueOf(splittedAmount[1]) == 0){
                amount = splittedAmount[0];
            }
        }

        if(price.replace(",", ".").contains(".")){
            String[] splittedPrice = price.split("\\.");
            if(Integer.valueOf(splittedPrice[1]) == 0){
                price = splittedPrice[0];
            }
        }

        String performedTransSharedPref = sharedPref.getString(selectedCryptoId, "");
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String[] performedTransSharedPrefSplit = performedTransSharedPref.split(itemSeparator);

        for(int i = 0; i < performedTransSharedPrefSplit.length - 3; i++){
            if(performedTransSharedPrefSplit[i] == null){
                continue;
            }

            if(performedTransSharedPrefSplit[i].equals(type) && performedTransSharedPrefSplit[i + 1].equals(amount) && performedTransSharedPrefSplit[i + 2].equals(price) && performedTransSharedPrefSplit[i + 3].equals(timestamp)){
                performedTransSharedPrefSplit[i] = null;
                performedTransSharedPrefSplit[i + 1] = null;
                performedTransSharedPrefSplit[i + 2] = null;
                performedTransSharedPrefSplit[i + 3] = null;
            }
        }

        String performedTransToSave = ""; /* final String will be saved into SharedPreferences */
        for(int i = 0; i < performedTransSharedPrefSplit.length; i++){
            if(performedTransSharedPrefSplit[i] == null){
                continue;
            }else if(i != 0){ /* add information regarding to saved crypto */
                performedTransToSave += itemSeparator;
                performedTransToSave += performedTransSharedPrefSplit[i];
            }
        }

        sharedPrefEd.putString(selectedCryptoId, performedTransToSave);
        sharedPrefEd.commit();

        ArrayList<FragmentHistoryCrypto.PieceTransactionData> modifiedList = new ArrayList<>();
        for(int i = 0; i < ownedCryptoList.size(); i++){
            FragmentHistoryCrypto.PieceTransactionData performedTrans = ownedCryptoList.get(i);

            String typeString = "";
            if(performedTrans.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY){
                typeString = appActivity.getResources().getString(R.string.sharedpref_transaction_type_buy);
            }else if(performedTrans.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL){
                typeString = appActivity.getResources().getString(R.string.sharedpref_transaction_type_sell);
            }

            String amountString = String.valueOf(performedTrans.getAmount());
            String priceString = String.valueOf(performedTrans.getPrice());

            if(amountString.replace(",", ".").contains(".")){
                String[] splittedAmount = amountString.split("\\.");

                if(Integer.valueOf(splittedAmount[1]) == 0){
                    amountString = splittedAmount[0];
                }
            }

            if(priceString.replace(",", ".").contains(".")){
                String[] splittedPrice = priceString.split("\\.");
                if(Integer.valueOf(splittedPrice[1]) == 0){
                    priceString = splittedPrice[0];
                }
            }

            /* if type, amount, price and timestamp match, then do not add to list */
            if(type.equals(typeString) && amount.trim().equals(amountString.trim()) && price.equals(priceString) && timestamp.equals(String.valueOf(performedTrans.getTimestamp()))){ /* found match => delete */
                continue;
            }else{
                modifiedList.add(performedTrans);
            }
        }

        ownedCryptoList = modifiedList;
        notifyDataSetChanged();
    }

    static class ViewHolder{
        TextView itemType;
        TextView itemAmount;
        TextView itemPrice;
        TextView itemTimestamp;
        Button itemRemove;
    }

    /**
     * Converts unix timestamp to human readable date and time.
     * @param timestamp unix timestamp
     * @return String which represents date and time when was wallet added
     */
    private String convertTimestampToReadable(long timestamp){
        Date currentDate = new Date(timestamp);

        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String yearCut = String.valueOf(year).substring(2);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);

        /* edit hour & minute & sec to always have two digits - START */
        String hourEdit = String.valueOf(hour);
        String minEdit = String.valueOf(min);

        if(hourEdit.length() == 1){
            hourEdit = "0" + hourEdit;
        }

        if(minEdit.length() == 1){
            minEdit = "0" + minEdit;
        }
        /* edit hour & minute & sec to always have two digits - END */

        String dateEdit = day + "." + month + "." + yearCut + " " + hourEdit + ":" + minEdit;

        return dateEdit;
    }
}
