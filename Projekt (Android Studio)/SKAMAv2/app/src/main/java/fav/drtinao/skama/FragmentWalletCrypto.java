package fav.drtinao.skama;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.skamav2.R;

/**
 * Displays part of the application in which can user get information about the actual amount stored in wallet (bitcoin + litecoin supported)
 */
public class FragmentWalletCrypto extends Fragment {
    /* variables regarding to UI elements - START */
    private Button walletCryptoCheckBtn; /* reference to wallet_crypto_check_btn_id; button which triggers wallet check when tapped */
    private Button walletCryptoFromListBtn; /* reference to wallet_crypto_from_list_btn_id; displays list with saved wallets when tapped */
    private Button walletCryptoAddAddrBtn; /* reference to wallet_crypto_add_addr_btn_id; allows user to save valid wallet address */
    private View createdLayoutView; /* reference to inflated layout, needed for referencing to elements inside */
    /* variables regarding to UI elements - END */

    private String walletAddress; /* address of the wallet which has been checked as last one */
    private String walletType; /* type of the wallet which has been checked as last one */

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEd;

    /**
     * Is called by Android OS when layout should be inflated, here wallet_crypto_fragment.xml layout is used.
     * @param inflater used to inflate layout
     * @param container parent View of the layout
     * @param savedInstanceState not used in this case
     * @return View with desired layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.wallet_crypto_menu);

        setHasOptionsMenu(true);
        View createdLayout = inflater.inflate(R.layout.wallet_crypto_fragment, container, false);
        createdLayoutView = createdLayout;

        walletCryptoCheckBtn = createdLayoutView.findViewById(R.id.wallet_crypto_check_btn_id);
        walletCryptoFromListBtn = createdLayoutView.findViewById(R.id.wallet_crypto_from_list_btn_id);
        walletCryptoAddAddrBtn = createdLayoutView.findViewById(R.id.wallet_crypto_add_addr_btn_id);

        prepCryptoTypeSpinner();
        prepWalletCryptoCheckBtn();
        prepWalletCryptoFromListBtn();
        prepWalletCryptoAddAddrBtn();

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
        checkSavedWallet();
    }

    /**
     * If some additional information was given to this fragment (using Bundle), then retrieve information from the Bundle.
     * Typically additional information is provided when user selected one of the saved wallets in other fragment and now is expecting to get info regarding to selected wallet.
     */
    private void checkSavedWallet(){
        Bundle givenDataBundle = getArguments();
        if(givenDataBundle != null){ /* if Bundle not null, then probably selected saved wallet from list -> load data regarding to selected wallet */
            String walletName = givenDataBundle.getString(getResources().getString(R.string.wallet_crypto_sharedpref_name));
            String walletType = givenDataBundle.getString(getResources().getString(R.string.wallet_crypto_sharedpref_type));
            String walletAddress = givenDataBundle.getString(getResources().getString(R.string.wallet_crypto_sharedpref_address));

            LogicWalletCrypto logicWalletCrypto; /* contains logic for retrieving wallet data - instance will depend on values retrieved from Bundle */
            String[] walletList = getResources().getStringArray(R.array.wallet_crypto_support_list);

            /* create instance of LogicWalletCrypto (depends on values given by user) */
            if(walletType.equals(walletList[0])){ /* Bitcoin (BTC) */
                logicWalletCrypto = new LogicWalletCrypto(walletAddress, LogicWalletCrypto.walletType.BITCOIN, createdLayoutView.getContext());
                logicWalletCrypto.execute();
            }else if(walletType.equals(walletList[1])){ /* Litecoin (LTC) */
                logicWalletCrypto = new LogicWalletCrypto(walletAddress, LogicWalletCrypto.walletType.LITECOIN, createdLayoutView.getContext());
                logicWalletCrypto.execute();
            }

            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_list_selected_display_start) + walletName + getResources().getString(R.string.wallet_list_selected_display_end), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        sharedPref = getActivity().getSharedPreferences(getResources().getString(R.string.sharedpref_name), 0);
        sharedPrefEd = sharedPref.edit(); /* modify preferences */

        addDemoWallets();
    }

