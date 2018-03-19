package com.apps.ing3ns.entregas.Actividades;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.view.KeyEvent;
import android.view.View;

import com.apps.ing3ns.entregas.Fragmentos.ContratoFragment;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import agency.tango.materialintroscreen.animations.IViewTranslation;

public class IntroActivity extends MaterialIntroActivity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("Preferences", Context.MODE_PRIVATE);

        enableLastSlideAlphaExitTransition(false);

        getBackButtonTranslationWrapper()
                .setEnterTranslation(new IViewTranslation() {
                    @Override
                    public void translate(View view, @FloatRange(from = 0, to = 1.0) float percentage) {
                        view.setAlpha(percentage);
                    }
                });

        addSlide(new SlideFragmentBuilder()
                        .backgroundColor(R.color.second_slide_background)
                        .buttonsColor(R.color.second_slide_buttons)
                        .neededPermissions(new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
                        .image(R.mipmap.slide2)
                        .title("Permisos")
                        .description("Para prestar un servicio de domicilios eficiente, requerimos de tu permiso de Ubicación, de esta forma el cliente podra ver como entragas su paquete en tiempo real")
                        .build(),
                new MessageButtonBehaviour(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));
                    }
                }, "Muy bien"));

        addSlide(new ContratoFragment());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.second_slide_background)
                .buttonsColor(R.color.second_slide_buttons)
                .image(R.mipmap.slide1)
                .title("Estas listo para usar De One")
                .description("El  servicio de domicilios mas eficiente")
                .build(),
            new MessageButtonBehaviour(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));
                }
            }, "De One"));
    }

    @Override
    public void onFinish() {
        super.onFinish();
        UtilsPreferences.setFirstTime(prefs);
        Intent intentMain = new Intent(IntroActivity.this, MainActivity.class);
        intentMain.putExtra("Fragment", Utils.KEY_LOGIN_FRAGMENT);
        startActivity(intentMain);
        IntroActivity.this.finish();
    }
}
