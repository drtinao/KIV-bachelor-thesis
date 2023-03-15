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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Displays part of the application, which shows actual stocks values and history.
 */
public class ActualValueStocksDetailFrag extends Fragment {
    private View createdLayoutView; /* reference to inflated layout, needed for referencing to elements inside */

    private TextView actualValueTv; /* reference to stocks_detail_frag1_act_val_tv2_id; TextView object which displays actual value of stock */
    private LineChart dataChart; /* reference to stocks_detail_frag1_chart_id or stocks_detail_frag2_chart_id; object with chart */

    private double actualPrice; /* actual price of stock in currency given by user */
    private ArrayList<LogicActualValueStocksDetail.HistoryDataPiece> historyData; /* ArrayList which stores historical data regarding given stock */

    private LogicActualValueStocksDetail logicActualValueStocksDetail;
    private int selectedTabNum; /* number of tab which was selected by user (1 = tab with actual info - 24h, 2 = tab with history) */
    private String selectedStockSym; /* symbol of the stock which was selected by user */

    private String dateFrom; /* represents date from which history is shown in given format: day.month.year */
    private String dateTo; /* represents date to which history is shown in given format: day.month.year */

    private double lastSelectedPrice; /* price - last selected point */
    private String lastSelectedDate; /* date - last selected point */
    private String lastSelectedTime; /* time - last selected point */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */

