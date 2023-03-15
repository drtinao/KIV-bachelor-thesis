package fav.drtinao.skama;

import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.Toast;

import com.example.skamav2.R;

import java.util.ArrayList;

/**
 * Fragment which allows user to pick one of the saved wallets.
 */
public class FragmentWalletListCrypto extends Fragment {
    private ListView walletListCryptoLV; /* reference to wallet_list_crypto_lv; ListView which will be used for displaying saved wallets */

    private View createdLayoutView; /* reference to inflated layout, needed for referencing to elements inside */

    private ArrayList<PieceWalletData> savedWalletList; /* ArrayList with PieceWalletData; each object represents one wallet saved by user */
    private String selectedWalletType;  /* type of wallets, which user wants to be displayed */

    /**
     * Is called by Android OS when layout should be inflated, here wallet_list_crypto_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.wallet_crypto_list_title);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.wallet_list_crypto_fragment, container, false);
        createdLayoutView = createdLayout;

        acquireRefViewsInfo();

        loadSelectedWalletType();
        loadSavedWalletData();
        showSavedWalletData();

        prepWalletListCryptoLV();

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
     * Method obtains reference to ListView, which is defined in xml regarding to this fragment (wallet_list_crypto_fragment.xml). The ListView shows saved wallets.
     */
    private void acquireRefViewsInfo(){
        walletListCryptoLV = createdLayoutView.findViewById(R.id.wallet_list_crypto_lv);
    }

    /**
     * Loads information regarding user selected wallet type from Bundle object. Only wallets with corresponding type are shown to user.
     */
    private void loadSelectedWalletType(){
        selectedWalletType = null;
        Bundle givenDataBundle = getArguments();
        if(givenDataBundle != null){
            selectedWalletType = givenDataBundle.getString(getResources().getString(R.string.wallet_crypto_sharedpref_selected_type));
        }
    }

    /**
     * Loads data regarding saved cryptocurrency wallets from SharedPreferences, name "saved_wallets_list".
     * Every saved wallet is saved in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added"
     */
    private void loadSavedWalletData(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);

        String savedWalletsSharedPref = sharedPref.getString(getResources().getString(R.string.sharedpref_crypto_wallets_list), ""); /* wallets are presented in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added" */
        String[] savedWalletsSharedPrefSplit = savedWalletsSharedPref.split(getResources().getString(R.string.sharedpref_item_separator)); /* get individual items - four items for for each wallet */

        savedWalletList = new ArrayList<>();

        /* start from 1 - first is always empty */
        for(int i = 1; i < savedWalletsSharedPrefSplit.length; i += 4){
            String name = savedWalletsSharedPrefSplit[i]; /* first is wallet´s name */
            String type = savedWalletsSharedPrefSplit[i + 1]; /* second is wallet´s type */
            String address = savedWalletsSharedPrefSplit[i + 2]; /* third is wallet´s address */
            String timestamp = savedWalletsSharedPrefSplit[i + 3]; /* fourth is wallet´s timestamp */

            long timestampConverted; /* convert number from String to long => if for some reason error occurs, then use actual date & time (should not ever occur) */
            try{
                timestampConverted = Long.valueOf(timestamp);
            }catch(NumberFormatException exception){
                timestampConverted = System.currentTimeMillis();
            }

            PieceWalletData savedWallet = new PieceWalletData(name, type, address, timestampConverted);
            if(selectedWalletType != null && type.equals(selectedWalletType)){ /* check if wallet is of wanted type */
                savedWalletList.add(savedWallet);
            }
        }
    }

    /**
     * Takes care of representing data stored in ArrayList savedWalletList to user. Each object in mentioned ArrayList represents one wallet.
     */
    private void showSavedWalletData(){
        if(savedWalletList.size() == 0){ /* no wallets saved yet */
            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_list_nothing_to_display_type_start) + selectedWalletType + getResources().getString(R.string.wallet_list_display_type_end), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_list_display_type_start) + selectedWalletType + getResources().getString(R.string.wallet_list_display_type_end), Toast.LENGTH_LONG).show();
        SavedWalletAdapter savedWalletAdapter = new SavedWalletAdapter(getActivity(), savedWalletList);
        walletListCryptoLV.setAdapter(savedWalletAdapter);
        savedWalletAdapter.notifyDataSetChanged();
    }

    /**
     * Prepares ListView with saved wallets list for interactivity. When item from list is tapped, then information regarding the selected wallet is shown to user.
     */
    private void prepWalletListCryptoLV(){
        walletListCryptoLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentWalletCrypto fragmentWalletCrypto = new FragmentWalletCrypto();

                /* pack needed information - name + address + type */
                PieceWalletData selectedWallet = savedWalletList.get(position);

                Bundle dataBundle = new Bundle();
                dataBundle.putString(getResources().getString(R.string.wallet_crypto_sharedpref_tap_name), selectedWallet.getName());
                dataBundle.putString(getResources().getString(R.string.wallet_crypto_sharedpref_tap_type), selectedWallet.getType());
                dataBundle.putString(getResources().getString(R.string.wallet_crypto_sharedpref_tap_address), selectedWallet.getAddress());
                fragmentWalletCrypto.setArguments(dataBundle);

                ((AppCompatActivity)createdLayoutView.getContext()).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, fragmentWalletCrypto).addToBackStack(null).commit();
            }
        });
    }

    /**
     * Inner data class, which is used for carrying data related to one particular wallet saved by user. Each wallet entry consists of four items (name, type, address and timestamp, which tells, when was wallet added).
     */
    public static class PieceWalletData{
        private String name; /* name of the wallet - selected by user */
        private String type; /* type of the wallet - btc, ltc... */
        private String address; /* address of the wallet */
        private long timestamp; /* timestamp - when was wallet added */

        /**
         * Constructor takes care of initialization of values regarding to the wallet - name, type, address and timestamp.
         * @param name name of the wallet - selected by user
         * @param type type of the wallet - btc, ltc and so on
         * @param address address of the wallet
         * @param timestamp timestamp tells, when was wallet added
         */
        public PieceWalletData(String name, String type, String address, long timestamp){
            this.name = name;
            this.type = type;
            this.address = address;
            this.timestamp = timestamp;
        }

        /**
         * Getter for wallet name. Name is given by user.
         * @return wallet name
         */
        public String getName() {
            return name;
        }

        /**
         * Getter for wallet type. Type corresponds with values which are available for selection in Spinner with id "wallet_crypto_type_spinner_id" (located in "wallet_crypto_fragment.xml").
         * @return wallet type
         */
        public String getType() {
            return type;
        }

        /**
         * Getter for wallet address.
         * @return wallet address
         */
        public String getAddress() {
            return address;
        }

        /**
         * Getter for unix timestamp. Tells when was wallet saved by user.
         * @return unix timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}
