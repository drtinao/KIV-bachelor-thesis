package fav.drtinao.skama;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;

/**
 * Is responsible for displaying part of the app, which shows actual cryptocurrency values and history.
 */
public class ActualValueCryptoDetailFrag extends Fragment {
    private LogicActualValueCryptoDetail logicActualValueCryptoDetail;
    private int selectedTabNum; /* number of tab which was selected by user (1 = tab with actual info - 24h, 2 = tab with history) */
    private String selectedCryptoId; /* id of cryptocurrency which was selected by user */
    private String selectedCryptoName; /* name of cryptocurrency which was selected by user */
    private String selectedCryptoSymbol; /* symbol of cryptocurrency which was selected by user */

    private String dateFrom; /* represents date from which history is shown in given format: day.month.year */
    private String dateTo; /* represents date to which history is shown in given format: day.month.year */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */

    private final long YEARS_4_MILIS = 126227808000l;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Bundle dataBundle = getArguments();
        selectedTabNum = dataBundle.getInt(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_tab), -1);
        selectedCryptoId = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id));
        selectedCryptoName = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name));
        selectedCryptoSymbol = dataBundle.getString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_symbol));

        ((MainActivity)getActivity()).getSupportActionBar().setTitle(selectedCryptoName);

        View createdLayout = null;

        switch(selectedTabNum){
            case 2:
                createdLayout = inflater.inflate(R.layout.actual_value_crypto_detail_frag2, container, false);
                TextView cryptoId = createdLayout.findViewById(R.id.crypto_detail_frag2_crypto_id_hidden_id); /* set cryptocurrency id to hidden TextView */
                cryptoId.setText(selectedCryptoId);
                prepSelDateFromEt(createdLayout);
                prepSelDateToEt(createdLayout);
                prepSubDateBtn(createdLayout);
                break;

            default:
                createdLayout = inflater.inflate(R.layout.actual_value_crypto_detail_frag1, container, false);
                prepActValAddBtn(createdLayout);
                logicActualValueCryptoDetail = new LogicActualValueCryptoDetail(getContext(), LogicActualValueCryptoDetail.tabList.GENERAL_TAB, selectedCryptoId);
        }

        return createdLayout;
    }

    /**
     * Loads crypto related preferences. In this case: preferred ordinal currency (eur, usd...).
     */
    private void loadPreferences(){
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        sharedPrefEd = sharedPref.edit(); /* modify preferences */
        loadPreferences();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(selectedTabNum != 2){ /* execute just when first tab is selected */
            logicActualValueCryptoDetail.execute();
        }
    }

    /**
     * Modifies ActionBar, adds button for buy simulation.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.simulation_nav, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Is called when button / icon from ActionBar is tapped - icon for simulation and icon for alert present.
     * @param item MenuItem object which represents tapped item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.ic_alert_simulation:
                /* check if alert for specific crypto is yet saved; if not -> display save dialog */
                String savedCryptoAlerts = sharedPref.getString(getResources().getString(R.string.sharedpref_crypto_alerts), ""); /* get already saved alerts regarding to crypto */

                /* check if alert for specific cryptocurrency is yet created or not */
                if(savedCryptoAlerts.contains(getResources().getString(R.string.sharedpref_item_separator) + selectedCryptoName + getResources().getString(R.string.sharedpref_item_separator))){ /* alert for crypto with given name already saved */
                    Toast.makeText(getContext(), getResources().getString(R.string.already_added_crypto_alert), Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }

                /* no alert for selected crypto found, continue with alert */
                final View dialogAddAlert = getLayoutInflater().inflate(R.layout.dialog_alert_actual_value_crypto_detail_frag, null);
                /* set currency */
                String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                TextView priceCurrencyTv = dialogAddAlert.findViewById(R.id.dialog_alert_actual_value_price_tv2_id);
                priceCurrencyTv.setText(currencyArray[currencyPref]);

                /* create dialog for user input */
                final AlertDialog addAlertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.actual_value_crypto_detail_sim_title))
                        .setMessage(getActivity().getResources().getString(R.string.actual_value_crypto_detail_sim_message))
                        .setView(dialogAddAlert)
                        .setPositiveButton(getActivity().getResources().getString(R.string.actual_value_crypto_detail_sim_add), null)
                        .setNegativeButton(getActivity().getResources().getString(R.string.actual_value_crypto_detail_sim_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.actual_value_crypto_detail_sim_not_added), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create();

                addAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button positiveButton = addAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                saveAlert(dialogAddAlert, addAlertDialog);
                            }
                        });
                    }
                });

                addAlertDialog.show();
                break;

            case R.id.ic_buy_simulation:
                /* check is point from chart selected, else return */
                if(logicActualValueCryptoDetail == null || logicActualValueCryptoDetail.getLastSelectedDate() == null){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_type_title));
                    alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_no_point_message));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
                    alertBuilder.show();
                    return super.onOptionsItemSelected(item);
                }

                final View dialogSimulateTransaction = getLayoutInflater().inflate(R.layout.dialog_history_crypto_detail_frag, null); /* layout is the same as the one used when adding real transaction */

                /* create dialog, through which can user give necessary information */
                final AlertDialog simulateTransactionDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.history_crypto_simulation_dialog_title))
                        .setMessage(getActivity().getResources().getString(R.string.history_crypto_simulation_dialog_message))
                        .setView(dialogSimulateTransaction)
                        .setPositiveButton(getActivity().getResources().getString(R.string.history_crypto_simulation_dialog_simulate), null)
                        .setNegativeButton(getActivity().getResources().getString(R.string.history_crypto_dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.history_crypto_detail_not_simulated), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create();

                simulateTransactionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button positiveButton = simulateTransactionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                performSimulation(dialogSimulateTransaction, simulateTransactionDialog); /* perform simulation */
                            }
                        });
                    }
                });

                /* set price */
                EditText historyStockPriceEt = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_price_et_id);
                String[] curArr = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                historyStockPriceEt.setText(String.valueOf(logicActualValueCryptoDetail.getLastSelectedPrice()));
                historyStockPriceEt.setFocusable(false);

                /* set currency */
                TextView historyStockCurrencyTv = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_currency_tv_id);
                historyStockCurrencyTv.setText(curArr[currencyPref]);

                /* set date */
                EditText historyStockDateEt = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_date_et_id);
                historyStockDateEt.setText(logicActualValueCryptoDetail.getLastSelectedDate());
                historyStockDateEt.setFocusable(false);

                /* set time */
                EditText historyStockTimeEt = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_time_et_id);
                historyStockTimeEt.setText(logicActualValueCryptoDetail.getLastSelectedTime());
                historyStockTimeEt.setFocusable(false);

                simulateTransactionDialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Performs simulation with information retrieved from graph and amount given by user.
     * @param simulateTransactionLayout reference to simulation dialog´s layout
     * @param simulateTransactionAlertDialog reference to AlertDialog object
     */
    private void performSimulation(View simulateTransactionLayout, AlertDialog simulateTransactionAlertDialog){
        /* determine type of the transaction */
        String transType = getResources().getString(R.string.sharedpref_transaction_type_buy); /* default buy action - will be replaced by user selection */
        RadioButton simulationStockBuyRb = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_buy_rb_id);
        RadioButton simulationStockSellRb = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_sell_rb_id);

        /* check of some values entered by user is required before submitting transaction (fields cannot be empty and so on) */
        /* get transaction type */
        if(simulationStockBuyRb.isChecked()){
            transType = getResources().getString(R.string.sharedpref_transaction_type_buy);
        }else if(simulationStockSellRb.isChecked()){
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
        EditText historyStockAmountEt = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_amount_et_id);
        String amountText = historyStockAmountEt.getText().toString();
        if(amountText.length() == 0 || amountText.equals(".") || amountText.equals(",")){ /* wrong input in amount field - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.history_crypto_detail_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.history_crypto_detail_err_amount_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_detail_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get price of one piece */
        EditText historyStockPriceEt = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_price_et_id);
        String priceText = historyStockPriceEt.getText().toString();

        /* get date */
        EditText historyStockDateEt = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_date_et_id);
        String dateText = historyStockDateEt.getText().toString();

        /* get time */
        EditText historyStockTimeEt = simulateTransactionLayout.findViewById(R.id.dialog_history_crypto_time_et_id);
        String timeText = historyStockTimeEt.getText().toString();

        /* get actual price of one piece */
        String[] latestDataArr = logicActualValueCryptoDetail.getLatestPriceReadable();
        double latestPrice = Double.valueOf(latestDataArr[0]);
        String latestTime = latestDataArr[1];

        String[] latesTimeStringSplit = latestTime.split(" "); /* get day & time */
        String dayString = latesTimeStringSplit[0];
        String timeString = latesTimeStringSplit[1];

        double totalInvestedHistory = Double.valueOf(amountText) * Double.valueOf(priceText); /* how much invested back then */
        double totalInvestedNow = Double.valueOf(amountText) * latestPrice; /* how much would be invested now */

        String resultToDisplay;

        String[] currencyArray = getActivity().getResources().getStringArray(R.array.settings_general_currency_pref_list);
        DecimalFormat decFor = new DecimalFormat("#.####");
        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
        decForSym.setDecimalSeparator('.');
        decFor.setDecimalFormatSymbols(decForSym);

        if(transType.equals(getResources().getString(R.string.sharedpref_transaction_type_buy))){ /* simulate buy */
            double gain = totalInvestedNow - totalInvestedHistory;

            String beforeBuy = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_1) + " " + dateText + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_2) + " " + timeText + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_3) + " " + decFor.format(totalInvestedHistory) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_4);
            String latestBuy = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_5) + " " + dayString + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_6) + " " + timeString + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_7) + " " + decFor.format(latestPrice) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_4) + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_8) + " " + decFor.format(totalInvestedNow) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_4);

            if(gain > 0){
                resultToDisplay = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_9_gain) + " " + decFor.format(gain) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_10_gain);
            }else{
                resultToDisplay = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_9_loss) + " " + decFor.format(Math.abs(gain)) + " " + currencyArray[currencyPref] + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_buy_10_loss);
            }

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_title));
            alertBuilder.setMessage(beforeBuy + "\n\n" + latestBuy + "\n\n" + resultToDisplay);
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_dismiss),null);
            alertBuilder.show();

            simulateTransactionAlertDialog.dismiss();
        }else{ /* simulate sell */
            double gain = totalInvestedHistory - totalInvestedNow;

            String beforeSell = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_1) + " " + dateText + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_2) + " " + timeText + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_3) + " " + decFor.format(totalInvestedHistory) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_4);
            String latestSell = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_5) + " " + dayString + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_6) + " " + timeString + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_7) + " " + decFor.format(latestPrice) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_4) + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_8) + " " + decFor.format(totalInvestedNow) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_4);

            if(gain > 0){
                resultToDisplay = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_9_gain) + " " + decFor.format(gain) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_10_gain);
            }else{
                resultToDisplay = getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_9_loss) + " " + decFor.format(Math.abs(gain)) + " " + currencyArray[currencyPref] + " " + getResources().getString(R.string.actual_value_crypto_detail_dialog_sim_sell_10_loss);
            }

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_title));
            alertBuilder.setMessage(beforeSell + "\n\n" + latestSell + "\n\n" + resultToDisplay);
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_dismiss),null);
            alertBuilder.show();

            simulateTransactionAlertDialog.dismiss();
        }
    }

    /**
     * Saves alert for specific cryptocurrency.
     * @param dialogAddAlert reference to add alert dialog´s layout
     * @param addAlertDialog reference to AlertDialog object
     */
    private void saveAlert(View dialogAddAlert, AlertDialog addAlertDialog){
        String typeAlert = "BELOW"; /* default will be below price; will be replaced by user selection - can be below or above */
        RadioButton alertBelowRB = dialogAddAlert.findViewById(R.id.dialog_alert_actual_value_range_below_rb_id);
        RadioButton alertAboveRB = dialogAddAlert.findViewById(R.id.dialog_alert_actual_value_range_above_rb_id);

        if(alertBelowRB.isChecked()){
            typeAlert = "BELOW";
        }else if(alertAboveRB.isChecked()){
            typeAlert = "ABOVE";
        }else{ /* no RB checked - error */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_radio_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* get user entered amount */
        EditText actualPriceEt = dialogAddAlert.findViewById(R.id.dialog_alert_actual_value_price_et_id);
        String actualPriceText = actualPriceEt.getText().toString();

        /* check if amount is valid */
        if(actualPriceText.length() == 0 || actualPriceText.equals(".") || actualPriceText.equals(",")){ /* wrong input in the field - err */
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_title));
            alertBuilder.setMessage(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_amount_message));
            alertBuilder.setPositiveButton(getResources().getString(R.string.dialog_alert_actual_value_crypto_err_dismiss),null);
            alertBuilder.show();
            return;
        }

        /* valid input => go on; save to SharedPref */
        String savedCryptoAlerts = sharedPref.getString(getResources().getString(R.string.sharedpref_crypto_alerts), ""); /* get already saved alerts regarding to crypto */

        String itemSeparator = getResources().getString(R.string.sharedpref_item_separator);
        String selectedCryptoInfo = itemSeparator + selectedCryptoName + itemSeparator + typeAlert + itemSeparator + actualPriceText; /* save in format: ;;;crypto_name;;;BELOW/ABOVE;;;target_price */

        /* check if alert for specific cryptocurrency is yet created or not */
        if(!savedCryptoAlerts.contains(itemSeparator + selectedCryptoName + itemSeparator)){ /* no alert present for the crypto with given id, go on - save */
            savedCryptoAlerts += selectedCryptoInfo;
            sharedPrefEd.putString(getResources().getString(R.string.sharedpref_crypto_alerts), savedCryptoAlerts);
            sharedPrefEd.commit();

            Toast.makeText(getContext(), getResources().getString(R.string.newly_added_crypto_alert), Toast.LENGTH_SHORT).show();
        }else{ /* alert already present, do not add another one */
           Toast.makeText(getContext(), getResources().getString(R.string.already_added_crypto_alert), Toast.LENGTH_SHORT).show();
        }

        addAlertDialog.dismiss();
    }

    /**
     * Performs actions which leads to displaying chart with cryptocurrency value history (date is chosen by user).
     * @param view needed for interaction with UI elements
     */
    private void prepSubDateBtn(final View view){
        final Button submitDateBtn = view.findViewById(R.id.crypto_detail_frag2_submit_date_btn_id);

        submitDateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                TextView cryptoId = view.findViewById(R.id.crypto_detail_frag2_crypto_id_hidden_id); /* get cryptocurrency id, which is in hidden TextView */
                EditText dateHistoryFrom = view.findViewById(R.id.crypto_detail_frag2_sel_date_from_et_id); /* get date from which history should be retrieved */
                EditText dateHistoryTo = view.findViewById(R.id.crypto_detail_frag2_sel_date_to_et_id); /* get date to which history should be retrived */

                /* check values given by user - START */
                if(dateHistoryFrom.getText().toString().equals("")){ /* EditText which should contain date from which history should be displayed is empty */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_specify_start_date_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }

                if(dateHistoryTo.getText().toString().equals("")){ /* EditText which should contain date to which history should be displayed is empty */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_specify_end_date_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }

                /* date to must not exceed date from, check - START */
                String[] splitHistoryDateFrom = dateHistoryFrom.getText().toString().split("\\.");
                String historyDateFromDay = splitHistoryDateFrom[0];
                String historyDateFromMonth = splitHistoryDateFrom[1];
                String historyDateFromYear = splitHistoryDateFrom[2];

                String[] splitHistoryDateTo = dateHistoryTo.getText().toString().split("\\.");
                String historyDateToDay = splitHistoryDateTo[0];
                String historyDateToMonth = splitHistoryDateTo[1];
                String historyDateToYear = splitHistoryDateTo[2];

                Date startDate = new Date(Integer.parseInt(historyDateFromYear) - 1900, Integer.parseInt(historyDateFromMonth) - 1, Integer.parseInt(historyDateFromDay));

                Date endDate = new Date(Integer.parseInt(historyDateToYear) - 1900, Integer.parseInt(historyDateToMonth) - 1, Integer.parseInt(historyDateToDay));

                boolean skipCheck = false;
                Date today = new Date();

                if(endDate.equals(startDate)){
                    skipCheck = true;
                }

                if(!endDate.after(startDate) && !skipCheck){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_end_before_start_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }
                /* date to must not exceed date from, check - END */

                /* date from and to must not exceed actual date, check - START */
                if(startDate.after(today)){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_from_after_actual_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }

                if(endDate.after(today)){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_to_after_actual_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }
                /* date from and to must not exceed actual date, check - END */

                long diffDates = endDate.getTime() - startDate.getTime();
                if(diffDates > YEARS_4_MILIS){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_crypto_detail_dialog_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_crypto_detail_dialog_4_years_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_crypto_detail_dialog_ok),null);
                    alertBuilder.show();
                    return;
                }

                /* check values given by user - END */
                logicActualValueCryptoDetail = new LogicActualValueCryptoDetail(view.getContext(), LogicActualValueCryptoDetail.tabList.HISTORY_TAB, cryptoId.getText().toString(), dateHistoryFrom.getText().toString(), dateHistoryTo.getText().toString());
                logicActualValueCryptoDetail.execute();
            }
        });
    }

    /**
     * Prepares EditText, which allows user to pick date from which cryptocurrency history will be displayed.
     * @param view needed for interaction with UI elements
     */
    private void prepSelDateFromEt(View view){
        final EditText selDateFromEt = view.findViewById(R.id.crypto_detail_frag2_sel_date_from_et_id);

        selDateFromEt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dateFrom = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
                                selDateFromEt.setText(dateFrom);
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });
    }

    /**
     * Prepares EditText, which allows user to pick date to which cryptocurrency history will be displayed.
     * @param view needed for interaction with UI elements
     */
    private void prepSelDateToEt(View view){
        final EditText selDateToEt = view.findViewById(R.id.crypto_detail_frag2_sel_date_to_et_id);

        selDateToEt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dateTo = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
                                selDateToEt.setText(dateTo);
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });
    }

    /**
     * Prepares Button, which allows user to add cryptocurrency to list of owned cryptocurrencies.
     * @param view needed for interaction with UI elements
     */
    private void prepActValAddBtn(final View view){
        final Button actValAddBtn = view.findViewById(R.id.crypto_detail_frag1_act_val_add_btn_id);

        actValAddBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /* get info regarding owned crypto */
                String ownedCryptoSharedPref = sharedPref.getString(getResources().getString(R.string.sharedpref_crypto_owned_list), ""); /* presented in format: "crypto_id;;;crypto_symbol;;;crypto_name" */

                /* build String regarding selected crypto which will match the format - needed for check if the crypto is already present or not */
                String itemSeparator = getResources().getString(R.string.sharedpref_item_separator);
                String selectedCryptoInfo = itemSeparator + selectedCryptoId + itemSeparator + selectedCryptoSymbol + itemSeparator + selectedCryptoName;

                if(!ownedCryptoSharedPref.contains(selectedCryptoInfo)){ /* if user has not already added given crypto to list */
                    ownedCryptoSharedPref += selectedCryptoInfo;
                    sharedPrefEd.putString(getResources().getString(R.string.sharedpref_crypto_owned_list), ownedCryptoSharedPref);
                    sharedPrefEd.commit();
                    Toast.makeText(view.getContext(), getResources().getString(R.string.newly_added_crypto), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(view.getContext(), getResources().getString(R.string.already_added_crypto), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
