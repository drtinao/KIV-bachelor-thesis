package fav.drtinao.skama;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.skamav2.R;

/**
 * Main purpose of this activity is to display the navigation panel, which allows user to quickly pick an application function in which is he/she interested in.
 * All items present in navigation can be found in res/menu/menu_nav.xml (in this project)
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer_nav; /* reference to DrawerLayout - responsible for displaying navigation on left side */
    private NavigationView navigation_nav; /* reference to NavigationView - delivers content to DrawerLayout */

    /**
     * Method is typically called when Android creates the activity for the first time - usually used for performing basic setup of the application.
     * Main purpose in this application is to set up objects of DrawerLayout and NavigationView, which displays navigation panel (swipe from left
     * side of the screen to see it).
     * @param savedInstanceState data regarding to previous states of the application (not used in this case)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_nav_id);
        setSupportActionBar(toolbar);

        drawer_nav = findViewById(R.id.drawer_nav_id);
        navigation_nav = findViewById(R.id.navigation_nav_id);
        navigation_nav.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawer_nav, toolbar, R.string.drawer_nav_open, R.string.drawer_nav_close);
        drawer_nav.addDrawerListener(drawerToggle);

        final InputMethodManager inputMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        drawer_nav.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {
                inputMM.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                inputMM.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            @Override
            public void onDrawerClosed(@NonNull View view) {
                inputMM.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });

        drawerToggle.syncState();

        /* if app is opened for the first time, then display one of the Fragments (actual value of crypto in this case) */
        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentWalletCrypto()).commit();
            navigation_nav.setCheckedItem(R.id.wallet_crypto_id);
        }
    }

    /**
     * Method is called when user presses the back button (native Android function).
     * Behaviour is different, depending on situation:
     * a) navigation panel is visible -> navigation panel will be hidden
     * b) navigation panel is NOT visible -> application will be backgrounded
     */
    @Override
    public void onBackPressed() {
        if(drawer_nav.isDrawerOpen(GravityCompat.START)){
            drawer_nav.closeDrawer(GravityCompat.START);
        }else{
            super.onBackPressed();
        }
    }

    /**
     * Code inside is executed when item from navigation panel is tapped.
     * Action which is performed depends on item, which is tapped. Simply said - this method displays Fragment (part of application) which is relevant
     * to item selected from navigation panel.
     * @param menuItem MenuItem object which represents tapped item (from navigation panel)
     * @return true (= ok, item selected, action performed)
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            /* display crypto actual value Fragment */
            case R.id.actual_value_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentActualValueCrypto()).addToBackStack(null).commit();
                break;

            /* display crypto news Fragment */
            case R.id.news_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentNewsCrypto()).addToBackStack(null).commit();
                break;

            /* display crypto buy&sell history Fragment */
            case R.id.history_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentHistoryCrypto()).addToBackStack(null).commit();
                break;

            /* display crypto alerts Fragment */
            case R.id.alert_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentAlertsCrypto()).addToBackStack(null).commit();
                break;

            /* display crypto wallet Fragment */
            case R.id.wallet_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentWalletCrypto()).addToBackStack(null).commit();
                break;

            /* display crypto converter Fragment */
            case R.id.converter_crypto_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentConverterCrypto()).addToBackStack(null).commit();
                break;

            /* display stocks actual value Fragment */
            case R.id.actual_value_stocks_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentActualValueStocks()).addToBackStack(null).commit();
                break;

            /* display stocks news Fragment */
            case R.id.news_stocks_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentNewsStocks()).addToBackStack(null).commit();
                break;

            /* display stocks buy&sell history Fragment */
            case R.id.history_stocks_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentHistoryStocks()).addToBackStack(null).commit();
                break;

            /* display stocks alerts Fragment */
            case R.id.alert_stocks_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentAlertsStocks()).addToBackStack(null).commit();
                break;

            /* display settings fragment */
            case R.id.settings_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentSettingsMain()).addToBackStack(null).commit();
                break;

            /* display about fragment */
            case R.id.about_id:
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentAbout()).addToBackStack(null).commit();
                break;
        }

        /* close navigation after item selection */
        drawer_nav.closeDrawer(GravityCompat.START);
        return true;
    }
}