    private final long YEARS_4_MILIS = 126227808000l;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Bundle dataBundle = getArguments();
        selectedTabNum = dataBundle.getInt(getResources().getString(R.string.actual_value_stocks_detail_sharedpref_tab), -1);
        selectedStockSym = dataBundle.getString(getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol));

        ((MainActivity)getActivity()).getSupportActionBar().setTitle(selectedStockSym);

        View createdLayout = null;

        switch(selectedTabNum){
            case 2:
                createdLayout = inflater.inflate(R.layout.actual_value_stocks_detail_frag2, container, false);
                TextView selectedStockSymTv = createdLayout.findViewById(R.id.stocks_detail_frag2_stocks_id_hidden_id); /* set stock symbol to hidden TextView */
                selectedStockSymTv.setText(selectedStockSym);

                prepareSelDateFromEt(createdLayout);
                prepareSelDateToEt(createdLayout);
                prepareSubDateBtn(createdLayout);
                break;

            default:
                createdLayout = inflater.inflate(R.layout.actual_value_stocks_detail_frag1, container, false);
                prepActValAddBtn(createdLayout);
                logicActualValueStocksDetail = new LogicActualValueStocksDetail(getContext(), this, LogicActualValueStocksDetail.tabList.GENERAL_TAB, selectedStockSym);
        }

        createdLayoutView = createdLayout;
        acquireRefViewsInfo();

        return createdLayout;
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
            logicActualValueStocksDetail.execute();
        }
    }

    /**
     * Performs actions which leads to displaying chart with stock value history (date is chosen by user).
     * @param view needed for interaction with UI elements
     */
    private void prepareSubDateBtn(final View view){
        final Button submitDateBtn = view.findViewById(R.id.stocks_detail_frag2_submit_date_btn_id);
        final ActualValueStocksDetailFrag thisInstance = this;

        submitDateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                TextView stockSym = view.findViewById(R.id.stocks_detail_frag2_stocks_id_hidden_id); /* get stock symbol, which is in hidden TextView */
                EditText dateHistoryFrom = view.findViewById(R.id.stocks_detail_frag2_sel_date_from_et_id); /* get date from which history should be retrieved */
                EditText dateHistoryTo = view.findViewById(R.id.stocks_detail_frag2_sel_date_to_et_id); /* get date to which history should be retrieved */

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
                logicActualValueStocksDetail = new LogicActualValueStocksDetail(getContext(), thisInstance, LogicActualValueStocksDetail.tabList.HISTORY_TAB, stockSym.getText().toString(), dateHistoryFrom.getText().toString(), dateHistoryTo.getText().toString());
                logicActualValueStocksDetail.execute();
            }
        });
    }

    /**
     * Prepares EditText, which allows user to pick date from which stock history will be displayed.
     * @param view needed for interaction with UI elements
     */
    private void prepareSelDateFromEt(View view){
        final EditText selDateFromEt = view.findViewById(R.id.stocks_detail_frag2_sel_date_from_et_id);

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
     * Prepares EditText, which allows user to pick date to which stock history will be displayed.
     * @param view needed for interaction with UI elements
     */
    private void prepareSelDateToEt(View view){
        final EditText selDateToEt = view.findViewById(R.id.stocks_detail_frag2_sel_date_to_et_id);

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
     * Used for returning actual value of stock from LogicActualValueStocksDetail.
     * @param actualPrice actual price of stock in currency given by user
     */
    public void setActualPrice(double actualPrice){
        this.actualPrice = actualPrice;
    }

    /**
     * Used for returning historical data regarding stock selected by user. Typically called from instance of LogicActualValueStocksDetail.
     * @param historyData ArrayList which stores historical data regarding given stock
     */
    public void setHistoryData(ArrayList<LogicActualValueStocksDetail.HistoryDataPiece> historyData){
        this.historyData = historyData;
        if(historyData != null){
            showRetrievedInfo();
        }else{
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_stocks_fill_title));
            alertBuilder.setMessage(getResources().getString(R.string.actual_value_stocks_data_mes));
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_stocks_fill_ok),null);
            alertBuilder.show();
        }
    }

    /**
     * Gets references to some objects which are later used to display retrieved data (regarding stocks values).
     */
    private void acquireRefViewsInfo(){
        switch(selectedTabNum){
            case 1: /* general (first) tab */
                actualValueTv = createdLayoutView.findViewById(R.id.stocks_detail_frag1_act_val_tv2_id);
                dataChart = createdLayoutView.findViewById(R.id.stocks_detail_frag1_chart_id);
                break;

            case 2: /* history (second) tab */
                dataChart = createdLayoutView.findViewById(R.id.stocks_detail_frag2_chart_id);
                break;

            default:  /* unknown tab */
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
     * Layouts from cryptocurrency part of the application are used - functionality principle is the same for both application parts (crypto and stocks).
     * @param item MenuItem object which represents tapped item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.ic_alert_simulation:
                /* check if alert for selected stock not yet saved; if not -> display save dialog */
                String savedStocksAlerts = sharedPref.getString(getResources().getString(R.string.sharedpref_stocks_alerts), ""); /* get saved stocks alerts */

                /* check if alert for specific stock is yet created or not */
                if(savedStocksAlerts.contains(getResources().getString(R.string.sharedpref_item_separator) + selectedStockSym + getResources().getString(R.string.sharedpref_item_separator))){ /* alert for selected stock already created */
                    Toast.makeText(getContext(), getResources().getString(R.string.already_added_stock_alert), Toast.LENGTH_SHORT).show();
                    return super.onOptionsItemSelected(item);
                }

                /* no alert for selected stock yet found, go on... */
                final View dialogAddAlert = getLayoutInflater().inflate(R.layout.dialog_alert_actual_value_crypto_detail_frag, null);
                /* set currency */
                String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                TextView priceCurrencyTv = dialogAddAlert.findViewById(R.id.dialog_alert_actual_value_price_tv2_id);
                priceCurrencyTv.setText(currencyArray[currencyPref]);

                /* create dialog for user input */
                final AlertDialog addAlertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.actual_value_stocks_detail_dialog_alert_title))
                        .setMessage(getActivity().getResources().getString(R.string.actual_value_stocks_detail_dialog_alert_message))
                        .setView(dialogAddAlert)
                        .setPositiveButton(getActivity().getResources().getString(R.string.actual_value_stocks_detail_dialog_alert_add), null)
                        .setNegativeButton(getActivity().getResources().getString(R.string.actual_value_stocks_detail_dialog_alert_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.actual_value_stocks_detail_dialog_alert_not_added), Toast.LENGTH_SHORT).show();
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
                if(logicActualValueStocksDetail == null || lastSelectedDate == null){
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
                historyStockPriceEt.setText(String.valueOf(lastSelectedPrice));
                historyStockPriceEt.setFocusable(false);

                /* set currency */
                TextView historyStockCurrencyTv = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_currency_tv_id);
                historyStockCurrencyTv.setText(curArr[currencyPref]);

                /* set date */
                EditText historyStockDateEt = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_date_et_id);
                historyStockDateEt.setText(lastSelectedDate);
                historyStockDateEt.setFocusable(false);

                /* set time */
                EditText historyStockTimeEt = dialogSimulateTransaction.findViewById(R.id.dialog_history_crypto_time_et_id);
                historyStockTimeEt.setText(lastSelectedTime);
                historyStockTimeEt.setFocusable(false);

                simulateTransactionDialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Saves alert for specific stock.
     * @param dialogAddAlert reference to add alert dialog´s layout
     * @param addAlertDialog reference to AlertDialog object
     */
    private void saveAlert(View dialogAddAlert, AlertDialog addAlertDialog) {
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
        String savedStocksAlerts = sharedPref.getString("stocks_alerts", ""); /* get already saved alerts regarding to stocks */
        String selectedStockInfo = ";;;" + selectedStockSym + ";;;" + typeAlert + ";;;" + actualPriceText; /* save in format: ;;;stock_sym;;;BELOW/ABOVE;;;target_price */

        /* check if alert for specific stock is yet created or not */
        if(!savedStocksAlerts.contains(";;;" + selectedStockSym + ";;;")){ /* no alert present for the stock with given symbol, add one */
            savedStocksAlerts += selectedStockInfo;
            sharedPrefEd.putString("stocks_alerts", savedStocksAlerts);
            sharedPrefEd.commit();

            Toast.makeText(getContext(), getResources().getString(R.string.newly_added_stock_alert), Toast.LENGTH_SHORT).show();
        }else{ /* alert already present, do not add another one */
            Toast.makeText(getContext(), getResources().getString(R.string.already_added_stock_alert), Toast.LENGTH_SHORT).show();
        }

        addAlertDialog.dismiss();
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
        LogicActualValueStocksDetail.HistoryDataPiece latestStockPrice = logicActualValueStocksDetail.getLatestPrice();
        double latestPrice = latestStockPrice.getValue();
        long latestTime = latestStockPrice.getTime();
        String latestTimeString = dataChart.getXAxis().getValueFormatter().getFormattedValue(latestTime, null); /* convert unix timestamp to format: "dd.mm.yy hour:min:sec" */

        String[] latesTimeStringSplit = latestTimeString.split(" "); /* get day & time */
        String dayString = latesTimeStringSplit[0];
        String timeString = latesTimeStringSplit[1];

        double totalInvestedHistory = Double.valueOf(amountText) * Double.valueOf(priceText); /* how much invested back then */
        double totalInvestedNow = Double.valueOf(amountText) * latestPrice; /* how much would be invested now */

        String resultToDisplay;

        DecimalFormat decFor = new DecimalFormat("#.####");
        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
        decForSym.setDecimalSeparator('.');
        decFor.setDecimalFormatSymbols(decForSym);

        String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);

        if(transType.equals(getResources().getString(R.string.sharedpref_transaction_type_buy))){ /* simulate buy */
            double gain = totalInvestedNow - totalInvestedHistory;

            String beforeBuy = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_1) + " " + dateText + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_2) + " " + timeText + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_3) + " " + decFor.format(totalInvestedHistory) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_4);
            String latestBuy = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_5) + " " + dayString + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_6) + " " + timeString + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_7) + " " + decFor.format(latestPrice) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_4) + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_8) + " " + decFor.format(totalInvestedNow) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_4);

            if(gain > 0){
                resultToDisplay = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_9_gain) + " " + decFor.format(gain) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_10_gain);
            }else{
                resultToDisplay = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_9_loss) + " " + decFor.format(Math.abs(gain)) + " " + currencyArray[currencyPref] + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_10_loss);
            }

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_title));
            alertBuilder.setMessage(beforeBuy + "\n\n" + latestBuy + "\n\n" + resultToDisplay);
            alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_buy_dismiss),null);
            alertBuilder.show();

            simulateTransactionAlertDialog.dismiss();
        }else{ /* simulate sell */
            double gain = totalInvestedHistory - totalInvestedNow;

            String beforeSell = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_1) + " " + dateText + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_2) + " " + timeText + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_3) + " " + decFor.format(totalInvestedHistory) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_4);
            String latestSell = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_5) + " " + dayString + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_6) + " " + timeString + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_7) + " " + decFor.format(latestPrice) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_4) + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_8) + " " + decFor.format(totalInvestedNow) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_4);

            if(gain > 0){
                resultToDisplay = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_9_gain) + " " + decFor.format(gain) + " " + currencyArray[currencyPref] + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_10_gain);
            }else{
                resultToDisplay = getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_9_loss) + " " + decFor.format(Math.abs(gain)) + " " + currencyArray[currencyPref] + " " + getResources().getString(R.string.actual_value_stocks_detail_dialog_sim_sell_10_loss);
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
     * Shows retrieved information (regarding stock values) to user.
     */
    private void showRetrievedInfo(){
        if(selectedTabNum == 1){ /* for general tab display actual price + chart with 24 hour history data */
            dataChart.setTouchEnabled(true);
            dataChart.setDrawMarkerViews(true);
            ActualValueMarker customMarkerView = new ActualValueMarker(createdLayoutView.getContext(), R.layout.actual_value_item);
            dataChart.setMarkerView(customMarkerView);

            /* hide legend */
            dataChart.getLegend().setEnabled(false);
            dataChart.getDescription().setEnabled(false);

            /* limit list of visible x value labels (solves text overlapping) - START */
            dataChart.getXAxis().setGranularityEnabled(true);
            dataChart.getXAxis().setGranularity(1.0f);
            dataChart.getXAxis().setLabelCount(3);
            /* limit list of visible x value labels (solves text overlapping) - END */

            /* display actual value of stock - START */
            String[] currencyArray = getActivity().getResources().getStringArray(R.array.settings_general_currency_pref_list);
            DecimalFormat decFor = new DecimalFormat("#.####");
            DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
            decForSym.setDecimalSeparator('.');
            decFor.setDecimalFormatSymbols(decForSym);
            actualValueTv.setText(" " + decFor.format(actualPrice) + " " + currencyArray[currencyPref]);
            /* display actual value of stock - END */

            /* display chart with 24 hour history - START */
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i = 0; i < historyData.size(); i++){
                LogicActualValueStocksDetail.HistoryDataPiece onePiece = historyData.get(i);
                entries.add(new Entry(onePiece.getTime(), (float) onePiece.getValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Customized values");
            LineData data = new LineData(dataSet);
            dataChart.setData(data);

            IAxisValueFormatter dateFormatter = new DateFormatter();
            XAxis xAxis = dataChart.getXAxis();
            xAxis.setValueFormatter(dateFormatter);
            dataChart.invalidate();
            /* display chart with 24 hour history - END */
        }else if(selectedTabNum == 2){ /* for history tab display just chart with history data for days between user given dates */
            dataChart.setTouchEnabled(true);
            dataChart.setDrawMarkerViews(true);
            ActualValueMarker customMarkerView = new ActualValueMarker(createdLayoutView.getContext(), R.layout.actual_value_item);
            dataChart.setMarkerView(customMarkerView);

            /* hide legend */
            dataChart.getLegend().setEnabled(false);
            dataChart.getDescription().setEnabled(false);

            /* limit list of visible x value labels (solves text overlapping) - START */
            dataChart.getXAxis().setGranularityEnabled(true);
            dataChart.getXAxis().setGranularity(1.0f);
            dataChart.getXAxis().setLabelCount(3);
            /* limit list of visible x value labels (solves text overlapping) - END */

            /* display chart with history - START */
            ArrayList<Entry> entries = new ArrayList<>();
            for(int i = 0; i < historyData.size(); i++){
                LogicActualValueStocksDetail.HistoryDataPiece onePiece = historyData.get(i);
                entries.add(new Entry(onePiece.getTime(), (float) onePiece.getValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Customized values");
            LineData data = new LineData(dataSet);
            dataChart.setData(data);

            IAxisValueFormatter dateFormatter = new DateFormatter();
            XAxis xAxis = dataChart.getXAxis();
            xAxis.setValueFormatter(dateFormatter);
            dataChart.invalidate();
            /* display chart with history - END */
        }
    }

    /**
     * Loads stocks related preferences. In this case: preferred ordinal currency (eur, usd...).
     */
    private void loadPreferences(){
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Inner class which helps with converting of unix timestamp to human readable date (and time). Converted value is finally displayed in chart. Used for 24 hour history.
     */
    public class DateFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Date date = new Date((long) value);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

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

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    public class ActualValueMarker extends MarkerView {
        private TextView actualPriceTv;
        private TextView actualTimeTv;

        public ActualValueMarker(Context context, int layoutResource) {
            super(context, layoutResource);
            this.actualPriceTv = findViewById(R.id.actual_value_item_date_tv2_id);
            this.actualTimeTv = findViewById(R.id.actual_value_item_price_tv2_id);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            Date date = new Date((long) e.getX());

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

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

            lastSelectedDate = day + "." + month + "." + yearCut;
            lastSelectedTime = hourEdit + ":" + minEdit + ":" + secEdit;

            actualPriceTv.setText(dateEdit);
            String[] currencyArray = getActivity().getResources().getStringArray(R.array.settings_general_currency_pref_list);
            DecimalFormat decFor = new DecimalFormat("#.####");
            DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
            decForSym.setDecimalSeparator('.');
            decFor.setDecimalFormatSymbols(decForSym);
            actualTimeTv.setText(String.valueOf(decFor.format(e.getY())) + " " + currencyArray[currencyPref]);

            String[] splitPrice = actualTimeTv.getText().toString().split(" ");
            lastSelectedPrice = Double.valueOf(splitPrice[0]);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth()), -getHeight());
        }
    }

    /**
     * Prepares Button through which can user add stock to list of owned stocks.
     * @param view needed for interaction with UI elements
     */
    private void prepActValAddBtn(final View view){
        final Button actValAddBtn = view.findViewById(R.id.stocks_detail_frag1_act_val_add_btn_id);

        actValAddBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /* get info regarding already owned stocks */
                String ownedStockSharedPref = sharedPref.getString(getResources().getString(R.string.sharedpref_stocks_owned_list), ""); /* presented in format: "stock_symbol" */

                String selectedStock = getResources().getString(R.string.sharedpref_item_separator) + selectedStockSym;

                /* check if stock is already present or not */
                if(!ownedStockSharedPref.contains(selectedStock)){ /* if not */
                    ownedStockSharedPref += selectedStock;
                    sharedPrefEd.putString(getResources().getString(R.string.sharedpref_stocks_owned_list), ownedStockSharedPref);
                    sharedPrefEd.commit();
                    Toast.makeText(view.getContext(), getResources().getString(R.string.newly_added_stock), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(view.getContext(), getResources().getString(R.string.already_added_stock), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
