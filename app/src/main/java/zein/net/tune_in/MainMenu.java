package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static zein.net.tune_in.Manager.manager;
/**
 * Created by Zein's on 2/2/2016.
 */
public class MainMenu extends Activity{


    private Button btnHost, btnJoin;
    private EditText txtKey, txtName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activit_main_menu);
        initManager();
        initView();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
    }

    private void initManager(){
        manager = new Manager();
        manager.currentActivity = this;
    }

    private void initView(){
        btnHost = (Button) findViewById(R.id.btnHost);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        txtKey = (EditText) findViewById(R.id.etxtAddress);
        txtName = (EditText) findViewById(R.id.etxtSessionName);
    }

    public void onClick(View v) {
        if(v.getId() == btnHost.getId()){
            //Make sure they typed something
            if(!isNetworkAvailable()){
                error("You're not connected to the internet!");
                return;
            }

            /* TODO: Work on this
            if(!manager.isServerOnline()){
                error("Could not find the server!");
                return;
            }
            */
           final String name = txtName.getText().toString();
            if(name.length() == 0 || getOnlySpaces(name)){
                error("Fill in the session name");
                return;
            }

            if(hasInvalidText(name)){
                error("The + symbol can not be in your session name or username");
                return;
            }

            getUsername(name, true);

        }
        if(v.getId() == btnJoin.getId()){
            //Make sure they typed something
            if(!isNetworkAvailable()){
                error("You're not connected to the internet!");
                return;
            }

            final String name = txtKey.getText().toString();
            if(name.length() == 0 || getOnlySpaces(name)){
                error("Fill in the session name");
                return;
            }

            if(hasInvalidText(name)){
                error("The + symbol can not be in your session name or username");
                return;
            }

            getUsername(name, false);
        }
    }

    private void getUsername(final String name, final boolean isHosting){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Input a username");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    try{
                        if(search.getText().toString().length() == 0 || getOnlySpaces(search.getText().toString())){
                            error("Fill in the username name");
                            return false;
                        }

                        if(hasInvalidText(search.getText().toString())){
                            error("The + symbol can not be in your session name, key, or username");
                            return false;
                        }

                        if(isHosting)
                            startServer(search.getText().toString(), name);
                        else
                            joinServer(search.getText().toString(), name.toUpperCase());

                    } catch (Exception e){
                        e.printStackTrace();
                        error("There was an error");
                    }

                    return true;
                }
                return false;
            }
        });
        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try{
                    if(search.getText().toString().length() == 0 || getOnlySpaces(search.getText().toString())){
                        error("Fill in the username name");
                        return;
                    }

                    if(hasInvalidText(search.getText().toString())){
                        error("The + symbol can not be in your session name, key, or username");
                        return;
                    }

                    if(isHosting)
                        startServer(search.getText().toString(), name);
                    else
                        joinServer(search.getText().toString(), name.toUpperCase());
                } catch (Exception e){
                    e.printStackTrace();
                    error("There was an error");
                    dialog.cancel();
                }
            }
        });
        nameDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        nameDialog.show();
    }

    private AlertDialog.Builder error(String errorMessage){
        AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(errorMessage);
        errorDialog.setPositiveButton("OK", null);
        errorDialog.setNegativeButton("Cancel", null);
        errorDialog.show();
        return errorDialog;
    }

    private void startServer(final String username, final String sessionName){
        // Get a handler that can be used to post to the main thread
        android.os.Handler mainHandler = new android.os.Handler(this.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                manager.sessionName = sessionName;
                manager.currentUser = new User(username);
                manager.hostServer(manager.sessionName);
                manager.isServer = true;
                manager.sendUser(manager.getHostKey(), manager.currentUser);
            } // This is your code
        };
        mainHandler.post(myRunnable);

        this.startActivity(new Intent(this, TuneInSession.class));
        finish();
    }

    private void joinServer(final String username,final String sessionKey){
        if(!manager.checkServerExists(sessionKey)){
            error("Can not find a server with the key: " + sessionKey);
            return;
        }

        // Get a handler that can be used to post to the main thread
        android.os.Handler mainHandler = new android.os.Handler(this.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                manager.currentUser = new User(username);
                manager.isServer = false;
                manager.setServerKey(sessionKey);
                manager.sendUser(manager.getHostKey(), manager.currentUser);
            } // This is your code
        };
        mainHandler.post(myRunnable);

        this.startActivity(new Intent(this, TuneInSession.class));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private boolean getOnlySpaces(String text){
        boolean onlySpaces = true;
        for(int i = 0; i < text.length(); i++){
            char c = text.charAt(i);
            if(c != ' ')
                onlySpaces = false;
        }

        return onlySpaces;
    }


    private boolean hasInvalidText(String text){
        boolean error = false;
        for(int i = 0; i < text.length(); i++){
            if(text.charAt(i) == '+'){
                error = true;
                break;
            }
        }

        return error;
    }

}

