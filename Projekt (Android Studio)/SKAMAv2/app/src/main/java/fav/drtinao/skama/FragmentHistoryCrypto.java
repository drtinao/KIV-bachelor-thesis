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
 * Class is responsible for displaying part of the application which allows user to edit his/her buy&sell cryptocurrency diary.
 */
public class FragmentHistoryCrypto extends Fragment {
    private ListView historyCryptoLV; /* reference to history_crypto_lv; ListView object which will be used for displaying cryptocurrencies owned by user */

    private View createdLayoutView; /* reference to inflated layout, needed for working with elements defined in the layout */

    private ArrayList<PieceCryptoData> ownedCryptoList; /* ArrayList with PieceCryptoData objects; each object represents one cryptocurrency owned by user */

    private int currencyPref; /* currency preference (czk, usd, eur...) - default pref is 0 */
    private SharedPreferences sharedPref;

    /**
     * Is called by Android OS when layout should be inflated, here history_crypto_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.history_crypto_menu);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.history_crypto_fragment, container, false);
        createdLayoutView = createdLayout;

        acquireRefViewsInfo();

        loadOwnedCryptoData();
        showOwnedCryptoData();

        prepHistoryCryptoLV();

        return createdLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        loadPreferences();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Method obtains reference to ListView, which is defined in xml regarding to this fragment.
     */
    private void acquireRefViewsInfo(){
        historyCryptoLV = createdLayoutView.findViewById(R.id.history_crypto_lv);
    }

    /**
     * Takes care of ActionBar modification, adds buttons for adding cryptocurrencies.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.graph_history_nav, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Is called when icon for displaying graph or adding cryptocurrency is tapped (located in ActionBar).
     * @param item MenuItem object which represents tapped item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){ /* switch between items which are in the menu */
            case R.id.ic_graph_graph_history: /* show graph button -> display dialog with graph */
                loadOwnedCryptoData();

                if(ownedCryptoList.size() == 0){ /* no owned crypto -> cannot create graph */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(getResources().getString(R.string.history_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.history_crypto_alert_no_data_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_alert_no_data_ok),null);
                    alertBuilder.show();
                    return super.onOptionsItemSelected(item);
                }
                Toast.makeText(getContext(), getResources().getString(R.string.history_crypto_display_toast), Toast.LENGTH_LONG).show();

                ArrayList<PieEntry> graphEntries = new ArrayList<>();

                /* get information regarding to owned cryptocurrencies */
                for(int i = 0; i < ownedCryptoList.size(); i++){ /* go through cryptocurrency list */
                    PieceCryptoData cryptoData = ownedCryptoList.get(i);

                    String name; /* get name of the crypto (or symbol if count > 10) */
                    if(ownedCryptoList.size() > 10){
                        name = cryptoData.getName();
                    }else{
                        name = cryptoData.getSymbol();
                    }

                    double balance = 0; /* deduce final balance from individual transactions */
                    double ownedPieces = 0;

                    ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = cryptoData.getCryptoTransactions(); /* get transaction data related to cryptocurrency, which is owned by user */
                    if(transactionData.size() == 0){ /* no performed transactions related to the crypto */
                        balance = 0;
                    }else{ /* deduce final balance */
                        for(int j = 0; j < transactionData.size(); j++){ /* go through performed transactions */
                            PieceTransactionData transaction = transactionData.get(j); /* get one performed transaction */

                            if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY){ /* user bought crypto - add amount */
                                balance += transaction.getAmount() * transaction.getPrice();
                                ownedPieces += transaction.getAmount();
                            }else if(transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL){ /* user sold crypto - remove amount */
                                balance -= transaction.getAmount() * transaction.getPrice();
                                ownedPieces -= transaction.getAmount();
                            }
                        }
                    }

