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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Class represents bridge between ListView, which will be used for displaying wallets saved by user and ArrayList, which contains data regarding to saved wallets
 */
public class SavedWalletAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<FragmentWalletListCrypto.PieceWalletData> savedWalletList; /* saved wallets */
    /* variables assigned in constructor - END */

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList with saved wallets data.
     * @param appActivity Activity of the application
     * @param savedWalletList ArrayList with PieceWalletData instances - each represents one wallet
     */
    public SavedWalletAdapter(Activity appActivity, ArrayList<FragmentWalletListCrypto.PieceWalletData> savedWalletList){
        this.appActivity = appActivity;
        this.savedWalletList = savedWalletList;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    /**
     * Returns number of items presented in the ListView object. In this case returns number of saved wallets.
     * @return number of saved wallets
     */
    @Override
    public int getCount() {
        return savedWalletList.size();
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
     * Method is called for every item passed to the adapter - sets layout for each item.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        if(convertView == null) { /* if layout not yet inflated, inflate it */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.saved_wallet_item_lv, parent, false);

            viewHolder.walletName = convertView.findViewById(R.id.saved_wallet_item_name_tv_id);
            viewHolder.walletAddress = convertView.findViewById(R.id.saved_wallet_item_addr_tv_id);
            viewHolder.walletTimestamp = convertView.findViewById(R.id.saved_wallet_item_datetime_tv_id);
            viewHolder.walletRemove = convertView.findViewById(R.id.saved_wallet_item_remove_id);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final FragmentWalletListCrypto.PieceWalletData savedWallet = savedWalletList.get(position);

        viewHolder.walletName.setText(savedWallet.getName());
        viewHolder.walletAddress.setText(savedWallet.getAddress());
        viewHolder.walletTimestamp.setText(convertTimestampToReadable(savedWallet.getTimestamp()));
        viewHolder.walletRemove.setOnClickListener(new View.OnClickListener(){  /* reference to saved_wallet_item_remove_id; Button through which can user delete wallet from the list */
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(appActivity)
                        .setTitle(appActivity.getResources().getString(R.string.saved_wallet_dialog_title))
                        .setMessage(appActivity.getResources().getString(R.string.saved_wallet_dialog_message_main) + "\n\n"
                        + appActivity.getResources().getString(R.string.saved_wallet_dialog_message_name) + "\n" + savedWallet.getName()  + "\n"
                        + appActivity.getResources().getString(R.string.saved_wallet_dialog_message_address) + "\n" + savedWallet.getAddress() + "\n"
                        + appActivity.getResources().getString(R.string.saved_wallet_dialog_message_type) + "\n" + savedWallet.getType() + "\n"
                        + appActivity.getResources().getString(R.string.saved_wallet_dialog_message_datetime) + "\n" + convertTimestampToReadable(savedWallet.getTimestamp()))
                        .setPositiveButton(appActivity.getResources().getString(R.string.saved_wallet_dialog_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeSavedWallet(savedWallet.getAddress(), savedWallet.getType(), savedWallet.getName());
                                Toast.makeText(appActivity, appActivity.getResources().getString(R.string.saved_wallet_removed_start) + savedWallet.getName() + appActivity.getResources().getString(R.string.saved_wallet_removed_end), Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton(appActivity.getResources().getString(R.string.saved_wallet_dialog_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(appActivity, appActivity.getResources().getString(R.string.saved_wallet_not_removed_start) + savedWallet.getName() + appActivity.getResources().getString(R.string.saved_wallet_not_removed_end), Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });

        return convertView;
    }

    /**
     * Removes wallet selected by user from SharedPreferences.
     * @param walletAddressRemove address of the wallet, which user wants to remove
     * @param walletTypeRemove type of the wallet, which user wants to remove
     * @param walletNameRemove name of the wallet, which user wants to remove
     *
     */
    private void removeSavedWallet(String walletAddressRemove, String walletTypeRemove, String walletNameRemove){
        /* first load String, which contains saved wallets */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        String savedWalletsSharedPref = sharedPref.getString(appActivity.getResources().getString(R.string.sharedpref_crypto_wallets_list), "");
        String itemSeparator = appActivity.getResources().getString(R.string.sharedpref_item_separator);
        String[] savedWalletsSharedPrefSplit = savedWalletsSharedPref.split(itemSeparator); /* get individual items - four items for each wallet */

        /* find position of the wallet related items in array and remove items which are related to the wallet with given information ; wallet is always presented in format: name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added */
        for(int i = 0; i < savedWalletsSharedPrefSplit.length - 3; i++){ /* check if name, type and address match */
            if(savedWalletsSharedPrefSplit[i] == null){ /* null if deleted in one of the previous rounds */
                continue;
            }

            if(savedWalletsSharedPrefSplit[i].equals(walletNameRemove) && savedWalletsSharedPrefSplit[i + 1].equals(walletTypeRemove) && savedWalletsSharedPrefSplit[i + 2].equals(walletAddressRemove)){ /* set null on items related to the wallet */
                savedWalletsSharedPrefSplit[i] = null;
                savedWalletsSharedPrefSplit[i + 1] = null;
                savedWalletsSharedPrefSplit[i + 2] = null;
                savedWalletsSharedPrefSplit[i + 3] = null;
            }
        }

        /* create new String which will represent saved wallets (without the removed one) */
        String walletsToSave = ""; /* String which will be saved into SharedPreferences */
        for(int i = 0; i < savedWalletsSharedPrefSplit.length; i++){
            if(savedWalletsSharedPrefSplit[i] == null){ /* removed wallet content, don´t save */
                continue;
            }else if(i != 0){ /* add information to final string, which will be saved */
                walletsToSave += itemSeparator;
                walletsToSave += savedWalletsSharedPrefSplit[i];
            }
        }

        /* save edited String which contains information regarding wallets to SharedPreferences */
        sharedPrefEd.putString(appActivity.getResources().getString(R.string.sharedpref_crypto_wallets_list), walletsToSave);
        sharedPrefEd.commit();

        /* remove entry regarding the wallet from ArrayList containing information regarding wallets */
        ArrayList<FragmentWalletListCrypto.PieceWalletData> modifiedList = new ArrayList<>();

        for(int i = 0; i < savedWalletList.size(); i++){
            FragmentWalletListCrypto.PieceWalletData savedWallet = savedWalletList.get(i);

            /* check if name, type and address of the wallet match the deleted wallet */
            String nameWallet = savedWallet.getName();
            String typeWallet = savedWallet.getType();
            String addressWallet = savedWallet.getAddress();

            if(nameWallet.equals(walletNameRemove) && typeWallet.equals(walletTypeRemove) && addressWallet.equals(walletAddressRemove)){ /* found match => delete */
                continue;
            }else{ /* other wallet => add to list */
                modifiedList.add(savedWallet);
            }
        }

        savedWalletList = modifiedList;
        notifyDataSetChanged();
    }

    static class ViewHolder{
        TextView walletName;
        TextView walletAddress;
        TextView walletTimestamp;
        Button walletRemove;
    }

    /**
     * Converts unix timestamp to human readable date and time.
     * @param timestamp unix timestamp
     * @return String which represents date and time when was wallet added
     */
    private String convertTimestampToReadable(long timestamp){
        //TimeZone.setDefault(TimeZone.getTimeZone("Europe/Prague"));
        Date currentDate = new Date(timestamp);

        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);

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
}
