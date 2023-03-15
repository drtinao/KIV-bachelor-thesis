package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
 * Serves as a bridge between ListView, which should display list of owned cryptocurrencies and ArrayList, which contains the information about owned cryptocurrencies.
 */
public class CryptoOwnedItemAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<FragmentHistoryCrypto.PieceCryptoData> ownedCryptoList; /* owned cryptocurrencies */
    /* variables assigned in constructor - END */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList with owned cryptocurrency data.
     * @param appActivity application Activity
     * @param ownedCryptoList ArrayList with cryptocurrency data
     */
    public CryptoOwnedItemAdapter(Activity appActivity, ArrayList<FragmentHistoryCrypto.PieceCryptoData> ownedCryptoList){
        this.appActivity = appActivity;
        this.ownedCryptoList = ownedCryptoList;

        sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        currencyPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Returns number of items presented in ListView object. In this case returns number of owned cryptocurrencies.
     * @return number of owned cryptocurrencies
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
     * Method is called for every item passed to this adapter and sets layout for each item.
     */
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @Nullable ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        if(convertView == null) { /* if layout not yet inflated, inflate it */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.owned_item_lv, parent, false);

            viewHolder.cryptoName = convertView.findViewById(R.id.crypto_owned_item_name_tv_id);
            viewHolder.cryptoSymbol = convertView.findViewById(R.id.crypto_owned_item_symbol_tv_id);
            viewHolder.cryptoAmount = convertView.findViewById(R.id.crypto_owned_item_amount_id);
            viewHolder.cryptoRemove = convertView.findViewById(R.id.crypto_owned_item_remove_id);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FragmentHistoryCrypto.PieceCryptoData ownedCrypto = ownedCryptoList.get(position);

        /* get transaction data related to cryptocurrency, which is owned by user */
        ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = ownedCrypto.getCryptoTransactions();
        if(transactionData.size() == 0){ /* no transactions performed => show zero balance */
            viewHolder.cryptoAmount.setText("0");
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

            viewHolder.cryptoAmount.setText(decFor.format(finalBalance));
        }
        viewHolder.cryptoAmount.append(" " + appActivity.getResources().getString(R.string.owned_item_pieces));

        viewHolder.cryptoName.setText(ownedCrypto.getName());
        viewHolder.cryptoSymbol.setText(ownedCrypto.getSymbol());
        viewHolder.cryptoRemove.setOnClickListener(new View.OnClickListener(){  /* reference to crypto_owned_item_remove_id; Button through which can user delete owned cryptocurrency from list */
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(appActivity)
                        .setTitle(appActivity.getResources().getString(R.string.owned_item_dialog_title))
                        .setMessage(appActivity.getResources().getString(R.string.owned_item_dialog_message_main) + "\n\n"
                                + appActivity.getResources().getString(R.string.owned_item_dialog_message_name) + "\n" + ownedCrypto.getName() + "\n"
                                + appActivity.getResources().getString(R.string.owned_item_dialog_message_symbol) + "\n" + ownedCrypto.getSymbol() + "\n")
                        .setPositiveButton(appActivity.getResources().getString(R.string.owned_item_dialog_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeCryptoTransactions(ownedCrypto.getId(), ownedCrypto.getSymbol(), ownedCrypto.getName());
                                Toast.makeText(appActivity, appActivity.getResources().getString(R.string.owned_item_removed_start) + ownedCrypto.getName() + appActivity.getResources().getString(R.string.owned_item_removed_end), Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton(appActivity.getResources().getString(R.string.owned_item_dialog_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(appActivity, appActivity.getResources().getString(R.string.owned_item_not_removed_start) + ownedCrypto.getName() + appActivity.getResources().getString(R.string.owned_item_not_removed_end), Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });

        return convertView;
    }

    /**
     * Removes saved transactions related to the selected cryptocurrency from SharedPreferences.
     * @param cryptoId id of the cryptocurrency, which transactions user wants to remove
     * @param cryptoSymbol symbol of the cryptocurrency, which transactions user wants to remove
     * @param cryptoName full name of the cryptocurrency, which transactions user wants to remove
     */
    private void removeCryptoTransactions(String cryptoId, String cryptoSymbol, String cryptoName){
        /* load String, which contains information regarding to owned cryptocurrencies */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        String savedCryptoSharedPref = sharedPref.getString(appActivity.getResources().getString(R.string.sharedpref_crypto_owned_list), ""); /* presented in format: "crypto_id;;;crypto_symbol;;;crypto_name" */
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String[] savedCryptoSharedPrefSplit = savedCryptoSharedPref.split(itemSeparator); /* get individual items - three items for each owned cryptocurrency */

        /* go through array and find position of the items related to cryptocurrency owned by user which should be deleted */
        for(int i = 0; i < savedCryptoSharedPrefSplit.length - 2; i++){ /* check if id, symbol and name of the crypto match */
            if(savedCryptoSharedPrefSplit[i] == null){ /* item in array will be null if the position originally contained information regarding to deleted cryptocurrency */
                continue;
            }

            if(savedCryptoSharedPrefSplit[i].equals(cryptoId) && savedCryptoSharedPrefSplit[i + 1].equals(cryptoSymbol) && savedCryptoSharedPrefSplit[i + 2].equals(cryptoName)){ /* set null on items related to owned cryptocurrency */
                savedCryptoSharedPrefSplit[i] = null;
                savedCryptoSharedPrefSplit[i + 1] = null;
                savedCryptoSharedPrefSplit[i + 2] = null;
            }
        }

        /* build String which will represent user owned cryptocurrencies (removed crypto excluded) */
        String cryptoToSave = ""; /* final String will be saved into SharedPreferences */
        for(int i = 0; i < savedCryptoSharedPrefSplit.length; i++){
            if(savedCryptoSharedPrefSplit[i] == null){
                continue;
            }else if(i != 0){ /* add information regarding to saved crypto */
                cryptoToSave += itemSeparator;
                cryptoToSave += savedCryptoSharedPrefSplit[i];
            }
        }

        /* save edited text representation of saved crypto */
        sharedPrefEd.putString(appActivity.getResources().getString(R.string.sharedpref_crypto_owned_list), cryptoToSave);
        sharedPrefEd.commit();

        /* remove entry from the ArrayList which is connected with ListView */
        ArrayList<FragmentHistoryCrypto.PieceCryptoData> modifiedList = new ArrayList<>();
        for(int i = 0; i < ownedCryptoList.size(); i++){
            FragmentHistoryCrypto.PieceCryptoData ownedCrypto = ownedCryptoList.get(i);

            /* if id, symbol and name match, then do not add to list */
            String id = ownedCrypto.getId();
            String symbol = ownedCrypto.getSymbol();
            String name = ownedCrypto.getName();

            if(id.equals(cryptoId) && symbol.equals(cryptoSymbol) && name.equals(cryptoName)){ /* found match => delete */
                continue;
            }else{
                modifiedList.add(ownedCrypto);
            }
        }

        String performedTransSharedPref = sharedPref.getString(cryptoId, "");
        if(performedTransSharedPref.length() != 0){
            performedTransSharedPref = "";
            sharedPrefEd.putString(cryptoId, performedTransSharedPref);
            sharedPrefEd.commit();
        }

        ownedCryptoList = modifiedList;
        notifyDataSetChanged();
    }

    /**
     * Inner class used just for smoother work with ListView - stores components (=> no need to repeatedly use findViewById).
     */
    static class ViewHolder{
        TextView cryptoName;
        TextView cryptoSymbol;
        TextView cryptoAmount;
        Button cryptoRemove;
    }
}
