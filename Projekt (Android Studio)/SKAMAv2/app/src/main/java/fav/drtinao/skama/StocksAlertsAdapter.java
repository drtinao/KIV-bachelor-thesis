package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.util.ArrayList;

/**
 * Basically bridge between ListView, which is used for displaying active alerts and ArrayList, which contains information regarding to active alerts.
 */
public class StocksAlertsAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<FragmentAlertsStocks.AlertStockData> activeAlerts; /* active stocks alerts */
    private boolean crypto; /* true if crypto related alerts are processed */
    /* variables assigned in constructor - END */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList with active alerts.
     * @param appActivity application Activity
     * @param activeAlerts ArrayList with active alerts data
     * @param crypto true if crypto related alerts are processed (else false - stocks)
     */
    public StocksAlertsAdapter(Activity appActivity, ArrayList<FragmentAlertsStocks.AlertStockData> activeAlerts, boolean crypto){
        this.appActivity = appActivity;
        this.activeAlerts = activeAlerts;
        this.crypto = crypto;

        sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Returns number of items, which are presented in the ListView object. In this case return number of active stocks alerts.
     * @return number of active stocks alerts
     */
    @Override
    public int getCount() {
        return activeAlerts.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * Called for every item passed to the adapter and sets layout for each item.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        if(convertView == null){ /* inflate layout if not yet inflated */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.created_alert_lv, parent, false);

            viewHolder.itemType = convertView.findViewById(R.id.created_alert_item_type_tv_id);
            viewHolder.itemPrice = convertView.findViewById(R.id.created_alert_item_price_tv_id);
            viewHolder.itemName = convertView.findViewById(R.id.created_alert_item_name_tv_id);
            viewHolder.itemRemove = convertView.findViewById(R.id.created_alert_item_remove_id);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FragmentAlertsStocks.AlertStockData createdAlert = activeAlerts.get(position);

        /* get information related to the one alert */
        String typeToDisplay;
        if(createdAlert.getTypeAlert().equals(appActivity.getResources().getString(R.string.sharedpref_alert_type_below))){ /* alert when below the price point */
            typeToDisplay = appActivity.getResources().getString(R.string.created_alert_below);
        }else{ /* alert when above the price point */
            typeToDisplay = appActivity.getResources().getString(R.string.created_alert_above);
        }

        viewHolder.itemType.setText(typeToDisplay);
        if(createdAlert.getTypeAlert().equals(appActivity.getResources().getString(R.string.sharedpref_alert_type_below))){
            viewHolder.itemType.setTextColor(Color.RED);
        }else{
            viewHolder.itemType.setTextColor(Color.GREEN);
        }
        viewHolder.itemPrice.setText(String.valueOf(createdAlert.getTargetPrice()));
        String[] currencyArray = appActivity.getResources().getStringArray(R.array.settings_general_currency_pref_list);
        viewHolder.itemPrice.append(" " + currencyArray[currencyPref]);
        viewHolder.itemName.setText(createdAlert.getStockSymbol());
        viewHolder.itemRemove.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!crypto){ /* display stock related delete dialog */
                    new AlertDialog.Builder(appActivity)
                            .setTitle(appActivity.getResources().getString(R.string.created_alert_dialog_title))
                            .setMessage(appActivity.getResources().getString(R.string.created_alert_dialog_stock_message) + "\n\n"
                                    + appActivity.getResources().getString(R.string.created_alert_dialog_stock_message_symbol) + "\n" + createdAlert.getStockSymbol() + "\n")
                            .setPositiveButton(appActivity.getResources().getString(R.string.created_alert_dialog_stock_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeStockAlert(createdAlert.getStockSymbol());
                                    Toast.makeText(appActivity, appActivity.getResources().getString(R.string.created_alert_dialog_stock_removed_start) + createdAlert.getStockSymbol() + appActivity.getResources().getString(R.string.created_alert_dialog_stock_removed_end), Toast.LENGTH_SHORT).show();
                                }
                            }).setNegativeButton(appActivity.getResources().getString(R.string.created_alert_dialog_stock_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(appActivity, appActivity.getResources().getString(R.string.created_alert_dialog_stock_not_removed_start) + createdAlert.getStockSymbol() + appActivity.getResources().getString(R.string.created_alert_dialog_stock_not_removed_end), Toast.LENGTH_SHORT).show();
                        }
                    }).show();
                }else{ /* display crypto related delete dialog */
                    new AlertDialog.Builder(appActivity)
                            .setTitle(appActivity.getResources().getString(R.string.created_alert_dialog_title))
                            .setMessage(appActivity.getResources().getString(R.string.created_alert_dialog_crypto_message) + "\n\n"
                                    + appActivity.getResources().getString(R.string.created_alert_dialog_crypto_message_name) + "\n" + createdAlert.getStockSymbol() + "\n")
                            .setPositiveButton(appActivity.getResources().getString(R.string.created_alert_dialog_stock_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeStockAlert(createdAlert.getStockSymbol());
                                    Toast.makeText(appActivity, appActivity.getResources().getString(R.string.created_alert_dialog_crypto_removed_start) + createdAlert.getStockSymbol() + appActivity.getResources().getString(R.string.created_alert_dialog_stock_removed_end), Toast.LENGTH_SHORT).show();
                                }
                            }).setNegativeButton(appActivity.getResources().getString(R.string.created_alert_dialog_stock_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(appActivity, appActivity.getResources().getString(R.string.created_alert_dialog_crypto_not_removed_start) + createdAlert.getStockSymbol() + appActivity.getResources().getString(R.string.created_alert_dialog_stock_not_removed_end), Toast.LENGTH_SHORT).show();
                        }
                    }).show();
                }
            }
        });

        return convertView;
    }

    /**
     * Removes alert for stock with specific symbol from SharedPreferences.
     * @param stockSymbol symbol of the stock, for which alert should be removed
     */
    public void removeStockAlert(String stockSymbol){
        /* load String, which contains information regarding to already saved stock alerts */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        String loadFrom;
        if(!crypto){
            loadFrom = appActivity.getResources().getString(R.string.sharedpref_stocks_alerts);
        }else{
            loadFrom = appActivity.getResources().getString(R.string.sharedpref_crypto_alerts);
        }

        String savedStockAlerts = sharedPref.getString(loadFrom, ""); /* each alert is in format: ;;;stock_sym;;;BELOW/ABOVE;;;target_price */
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String[] savedStockAlertsSplit = savedStockAlerts.split(itemSeparator); /* get individual items */

        /* go through array with individual items and remove items related to the selected stock symbol */
        for(int i = 0; i < savedStockAlertsSplit.length - 2; i++){ /* check if stock symbol matches */
            if(savedStockAlertsSplit[i] == null){ /* item in array will be null if previously contained information regarding to deleted stock alert */
                continue;
            }

            if(savedStockAlertsSplit[i].equals(stockSymbol)){ /* set null on items related to selected stock */
                savedStockAlertsSplit[i] = null;
                savedStockAlertsSplit[i + 1] = null;
                savedStockAlertsSplit[i + 2] = null;
            }
        }

        /* create new String which will represent active stock alerts (without the deleted one) */
        String alertsToSave = ""; /* new String which will be saved to SharedPreferences */
        for(int i = 0; i < savedStockAlertsSplit.length; i++){
            if(savedStockAlertsSplit[i] == null){
                continue;
            }else if(i != 0){
                alertsToSave += itemSeparator;
                alertsToSave += savedStockAlertsSplit[i];
            }
        }

        /* save new String to ShredPref */
        sharedPrefEd.putString(loadFrom, alertsToSave);
        sharedPrefEd.commit();

        /* update ListView with stocks alerts */
        ArrayList<FragmentAlertsStocks.AlertStockData> modifiedList = new ArrayList<>();
        for(int i = 0; i < activeAlerts.size(); i++){
            FragmentAlertsStocks.AlertStockData alert = activeAlerts.get(i);

            /* if symbol matches, then do not add to list */
            String symbol = alert.getStockSymbol();

            if(symbol.equals(stockSymbol)){ /* found match => delete */
                continue;
            }else{
                modifiedList.add(alert);
            }
        }

        activeAlerts = modifiedList;
        notifyDataSetChanged();
    }

    /**
     * Inner class is used for smoother work with ListView - stores components (=> no need to repeatedly use findViewById).
     */
    static class ViewHolder{
        TextView itemType;
        TextView itemPrice;
        TextView itemName;
        TextView itemRemove;
    }
}
