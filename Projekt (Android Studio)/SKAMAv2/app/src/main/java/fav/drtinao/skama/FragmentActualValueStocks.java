package fav.drtinao.skama;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.util.ArrayList;

/**
 * This class is based on Fragment class and is responsible for displaying actual values of stocks.
 */
public class FragmentActualValueStocks extends Fragment {
    private Button actualValueStocksSearchBtn; /* reference to actual_value_stocks_search_btn_id; Button which will display history fragment when tapped */
    private Button actualValueStocksWebsiteSelectBtn; /* reference to actual_value_stocks_select_from_website_btn_id; Button which displays website from which can user get cryptocurrency symbol */
    private SearchView actualValueStocksSearchSv; /* reference to actual_value_stocks_search_sv_id; SearchView which contains symbol of stock that user wants to examine */

    private ListView actualValueStocksLv; /* reference to actual_value_stocks_lv_id; ListView, which will be used for displaying famous stocks list */

    private View createdLayoutView; /* reference to inflated layout, needed for referencing elements inside */

    /**
     * Is called by Android OS when layout should be inflated, here actual_value_stocks_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.actual_value_stocks_menu);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.actual_value_stocks_fragment, container, false);
        createdLayoutView = createdLayout;

        actualValueStocksSearchBtn = createdLayout.findViewById(R.id.actual_value_stocks_search_btn_id);
        actualValueStocksSearchSv = createdLayout.findViewById(R.id.actual_value_stocks_search_sv_id);
        actualValueStocksWebsiteSelectBtn = createdLayoutView.findViewById(R.id.actual_value_stocks_select_from_website_btn_id);
        actualValueStocksLv = createdLayoutView.findViewById(R.id.actual_value_stocks_lv_id);

        prepActualValueSearchBtn();
        prepActualValueStocksWebsiteSelectBtn();
        prepActualValueStocksLv();

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
    }

    /**
     * Prepares button which takes user to history fragment for interactivity.
     */
    public void prepActualValueSearchBtn(){
        final FragmentActualValueStocks thisInstance = this;

        actualValueStocksSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(actualValueStocksSearchSv.getQuery().toString().isEmpty()){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.actual_value_stocks_fill_title));
                    alertBuilder.setMessage(getResources().getString(R.string.actual_value_stocks_fill_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.actual_value_stocks_fill_ok),null);
                    alertBuilder.show();
                    return;
                }

                LogicActualValueStocksDetail logicActualValueStocksDetail = new LogicActualValueStocksDetail(actualValueStocksSearchSv.getQuery().toString(), thisInstance, getActivity()); /* check if user given stock symbol exists before fragment switch */
                logicActualValueStocksDetail.execute();
            }
        });
    }

    /**
     * Prepares button with id actualValueStocksWebsiteSelectBtn. After tap redirects user to website, from which can user get stock symbol.
     */
    public void prepActualValueStocksWebsiteSelectBtn(){
        actualValueStocksWebsiteSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.search_pick_website_link_stocks))));
            }
        });
    }

    /**
     * Prepares ListView with actual_value_stocks_lv_id. Used for displaying famous stock symbols.
     */
    private void prepActualValueStocksLv(){
        String[] stockNames = getResources().getStringArray(R.array.actual_value_famous_list_name_stocks);
        String[] stockSymbols = getResources().getStringArray(R.array.actual_value_famous_list_sym_stocks);

        final ArrayList<String[]> stockData = new ArrayList<>();
        for(int i = 0; i < stockNames.length; i++){
            String[] oneStock = new String[2];
            oneStock[0] = stockNames[i];
            oneStock[1] = stockSymbols[i];

            stockData.add(oneStock);
        }

        final ArrayAdapter famousStockAdapter = new ArrayAdapter(createdLayoutView.getContext(), android.R.layout.simple_list_item_2, android.R.id.text1, stockData) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(stockData.get(position)[0]);
                text2.setText(stockData.get(position)[1]);
                return view;
            }
        };

        actualValueStocksLv.setAdapter(famousStockAdapter);

        actualValueStocksLv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                actualValueStocksSearchSv.setQuery(stockData.get(position)[1], false);
                actualValueStocksSearchBtn.performClick();
            }
        });
    }

    /**
     * Used for returning information about stock existence from LogicActualValueStocksDetail.
     * @param stockExists true if stock with user given symbol was found, else false
     */
    public void setStockExistence(boolean stockExists){
        if(stockExists){ /* switch fragment if user given stock exists */
            ActualValueStocksDetailFragMain actualValueStocksDetailFragMain = new ActualValueStocksDetailFragMain();
            Bundle dataBundle = new Bundle();

            dataBundle.putString(getResources().getString(R.string.actual_value_stocks_detail_sharedpref_symbol), actualValueStocksSearchSv.getQuery().toString());
            actualValueStocksDetailFragMain.setArguments(dataBundle);
            ((AppCompatActivity)createdLayoutView.getContext()).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, actualValueStocksDetailFragMain).addToBackStack(null).commit();
        }else{
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.actual_value_stocks_detail_not_exist), Toast.LENGTH_LONG).show();
        }
    }
}
