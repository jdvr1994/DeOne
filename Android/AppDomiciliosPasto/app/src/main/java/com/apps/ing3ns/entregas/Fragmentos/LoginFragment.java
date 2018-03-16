package com.apps.ing3ns.entregas.Fragmentos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

/**
 * Created by JuanDa on 14/02/2018.
 */

public class LoginFragment extends Fragment implements DomiciliarioListener {

    private SharedPreferences prefs;
    public static final String DATA = "data";
    Gson gson = new GsonBuilder().create();
    DomiciliarioController domiciliarioController;
    FragmentsListener listener;
    //---------------------Objetos UI ---------------------
    EditText etUser;
    EditText etPass;
    Button btnEntrar;
    RelativeLayout cargando;

    public LoginFragment() {

    }

    public void bindUI(View view){
        etUser = view.findViewById(R.id.editTextUser);
        etPass = view.findViewById(R.id.editTextPass);
        btnEntrar = view.findViewById(R.id.boton_entrar);
        cargando = view.findViewById(R.id.layout_progress_bar);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        bindUI(view);

        FirebaseMessaging.getInstance().subscribeToTopic(Utils.TOPIC_STATE_0);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_1);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_2);

        prefs = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        domiciliarioController = new DomiciliarioController(this);

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etUser.getText().toString();
                String pass = etPass.getText().toString();

                if(isValidEmail(email)){
                    HashMap<String,String> map = new HashMap<>();
                    map.put("email",email);
                    map.put("password",pass);
                    domiciliarioController.signInDomiciliario(map);
                    cargando.setVisibility(View.VISIBLE);
                }else{
                    makeToast("Por favor ingrese una direccion de correo valida");
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (FragmentsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement OnItemClickedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void makeToast(String text){
        Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    //######################################################################################
    //--------------------------------- Funciones Api Rest ---------------------------------
    //######################################################################################

    private boolean isValidEmail(String email){
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {

    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {
        cargando.setVisibility(View.INVISIBLE);
        UtilsPreferences.saveToken(prefs,token);
        UtilsPreferences.saveDomiciliario(prefs,gson.toJson(domiciliario));
        if(domiciliario.getState()!=Utils.DOMICILIARIO_ENTREGANDO) listener.setOnChangeToDomiciliario(Utils.KEY_LOGIN_FRAGMENT,R.id.boton_entrar);
        else listener.setOnChangeToMap(Utils.KEY_LOGIN_FRAGMENT,R.id.boton_entrar);
    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DomiciliarioController.SIGNIN)){
            Toast.makeText(getActivity(), errorMessage + " ,por favor vuelve a intentarlo", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DomiciliarioController.SIGNIN)){
            Toast.makeText(getActivity(), "Ha ocurrido un error, asegurate que tienes conexión a internet", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }
    }
}
