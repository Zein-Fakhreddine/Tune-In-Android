package zein.net.dynamic_dj;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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

public class SettingsSession extends Activity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{
    private Button btnSpotifyLink;
    private ProgressBar pbSpotifyLoading;
    private AlertDialog.Builder chooseDialog;
    private Button btnBackupPlayList;
    private TextView currentPlayList;
    private Switch swChooseRandomly;
    private ArrayList<Playlist> playlists;
    private ArrayList<Album> albums;
    private ArrayList<Track> tracks;

    private PlaylistAdapter pA;
    private AlbumAdapter aA;
    private TrackAdapter tA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        initView();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        tracks = new ArrayList<>();
        try {
            if (getIntent().getExtras().getString("reason").equals("link"))
                btnSpotifyLink.performClick();

        } catch (NullPointerException e) {}
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
        currentPlayList.setVisibility(View.INVISIBLE);
        switch (manager.mediaManager.getCurrentBackupType()){
            case PLAYLIST:
                if(manager.mediaManager.getBackupPlaylist() != null){
                    currentPlayList.setText("Current Playlist: " + manager.mediaManager.getBackupPlaylist().getPlaylistName());
                    currentPlayList.setVisibility(View.VISIBLE);
                }
                break;
            case ALBUM:
                if(manager.mediaManager.getBackupAlbum() != null){
                    currentPlayList.setText("Current Album: " + manager.mediaManager.getBackupAlbum().getAlbumName());
                    currentPlayList.setVisibility(View.VISIBLE);
                }
                break;
            case TRACK:
                if(manager.mediaManager.getBackupTrack() != null){
                    currentPlayList.setText("Current Track: " + manager.mediaManager.getBackupTrack().getTrackTitle());
                    currentPlayList.setVisibility(View.VISIBLE);
                }
            case UNDEFINED:
                break;
        }
        pbSpotifyLoading = (ProgressBar) findViewById(R.id.pbLoadingSpotify);
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
                Log.d("TUNEIN", response.getAccessToken());
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        manager.spotifyToken = response.getAccessToken();
                        manager.mediaManager.setSpotifyPlayer(player);

                        manager.isLinkedWithSpotify = true;
                        pbSpotifyLoading.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        pbSpotifyLoading.setVisibility(View.INVISIBLE);
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

    public void onClick(View v) {
        if (btnBackupPlayList.getId() == v.getId())
            showChooseBackupPopup(v);

        if (btnSpotifyLink.getId() == v.getId()) {
            if (!manager.isLinkedWithSpotify) {
                pbSpotifyLoading.setVisibility(View.VISIBLE);
                AuthenticationRequest.Builder builder =
                        new AuthenticationRequest.Builder(MediaManager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, MediaManager.REDIRECT_URI);
                builder.setScopes(Manager.SPOTIFY_SCOPES);
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginActivity(this, MediaManager.REQUEST_CODE, request);
            }
        }
        if (swChooseRandomly.getId() == v.getId()) {
            manager.mediaManager.setChoosingRandomlyFromBp(!manager.mediaManager.isChoosingRandomlyFromBp());
            swChooseRandomly.setChecked(manager.mediaManager.isChoosingRandomlyFromBp());
        }
    }

