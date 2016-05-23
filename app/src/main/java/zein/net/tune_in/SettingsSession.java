package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;

/**
 * Created by Zein's on 3/18/2016.
 */
public class SettingsSession extends Activity implements View.OnClickListener {

    private Button btnLink, btnSpotifyLink;
    private ProgressBar pbSpotifyLoading;
    private AlertDialog.Builder chooseDialog;
    private Button btnBackupPlayList;
    private TextView currentPlayList;
    private Switch swChooseRandomly;
    ArrayList<Playlist> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        initView();
        initManager();
        playlists = new ArrayList<>();
    }

    private void initView() {
        btnSpotifyLink = (Button) findViewById(R.id.btnSpotifyLink);
        btnSpotifyLink.setOnClickListener(this);
        btnBackupPlayList = (Button) findViewById(R.id.btnBackupPlaylist);
        btnBackupPlayList.setOnClickListener(this);
        swChooseRandomly = (Switch) findViewById(R.id.swChooseRandomly);
        swChooseRandomly.setChecked(manager.mediaManager.isChoosingRandomlyFromBp());
        swChooseRandomly.setOnClickListener(this);
        currentPlayList = (TextView) findViewById(R.id.txtCurrentPlaylist);
        if(manager.mediaManager.getBackupPlaylist() != null)
            currentPlayList.setText("Current Playlist: " + manager.mediaManager.getBackupPlaylist().getPlaylistName());
        pbSpotifyLoading = (ProgressBar) findViewById(R.id.pbLoadingSpotify);
    }

    private void initManager() {
        manager.currentActivity = this;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("TUNEIN", String.valueOf(requestCode));
        // Check if result comes from the correct activity
        if (requestCode == MediaManager.REQUEST_CODE) {
            Log.d("TUNEIN", "GOODIN");
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.d("TUNEIN", "Best");
                Config playerConfig = new Config(this, response.getAccessToken(), MediaManager.SPOTIFY_CLIENT_ID);
                Log.d("TUNEIN", response.getAccessToken());
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        manager.spotifyToken = response.getAccessToken();
                        manager.mediaManager.setSpotifyPlayer(player);

                        Log.d("TUNEIN", "Connected");
                        manager.isLinkedWithSpotify = true;
                        pbSpotifyLoading.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                        error("Unexpected error occured");
                    }
                });
            }
        }
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


    @Override
    public void onClick(View v) {
        if (btnBackupPlayList.getId() == v.getId())
            showPlaylistSearchDialog();

        if (btnSpotifyLink.getId() == v.getId()) {
            if (!manager.isLinkedWithSpotify) {
                pbSpotifyLoading.setVisibility(View.VISIBLE);
                AuthenticationRequest.Builder builder =
                        new AuthenticationRequest.Builder(MediaManager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, MediaManager.REDIRECT_URI);
                builder.setScopes(new String[]{"user-library-read", "streaming"});
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginActivity(this, MediaManager.REQUEST_CODE, request);
            }
        }
        if(swChooseRandomly.getId() == v.getId()){
            manager.mediaManager.setChoosingRandomlyFromBp(!manager.mediaManager.isChoosingRandomlyFromBp());
            swChooseRandomly.setChecked(manager.mediaManager.isChoosingRandomlyFromBp());
        }
    }


    private AlertDialog.Builder showPlaylistSearchDialog() {
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Spotify playlist: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) && search.getText().toString().length() != 0;
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

    private void searchPlaylist(final String search) {
        pbSpotifyLoading.setVisibility(View.VISIBLE);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchPlaylists(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("playlists");
                    JSONArray jArray = trackJson.getJSONArray("items");

                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject js = jArray.getJSONObject(i);

                        Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                        Playlist p = new Playlist(js);

                        playlists.add(p);
                    }
                    Manager.manager.isDisplayingSpotifyLikes = false;
                    manager.currentSpotifyOffset = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            choosePlayList();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        };
        thread.start();
    }

    private void choosePlayList(){
        pbSpotifyLoading.setVisibility(View.INVISIBLE);
        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose the backup playlist")
                .setAdapter(new PlaylistAdapter(playlists), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            manager.mediaManager.setBackupPlaylist(playlists.get(which));
                            currentPlayList.setText("Current Playlist: " + playlists.get(which).getPlaylistName());
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

    private class PlaylistAdapter extends ArrayAdapter<Playlist> {
        public PlaylistAdapter(ArrayList<Playlist> playlists) {
            super(SettingsSession.this, R.layout.playlist_view, playlists);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            Log.d("TUNEIN", "This was called here!!!");
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.playlist_view,
                        parent, false);

            Playlist currentPlaylist = playlists.get(position);
            Log.d("TUNEIN", "We also got here");
            ImageView avatar = (ImageView) itemView.findViewById(R.id.imgArtwork);
            avatar.setImageBitmap(currentPlaylist.getPlaylistArtwork());
            avatar.getLayoutParams().width = 250;
            avatar.getLayoutParams().height = 250;

            TextView txtName = (TextView) itemView.findViewById(R.id.txtPlaylistName);
            txtName.setText(currentPlaylist.getPlaylistName());

            TextView txtTrackcount = (TextView) itemView.findViewById(R.id.txtTrackCount);
            txtTrackcount.setText("Count: " + String.valueOf(currentPlaylist.getTrackCount()));
            return itemView;
        }
    }
}
