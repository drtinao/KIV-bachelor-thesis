package fav.drtinao.skama;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

/**
 * Class is responsible for displaying part of the application which allows user to edit his/her buy&sell stocks diary.
 */
public class FragmentHistoryStocks extends Fragment {
    private ListView historyStocksLV; /* reference to history_stocks_lv; ListView object which will be used for displaying stocks owned by user */

    private View createdLayoutView; /* reference to inflated layout, needed for working with elements defined in the layout */

    private ArrayList<PieceStockData> ownedStockList; /* contains PieceStockData objects - each object represents one stock owned by user */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    /**
     * Is called by Android OS when layout should be inflated, here history_stocks_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.history_stocks_menu);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.history_stocks_fragment, container, false);
        createdLayoutView = createdLayout;

        /* get reference to ListView with stock listing */
        historyStocksLV = createdLayoutView.findViewById(R.id.history_stocks_lv);

        loadOwnedStockData();
        showOwnedStockData();

        prepHistoryStocksLV();

        return createdLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        loadPreferences();
    }

    /**
     * Adds button through which can user add another stock (in ActionBar).
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.graph_history_nav, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Is called when icon for displaying graph or adding stock is tapped (located in ActionBar).
     * @param item MenuItem object which represents tapped item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){ /* switch between items which are in the menu */
            case R.id.ic_graph_graph_history: /* show graph button -> display dialog with graph */
                loadOwnedStockData();

                if(ownedStockList.size() == 0){ /* no owned stocks -> cannot create graph */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(getResources().getString(R.string.history_stocks_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.history_stocks_alert_no_data_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.history_stocks_alert_no_data_ok),null);
                    alertBuilder.show();
                    return super.onOptionsItemSelected(item);
                }
                Toast.makeText(getContext(), getResources().getString(R.string.history_crypto_display_toast), Toast.LENGTH_LONG).show();

                /* some resources come from crypto part of the app (mainly strings) - functionality of these modules are similar */
                ArrayList<PieEntry> graphEntries = new ArrayList<>();

                /* get information regarding stocks owned by user */
                for(int i = 0; i < ownedStockList.size(); i++){ /* go through stocks owned by user */
                    FragmentHistoryStocks.PieceStockData stockData = ownedStockList.get(i);

                    String symbol = stockData.getSymbol(); /* symbol will be used as a label in chart */
                    double balance = 0; /* deduce final balance from individual transactions */
                    double ownedPieces = 0;

                    ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = stockData.getStockTransactions(); /* get transaction data related to the stock, which is owned by user */
                    if(transactionData.size() == 0){ /* no performed transactions related to the stock */
                        balance = 0;
                    }else{ /* deduce final balance */
                        for(int j = 0; j < transactionData.size(); j++){ /* go through performed transactions */
                            FragmentHistoryCrypto.PieceTransactionData transaction = transactionData.get(j); /* get one performed transaction */

                            if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY){ /* user bought stocks - add amount */
                                balance += transaction.getAmount() * transaction.getPrice();
                                ownedPieces += transaction.getAmount();
                            }else if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL){ /* user sold stock - remove amount */
                                balance -= transaction.getAmount() * transaction.getPrice();
                                ownedPieces -= transaction.getAmount();
                            }
                        }
                    }

                    /* got name and balance - add to list */
                    if(ownedPieces > 0 && balance > 0){
                        graphEntries.add(new PieEntry((float) balance, symbol));
                    }
                }

                if(graphEntries.size() == 0){ /* no stocks with amount over > 0 */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(getResources().getString(R.string.history_stocks_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.history_stocks_alert_no_over_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.history_stocks_alert_no_data_ok),null);
                    alertBuilder.show();
                    return super.onOptionsItemSelected(item);
                }

                final View dialogShowChart = getLayoutInflater().inflate(R.layout.history_stocks_dialog, null);

                final PieChart dataChart = dialogShowChart.findViewById(R.id.history_stocks_dialog_chart_id);
                dataChart.getDescription().setEnabled(false);
                dataChart.getLegend().setEnabled(false);
                dataChart.setUsePercentValues(true);

                PieDataSet dataSet = new PieDataSet(graphEntries, "");
                dataSet.setValueFormatter(new PercentFormatter());
                dataSet.setValueTextSize(16f);
                final PieData data = new PieData(dataSet);

                /* put more colors together... */
                int[] colors = new int[ColorTemplate.COLORFUL_COLORS.length + ColorTemplate.MATERIAL_COLORS.length + ColorTemplate.PASTEL_COLORS.length];
                for(int i = 0; i < ColorTemplate.COLORFUL_COLORS.length; i++){
                    colors[i] = ColorTemplate.COLORFUL_COLORS[i];
                }

                for(int i = ColorTemplate.COLORFUL_COLORS.length; i < (ColorTemplate.COLORFUL_COLORS.length + ColorTemplate.PASTEL_COLORS.length); i++){
                    colors[i] = ColorTemplate.PASTEL_COLORS[i - ColorTemplate.COLORFUL_COLORS.length];
                }

                for(int i = ColorTemplate.COLORFUL_COLORS.length + ColorTemplate.PASTEL_COLORS.length; i < (ColorTemplate.COLORFUL_COLORS.length + ColorTemplate.PASTEL_COLORS.length + ColorTemplate.MATERIAL_COLORS.length); i++){
                    colors[i] = ColorTemplate.MATERIAL_COLORS[i - ColorTemplate.COLORFUL_COLORS.length - ColorTemplate.PASTEL_COLORS.length];
                }

                dataSet.setColors(colors);

                dataChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        TextView symbolTV = dialogShowChart.findViewById(R.id.history_stocks_dialog_symbol_tv);
                        TextView countTv = dialogShowChart.findViewById(R.id.history_stocks_dialog_count_tv);
                        TextView investedTv = dialogShowChart.findViewById(R.id.history_stocks_dialog_invested_tv);

                        /* set information regarding to selected stock */
                        FragmentHistoryStocks.PieceStockData stock = ownedStockList.get((int) h.getX());
                        symbolTV.setText(getResources().getString(R.string.history_crypto_dialog_symbol_tv) + " " + stock.getSymbol());

                        DecimalFormat decFor = new DecimalFormat("#.####");
                        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
                        decForSym.setDecimalSeparator('.');
                        decFor.setDecimalFormatSymbols(decForSym);

                        investedTv.setText(getResources().getString(R.string.history_crypto_dialog_invested_tv) + " " + decFor.format(h.getY()));

                        /* set currency */
                        String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                        investedTv.append(" " + currencyArray[currencyPref]);

                        double balance = 0;
                        ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = stock.getStockTransactions();
                        if(transactionData.size() != 0){
                            balance = 0;
                            for (int j = 0; j < transactionData.size(); j++) {
                                FragmentHistoryCrypto.PieceTransactionData transaction = transactionData.get(j);

                                if (transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY) { /* user bought stock - add amount */
                                    balance += transaction.getAmount();
                                } else if (transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL) { /* user sold stock - remove amount */
                                    balance -= transaction.getAmount();
                                }
                            }
                        }

                        countTv.setText(getResources().getString(R.string.history_crypto_dialog_count_tv) + " " + decFor.format(balance));
                    }

                    @Override
                    public void onNothingSelected() {
                        TextView symbolTV = dialogShowChart.findViewById(R.id.history_stocks_dialog_symbol_tv);
                        TextView countTv = dialogShowChart.findViewById(R.id.history_stocks_dialog_count_tv);
                        TextView investedTv = dialogShowChart.findViewById(R.id.history_stocks_dialog_invested_tv);

                        symbolTV.setText(getResources().getString(R.string.history_crypto_dialog_symbol_tv));
                        countTv.setText(getResources().getString(R.string.history_crypto_dialog_count_tv));
                        investedTv.setText(getResources().getString(R.string.history_crypto_dialog_invested_tv));
                    }
                });

                dataChart.setData(data);

                dataChart.setEntryLabelColor(Color.BLACK);
                dataChart.getDescription().setTextSize(16f);

                final AlertDialog chartDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.history_stocks_dialog_chart_title))
                        .setView(dialogShowChart)
                        .setPositiveButton(getActivity().getResources().getString(R.string.history_crypto_dialog_chart_dismiss), null)
                        .create();

                dataChart.animateX(3000);
                chartDialog.show();
                break;

            case R.id.ic_add_graph_history: /* add stock button -> go to another Fragment, which allows stock adding */
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentActualValueStocks()).addToBackStack(null).commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads stocks related preferences. In this case: preferred ordinal currency (eur, usd...).
     */
    private void loadPreferences(){
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Loads data regarding to user owned stocks from SharedPreferences, name "owned_stock_list".
     * Each stock is saved in format: ";;;stock_symbol".
     */
    private void loadOwnedStockData(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);

        String ownedStockDataString = sharedPref.getString(getResources().getString(R.string.sharedpref_stocks_owned_list), "");
        String[] ownedStockDataSplit = ownedStockDataString.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items */

        ownedStockList = new ArrayList<>();

        /* start from 1 - first empty */
        for(int i = 1; i < ownedStockDataSplit.length; i++){
            String stockSym = ownedStockDataSplit[i];

            PieceStockData ownedStock = new PieceStockData(stockSym);
            ownedStockList.add(ownedStock);

            /* check transaction count */
            String transactionDataString = sharedPref.getString(stockSym, "");
            if(transactionDataString.length() == 0){ /* no transactions are saved regarding to the stock */
                continue;
            }
            String[] transactionDataStringSplit = transactionDataString.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items - four for each transaction */
            if(transactionDataStringSplit.length == 0){ /* no transactions are saved regarding to the stock */
                continue;
            }

            /* start from 1 - first is always empty */
            for(int j = 1; j < transactionDataStringSplit.length; j += 4){
                String transType = transactionDataStringSplit[j]; /* transaction type - buy or sell */
                String transAmount = transactionDataStringSplit[j + 1]; /* transaction amount - tells how many stocks were processed during the transactions */
                String transPrice = transactionDataStringSplit[j + 2]; /* price of one stock unit */
                String transTime = transactionDataStringSplit[j + 3]; /* transaction timestamp */

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
                ownedStock.addTransaction(performedTransaction);
            }
        }
    }

    /**
     * Takes care of showing data from ArrayList ownedStockList to user. Contains information regarding to stocks owned by user.
     */
    private void showOwnedStockData(){
        if(ownedStockList.size() == 0){ /* no owned stock */
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.history_stocks_nothing_to_display), Toast.LENGTH_LONG).show();
            return;
        }

        StocksOwnedItemAdapter ownedStockAdapter = new StocksOwnedItemAdapter(getActivity(), ownedStockList);
        historyStocksLV.setAdapter(ownedStockAdapter);
        ownedStockAdapter.notifyDataSetChanged();
    }

    /**
     * Prepares ListView with user owned stocks for interactivity. After clicking on stock name transactions related to the stock should appear.
     */
    private void prepHistoryStocksLV(){
        final HistoryStocksDetailFrag historyStockDetailFrag = new HistoryStocksDetailFrag();
        final Bundle dataBundle = new Bundle();

        historyStocksLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dataBundle.putString(getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol), ownedStockList.get(position).getSymbol());
                historyStockDetailFrag.setArguments(dataBundle);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, historyStockDetailFrag).addToBackStack(null).commit();
            }
        });
    }

    /**
     * Inner data class, carries symbol of the stock and transactions associated with the stock.
     */
    public static class PieceStockData{
        private String symbol; /* symbol of the stock */

        private ArrayList<FragmentHistoryCrypto.PieceTransactionData> stockTransactions; /* history of transactions performed with the given stock */
        /**
         * Constructor is used for initialization of basic values regarding to the stock.
         * @param symbol symbol of the stock
         */
        public PieceStockData(String symbol){
            this.symbol = symbol;

            stockTransactions = new ArrayList<>();
        }

        /**
         * Adds transaction (object FragmentHistoryCrypto.PieceTransactionData) provided by user to the ArrayList.
         * Transaction class from crypto part of the app can be used, because the type of information stored for transactions associated with stocks is the same as with cryptocurrency transactions.
         * @param transaction object FragmentHistoryCrypto.PieceTransactionData which represents one transaction related to the stock (buy or sell)
         */
        public void addTransaction(FragmentHistoryCrypto.PieceTransactionData transaction){
            this.stockTransactions.add(transaction);
        }

        /**
         * Returns all transactions associated with the stock. Each transaction is represented by FragmentHistoryCrypto.PieceTransactionData object.
         * PieceTransactionData class from crypto part of the app is used - type of information stored for each stock transaction are same as information stored for cryptocurrency transaction.
         * @return ArrayList object which contains FragmentHistoryCrypto.PieceTransactionData objects - each one represents one transaction related to the stock
         */
        public ArrayList<FragmentHistoryCrypto.PieceTransactionData> getStockTransactions(){
            return stockTransactions;
        }

        /**
         * Getter for stock symbol.
         * @return stock symbol
         */
        public String getSymbol(){
            return symbol;
        }
    }
}
