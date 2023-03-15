package fav.drtinao.skama;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.util.ArrayList;

/**
 * Main purpose of this class is to to perform steps which result in displaying cryptocurrency converter.
 */
public class FragmentConverterCrypto extends Fragment {
    private ListView converterCryptoLV; /* reference to converter_crypto_lv; ListView object; used multiple times, use findViewById just once => save resources */
    private EditText converterCryptoSelFromET; /* reference to converter_crypto_sel_from_et_id; contains symbol of cryptocurrency, from which user given amount should be converted */
    private TextView converterCryptoSelFromHiddenTV; /* reference to converter_crypto_sel_from_crypto_id_hidden_id; contains id of cryptocurrency from which value should be converted (id is given by website) */
    private EditText converterCryptoSelToET; /* reference to converter_crypto_sel_to_et_id; contains symbol of cryptocurrency, to which user given amount should be converted */
    private TextView converterCryptoSelToHiddenTV; /* reference to converter_crypto_sel_to_crypto_id_hidden_id; contains id of cryptocurrency to which value should be converted (id is given by website) */
    private Button converterCryptoConvertBtn; /* reference to converter_crypto_convert_btn_id; button which triggers conversion process */

    private int converterEditTextSel; /* value is changed according to latest selected EditText; 0 = init, no ET selected, 1 = from ET selected, 2 = to ET selected */
    private int selectedFromCryptoPos; /* position of from cryptocurrency in ArrayLists below */
    private int selectedToCryptoPos; /* position of To cryptocurrency in ArrayLists below */

    private ArrayList<LogicCryptoList.CryptoSupport> cryptoList; /* list with supported crypto (contains id, name, symbol) */
    private ArrayList<String> cryptoNames; /* cryptocurrency names (for correct work of search function) */
    private ArrayList<String> cryptoSymbols; /* cryptocurrency symbols (for correct work of search function) */
    private ArrayList<String> cryptoIds; /* cryptocurrency id´s (for finding information after cryptocurrency selection) */

    private View createdLayoutView; /* reference to inflated layout, needed for referencing to elements inside */

    ArrayList<LogicCryptoList.CryptoSupport> visibleCryptoList; /* list of currently visible cryptocurrencies (some can be ommited in comparison with cryptoList - when filtering is applied */

