package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;
/**
 * Created by Zein's on 3/18/2016.
 */
public class SettingsSession extends Activity implements View.OnClickListener{

    private Button btnLink, btnSpotifyLink;
    private ProgressBar pbLoading, pbSpotifyLoading;
    private TextView soundcloudUser;
    private AlertDialog.Builder chooseDialog;
    private Button btnBackupPlayList;
    private TextView currentPlayList;
    private Switch swChooseRandomly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        initView();
        initManager();
        updateList();
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            if(extras.getBoolean("Link"))
                showSearchDialog();
        }
    }

    private void initView(){
        btnLink = (Button) findViewById(R.id.btnLink);
        btnLink.setOnClickListener(this);
        btnSpotifyLink = (Button) findViewById(R.id.btnSpotifyLink);
        btnSpotifyLink.setOnClickListener(this);
        btnBackupPlayList = (Button) findViewById(R.id.btnBackupPlaylist);
        btnBackupPlayList.setOnClickListener(this);
        swChooseRandomly = (Switch) findViewById(R.id.swChooseRandomly);
        currentPlayList = (TextView) findViewById(R.id.txtCurrentPlaylist);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        pbSpotifyLoading = (ProgressBar)findViewById(R.id.pbLoadingSpotify);
        soundcloudUser = (TextView) findViewById(R.id.txtSoundcloudUsername);
    }

    private void initManager(){
        manager.currentActivity = this;
    }

    private void updateList() {
        final android.os.Handler handler = new android.os.Handler();

        final Runnable r = new Runnable() {
            public void run() {
                if (!manager.isUserSearchingForUser && manager.isChoosing){
                    chooseUser();
                    manager.isChoosing = false;
                    pbLoading.setVisibility(View.INVISIBLE);
                }
                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);
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

                        /*
                        mPlayer = player;
                        mPlayer.addConnectionStateCallback(MainMenu.this);
                        mPlayer.addPlayerNotificationCallback(MainMenu.this);
                        mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
                        */
                        manager.spotifyPlayer = player;
                        Log.d("TUNEIN", "Connected");
                        manager.isLinkedWithSpotify = true;
                        pbSpotifyLoading.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                        //error("Unexpected error occured");
                    }
                });
            }
        }
    }

    private void chooseUser(){
        if(manager.currentSeachUsers.size() == 0){
            Log.d("TUNEIN", "No users");
            return;
        }
        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose your userame")
                .setAdapter(new UserListAdapter(manager.currentSeachUsers), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Log.d("TUNEIN", "Chose user with the name: " + manager.currentSeachUsers.get(which));
                            manager.currentUser.setSoundcloudUser(manager.currentSeachUsers.get(which));
                            soundcloudUser.setText("Current Username: " + manager.currentUser.getSoundcloudUser().getName());
                        } catch (Exception e) {
                            Log.d("TUNEIN","There was an error choosing");
                            e.printStackTrace();
                        }
                    }
                });

        try{
            chooseDialog.show();
        } catch (WindowManager.BadTokenException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        if(btnLink.getId() == v.getId()){
            showSearchDialog();
        }
        if(btnBackupPlayList.getId() == v.getId()){
            showPlaylistSearchDialog();
        }
        if(btnSpotifyLink.getId()== v.getId()){
            if(!manager.isLinkedWithSpotify){
                pbSpotifyLoading.setVisibility(View.VISIBLE);
                AuthenticationRequest.Builder builder =
                        new AuthenticationRequest.Builder(manager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, manager.REDIRECT_URI);
                builder.setScopes(new String[]{"user-library-read", "streaming"});
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginActivity(this, manager.REQUEST_CODE, request);
            }
        }
    }

    private AlertDialog.Builder showSearchDialog(){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for your soundcloud: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    searchUser(search.getText().toString());
                    return true;
                }
                return false;
            }
        });
        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchUser(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        nameDialog.show();

        return nameDialog;
    }

    private AlertDialog.Builder showPlaylistSearchDialog(){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Soundcloud playlist: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    if(search.getText().toString().length() == 0)
                        return false; //TODO: Do Somehting when the user doesnt input anything

                    return true;
                }
                return false;
            }
        });
        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchPlaylist(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        nameDialog.show();

        return nameDialog;
    }

    private void searchUser(String search){
        Intent userSearch = new Intent(this, UserSearch.class);
        userSearch.setData(Uri.parse(search));
        this.startService(userSearch);
        manager.isUserSearchingForUser = true;
        pbLoading.setVisibility(View.VISIBLE);
    }

    private void searchPlaylist(String search){
        Intent playlistSearch = new Intent(this, PlaylistSearch.class);
        playlistSearch.setData(Uri.parse(search));
        this.startService(playlistSearch);
        manager.isUserSearchingForPlaylist = true;
        pbLoading.setVisibility(View.VISIBLE);
    }

    private class UserListAdapter extends ArrayAdapter<ScUser> {
        public UserListAdapter(ArrayList<ScUser> users){
            super(SettingsSession.this, R.layout.user_view, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.user_view,
                        parent, false);
            }

            ScUser currentUser = manager.currentSeachUsers.get(position);

            ImageView avatar = (ImageView) itemView.findViewById(R.id.imgProfile);
            avatar.setImageBitmap(currentUser.getUserBitMap());
            avatar.getLayoutParams().width = 250;
            avatar.getLayoutParams().height = 250;

            TextView txtName = (TextView) itemView.findViewById(R.id.txtUserName);
            txtName.setText(currentUser.getName());
            return itemView;
        }
    }
}
