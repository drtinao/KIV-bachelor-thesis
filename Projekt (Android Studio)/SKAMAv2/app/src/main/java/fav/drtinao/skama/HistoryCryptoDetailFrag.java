package fav.drtinao.skama;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.skamav2.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * Displays fragment with transaction history (defined by user) of one selected cryptocurrency to user.
 */
public class HistoryCryptoDetailFrag extends Fragment {
    private String selectedCryptoId; /* id of cryptocurrency; used for detecting related transactions */
    private ListView historyCryptoDetailLV; /* reference to history_crypto_detail_lv; ListView object which will display transactions related to the cryptocurrency owned by user */
    private View createdLayoutView; /* reference to inflated layout, needed for working with elements defined in the layout */

    private PerformedTransactionAdapter performedTransactionAdapter; /* adapter for performed transactions */
    private ArrayList<FragmentHistoryCrypto.PieceTransactionData> performedTransactionsList; /* ArrayList which contains PieceTransactionData instances; each instance represents one transaction performed by user */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        /* load information about selected crypto */
        Bundle dataBundle = getArguments();
        selectedCryptoId = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id));
        String selectedCryptoName = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name));

        ((MainActivity)getActivity()).getSupportActionBar().setTitle(getResources().getString(R.string.history_crypto_detail_title) + " " +selectedCryptoName);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.history_crypto_detail_frag, container, false);
        createdLayoutView = createdLayout;

        acquireRefViewsInfo();

        loadPerformedTransactions();
        showPerformedTransactions();

        return createdLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        loadPreferences();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Method obtains reference to ListView which is defined in xml regarding to this fragment.
     */
    private void acquireRefViewsInfo(){
        historyCryptoDetailLV = createdLayoutView.findViewById(R.id.history_crypto_detail_lv);
    }

    /**
     * Modifies ActionBar, adds button for adding another transaction associated with the selected cryptocurrency.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.history_nav, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Method is called when button for adding transaction is tapped (located in ActionBar).
     * @param item MenuItem object which represent tapped item (plus sign in this case - add transaction)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.add_history_id: /* add transaction button -> display dialog, which allows transaction adding */
                final View dialogAddTransaction = getLayoutInflater().inflate(R.layout.dialog_history_crypto_detail_frag, null);

                /* set currency */
                String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                TextView historyCryptoCurrencyTv = dialogAddTransaction.findViewById(R.id.dialog_history_crypto_currency_tv_id);
                historyCryptoCurrencyTv.setText(currencyArray[currencyPref]);

                prepCryptoDateEt(dialogAddTransaction);
                prepCryptoTimeEt(dialogAddTransaction);

                /* create floating dialog, through which can user give information regarding to transaction */
                final AlertDialog transactionDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.history_crypto_dialog_title))
                        .setMessage(getActivity().getResources().getString(R.string.history_crypto_dialog_message))
                        .setView(dialogAddTransaction)
                        .setPositiveButton(getActivity().getResources().getString(R.string.history_crypto_dialog_add), null)
                        .setNegativeButton(getActivity().getResources().getString(R.string.history_crypto_dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.history_crypto_detail_not_saved), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create();

                transactionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button positiveButton = transactionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                               saveTransaction(dialogAddTransaction, transactionDialog);
                            }
                        });
                    }
                });
                transactionDialog.show();

                /* load String, which contains information regarding to performed transactions with the cryptocurrency */
                SharedPreferences sharedPref = getActivity().getSharedPreferences("preferences", 0);
                SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

                String performedTransSharedPref = sharedPref.getString(selectedCryptoId, "");

                Random random = new Random();
                for(int i = 0; i < 0; i++){
                    Double randomAmount = 500 - (random.nextDouble() * (600 - 500));
                    Double randomPrice = 500 - (random.nextDouble() * (600 - 500));;
                    long timeStamp = System.currentTimeMillis();
                    performedTransSharedPref += ";;;" + "BUY" + ";;;" + String.valueOf(randomAmount) + ";;;" + String.valueOf(randomPrice) + ";;;" + String.valueOf(timeStamp);
                }

                //Log.i("Info", "Added string: " + performedTransSharedPref);
                sharedPrefEd.putString(selectedCryptoId, performedTransSharedPref);
                sharedPrefEd.commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Prepares EditText, through which can user pick date of the transaction.
     * @param view needed for interaction with layout elements
     */
    private void prepCryptoDateEt(View view){
        final EditText cryptoDateEt = view.findViewById(R.id.dialog_history_crypto_date_et_id);
        cryptoDateEt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                String dateFrom = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
                                cryptoDateEt.setText(dateFrom);
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });
    }

    /**
     * Prepares EditText, through which can user pick time of the transaction.
     * @param view needed for interaction with layout elements
     */
    private void prepCryptoTimeEt(View view){
        final EditText cryptoTimeEt = view.findViewById(R.id.dialog_history_crypto_time_et_id);
        cryptoTimeEt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                TimePickerDialog timePicker = new TimePickerDialog(getContext(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                String timeDisplay = "";
                                if(hourOfDay < 10){
                                    timeDisplay += "0";
                                }
                                timeDisplay += hourOfDay;

                                timeDisplay += ":";

                                if(minute < 10){
                                    timeDisplay += "0";
                                }
                                timeDisplay += minute;

                                cryptoTimeEt.setText(timeDisplay);
                            }
                        }, calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), true);
                timePicker.show();
            }
        });
    }

    /**
     * Saves transaction with information provided by user.
     * @param transactionDialogLayout reference to transaction dialog´s layout
     * @param transactionDialogAlert reference to AlertDialog object
     */
    private void saveTransaction(View transactionDialogLayout, AlertDialog transactionDialogAlert){
        /* load String, which contains information regarding to performed transactions with the cryptocurrency */
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        SharedPreferences.Editor sharedPrefEd = sharedPref.edit(); /* modify preferences */

        String performedTransSharedPref = sharedPref.getString(selectedCryptoId, "");

        /* check of some values entered by user is required before submitting transaction (fields cannot be empty and so on) */
        /* determine type of the transaction */
        String transType = getResources().getString(R.string.sharedpref_transaction_type_buy); /* default buy action - will be replaced by user selection */
        RadioButton historyCryptoBuyRb = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_buy_rb_id);
        RadioButton historyCryptoSellRb = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_sell_rb_id);
        if(historyCryptoBuyRb.isChecked()){
            transType = getResources().getString(R.string.sharedpref_transaction_type_buy);
        }else if(historyCryptoSellRb.isChecked()){
            transType = getResources().getString(R.string.sharedpref_transaction_type_sell);
        }else{ /* no RB checked - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_type_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_type_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get amount */
        EditText historyCryptoAmountEt = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_amount_et_id);
        String amountText = historyCryptoAmountEt.getText().toString();
        if(amountText.length() == 0 || amountText.equals(".") || amountText.equals(".")){ /* wrong input in amount field - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_amount_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get price for one crypto piece */
        EditText historyCryptoPriceEt = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_price_et_id);
        String priceText = historyCryptoPriceEt.getText().toString();
        if(priceText.length() == 0 || priceText.equals(".") || priceText.equals(".")){ /* wrong input in price field - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_price_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get date */
        EditText historyCryptoDateEt = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_date_et_id);
        String dateText = historyCryptoDateEt.getText().toString();
        if(dateText.length() == 0){ /* date empty - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_date_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* split date to get individual items (day, month, year) */
        String[] splitDate = dateText.split("\\.");
        String day = splitDate[0];
        String month = splitDate[1];
        String year = splitDate[2];

        /* date must not exceed actual date, check */
        Date today = new Date();
        Date givenDate = new Date(Integer.parseInt(year) - 1900, Integer.parseInt(month) - 1, Integer.parseInt(day));
        if(givenDate.after(today)){
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_date_exceed_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get time */
        EditText historyCryptoTimeEt = transactionDialogLayout.findViewById(R.id.dialog_history_crypto_time_et_id);
        String timeText = historyCryptoTimeEt.getText().toString();

        if(timeText.length() == 0){ /* time empty - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_time_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* split time to get individual items (hour, minute) */
        String[] splitTime = timeText.split(":");
        String hour = splitTime[0];
        String minute = splitTime[1];

        /* if date is same, then time cannot exceed actual time - check */
        givenDate.setHours(Integer.valueOf(hour));
        givenDate.setMinutes(Integer.valueOf(minute));
        if(!givenDate.before(today)){
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_time_exceed_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* all ok - perform save */
        String dateTimeString = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + "00";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar givenDateCal = Calendar.getInstance();
        try {
            givenDateCal.setTime(sdf.parse(dateTimeString));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String itemSeparator = getResources().getString(R.string.sharedpref_item_separator);

        performedTransSharedPref += itemSeparator + transType + itemSeparator + amountText + itemSeparator + priceText + itemSeparator + String.valueOf(givenDateCal.getTimeInMillis());

        sharedPrefEd.putString(selectedCryptoId, performedTransSharedPref);
        sharedPrefEd.commit();

        /* successfully saved - dismiss dialog */
        transactionDialogAlert.dismiss();

        /* add item to ListView */
        loadPerformedTransactions();
        showPerformedTransactions();

        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.history_crypto_detail_saved), Toast.LENGTH_SHORT).show();
    }

    /**
     * Loads crypto related preferences. In this case: preferred ordinal currency (eur, usd...).
     */
    private void loadPreferences(){
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Loads data regarding performed transactions with the cryptocurrency from SharedPreferences, name crypto_id.
     * Transactions are saved in following format: "transaction_type;;;amount;;;price;;;timestamp_created"
     */
    private void loadPerformedTransactions(){
        performedTransactionsList = new ArrayList<>();

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);

        String transactionDataString = sharedPref.getString(selectedCryptoId, "");
        if(transactionDataString.length() == 0){ /* no transactions are saved regarding to the crypto */
            return;
        }
        String[] transactionDataStringSplit = transactionDataString.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items - four for each transaction */
        if(transactionDataStringSplit.length == 0){ /* no transactions are saved regarding to the crypto */
            return;
        }

        /* start from 1 - first is always empty */
        for(int i = 1; i < transactionDataStringSplit.length; i += 4){
            String transType = transactionDataStringSplit[i]; /* first is transaction type - buy or sell */
            String transAmount = transactionDataStringSplit[i + 1]; /* second is transaction amount - tells how much crypto was processed during the transactions */
            String transPrice = transactionDataStringSplit[i + 2]; /* third is the price of one crypto unit */
            String transTime = transactionDataStringSplit[i + 3]; /* fourth is transaction timestamp */

            double transAmountDouble = Double.valueOf(transAmount);
            double transPriceDouble = Double.valueOf(transPrice);

            long timestampConverted; /* convert number from String to long => if for some reason error occurs, then use actual date & time (should not ever occur) */
            try{
                timestampConverted = Long.valueOf(transTime);
            }catch(NumberFormatException exception){
                timestampConverted = System.currentTimeMillis();
            }

            FragmentHistoryCrypto.PieceTransactionData.transactionType type = null;
            if(transType.equals(getResources().getString(R.string.sharedpref_transaction_type_buy))){
                type = FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY;
            }else if(transType.equals(getResources().getString(R.string.sharedpref_transaction_type_sell))){
                type = FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL;
            }

            FragmentHistoryCrypto.PieceTransactionData performedTransaction = new FragmentHistoryCrypto.PieceTransactionData(type, transAmountDouble, transPriceDouble, timestampConverted);
            performedTransactionsList.add(performedTransaction);
        }
    }

    /**
     * Presents saved transactions which are in ArrayList performedTransactionsList to user. Each instance in this ArrayList represents one performed transaction.
     */
    private void showPerformedTransactions(){
        performedTransactionAdapter = new PerformedTransactionAdapter(getActivity(), performedTransactionsList, selectedCryptoId);
        if(performedTransactionsList.size() == 0){ /* no saved transactions */
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.history_crypto_detail_nothing_to_display), Toast.LENGTH_LONG).show();
            return;
        }

        historyCryptoDetailLV.setAdapter(performedTransactionAdapter);
        performedTransactionAdapter.notifyDataSetChanged();
    }
}