    /**
     * Takes care of adding demo wallets for testing purposes (one for btc and one for ltc). Wallets are added just on first run.
     */
    private void addDemoWallets(){
        /* check if first run */
        boolean firstRun = sharedPref.getBoolean(getResources().getString(R.string.sharedpref_first_run_flag), true);
        if(!firstRun){
            return;
        }

        sharedPrefEd.putBoolean(getResources().getString(R.string.sharedpref_first_run_flag), false);

        final String itemSeparator = getResources().getString(R.string.sharedpref_item_separator); /* individual entries are separated by ";;;" */
        final String locationWalletSharedPref = getResources().getString(R.string.sharedpref_crypto_wallets_list); /* location of wallet data in Shared Preferences */

        /* load previously saved wallets from SharedPreferences */
        String savedWalletsSharedPref = sharedPref.getString(locationWalletSharedPref, ""); /* wallets are presented in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added" */

        /* build String which will represent demo btc wallet */
        String demoBtcName = getResources().getString(R.string.wallet_crypto_sharedpref_demo_btc_name);
        String demoBtcType = getResources().getString(R.string.wallet_crypto_sharedpref_demo_btc_type);
        String demoBtcAddress = getResources().getString(R.string.wallet_crypto_sharedpref_demo_btc_address);
        long currentTime = System.currentTimeMillis(); /* get current time - unix timestamp */
        String demoBtcWallet = itemSeparator + demoBtcName + itemSeparator + demoBtcType + itemSeparator + demoBtcAddress + itemSeparator + currentTime;
        savedWalletsSharedPref += demoBtcWallet;

        String demoBtcName2 = getResources().getString(R.string.wallet_crypto_sharedpref_demo_btc_name2);
        String demoBtcAddress2 = getResources().getString(R.string.wallet_crypto_sharedpref_demo_btc_address2);
        String demoBtcWallet2 = itemSeparator + demoBtcName2 + itemSeparator + demoBtcType + itemSeparator + demoBtcAddress2 + itemSeparator + currentTime;
        savedWalletsSharedPref += demoBtcWallet2;

        /* build String which will represent demo ltc wallet */
        String demoLtcName = getResources().getString(R.string.wallet_crypto_sharedpref_demo_ltc_name);
        String demoLtcType = getResources().getString(R.string.wallet_crypto_sharedpref_demo_ltc_type);
        String demoLtcAddress = getResources().getString(R.string.wallet_crypto_sharedpref_demo_ltc_address);
        String demoLtcWallet = itemSeparator + demoLtcName + itemSeparator + demoLtcType + itemSeparator + demoLtcAddress + itemSeparator + currentTime;
        savedWalletsSharedPref += demoLtcWallet;

        String demoLtcName2 = getResources().getString(R.string.wallet_crypto_sharedpref_demo_ltc_name2);
        String demoLtcAddress2 = getResources().getString(R.string.wallet_crypto_sharedpref_demo_ltc_address2);
        String demoLtcWallet2 = itemSeparator + demoLtcName2 + itemSeparator + demoLtcType + itemSeparator + demoLtcAddress2 + itemSeparator + currentTime;
        savedWalletsSharedPref += demoLtcWallet2;

        sharedPrefEd.putString(locationWalletSharedPref, savedWalletsSharedPref);
        sharedPrefEd.commit();
    }

