package com.apps.ing3ns.entregas.Menu;

import android.app.Activity;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.apps.ing3ns.entregas.R;
import com.google.gson.Gson;

/**
 * Created by JuanDa on 14/02/2018.
 */

public class MenuController {

    AppCompatActivity activity;
    MenuListener listener;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    public MenuController(AppCompatActivity activity) {
        this.activity = activity;
        this.listener = (MenuListener) activity;
    }

    //#########################################################################
    //------------------------- FUNCIONES TOOLBAR -----------------------------
    //#########################################################################

    public void setToolbar() {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_test);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbarWithLogo();
    }

    public void toolbarWithLogo(){
        //activity.getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        //activity.getSupportActionBar().setTitle("De One");
    }

    public void toolbarWithOutLogo(){
        activity.getSupportActionBar().setLogo(null);
    }

    public void inflaterMenuToolbar(Menu menu){
        activity.getMenuInflater().inflate(R.menu.menu, menu);
    }

    public boolean selectItemToolbar(MenuItem item){
        listener.changeOptionMenu(item.getItemId());
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return false;
        }
    }


    //#########################################################################
    //----------------------- FUNCIONES MENU LATERAL --------------------------
    //#########################################################################

    public void setNavigationDrawer(){
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        navigationView = activity.findViewById(R.id.navview);
        configColorSubTitleMenu();
        if (navigationView != null) setupDrawerContent(navigationView);
        navigationView.getMenu().findItem(R.id.menu_home).setChecked(true);

        listener.menuCreated();
    }

    public boolean closeNavigationDrawer(){
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return true;
        }
        return false;
    }

    public void setMenuLateralLogOut() {
        navigationView.getMenu().findItem(R.id.menu_cerrar_sesion).setVisible(false);
        navigationView.getMenu().findItem(R.id.menu_cambiar_pass).setVisible(false);
    }

    public void setMenuLateralLogIn() {
        navigationView.getMenu().findItem(R.id.menu_cerrar_sesion).setVisible(true);
        navigationView.getMenu().findItem(R.id.menu_cambiar_pass).setVisible(true);
    }

    public void configColorSubTitleMenu() {
        Menu menu = navigationView.getMenu();
        MenuItem tools= menu.findItem(R.id.otras_opciones);
        SpannableString s = new SpannableString(tools.getTitle());
        s.setSpan(new TextAppearanceSpan(activity, R.style.TextAppearanceSubtitleMenu), 0, s.length(), 0);
        tools.setTitle(s);
    }

    public void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectItem(menuItem);
                        return true;
                    }
                }
        );
    }

    public void selectItem(MenuItem menuItem) {
        menuItem.setChecked(true);
        String title = menuItem.getTitle().toString();
        toolbarWithOutLogo();
        listener.changeOptionMenu(menuItem.getItemId());
        drawerLayout.closeDrawers();
    }

}
