package fav.drtinao.skama;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.skamav2.R;

import java.util.ArrayList;

/**
 * This class is based on Fragment class and is responsible for displaying actual values of cryptocurrencies.
 */
public class FragmentActualValueCrypto extends Fragment {
    private ListView actualValueCryptoLV; /* reference to actual_value_crypto_lv; ListView object which will display cryptocurrency list */

    private ArrayList<LogicCryptoList.CryptoSupport> cryptoList; /* list with supported crypto (contains id, name, symbol) */
    private ArrayList<String> cryptoNames; /* cryptocurrency names (for correct work of search function) */
    private ArrayList<String> cryptoSymbols; /* cryptocurrency symbols (for correct work of search function) */
    private ArrayList<String> cryptoIds; /* cryptocurrency id´s (for finding information after cryptocurrency selection) */

    private View createdLayoutView; /* reference to inflated layout, needed for referencing to elements inside */

    private ArrayList<LogicCryptoList.CryptoSupport> visibleCryptoList; /* list of currently visible cryptocurrencies (some can be ommited in comparison with cryptoList - when filtering is applied */

    /**
     * Is called by Android OS when layout should be inflated, here actual_value_crypto_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.actual_value_crypto_menu);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.actual_value_crypto_fragment, container, false);
        createdLayoutView = createdLayout;
        actualValueCryptoLV = createdLayout.findViewById(R.id.actual_value_crypto_lv);

        prepActualValueCryptoLV();
        prepActualValueCryptoSv();

        return createdLayoutView;
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
        if(cryptoList != null && cryptoList.size() > 0){
            return;
        }

        LogicCryptoList logicCryptoList = new LogicCryptoList(view.getContext(), this);
        logicCryptoList.execute();
    }

    /**
     * Prepares ListView which contains cryptocurrency data for interactivity.
     */
    private void prepActualValueCryptoLV(){
        actualValueCryptoLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActualValueCryptoDetailFragMain actualValueCryptoDetailFragMain = new ActualValueCryptoDetailFragMain();
                Bundle dataBundle = new Bundle();

                dataBundle.putString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_id), visibleCryptoList.get(position).getIdCrypto());
                dataBundle.putString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_name), visibleCryptoList.get(position).getNameCrypto());
                dataBundle.putString(getResources().getString(R.string.actual_value_crypto_detail_sharedpref_symbol), visibleCryptoList.get(position).getSymbolCrypto());
                actualValueCryptoDetailFragMain.setArguments(dataBundle);

                ((AppCompatActivity)createdLayoutView.getContext()).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, actualValueCryptoDetailFragMain).addToBackStack(null).commit();
            }
        });
    }

    /**
     * Prepares SearchView which is located in cryptocurrency actual value fragment for interactivity.
     */
    private void prepActualValueCryptoSv(){
        SearchView actualValueCryptoSearchSv = createdLayoutView.findViewById(R.id.actual_value_crypto_search_sv_id); /* get reference to cryptocurrency search bar */
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

                    actualValueCryptoLV.setAdapter(tempCryptoListAdapter);
                    visibleCryptoList = tempCryptoList;
                }else{ /* no match found */
                    if(actualValueCryptoLV.getAdapter().getCount() != cryptoList.size()){ /* if filtering was applied, then cancel it */
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

                        actualValueCryptoLV.setAdapter(cryptoListAdapter);
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

        actualValueCryptoLV.setAdapter(cryptoListAdapter);
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
