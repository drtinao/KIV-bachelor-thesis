package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

/**
 * Serves as a bridge between ListView, which should display list of owned stocks and ArrayList, which contains the information about owned stocks.
 */
public class StocksOwnedItemAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<FragmentHistoryStocks.PieceStockData> ownedStockList; /* owned stocks */
    /* variables assigned in constructor - END */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList with owned stock data.
     * @param appActivity application Activity
     * @param ownedStockList ArrayList with stock data
     */
    public StocksOwnedItemAdapter(Activity appActivity, ArrayList<FragmentHistoryStocks.PieceStockData> ownedStockList){
        this.appActivity = appActivity;
        this.ownedStockList = ownedStockList;

        sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Returns number of items presented in ListView object. In this case returns number of owned stocks.
     * @return number of owned stocks
     */
    @Override
    public int getCount() {
        return ownedStockList.size();
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

        if(convertView == null) { /* if layout not yet inflated, inflate it */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.owned_item_lv, parent, false);

            viewHolder.stockName = convertView.findViewById(R.id.crypto_owned_item_name_tv_id);
            viewHolder.stockSymbol = convertView.findViewById(R.id.crypto_owned_item_symbol_tv_id);
            viewHolder.stockAmount = convertView.findViewById(R.id.crypto_owned_item_amount_id);
            viewHolder.stockRemove = convertView.findViewById(R.id.crypto_owned_item_remove_id);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FragmentHistoryStocks.PieceStockData ownedStock = ownedStockList.get(position);

        /* get transaction data related to stock */
        ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = ownedStock.getStockTransactions();
        if(transactionData.size() == 0){ /* no transactions performed => show zero balance */
            viewHolder.stockAmount.setText("0");
        }else{ /* deduce final balance from performed transactions */
            double finalBalance = 0;

            for(int i = 0; i < transactionData.size(); i++){ /* go through all transactions */
                FragmentHistoryCrypto.PieceTransactionData oneTransaction = transactionData.get(i); /* get one transaction */

                if(oneTransaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY){ /* user bought crypto - add amount */
                    finalBalance += oneTransaction.getAmount();
                }else if(oneTransaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL){ /* user sold crypto - remove amount */
                    finalBalance -= oneTransaction.getAmount();
                }
            }
            DecimalFormat decFor = new DecimalFormat("#.####");
            DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
            decForSym.setDecimalSeparator('.');
            decFor.setDecimalFormatSymbols(decForSym);

            viewHolder.stockAmount.setText(decFor.format(finalBalance));
        }
        viewHolder.stockAmount.append(" " + appActivity.getResources().getString(R.string.owned_item_pieces));

        viewHolder.stockName.setText(ownedStock.getSymbol());
        viewHolder.stockSymbol.setText("");
        viewHolder.stockRemove.setOnClickListener(new View.OnClickListener(){  /* reference to crypto_owned_item_remove_id; Button through which can user delete owned cryptocurrency from list */
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(appActivity)
                        .setTitle(appActivity.getResources().getString(R.string.owned_item_dialog_title))
                        .setMessage(appActivity.getResources().getString(R.string.owned_item_dialog_message_stock_main) + "\n\n"
                                + appActivity.getResources().getString(R.string.owned_item_dialog_message_symbol) + "\n" + ownedStock.getSymbol() + "\n")
                        .setPositiveButton(appActivity.getResources().getString(R.string.owned_item_dialog_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeStockTransactions(ownedStock.getSymbol());
                                Toast.makeText(appActivity, appActivity.getResources().getString(R.string.owned_item_removed_stock_start) + ownedStock.getSymbol() + appActivity.getResources().getString(R.string.owned_item_removed_end), Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton(appActivity.getResources().getString(R.string.owned_item_dialog_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(appActivity, appActivity.getResources().getString(R.string.owned_item_not_removed_stock_start) + ownedStock.getSymbol() + appActivity.getResources().getString(R.string.owned_item_not_removed_end), Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });

        return convertView;
    }

    /**
     * Removes transactions which are related to the selected stock from SharedPreferences.
     * @param stockSym symbol of the stock, which data user wants to remove
     */
    private void removeStockTransactions(String stockSym){
        /* load String, which contains information regarding to owned stocks */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        String savedStocksSharedPref = sharedPref.getString(appActivity.getResources().getString(R.string.sharedpref_stocks_owned_list), ""); /* presented in format: "crypto_symbol" */
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String[] savedStocksSharedPrefSplit = savedStocksSharedPref.split(itemSeparator); /* get individual items */

        /* go through array and find position of the items related to stock owned by user which should be deleted */
        for(int i = 0; i < savedStocksSharedPrefSplit.length; i++){ /* check if symbol matches */
            if(savedStocksSharedPrefSplit[i] == null){ /* item in array will be null if the position originally contained information regarding to deleted stock */
                continue;
            }

            if(savedStocksSharedPrefSplit[i].equals(stockSym)){ /* set null on item related to selected stock */
                savedStocksSharedPrefSplit[i] = null;
            }
        }

        /* build String which will represent stocks owned by user (without just removed stock) */
        String stocksToSave = ""; /* final String will be saved to SharedPreferences */
        for(int i = 0; i < savedStocksSharedPrefSplit.length; i++){
            if(savedStocksSharedPrefSplit[i] == null){
                continue;
            }else if(i != 0){ /* add information regarding to user owned stock */
                stocksToSave += itemSeparator;
                stocksToSave += savedStocksSharedPrefSplit[i];
            }
        }

        /* save edited text representation of user owned stocks */
        sharedPrefEd.putString(appActivity.getResources().getString(R.string.sharedpref_stocks_owned_list), stocksToSave);
        sharedPrefEd.commit();

        /* remove entry from the ArrayList which is connected with ListView */
        ArrayList<FragmentHistoryStocks.PieceStockData> modifiedList = new ArrayList<>();
        for(int i = 0; i < ownedStockList.size(); i++){
            FragmentHistoryStocks.PieceStockData ownedStock = ownedStockList.get(i);

            /* if symbol matches, then do not add to list */
            String symbol = ownedStock.getSymbol();

            if(symbol.equals(stockSym)){ /* found match => delete */
                continue;
            }else{
                modifiedList.add(ownedStock);
            }
        }

        String performedTransSharedPref = sharedPref.getString(stockSym, "");
        if(performedTransSharedPref.length() != 0){
            performedTransSharedPref = "";
            sharedPrefEd.putString(stockSym, performedTransSharedPref);
            sharedPrefEd.commit();
        }

        ownedStockList = modifiedList;
        notifyDataSetChanged();
    }

    /**
     * Inner class used just for smoother work with ListView - stores components (=> no need to repeatedly use findViewById).
     */
    static class ViewHolder{
        TextView stockName;
        TextView stockSymbol;
        TextView stockAmount;
        Button stockRemove;
    }
}