    private void showChooseBackupPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.backupmenu, popup.getMenu());
        popup.show();
    }

    private AlertDialog.Builder showTrackSearchDialog(){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Spotify track: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setHint(R.string.track);
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
                searchTrack(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        final AlertDialog ad = nameDialog.show();

        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    searchTrack(search.getText().toString());
                    ad.dismiss();
                    return true;
                }
                return false;
            }
        });

        return nameDialog;
    }

    private void searchTrack(final String search){
        if(search.length() == 0)
            return;

        pbSpotifyLoading.setVisibility(View.VISIBLE);
        tracks.clear();
        tA = new TrackAdapter(tracks);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchTracks(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("tracks");
                    JSONArray jArray = trackJson.getJSONArray("items");
                    if (jArray.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SettingsSession.this, "No tracks found with that name", Toast.LENGTH_SHORT).show();
                                pbSpotifyLoading.setVisibility(View.INVISIBLE);
                            }
                        });

                        return;
                    }
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject js = jArray.getJSONObject(i);

                        Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                        Track t = new Track(js);
                        t.setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                            @Override
                            public void onLoaded(boolean loaded) {
                                if(!loaded)
                                    return;
                                SettingsSession.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tA.notifyDataSetChanged();
                                    }
                                });
                            }
                        });

                        tracks.add(t);
                    }
                    Manager.manager.isDisplayingSpotifyLikes = false;
                    manager.currentSpotifyOffset = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chooseTrack();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        };
        thread.start();
    }

    private void chooseTrack() {
        pbSpotifyLoading.setVisibility(View.INVISIBLE);

        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose a backup track")
                .setAdapter(tA, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setTrack(tracks.get(which));
                    }
                });

        try {
            chooseDialog.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }

    }

    private void setTrack(Track track){
        manager.mediaManager.setBackupTrack(track);
        currentPlayList.setText("Current Track: " + track.getTrackTitle());
        currentPlayList.setVisibility(View.VISIBLE);
    }

    private AlertDialog.Builder showAlbumSearchDialog(){
        Log.d("TUNEIN", "Showing album");
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Spotify album: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setHint(R.string.album);
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
                searchAlbum(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        final AlertDialog ad = nameDialog.show();

        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    searchAlbum(search.getText().toString());
                    ad.dismiss();
                    return true;
                }
                return false;
            }
        });

        return nameDialog;
    }

    private void searchAlbum(final String search){
        if(search.length() == 0)
            return;

        pbSpotifyLoading.setVisibility(View.VISIBLE);
        albums.clear();
        aA = new AlbumAdapter(albums);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchAlbums(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("albums");
                    JSONArray jArray = trackJson.getJSONArray("items");

                    if (jArray.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SettingsSession.this, "No playlists found with that name", Toast.LENGTH_SHORT).show();
                                pbSpotifyLoading.setVisibility(View.INVISIBLE);
                            }
                        });

                        return;
                    }
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject js = jArray.getJSONObject(i);

                        Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                        Album a = new Album(js);
                        a.setOnLoadedArtworkListener(new Album.onLoadedArtwork() {
                            @Override
                            public void onLoaded(boolean loaded) {
                                if(!loaded)
                                    return;
                                SettingsSession.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        aA.notifyDataSetChanged();
                                    }
                                });
                            }
                        });

                        albums.add(a);
                    }
                    Manager.manager.isDisplayingSpotifyLikes = false;
                    manager.currentSpotifyOffset = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chooseAlbum();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        };
        thread.start();
    }

    private void chooseAlbum() {
        pbSpotifyLoading.setVisibility(View.INVISIBLE);

        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose a backup album")
                .setAdapter(aA, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAlbum(albums.get(which));
                    }
                });

        try {
            chooseDialog.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }

    }

    private void setAlbum(Album album){
        manager.mediaManager.setBackupAlbum(album);
        currentPlayList.setText("Current Album: " + album.getAlbumName());
        currentPlayList.setVisibility(View.VISIBLE);
    }

    private AlertDialog.Builder showPlaylistSearchDialog() {
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Spotify playlist: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setHint(R.string.playlist);
        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);

        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchPlaylist(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        final AlertDialog ad = nameDialog.show();

        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    searchPlaylist(search.getText().toString());
                    ad.dismiss();
                    return true;
                }
                return false;
            }
        });
        return nameDialog;
    }

    private void searchPlaylist(final String search) {
        if(search.length() == 0)
            return;
        pbSpotifyLoading.setVisibility(View.VISIBLE);
        playlists.clear();
        pA = new PlaylistAdapter(playlists);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchPlaylists(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("playlists");
                    JSONArray jArray = trackJson.getJSONArray("items");

                    if (jArray.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SettingsSession.this, "No playlists found with that name", Toast.LENGTH_SHORT).show();
                                pbSpotifyLoading.setVisibility(View.INVISIBLE);
                            }
                        });

                        return;
                    }
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject js = jArray.getJSONObject(i);

                        Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                        Playlist p = new Playlist(js);
                        p.setOnLoadedArtworkListener(new Playlist.onLoadedArtwork() {
                            @Override
                            public void onLoaded(boolean loaded) {
                                if(!loaded)
                                    return;
                                SettingsSession.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pA.notifyDataSetChanged();
                                    }
                                });
                            }
                        });

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

    private void choosePlayList() {
        pbSpotifyLoading.setVisibility(View.INVISIBLE);

        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose a backup playlist")
                .setAdapter(pA, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setPlaylist(playlists.get(which));
                    }
                });

        try {
            chooseDialog.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }

    }


    private void setPlaylist(Playlist playlist){
        manager.mediaManager.setBackupPlaylist(playlist);
        currentPlayList.setText("Current Playlist: " + playlist.getPlaylistName());
        currentPlayList.setVisibility(View.VISIBLE);
        SharedPreferences settings = getSharedPreferences(Manager.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("backupplaylist", playlist.getPlaylistId());

        editor.apply();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            //Control pop up
            case R.id.playlist_id:
                showPlaylistSearchDialog();
                break;
            case R.id.album_id:
                showAlbumSearchDialog();
                break;
            case R.id.track_id:
                showTrackSearchDialog();
                break;
        }
        return false;
    }

    private class PlaylistAdapter extends ArrayAdapter<Playlist> {
        public PlaylistAdapter(ArrayList<Playlist> playlists) {
            super(SettingsSession.this, R.layout.playlist_view, playlists);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.playlist_view,
                        parent, false);
            final Playlist currentPlaylist = playlists.get(position);
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

    private class AlbumAdapter extends ArrayAdapter<Album> {
        public AlbumAdapter(ArrayList<Album> albums) {
            super(SettingsSession.this, R.layout.album_view, albums);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.album_view,
                        parent, false);
            final Album currentAlbum = albums.get(position);
            ImageView avatar = (ImageView) itemView.findViewById(R.id.imgArtwork);

            avatar.setImageBitmap(currentAlbum.getAlbumArtowrk());
            avatar.getLayoutParams().width = 250;
            avatar.getLayoutParams().height = 250;


            TextView txtName = (TextView) itemView.findViewById(R.id.txtAlbumName);
            txtName.setText(currentAlbum.getAlbumName());

            return itemView;
        }
    }

    private class TrackAdapter extends ArrayAdapter<Track> {
        public TrackAdapter(ArrayList<Track> tracks) {
            super(SettingsSession.this, R.layout.song_view, tracks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.song_view,
                        parent, false);
            final Track currentTrack = tracks.get(position);
            ImageView avatar = (ImageView) itemView.findViewById(R.id.imgIcon);

            avatar.setImageBitmap(currentTrack.getTrackBitmap());
            avatar.getLayoutParams().width = 250;
            avatar.getLayoutParams().height = 250;


            TextView txtName = (TextView) itemView.findViewById(R.id.txtSongName);
            txtName.setText(currentTrack.getTrackTitle());

            TextView txtArtists = (TextView) itemView.findViewById(R.id.txtTime);
            txtArtists.setText(currentTrack.getArtistString());
            return itemView;
        }
    }
}