                    /* got name and balance - add to list */
                    if(ownedPieces > 0 && balance > 0){
                        graphEntries.add(new PieEntry((float) balance, name));
                    }
                }

                if(graphEntries.size() == 0){ /* no crypto with amount over > 0 */
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(getResources().getString(R.string.history_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.history_crypto_alert_no_over_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.history_crypto_alert_no_data_ok),null);
                    alertBuilder.show();
                    return super.onOptionsItemSelected(item);
                }

                final View dialogShowChart = getLayoutInflater().inflate(R.layout.history_crypto_dialog, null);

                final PieChart dataChart = dialogShowChart.findViewById(R.id.history_crypto_dialog_chart_id);
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
                        TextView nameTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_name_tv);
                        TextView symbolTV = dialogShowChart.findViewById(R.id.history_crypto_dialog_symbol_tv);
                        TextView countTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_count_tv);
                        TextView investedTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_invested_tv);

                        DecimalFormat decFor = new DecimalFormat("#.####");
                        DecimalFormatSymbols decForSym = new DecimalFormatSymbols();
                        decForSym.setDecimalSeparator('.');
                        decFor.setDecimalFormatSymbols(decForSym);

                        /* set information regarding to selected cryptocurrency */
                        PieceCryptoData crypto = ownedCryptoList.get((int) h.getX());
                        nameTv.setText(getResources().getString(R.string.history_crypto_dialog_name_tv) + " " + crypto.getName());
                        symbolTV.setText(getResources().getString(R.string.history_crypto_dialog_symbol_tv) + " " + crypto.getSymbol());
                        investedTv.setText(getResources().getString(R.string.history_crypto_dialog_invested_tv) + " " + decFor.format(h.getY()));

                        /* set currency */
                        String[] currencyArray = getResources().getStringArray(R.array.settings_general_currency_pref_list);
                        investedTv.append(" " + currencyArray[currencyPref]);

                        double balance = 0;
                        ArrayList<FragmentHistoryCrypto.PieceTransactionData> transactionData = crypto.getCryptoTransactions();
                        if(transactionData.size() != 0){
                            balance = 0;
                            for (int j = 0; j < transactionData.size(); j++) {
                                PieceTransactionData transaction = transactionData.get(j);

                                if (transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.BUY) { /* user bought crypto - add amount */
                                    balance += transaction.getAmount();
                                } else if (transaction.getType() == FragmentHistoryCrypto.PieceTransactionData.transactionType.SELL) { /* user sold crypto - remove amount */
                                    balance -= transaction.getAmount();
                                }
                            }
                        }
                        countTv.setText(getResources().getString(R.string.history_crypto_dialog_count_tv) + " " + decFor.format(balance));
                    }

                    @Override
                    public void onNothingSelected() {
                        TextView nameTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_name_tv);
                        TextView symbolTV = dialogShowChart.findViewById(R.id.history_crypto_dialog_symbol_tv);
                        TextView countTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_count_tv);
                        TextView investedTv = dialogShowChart.findViewById(R.id.history_crypto_dialog_invested_tv);

                        nameTv.setText(getResources().getString(R.string.history_crypto_dialog_name_tv));
                        symbolTV.setText(getResources().getString(R.string.history_crypto_dialog_symbol_tv));
                        countTv.setText(getResources().getString(R.string.history_crypto_dialog_count_tv));
                        investedTv.setText(getResources().getString(R.string.history_crypto_dialog_invested_tv));
                    }
                });

                dataChart.setData(data);

                dataChart.setEntryLabelColor(Color.BLACK);
                dataChart.getDescription().setTextSize(16f);

                final AlertDialog chartDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getResources().getString(R.string.history_crypto_dialog_chart_title))
                        .setView(dialogShowChart)
                        .setPositiveButton(getActivity().getResources().getString(R.string.history_crypto_dialog_chart_dismiss), null)
                        .create();

                dataChart.animateX(3000);
                chartDialog.show();
                break;

            case R.id.ic_add_graph_history: /* add cryptocurrency button -> go to another Fragment, which allows cryptocurrency adding */
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentActualValueCrypto()).addToBackStack(null).commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads crypto related preferences. In this case: preferred ordinal currency (eur, usd...).
     */
    private void loadPreferences(){
        currencyPref = sharedPref.getInt(getActivity().getResources().getString(R.string.settings_sharedpref_currency_pref), 0); /* preferred currency number */
    }

    /**
     * Loads data regarding owned cryptocurrencies from SharedPreferences object, name "owned_crypto_list".
     * Every owned cryptocurrency is saved in following format: "crypto_id;;;crypto_symbol;;;crypto_name".
     */
    private void loadOwnedCryptoData(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);

        String ownedCryptoDataString = sharedPref.getString(getResources().getString(R.string.sharedpref_crypto_owned_list), "");
        String[] ownedCryptoDataSplit = ownedCryptoDataString.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items - three for each cryptocurrency */

        ownedCryptoList = new ArrayList<>();

        /* start from 1 - first is always empty */
        for(int i = 1; i < ownedCryptoDataSplit.length; i += 3){ /* every crypto contains of 3 items - id, symbol and name */
            String id = ownedCryptoDataSplit[i]; /* first is cryptocurrency id */
            String symbol = ownedCryptoDataSplit[i + 1]; /* second is cryptocurrency symbol */
            String name = ownedCryptoDataSplit[i + 2]; /* third is cryptocurrency name */

            PieceCryptoData ownedCrypto = new PieceCryptoData(id, symbol, name);
            ownedCryptoList.add(ownedCrypto);

            /* check transaction count */
            String transactionDataString = sharedPref.getString(id, "");
            if(transactionDataString.length() == 0){ /* no transactions are saved regarding to the crypto */
                continue;
            }
            String[] transactionDataStringSplit = transactionDataString.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items - four for each transaction */
            if(transactionDataStringSplit.length == 0){ /* no transactions are saved regarding to the crypto */
                continue;
            }

            /* start from 1 - first is always empty */
            for(int j = 1; j < transactionDataStringSplit.length; j += 4){
                String transType = transactionDataStringSplit[j]; /* first is transaction type - buy or sell */
                String transAmount = transactionDataStringSplit[j + 1]; /* second is transaction amount - tells how much crypto was processed during the transactions */
                String transPrice = transactionDataStringSplit[j + 2]; /* third is the price of one crypto unit */
                String transTime = transactionDataStringSplit[j + 3]; /* fourth is transaction timestamp */

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
                ownedCrypto.addTransaction(performedTransaction);
            }
        }
    }

    /**
     * Prepares ListView which contains cryptocurrencies owned by user for interactivity. After clicking on list entry transactions should appear.
     */
    private void prepHistoryCryptoLV(){
        final HistoryCryptoDetailFrag historyCryptoDetailFrag = new HistoryCryptoDetailFrag();
        final Bundle dataBundle = new Bundle();

        historyCryptoLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                dataBundle.putString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id), ownedCryptoList.get(position).getId());
                dataBundle.putString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name), ownedCryptoList.get(position).getName());
                historyCryptoDetailFrag.setArguments(dataBundle);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, historyCryptoDetailFrag).addToBackStack(null).commit();
            }
        });
    }

    /**
     * Shows data from Arraylist ownedCryptoList to user. The ArrayList contains information regarding user owned cryptocurrencies (id, symbol and name).
     */
    private void showOwnedCryptoData(){
        if(ownedCryptoList.size() == 0){ /* no owned crypto */
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.history_crypto_nothing_to_display), Toast.LENGTH_LONG).show();
            return;
        }

        CryptoOwnedItemAdapter ownedCryptoAdapter = new CryptoOwnedItemAdapter(getActivity(), ownedCryptoList);
        historyCryptoLV.setAdapter(ownedCryptoAdapter);
        ownedCryptoAdapter.notifyDataSetChanged();
    }

    /**
     * Inner data class, carries data regarding cryptocurrency owned by user - id, symbol, name of the crypto.
     * Also takes care of storing transactions, which user performed with respective cryptocurrency (buy or sell).
     */
    public static class PieceCryptoData{
        private String id; /* id of the cryptocurrency */
        private String symbol; /* symbol of the cryptocurrency */
        private String name; /* name of the cryptocurrency */

        private ArrayList<PieceTransactionData> cryptoTransactions; /* history of transactions performed with the given cryptocurrency */

        /**
         * Constructor is used for initialization of basic values regarding the cryptocurrency (id, symbol and name of the crypto).
         * @param id id of the cryptocurrency
         * @param symbol symbol of the cryptocurrency
         * @param name name of the cryptocurrency
         */
        public PieceCryptoData(String id, String symbol, String name){
            this.id = id;
            this.symbol = symbol;
            this.name = name;

            this.cryptoTransactions = new ArrayList<>();
        }

        /**
         * Adds transaction (object PieceTransactionData) provided by user to the ArrayList.
         * @param transaction object PieceTransactionData which represents one transaction (buy or sell)
         */
        public void addTransaction(PieceTransactionData transaction){
            this.cryptoTransactions.add(transaction);
        }

        /**
         * Returns all transactions which were created by user. Each transaction is represented by PieceTransactionData object.
         * @return ArrayList object which contains PieceTransactionData objects - each one represents one transaction
         */
        public ArrayList<PieceTransactionData> getCryptoTransactions() {
            return cryptoTransactions;
        }

        /**
         * Getter for cryptocurrency id.
         * @return cryptocurrency id
         */
        public String getId() {
            return id;
        }

        /**
         * Getter for cryptocurrency symbol.
         * @return cryptocurrency symbol
         */
        public String getSymbol() {
            return symbol;
        }

        /**
         * Getter for cryptocurrency name.
         * @return cryptocurrency name
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Inner data class, holds information regarding to one transaction.
     * Therefore provides information about transaction type (buy or sell), processed amount, price of one cryptocurrency piece and date, when was transaction carried out.
     */
    public static class PieceTransactionData{
        enum transactionType{SELL, BUY}; /* specifies type of transaction */

        private transactionType type; /* type of the transaction (buy or sell) */
        private double amount; /* amount which was processed during the transaction */
        private double price; /* price for which was one piece of the cryptocurrency sold / bought */
        private long timestamp; /* timestamp - when was transaction performed */

        /**
         * Constructor takes basic information regarding to one transaction.
         * @param type type of the transaction (buy or sell)
         * @param amount amount which was processed during the transaction
         * @param price price for which was one piece of the cryptocurrency sold or bought
         * @param timestamp timestamp tells, when was transaction performed
         */
        public PieceTransactionData(transactionType type, double amount, double price, long timestamp){
            this.type = type;
            this.amount = amount;
            this.price = price;
            this.timestamp = timestamp;
        }

        /**
         * Getter for transaction type.
         * @return type of the transaction (buy or sell)
         */
        public transactionType getType() {
            return type;
        }

        /**
         * Getter for amount which was processed during the transaction.
         * @return processed amount
         */
        public double getAmount() {
            return amount;
        }

        /**
         * Getter for price for which was one piece of the cryptocurrency sold or bought.
         * @return price of one cryptocurrency piece
         */
        public double getPrice() {
            return price;
        }

        /**
         * Getter for unix timestamp. Tells when was transaction performed.
         * @return unix timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}