    /**
     * Prepares button with id "wallet_crypto_check_btn_id" for interactivity. Gets information about content of wallet with specific address (given by user - TextEdit object) when tapped.
     */
    private void prepWalletCryptoCheckBtn(){
        walletCryptoCheckBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Spinner cryptoTypeSpinner = createdLayoutView.findViewById(R.id.wallet_crypto_type_spinner_id); /* get reference to Spinner with supported wallets (user chooses wallet type) */
                EditText cryptoAddrEt = createdLayoutView.findViewById(R.id.wallet_crypto_addr_et_id); /* reference to EditText with address of the wallet (given by user) */
                if(cryptoAddrEt.getText().toString().isEmpty()){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(createdLayoutView.getContext());
                    alertBuilder.setTitle(getResources().getString(R.string.wallet_crypto_alert_title));
                    alertBuilder.setMessage(getResources().getString(R.string.wallet_crypto_alert_address_not_pres_mes));
                    alertBuilder.setPositiveButton(getResources().getString(R.string.wallet_crypto_alert_ok),null);
                    alertBuilder.show();
                    return;
                }

                LogicWalletCrypto logicWalletCrypto; /* contains logic for retrieving wallet data - instance will depend on user given values */

                walletAddress = cryptoAddrEt.getText().toString(); /* address of the wallet (given by user) */
                walletType = cryptoTypeSpinner.getSelectedItem().toString(); /* type of the wallet (by user; could be bitcoin, litecoin and so on) */

                String[] walletList = getResources().getStringArray(R.array.wallet_crypto_support_list);

                if(walletType.equals(walletList[0])){ /* Bitcoin (BTC) */
                    logicWalletCrypto = new LogicWalletCrypto(walletAddress, LogicWalletCrypto.walletType.BITCOIN, createdLayoutView.getContext());
                    logicWalletCrypto.execute();
                }else if(walletType.equals(walletList[1])){ /* Litecoin (LTC) */
                    logicWalletCrypto = new LogicWalletCrypto(walletAddress, LogicWalletCrypto.walletType.LITECOIN, createdLayoutView.getContext());
                    logicWalletCrypto.execute();
                }
            }
        });
    }

    /**
     * Prepares style of Spinner with id wallet_crypto_type_spinner_id. User can choose wallet type through it.
     */
    private void prepCryptoTypeSpinner(){
        Spinner cryptoTypeSpinner = createdLayoutView.findViewById(R.id.wallet_crypto_type_spinner_id);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(createdLayoutView.getContext(), R.array.wallet_crypto_support_list, R.layout.settings_spinner_lay);
        dataAdapter.setDropDownViewResource(R.layout.settings_spinner_lay);
        cryptoTypeSpinner.setAdapter(dataAdapter);
    }

    /**
     * Prepares button with id "wallet_crypto_from_list_btn_id" for interactivity. Allows user to pick saved wallet from the list when tapped. Displays just saved wallets which have type corresponding with the type selected by user (using Spinner).
     */
    private void prepWalletCryptoFromListBtn(){
        walletCryptoFromListBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                FragmentWalletListCrypto fragmentWalletListCrypto = new FragmentWalletListCrypto();

                /* pack information regarding selected type of wallet -> then display just wallets with corresponding type */
                Bundle dataBundle = new Bundle();
                Spinner cryptoTypeSpinner = createdLayoutView.findViewById(R.id.wallet_crypto_type_spinner_id); /* get selected wallet type */

                dataBundle.putString(getResources().getString(R.string.wallet_crypto_sharedpref_selected_type), cryptoTypeSpinner.getSelectedItem().toString());
                fragmentWalletListCrypto.setArguments(dataBundle);

                ((AppCompatActivity)createdLayoutView.getContext()).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, fragmentWalletListCrypto).addToBackStack(null).commit();
            }
        });
    }


    /**
     * Prepares button with id "wallet_crypto_add_addr_btn_id" for interactivity. User can save wallet´s address through this button. This GUI part is visible only when current wallet address is not yet saved.
     */
    private void prepWalletCryptoAddAddrBtn(){
        walletCryptoAddAddrBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(walletAddress == null || walletType == null){ /* check whether wallet type and wallet address are both assigned */
                    return;
                }

                final String itemSeparator = getResources().getString(R.string.sharedpref_item_separator); /* individual entries are separated by ";;;" */
                final String locationWalletSharedPref = getResources().getString(R.string.sharedpref_crypto_wallets_list); /* location of wallet data in Shared Preferences */

                /* check if wallet already saved - if not, allow save, else display alert; check type of wallet and address */
                String selectedWalletAddr = itemSeparator + walletAddress; /* address of the last checked wallet */

                /* check if the wallet is already saved or not - START */
                String savedWalletsSharedPref = sharedPref.getString(locationWalletSharedPref, "");
                if(savedWalletsSharedPref.contains(selectedWalletAddr)){ /* wallet already saved, deny another save */
                    String[] savedWalletsSplit = savedWalletsSharedPref.split(selectedWalletAddr); /* retrieve name of the already saved wallet */
                    savedWalletsSplit = savedWalletsSplit[0].split(itemSeparator); /* before address of the wallet is type and then name of the wallet */

                    Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_already_saved_name_begin) + savedWalletsSplit[savedWalletsSplit.length - 2] + getResources().getString(R.string.wallet_already_saved_name_end), Toast.LENGTH_LONG).show();
                    return;
                }
                /* check if the wallet is already saved or not - END */

                /* display dialog and ask for name of the wallet */
                final EditText nameET = new EditText(getActivity());

                final AlertDialog alert = new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.wallet_name_title_dialog))
                        .setMessage(getResources().getString(R.string.wallet_name_message_dialog))
                        .setView(nameET)
                        .setPositiveButton(getResources().getString(R.string.wallet_name_positive_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /* do nothing => clicking of this button is handled later */
                            }
                        }).setNegativeButton(getResources().getString(R.string.wallet_name_negative_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_not_saved), Toast.LENGTH_SHORT).show();
                            }
                }).show();

                Button okBtnDialog = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                okBtnDialog.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) { /* check if length of the user given name > 0 */
                        String nameWallet = nameET.getText().toString(); /* name of the wallet entered by user */

                        /* check if length of the user given name > 0 */
                        if(nameWallet.length() == 0){
                            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_name_short_alert), Toast.LENGTH_LONG).show();
                        }else{
                            /* load previously saved wallets from SharedPreferences */
                            String savedWalletsSharedPref = sharedPref.getString(locationWalletSharedPref, ""); /* wallets are presented in format: "name_wallet;;;type_wallet;;;address_wallet;;;timestamp_added" */

                            /* build String which will represent current wallet */
                            long currentTime = System.currentTimeMillis(); /* get current time - unix timestamp */
                            String selectedWallet = itemSeparator + nameWallet + itemSeparator + walletType + itemSeparator + walletAddress + itemSeparator + currentTime;

                            /* append wallet representation to existing wallet records and save - no need to check if address already saved (button for saving is visible only when address is not yet saved) */
                            savedWalletsSharedPref += selectedWallet;
                            sharedPrefEd.putString(locationWalletSharedPref, savedWalletsSharedPref);
                            sharedPrefEd.commit();

                            alert.dismiss();

                            Toast.makeText(createdLayoutView.getContext(), getResources().getString(R.string.wallet_success_saved_begin) + nameWallet + getResources().getString(R.string.wallet_success_saved_end), Toast.LENGTH_LONG).show();
                        }
                    }
                });


                nameET.post(new Runnable(){ /* perform focus on EditText with name */
                    public void run(){
                        nameET.requestFocusFromTouch();
                        InputMethodManager inputMan = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMan.showSoftInput(nameET, 0);
                    }
                });
            }
        });
    }
}