    /**
     * Is called by Android OS when layout should be inflated, here converter_crypto_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.converter_crypto_menu);

        setHasOptionsMenu(true);

        converterEditTextSel = 0;
        selectedFromCryptoPos = 0;
        selectedToCryptoPos = 0;

        View createdLayout = inflater.inflate(R.layout.converter_crypto_fragment, container, false);
        createdLayoutView = createdLayout;
        converterCryptoLV = createdLayout.findViewById(R.id.converter_crypto_lv);
        converterCryptoSelFromET = createdLayout.findViewById(R.id.converter_crypto_sel_from_et_id);
        converterCryptoSelFromHiddenTV = createdLayout.findViewById(R.id.converter_crypto_sel_from_crypto_id_hidden_id);
        converterCryptoSelToET = createdLayout.findViewById(R.id.converter_crypto_sel_to_et_id);
        converterCryptoSelToHiddenTV = createdLayout.findViewById(R.id.converter_crypto_sel_to_crypto_id_hidden_id);
        converterCryptoConvertBtn = createdLayout.findViewById(R.id.converter_crypto_convert_btn_id);

        prepConverterCryptoSelFromEt();
        prepConverterCryptoSelToEt();
        prepConverterCryptoLv();
        prepConverterCryptoSv();
        prepConverterCryptoConvertBtn();

        return createdLayout;
    }

    /**
     * This fragment is not supposed to have menu in ActionBar - the method is used just for clearing menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LogicCryptoList logicConverterCrypto = new LogicCryptoList(view.getContext(), this);
        logicConverterCrypto.execute();
    }

    /**
     * Prepares button with id converterCryptoConvertBtn. Performs conversion of value given in one cryptocurrency to another cryptocurrency when tapped.
     */
    private void prepConverterCryptoConvertBtn(){
        converterCryptoConvertBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /* check if all required fields are filled in - START */
                EditText conCryptoUserAmountEt = createdLayoutView.findViewById(R.id.converter_crypto_user_amount_et_id); /* contains amount to be converted */
                EditText conCryptoSelFromEt = createdLayoutView.findViewById(R.id.converter_crypto_sel_from_et_id); /* contains symbol of crypto from which value should be converted */
                TextView conCryptoSelFromCryptoId = createdLayoutView.findViewById(R.id.converter_crypto_sel_from_crypto_id_hidden_id); /* contains id of crypto from which value should be converted; invisible; id is given by website */
                EditText conCryptoSelToEt = createdLayoutView.findViewById(R.id.converter_crypto_sel_to_et_id); /* contains symbol of crypto to which value should be converted */
                TextView conCryptoSelToCryptoId = createdLayoutView.findViewById(R.id.converter_crypto_sel_to_crypto_id_hidden_id); /* contains id of crypto to which value should be converted; invisible; id is given by website */

                if(conCryptoUserAmountEt.getText().toString().isEmpty() || conCryptoUserAmountEt.getText().toString().equals(".")){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.converter_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.converter_crypto_alert_amount_not_pres_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.converter_crypto_alert_ok),null);
                    alertBuilder.show();
                    return;
                }

                if(conCryptoSelFromEt.getText().toString().isEmpty()){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.converter_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.converter_crypto_alert_crypto_from_not_pres_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.converter_crypto_alert_ok),null);
                    alertBuilder.show();
                    return;
                }

                if(conCryptoSelToEt.getText().toString().isEmpty()){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.converter_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.converter_crypto_alert_crypto_to_not_pres_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.converter_crypto_alert_ok),null);
                    alertBuilder.show();
                    return;
                }
                /* check if all required fields are filled in - END */

                double amountToConvert = Double.valueOf(conCryptoUserAmountEt.getText().toString()); /* user given amount to be converted */
                String idCryptoFrom = conCryptoSelFromCryptoId.getText().toString(); /* id of cryptocurrency from which amount should be converted */
                String idCryptoTo = conCryptoSelToCryptoId.getText().toString(); /* id of cryptocurrency to which amount should be converted */

                LogicConverterCrypto logicConverterCrypto = new LogicConverterCrypto(createdLayoutView.getContext(), amountToConvert, idCryptoFrom, idCryptoTo);
                logicConverterCrypto.execute();
            }
        });
    }

    /**
     * Prepares TextView which displays cryptocurrency, from which user given amount should be converted, for interaction with user.
     */
    private void prepConverterCryptoSelFromEt(){
        EditText converterCryptoSelFromEt = createdLayoutView.findViewById(R.id.converter_crypto_sel_from_et_id);

        /* show message - tells user that selection from list is expected */
        converterCryptoSelFromEt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                converterEditTextSel = 1; /* from ET selected */
                Toast.makeText(v.getContext(), getResources().getString(R.string.converter_crypto_alert_crypto_from_toast), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Prepares TextView which displays cryptocurrency, to which user given amount should be converted, for interaction with user.
     */
    private void prepConverterCryptoSelToEt(){
        /* show message - tells user that selection from list is expected */
        converterCryptoSelToET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                converterEditTextSel = 2; /* to ET selected */
                Toast.makeText(v.getContext(), getResources().getString(R.string.converter_crypto_alert_crypto_to_toast), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Prepares ListView which contains cryptocurrency list for interactivity.
     */
    private void prepConverterCryptoLv(){
        converterCryptoLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(converterEditTextSel == 0){ /* no ET selected yet */
                    Toast.makeText(view.getContext(), getResources().getString(R.string.converter_crypto_alert_crypto_first_sel_toast), Toast.LENGTH_LONG).show();
                }else if(converterEditTextSel == 1){ /* from ET selected */
                    selectedFromCryptoPos = position; /* keep selected cryptocurrency position in ArrayList */
                    converterCryptoSelFromET.setText(visibleCryptoList.get(selectedFromCryptoPos).getSymbolCrypto());
                    converterCryptoSelFromHiddenTV.setText(visibleCryptoList.get(selectedFromCryptoPos).getIdCrypto());

                }else if(converterEditTextSel == 2){ /* to ET selected */
                    selectedToCryptoPos = position; /* keep selected cryptocurrency position in ArrayList */
                    converterCryptoSelToET.setText(visibleCryptoList.get(selectedToCryptoPos).getSymbolCrypto());
                    converterCryptoSelToHiddenTV.setText(visibleCryptoList.get(selectedToCryptoPos).getIdCrypto());
                }
            }
        });
    }

    /**
     * Prepares SearchView which is located in cryptocurrency converter fragment for interactivity.
     */
    private void prepConverterCryptoSv(){
        SearchView actualValueCryptoSearchSv = createdLayoutView.findViewById(R.id.converter_crypto_search_sv_id); /* get reference to cryptocurrency search bar */
        actualValueCryptoSearchSv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final ArrayList<LogicCryptoList.CryptoSupport> tempCryptoList = new ArrayList<>();

                for(int i = 0; i < cryptoList.size(); i++){ /* go through names and crypto symbols and try to find match */
                    if(cryptoNames.get(i).toLowerCase().contains(newText.toLowerCase()) || cryptoSymbols.get(i).toLowerCase().contains(newText.toLowerCase())){
                        tempCryptoList.add(cryptoList.get(i)); /* match found */
                    }
                }

                if(!tempCryptoList.isEmpty()){ /* if any match found, filter results */
                    final ArrayAdapter tempCryptoListAdapter = new ArrayAdapter(createdLayoutView.getContext(), android.R.layout.simple_list_item_2, android.R.id.text1, tempCryptoList) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView text1 = view.findViewById(android.R.id.text1);
                            TextView text2 = view.findViewById(android.R.id.text2);

                            text1.setText(tempCryptoList.get(position).getNameCrypto());
                            text2.setText(tempCryptoList.get(position).getSymbolCrypto());
                            return view;
                        }
                    };

                    converterCryptoLV.setAdapter(tempCryptoListAdapter);
                    visibleCryptoList = tempCryptoList;
                }else{ /* no match found */
                    if(converterCryptoLV.getAdapter().getCount() != cryptoList.size()){ /* if filtering was applied, then cancel it */
                        ArrayAdapter cryptoListAdapter = new ArrayAdapter(createdLayoutView.getContext(), android.R.layout.simple_list_item_2, android.R.id.text1, cryptoList) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView text1 = view.findViewById(android.R.id.text1);
                                TextView text2 = view.findViewById(android.R.id.text2);

                                text1.setText(cryptoList.get(position).getNameCrypto());
                                text2.setText(cryptoList.get(position).getSymbolCrypto());
                                return view;
                            }
                        };

                        converterCryptoLV.setAdapter(cryptoListAdapter);
                        visibleCryptoList = cryptoList;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Used for returning cryptocurrency list from LogicCryptoList.
     * @param cryptoList ArrayList with cryptocurrency information
     */
    public void setCryptoList(ArrayList<LogicCryptoList.CryptoSupport> cryptoList){
        this.cryptoList = cryptoList;
        getNameSymId();
        showRetrievedInfo(createdLayoutView);
    }

    /**
     * Shows information regarding available cryptocurrencies in respective ListView object.
     */
    private void showRetrievedInfo(View view){
        final ArrayAdapter cryptoListAdapter = new ArrayAdapter(view.getContext(), android.R.layout.simple_list_item_2, android.R.id.text1, cryptoList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(cryptoList.get(position).getNameCrypto());
                text2.setText(cryptoList.get(position).getSymbolCrypto());
                return view;
            }
        };

        converterCryptoLV.setAdapter(cryptoListAdapter);
        visibleCryptoList = cryptoList;
    }

    /**
     * Fills ArrayLists which should contain cryptocurrency ids, symbols and names.
     */
    private void getNameSymId(){
        cryptoNames = new ArrayList<>();
        cryptoSymbols = new ArrayList<>();
        cryptoIds = new ArrayList<>();

        for(int i = 0; i < cryptoList.size(); i++){ /* go through cryptocurrency list items (each contains id, name and symbol) */
            LogicCryptoList.CryptoSupport oneCryptoItem = cryptoList.get(i);
            String cryptoId = oneCryptoItem.getIdCrypto();
            String cryptoSymbol = oneCryptoItem.getSymbolCrypto();
            String cryptoName = oneCryptoItem.getNameCrypto();

            cryptoNames.add(cryptoName);
            cryptoSymbols.add(cryptoSymbol);
            cryptoIds.add(cryptoId);
        }
    }
}
