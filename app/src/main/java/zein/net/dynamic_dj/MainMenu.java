package zein.net.dynamic_dj;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ikovac.timepickerwithseconds.MyTimePickerDialog;
import com.ikovac.timepickerwithseconds.TimePicker;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static zein.net.dynamic_dj.Manager.manager;

public class MainMenu extends Activity {

    private Button btnHost, btnJoin;
    private EditText txtKey, txtName;
    private TextView txtLoadMessage;
    private ProgressBar pbLoading;
    private String sessionName, userName;
    private Switch swFilter;
    private AlertDialog.Builder serverDialog;
    private ServerAdapter serverAdapter;
    private ArrayList<JSONObject> serversArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activit_main_menu);

        initManager();
        initView();
        try {
            String reason = getIntent().getExtras().getString("reason");
            if (reason != null) {
                if (reason.equals("host"))
                    Toast.makeText(this, "The host stopped the session", Toast.LENGTH_SHORT).show();
                if (reason.equals("server"))
                    Toast.makeText(this, "The server did not respond", Toast.LENGTH_SHORT).show();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("TUNEIN", String.valueOf(requestCode));
        // Check if result comes from the correct activity
        if (requestCode == MediaManager.REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), MediaManager.SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        manager.spotifyToken = response.getAccessToken();
                        manager.mediaManager.setSpotifyPlayer(player);
                        startServer(userName, sessionName);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                        error("Error connecting to Spotify");
                    }
                });
            } else
                error("We're having trouble authenticating your Spotify account");
        }
    }


    private void initManager() {
        manager = new Manager();
    }

    private void initView() {
        btnHost = (Button) findViewById(R.id.btnHost);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        txtKey = (EditText) findViewById(R.id.etxtAddress);
        txtName = (EditText) findViewById(R.id.etxtSessionName);
        txtLoadMessage = (TextView) findViewById(R.id.txtLoadMessage);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        swFilter = (Switch) findViewById(R.id.swFilter);
        swFilter.setChecked(manager.filterExplicit);
    }

    public void onClick(View v) {
        if (v.getId() == btnHost.getId()) {
            //Make sure they typed something
            if (!isNetworkAvailable()) {
                error("You're not connected to the internet!");
                return;
            }

            final String name = txtName.getText().toString();
            if (name.length() == 0 || getOnlySpaces(name)) {
                error("Fill in the session name");
                return;
            }

            if (name.length() > 15) {
                error("The session name can be no longer than 15 characters");
                return;
            }

            if (hasInvalidText(name)) {
                error("The + symbol can not be in your session name or username");
                return;
            }

            getUsername(name, true);
        }
        if (v.getId() == btnJoin.getId()) {
            //Make sure they typed something
            if (!isNetworkAvailable()) {
                error("You're not connected to the internet!");
                return;
            }

            final String name = txtKey.getText().toString();

            if (name.length() == 0) {
                Thread serverThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            setLoadMessage("Getting sessions on your network");
                            JSONArray servers = new JSONArray(manager.getServersOnNetwork());
                            if (servers.length() == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        error("Could not find any servers on your network");
                                        cancelLoading();
                                    }
                                });
                                return;
                            }
                            serversArray = new ArrayList<>();
                            for (int i = 0; i < servers.length(); i++){
                                JSONObject server = servers.getJSONObject(i);
                                if(server.getBoolean("stoped"))
                                    continue;
                                serversArray.add(servers.getJSONObject(i));
                            }


                            serverAdapter = new ServerAdapter(serversArray);

                            chooseServer();

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    error("Could not find any servers on your network");
                                    cancelLoading();
                                }
                            });
                        }
                    }
                };
                serverThread.start();
                return;
            }

            if (name.length() != 5 || getOnlySpaces(name)) {
                error("They key must be 5 characters");
                return;
            }

            if (hasInvalidText(name)) {
                error("The + symbol can not be in your session name or username");
                return;
            }

            getUsername(name, false);
        }
        if (v == swFilter) {
            if (swFilter.isChecked())
                showFilterDialog();
            else
                manager.filterExplicit = false;
        }

    }

    private void chooseServer(){
        cancelLoading();
        serverDialog = new AlertDialog.Builder(this).setTitle("Choose a server").setAdapter(serverAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try{
                    getUsername(serversArray.get(which).getString("serverKey"), false);
                } catch (Exception e){
                    error("An error occurred when trying to join a server");
                    e.printStackTrace();
                }
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serverDialog.show();
            }
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder filterDialog = new AlertDialog.Builder(this).setTitle("Filter").setPositiveButton("Okay", null);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.filter_view, null);
        filterDialog.setView(dialogView);

        final CheckBox cbFilterExplicit = (CheckBox) dialogView.findViewById(R.id.cbFilterExplicit);
        cbFilterExplicit.setChecked(manager.filterExplicit);
        cbFilterExplicit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.filterExplicit = !manager.filterExplicit;
                cbFilterExplicit.setChecked(manager.filterExplicit);
            }
        });

        final CheckBox cbMinTrackTime = (CheckBox) dialogView.findViewById(R.id.cbMinTrackTime);
        cbMinTrackTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyTimePickerDialog mTimePickter = new MyTimePickerDialog(MainMenu.this, new MyTimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute, int seconds) {

                    }
                }, 0, 0, 0, false);
                mTimePickter.show();
            }
        });

        filterDialog.show();
    }

    private void getUsername(final String name, final boolean isHosting) {
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Username:");
        //Set up the text search
        final EditText search = new EditText(this);
        search.setHint("username");
        search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);

        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                joinSession(name, search.getText().toString(), isHosting);
                dialog.cancel();
            }
        });
        nameDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });


        final AlertDialog ad = nameDialog.show();

        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    joinSession(name, search.getText().toString(), isHosting);
                    ad.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    private void joinSession(String name, String search, final boolean isHosting) {
        try {
            if (search.length() == 0 || getOnlySpaces(search)) {
                error("Fill in the username name");
                return;
            }

            if (hasInvalidText(search)) {
                error("The + symbol can not be in your session name, key, or username");
                return;
            }

            if (manager.mediaManager.getSpotifyPlayer() == null) {
                if (isHosting) {
                    requestSpotify();
                    userName = search;
                    sessionName = name;
                    return;
                }
            }

            if (isHosting)
                startServer(search, name);
            else
                joinServer(search, name.toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
            error("There was an error");
        }
    }

    private void requestSpotify() {
        if (manager.mediaManager != null && manager.mediaManager.getSpotifyPlayer() != null && !manager.mediaManager.getSpotifyPlayer().isShutdown()) {
            startServer(userName, sessionName);
            return;
        }
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(MediaManager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, MediaManager.REDIRECT_URI);
        builder.setScopes(Manager.SPOTIFY_SCOPES);
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, MediaManager.REQUEST_CODE, request);
    }

    private AlertDialog.Builder error(final String errorMessage) {
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

    private void startServer(final String username, final String sessionName) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    manager.sessionName = sessionName;
                    manager.currentUser = new User(username);
                    setLoadMessage("Attempting to host server");
                    manager.hostServer(manager.sessionName);
                    manager.isServer = true;
                    setLoadMessage("Setting up server");
                    manager.sendUser(manager.getHostKey(), manager.currentUser, manager.isServer);
                    manager.sendFilterExplicit(manager.getHostKey());
                    loadTuneIn();
                } catch (Exception e) {
                    e.printStackTrace();
                    error("There was an error trying to host a server");
                }
            }
        };
        thread.start();
    }

    private void joinServer(final String username, final String sessionKey) {
        Thread thread = new Thread() {
            public void run() {
                setLoadMessage("Checking if server exists");
                if (!manager.checkServerExists(sessionKey)) {
                    error("Can not find a server with the key: " + sessionKey);
                    cancelLoading();
                    return;
                }
                manager.currentUser = new User(username);
                manager.isServer = false;
                manager.setServerKey(sessionKey);
                setLoadMessage("Joining the server");
                String code = manager.sendUser(manager.getHostKey(), manager.currentUser, manager.isServer);
                if (code.startsWith("gg")) {
                    manager.currentIteration = Integer.parseInt(code.split("&ITE=")[1].split("&SES=")[0]);
                    manager.sessionName = code.split("&SES=")[1];
                    loadTuneIn();
                } else {
                    error((code.equals("ht")) ? "This username has already been taken" : "there was an error adding the user");
                    cancelLoading();
                }
            }
        };
        thread.start();
    }

    private void setLoadMessage(final String message) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if (pbLoading.getVisibility() == View.INVISIBLE)
                    pbLoading.setVisibility(View.VISIBLE);
                txtLoadMessage.setText(message);
            }
        });
    }

    private void cancelLoading() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                pbLoading.setVisibility(View.INVISIBLE);
                txtLoadMessage.setText("");
            }
        });
    }

    private void loadTuneIn() {
        startActivity(new Intent(this, DynamicDjSession.class));
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private boolean getOnlySpaces(String text) {
        boolean onlySpaces = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ') {
                onlySpaces = false;
                break;
            }
        }

        return onlySpaces;
    }


    private boolean hasInvalidText(String text) {
        boolean error = false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '+') {
                error = true;
                break;
            }
        }

        return error;
    }

    private class ServerAdapter extends ArrayAdapter<JSONObject> {
        private ServerAdapter(ArrayList<JSONObject> servers) {
            super(MainMenu.this, R.layout.server_view, servers);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.server_view,
                        parent, false);
            try{
                JSONObject currentServer = serversArray.get(position);

                TextView txtSessionName = (TextView) itemView.findViewById(R.id.txtSessionName);
                String sessionName = getString(R.string.session_name) + " " + currentServer.getString("serverName");
                txtSessionName.setText(sessionName);

                TextView txtHostName = (TextView) itemView.findViewById(R.id.txtHostName);
                String hostName = getString(R.string.host) + " " + getServerHostName(currentServer.getJSONArray("users"));
                txtHostName.setText(hostName);

                TextView txtUsers = (TextView) itemView.findViewById(R.id.txtUsers);
                String users = getString(R.string.users) + " " + "In progress";
                txtUsers.setText(users);

                TextView txtKey = (TextView) itemView.findViewById(R.id.txtKey);
                String serverKey = getString(R.string.key) + " " +  currentServer.getString("serverKey");
                txtKey.setText(serverKey);

                TextView txtExplicit = (TextView) itemView.findViewById(R.id.txtExplicit);
                String explicit = getString(R.string.explicitc) + currentServer.getString("filterExplicit");
                txtExplicit.setText(explicit);

            } catch (Exception e){
                e.printStackTrace();
            }

            return itemView;
        }
    }

    private String getServerHostName(JSONArray users) throws Exception{
        for(int i = 0; i < users.length(); i++)
            if(users.getJSONObject(i).getBoolean("isHosting"))
                return users.getJSONObject(i).getString("userName");

        return "undefined";
    }

}

