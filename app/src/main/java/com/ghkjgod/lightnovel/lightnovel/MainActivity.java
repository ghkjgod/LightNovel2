package com.ghkjgod.lightnovel.lightnovel;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.ghkjgod.lightnovel.framelayout.ConfigFragment;
import com.ghkjgod.lightnovel.framelayout.FavFragment;
import com.ghkjgod.lightnovel.framelayout.LatestFragment;
import com.ghkjgod.lightnovel.framelayout.RKListFragment;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static Boolean isExit = false; // used for exit by twice

    public enum FRAGMENT_LIST {
        RKLIST, LATEST, FAV, CONFIG
    }

    private FRAGMENT_LIST status = FRAGMENT_LIST.LATEST;

    public Fragment getCurrentFragment() {
        Fragment targerFragment = null;
        switch (status){
            case LATEST:
                targerFragment = new LatestFragment();
                break;
            case FAV:
                targerFragment = new FavFragment();
                break;
            case CONFIG:
                targerFragment = new ConfigFragment();
                break;
            case RKLIST:
                targerFragment = new RKListFragment();
                break;

        }
        return targerFragment;
    }

    public void setCurrentFragment(FRAGMENT_LIST f) {
        status = f;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // Fresco.initialize(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }
    @Override
    protected void onResume() {
        super.onResume();
       // changeFragment(getCurrentFragment());
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            exitBy2Click();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_latest, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_search) {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_latest) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_latest));
            status = FRAGMENT_LIST.LATEST;
            changeFragment(new LatestFragment());

        } else if (id == R.id.nav_rklist) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_rklist));
            changeFragment(new RKListFragment());
            status = FRAGMENT_LIST.RKLIST;



        } else if (id == R.id.nav_fav) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_fav));
            changeFragment(new FavFragment());
            status = FRAGMENT_LIST.FAV;

        } else if (id == R.id.nav_config) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_config));
            changeFragment(new ConfigFragment());
            status = FRAGMENT_LIST.CONFIG;

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void changeFragment(Fragment targetFragment) {
        // temporarily set elevation to remove rank list toolbar shadow
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, targetFragment, "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

    }

    private void exitBy2Click() {
        // press twice to exit
        Timer tExit;
        if (!isExit) {
            isExit = true; // ready to exit
            Toast.makeText(
                    this,
                    this.getResources().getString(R.string.press_twice_to_exit),
                    Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // cancel exit
                }
            }, 2000); // 2 seconds cancel exit task

        } else {
            finish();
            // call fragments and end streams and services
            System.exit(0);
        }
    }
}
