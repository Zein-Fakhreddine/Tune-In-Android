package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.audiofx.BassBoost;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import static zein.net.tune_in.Manager.manager;
/**
 * Created by Zein's on 2/2/2016.
 */
public class MainMenu extends Activity implements PlayerNotificationCallback, ConnectionStateCallback{
    
    private Button btnHost, btnJoin;
    private EditText txtKey, txtName;
    private TextView txtLoadMessage;
    private ProgressBar pbLoading;
    private ImageView imgSettings;
    private boolean isConnecting;


    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activit_main_menu);
        initManager();
        initView();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("TUNEIN", String.valueOf(requestCode));
        // Check if result comes from the correct activity
        if (requestCode == manager.REQUEST_CODE) {
            Log.d("TUNEIN", "GOODIN");
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.d("TUNEIN", "Best");
                Config playerConfig = new Config(this, response.getAccessToken(), manager.SPOTIFY_CLIENT_ID);

                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        manager.spotifyPlayer = player;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("TUNEIN", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("TUNEIN", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("TUNEIN", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("TUNEIN", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("TUNEIN", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("TUNEIN", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("TUNEIN", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
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
        txtLoadMessage = (TextView) findViewById(R.id.txtLoadMessage);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        imgSettings = (ImageView) findViewById(R.id.imgSettings);
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

        if(v.getId() == imgSettings.getId())
            this.startActivity(new Intent(this, SettingsSession.class));

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

                        if(isConnecting){
                            error("You are already connecting");
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

    private AlertDialog.Builder error(final String errorMessage){
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(errorMessage);
        errorDialog.setPositiveButton("OK", null);
        errorDialog.setNegativeButton("Cancel", null);

        this.runOnUiThread(new Runnable() {
            public void run() {
                errorDialog.show();
            }
        });

        return errorDialog;
    }

    private void startServer(final String username, final String sessionName){

        Thread thread = new Thread() {
            public void run() {
                try{
                    isConnecting = true;
                    manager.sessionName = sessionName;
                    manager.currentUser = new User(username);
                    setLoadMessage("Attempting to host server");
                    manager.hostServer(manager.sessionName);
                    manager.isServer = true;
                    setLoadMessage("Setting up server");
                    manager.sendUser(manager.getHostKey(), manager.currentUser);
                    loadTuneIn();
                } catch (Exception e){
                    e.printStackTrace();
                    isConnecting = false;
                    error("There was an error trying to host a server");
                }
            }
        };
        thread.start();
    }

    private void joinServer(final String username,final String sessionKey){

        Thread thread = new Thread() {
            public void run() {
                isConnecting = true;
                setLoadMessage("Checking if server exists");
                if(!manager.checkServerExists(sessionKey)){
                    error("Can not find a server with the key: " + sessionKey);
                    cancelLoading();
                    isConnecting = false;
                    return;
                }
                manager.currentUser = new User(username);
                manager.isServer = false;
                manager.setServerKey(sessionKey);
                setLoadMessage("Joining the server");
                String code = manager.sendUser(manager.getHostKey(), manager.currentUser);
                if(code.equals("gg"))
                    loadTuneIn();
                else{
                    error((code.equals("ht")) ? "This username has already been taken" : "there was an error added the user");
                    cancelLoading();
                    isConnecting = false;
                }
            }
        };
        thread.start();
    }

    private void setLoadMessage(final String message){
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(pbLoading.getVisibility() == View.INVISIBLE)
                    pbLoading.setVisibility(View.VISIBLE);
                txtLoadMessage.setText(message);
            }
        });
    }

    private void cancelLoading(){
        this.runOnUiThread(new Runnable() {
            public void run() {
                pbLoading.setVisibility(View.INVISIBLE);
                txtLoadMessage.setText("");
            }
        });
    }

    private void loadTuneIn(){
        startActivity(new Intent(this, TuneInSession.class));
        finish();
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
            if(c != ' '){
                onlySpaces = false;
                break;
            }
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

