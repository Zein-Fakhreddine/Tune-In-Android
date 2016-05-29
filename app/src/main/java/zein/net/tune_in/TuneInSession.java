package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
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

import static zein.net.tune_in.Manager.manager;

public class TuneInSession extends Activity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener {
    private ListView trackView;
    private ArrayList<Track> searchTracks;

    private ProgressBar pbSearching;
    private ImageView imgSong, imgPlayPause, imgControl, imgSearch;
    private EditText txtSongSearch;

    //Tracks
    private ArrayAdapter<Track> adapter;
    private boolean runThread, isConnectingToSpotify, doubleBackToExitPressedOnce;
    private int preLast;

    NotificationManager notificationManager;
    private Thread playSongThread, searchTrackThread, displayLikesThread;

    private AlertDialog.Builder songInfo;

    private ArrayList<String> usernames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_menu);
        searchTracks = new ArrayList<>();
        initManager();
        initView();
        if(manager.isServer)
            requestSpotify();
        update();
    }

    private void initManager() {
        TextView txtSessionName = (TextView) findViewById(R.id.txtSessionName);
        txtSessionName.append(" " + manager.sessionName);
    }

    private void initView() {

        trackView = (ListView) findViewById(R.id.lstTracks);
        registerListCallBack();
        trackView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if (lastItem == totalItemCount) {
                    if (preLast != lastItem) {
                        preLast = lastItem;
                        if (manager.isDisplayingSpotifyLikes && !manager.hasUserChoseSong && !manager.isUserSearching) {
                            adapter.notifyDataSetChanged();
                            manager.currentSpotifyOffset += 10;
                            displayLikes(10, manager.currentSpotifyOffset);
                        }
                    }
                }
            }
        });
        pbSearching = (ProgressBar) findViewById(R.id.pbSearching);
        TextView txtKey = (TextView) findViewById(R.id.txtKey);
        txtKey.setText("KEY: " + manager.getHostKey());
        imgSong = (ImageView) findViewById(R.id.imgSong);
        imgSong.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!manager.isServer)
                    return false;
                songInfo = new AlertDialog.Builder(TuneInSession.this).setTitle("Song Info:");
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.info_view, null);
                songInfo.setView(dialogView);

                Track clickedTrack = manager.mediaManager.getCurrentPlayingTrack();
                if (clickedTrack == null || clickedTrack.getTrackBitmap() == null)
                    return false;
                ImageView imgArtwork = (ImageView) dialogView.findViewById(R.id.imgAlbumArtwork);
                imgArtwork.setImageBitmap(clickedTrack.getTrackBitmap());
                TextView txtName = (TextView) dialogView.findViewById(R.id.txtTrackName);
                txtName.setText("Name: "  + clickedTrack.getTrackTitle());
                TextView txtArtists = (TextView) dialogView.findViewById(R.id.txtTrackArtists);

                txtArtists.setText("Artists: " + clickedTrack.getArtistString());
                TextView txtAlbumName = (TextView) dialogView.findViewById(R.id.txtAlbumName);
                txtAlbumName.setText("Album: " + clickedTrack.getAlbumName());
                TextView txtDuration = (TextView) dialogView.findViewById(R.id.txtTrackDuration);
                txtDuration.setText("Duration: " + clickedTrack.getDurationToTime());

                songInfo.show();
                return true;
            }
        });
        imgPlayPause = (ImageView) findViewById(R.id.imgPlayPause);
        imgPlayPause.setOnClickListener(this);
        imgControl = (ImageView) findViewById(R.id.imgControl);
        imgControl.setOnClickListener(this);
        txtSongSearch = (EditText) findViewById(R.id.etxtSongSearch);
        txtSongSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchTrack(txtSongSearch.getText().toString());
            }
        });
        imgSearch = (ImageView) findViewById(R.id.imgSearch);
        imgSearch.setOnClickListener(this);
        imgSearch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSearchPopup(v);
                return false;
            }
        });
        if (!manager.isServer)
            imgPlayPause.setVisibility(View.INVISIBLE);
        adapter = new SongListAdapter(searchTracks);
        trackView.setAdapter(adapter);


        buildIcon();
    }

    private void update() {
        runThread = true;
        final android.os.Handler handler = new android.os.Handler();

        Thread thread = new Thread() {
            public void run() {
                while (runThread && manager != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (manager.isServer && (manager.mediaManager.getSpotifyPlayer() == null || manager.mediaManager.getSpotifyPlayer().isShutdown()) && !isConnectingToSpotify) {
                        if (!isConnectingToSpotify)
                            requestSpotify();
                        isConnectingToSpotify = true;
                    }

                    try{
                        JSONObject serverInfo = new JSONObject(manager.getServerInfo(manager.getHostKey()));

                        if(!manager.isServer && runThread){
                            if(serverInfo.getBoolean("stoped"))
                                leaveSession("host");
                            if(serverInfo.getInt("serverIteration") != manager.currentIteration){
                                manager.currentIteration = serverInfo.getInt("serverIteration");
                                restartClient();
                            }
                        }

                        JSONArray users = serverInfo.getJSONArray("users");
                        for(int i = 0; i < users.length(); i++){
                            JSONObject user = users.getJSONObject(i);
                            final String username = user.getString("userName");
                            if(!usernames.contains(username)){
                                if(usernames.size() != 0) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(TuneInSession.this, username + " has joined", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                usernames.add(username);
                            }

                            String chosenSongId = user.getString("chosenSongId");
                            if(!chosenSongId.equals("-1")){
                                int currentTrackIteration;
                                try{
                                    currentTrackIteration = Integer.parseInt(chosenSongId.split("ITE")[1]);
                                    if(currentTrackIteration == manager.currentIteration){
                                        String trackId = chosenSongId.split("ITE")[0];
                                        if(!trackId.equals("-1") && !hasTrackId(trackId)){
                                            manager.currentChosenTracks.add(Track.getTrack(trackId, username));
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }

                        for(int i = 0; i < manager.currentChosenTracks.size(); i++){
                            Track track = manager.currentChosenTracks.get(i);
                            int vCount = 0;
                            for(int x = 0; x < users.length(); x++){
                                JSONObject user = users.getJSONObject(x);
                                try{
                                    String voteId = user.getString("votedSongId");
                                    if(voteId.equals(track.getTrackId()))
                                        vCount++;
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }

                            if(track.getVotes() != vCount)
                                track.setVote(vCount);
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                    }

                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private boolean hasTrackId(String trackId) {
        boolean hasTrackId = false;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            String chosenTrackId = manager.currentChosenTracks.get(i).getTrackId();
            Log.d("TUNEIN", "The track id is: " + trackId + " and the chosen track id is: " + chosenTrackId);
            if (trackId.equals(chosenTrackId)) {
                hasTrackId = true;
                break;
            }
        }
        return hasTrackId;
    }

    private void buildIcon() {
        Intent buttonsIntent = new Intent(this, NotifyActivityHandler.class);
        PendingIntent bIntent = PendingIntent.getActivity(this, 0 ,buttonsIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, NotifyActivityHandler.class);
        pauseIntent.setAction("pause");
        PendingIntent pPauseIntent = PendingIntent.getActivity(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Invoking the default notification service */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("New Message");
        mBuilder.setContentText("You've received new message.");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentIntent(bIntent);
        mBuilder.addAction(R.mipmap.ic_pause, "Pause", pPauseIntent);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

   /* notificationID allows you to update the notification later on. */
        notificationManager.notify(2119, mBuilder.build());
    }

    private void showControlPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.controlpopup, popup.getMenu());
        popup.show();
    }

    private void showSearchPopup(View v){
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.searchpopup, popup.getMenu());
        popup.show();
    }

    private void searchTrack(final String search) {
        if(manager.hasUserChoseSong)
            return;
        if(search.length() == 0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    searchTracks.clear();
                    adapter.notifyDataSetChanged();
                    manager.isUserSearching = false;
                    pbSearching.setVisibility(View.INVISIBLE);
                }
            });
            return;
        }
        adapter.notifyDataSetChanged();
        manager.isUserSearching = true;
        pbSearching.setVisibility(View.VISIBLE);
        final String searchText = txtSongSearch.getText().toString();
        searchTrackThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    if(!search.equals(txtSongSearch.getText().toString()))
                        return;
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchTracks(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("tracks");
                    final JSONArray jArray = trackJson.getJSONArray("items");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int i = 0; i < jArray.length(); i++) {
                                    if (manager.hasUserChoseSong)
                                        break;

                                    if(!search.equals(txtSongSearch.getText().toString()))
                                        return;

                                    JSONObject js = jArray.getJSONObject(i);
                                    if (i == 0)
                                        searchTracks.clear();
                                    Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                                    Track t = new Track(js);
                                    if (manager.hasUserChoseSong)
                                        break;
                                    searchTracks.add(t);

                                    adapter.notifyDataSetChanged();
                                }
                                Manager.manager.isDisplayingSpotifyLikes = false;
                                manager.currentSpotifyOffset = 0;

                                pbSearching.setVisibility(View.INVISIBLE);
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                                error("An error occured while searching for songs");
                            }

                            manager.isUserSearching = false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        searchTrackThread.start();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            //Control pop up
            case R.id.start_id:
                if (manager.currentChosenTracks.size() == 0) {
                    error("No one has chose a song yet");
                    break;
                } else if (manager.isServer) {
                    startSong();
                    Log.d("TUNEINE", "OMG IT WAS PRESSED");
                    break;
                } else
                    error("You have to be the host to start the session!");
                break;

            case R.id.end_id:
                endSession();
                break;
            case R.id.settings_id:
                this.startActivity(new Intent(this, SettingsSession.class));
                break;

            //Search popup
            case R.id.display_saved:
                loadFavorites();
                break;
        }
        return false;
    }

    private void endSession() {
        AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle("Are you sure you want to end the session");
        errorDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                leaveSession("user");
            }
        });
        errorDialog.setNegativeButton("No", null);
        errorDialog.show();
    }

    private void leaveSession(String reason) {
        runThread = false;
        if(manager.isServer){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("TUNEIN", "This was called");
                    manager.sendStopedSession(manager.getHostKey());
                }
            });
            thread.start();
        }

        if(manager.mediaManager != null)
            manager.mediaManager.stop();
        this.startActivity(new Intent(this, MainMenu.class).putExtra("reason",reason));
        manager = new Manager();
        finish();
    }


    private void loadFavorites() {
        if (manager.hasUserChoseSong) {
            error("You have already chosen a song!");
            return;
        }
        if (manager.mediaManager.getSpotifyPlayer() != null && !manager.mediaManager.getSpotifyPlayer().isShutdown()) {
            manager.currentSpotifyOffset = 0;
            displayLikes(10, 0);
        } else{
            final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
            errorDialog.setTitle("You need to link your Spotify account");
            errorDialog.setPositiveButton("Link", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(TuneInSession.this, SettingsSession.class).putExtra("reason", "link"));
                }
            });
            errorDialog.setNegativeButton("Cancel", null);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    errorDialog.show();
                }
            });
        }
    }

    private void startSong() {
        final int count = manager.currentChosenTracks.size();

        playSongThread = new Thread() {
            @Override
            public void run() {
                if (manager.mediaManager.isSongPlaying()) {
                    error("A song is already playing");
                    return;
                }
                if (count == 0 && manager.mediaManager.getBackupPlaylist() == null) {
                    error("No chosen songs to play");
                    return;
                }
                if (manager.mediaManager.getBackupPlaylist() != null && count == 0) {
                    if (!manager.mediaManager.playPlaylist()) {
                        error("Could not play the song");
                        return;
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbSearching.setVisibility(View.VISIBLE);
                            }
                        });
                        manager.currentIteration++;
                        startService(new Intent(TuneInSession.this, PlayAudio.class).setData(Uri.parse("")));
                    }
                }


                if (count != 0) {
                    Track trackToPlay = null;
                    for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
                        Track track = manager.currentChosenTracks.get(i);
                        Log.d("TUNEIN", "Song:" + track.getTrackTitle() + " has: " + track.getVotes() + "votes");
                        if (trackToPlay == null || track.getVotes() > trackToPlay.getVotes())
                            trackToPlay = track;
                    }

                    if (!manager.mediaManager.playSong(trackToPlay)) {
                        error("Could not play the song");
                        return;
                    } else{
                        manager.currentIteration++;
                        startService(new Intent(TuneInSession.this, PlayAudio.class).setData(Uri.parse("")));
                    }
                }

                manager.mediaManager.setOnFinishedPlaying(new MediaManager.onFinishedPlaying() {
                    @Override
                    public void finishedPlaying(MediaManager mm) {
                        if(!manager.mediaManager.isSongPlaying()){
                            Log.d("TUNEIN", "This is what is happening you idiot");
                            startSong();
                        }
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count != 0) {
                            imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                            imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
                            imgSong.setVisibility(View.VISIBLE);
                            imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
                            restart();
                        } else {
                            manager.mediaManager.setOnPreparedSong(new MediaManager.onPreparedSong() {
                                @Override
                                public void preparedSong(Track t) {
                                    if (t == manager.mediaManager.getCurrentPlayingTrack()) {
                                        imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                                        imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
                                        imgSong.setVisibility(View.VISIBLE);
                                        imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
                                        pbSearching.setVisibility(View.INVISIBLE);
                                        restart();
                                    }

                                }
                            });
                        }

                    }
                });
            }
        };
        playSongThread.start();

    }

    private void displayLikes(final int limit, final int offset) {
        if (manager.isUserSearching || manager.hasUserChoseSong)
            return;
        Log.d("TUNEIN", "Got here");
        adapter.notifyDataSetChanged();
        manager.isUserSearching = true;

        pbSearching.setVisibility(View.VISIBLE);
        displayLikesThread = new Thread() {
            @Override
            public void run() {
                try {
                    String search = manager.mediaManager.getSpotifySearch().getSavedTracks(offset, limit).toString();
                    JSONObject fullJson = new JSONObject(search);
                    JSONArray jArray = fullJson.getJSONArray("items");
                    for (int i = 0; i < jArray.length(); i++) {

                        if (manager.hasUserChoseSong)
                            break;
                        JSONObject js = jArray.getJSONObject(i);
                        final JSONObject track = js.getJSONObject("track");
                        Log.d("TUNEINE", track.toString());
                        if (i == 0 && offset == 0)
                            searchTracks.clear();

                        if (manager.hasUserChoseSong)
                            break;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Track t = new Track(track);
                                searchTracks.add(t);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }

                    Manager.manager.isDisplayingSpotifyLikes = true;
                } catch (Exception e) {
                    Log.d("TUNEIN", "Error");
                    e.printStackTrace();
                }
                Manager.manager.isUserSearching = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbSearching.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        displayLikesThread.start();

    }

    private void restart() {
        manager.hasUserChoseSong = false;
        manager.hasUserVotedForSong = false;
        manager.currentUser.setVotedTrack(null);
        adapter = new SongListAdapter(
                searchTracks);
        trackView.setAdapter(adapter);
        registerListCallBack();
    }

    private void restartClient() {
        Log.d("TUNEIN", "Restarted Client with server iteration: " + manager.currentIteration);
        manager.currentChosenTracks.clear();
        manager.hasUserChoseSong = false;
        manager.hasUserVotedForSong = false;
        manager.currentUser.setVotedTrack(null);
        this.runOnUiThread(new Runnable() {
            public void run() {
                adapter = new SongListAdapter(
                        searchTracks);
                trackView.setAdapter(adapter);
                registerListCallBack();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME));
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit",
                Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private void registerListCallBack() {
        trackView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    final int position, long id) {
                if (manager.hasUserChoseSong) {
                    if (manager.currentUser.getVotedTrack() == null)
                        voteForSong(position);
                } else {
                    if (manager.isUserSearching)
                        return;

                    manager.currentUser.setChosenTrack(searchTracks.get(position));
                    manager.hasUserChoseSong = true;
                    Thread thread = new Thread() {
                        public void run() {
                            manager.sendUsersChosenSong(manager.getHostKey(), manager.currentUser);
                        }
                    };
                    thread.start();

                    adapter = new SongListAdapter(
                            manager.currentChosenTracks);
                    trackView.setAdapter(adapter);
                    registerListCallBack();
                }
            }
        });

        trackView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                songInfo = new AlertDialog.Builder(TuneInSession.this).setTitle("Song Info:");
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.info_view, null);
                songInfo.setView(dialogView);

                Track clickedTrack = (manager.hasUserChoseSong) ? manager.currentChosenTracks.get(position) : searchTracks.get(position);
                if (clickedTrack == null || clickedTrack.getTrackBitmap() == null)
                    return false;
                ImageView imgArtwork = (ImageView) dialogView.findViewById(R.id.imgAlbumArtwork);
                imgArtwork.setImageBitmap(clickedTrack.getTrackBitmap());
                TextView txtName = (TextView) dialogView.findViewById(R.id.txtTrackName);
                txtName.setText("Name: "  + clickedTrack.getTrackTitle());
                TextView txtArtists = (TextView) dialogView.findViewById(R.id.txtTrackArtists);

                txtArtists.setText("Artists: " + clickedTrack.getArtistString());
                TextView txtAlbumName = (TextView) dialogView.findViewById(R.id.txtAlbumName);
                txtAlbumName.setText("Album: " + clickedTrack.getAlbumName());
                TextView txtDuration = (TextView) dialogView.findViewById(R.id.txtTrackDuration);
                txtDuration.setText("Duration: " + clickedTrack.getDurationToTime());

                songInfo.show();
                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == imgPlayPause && manager.mediaManager.getCurrentPlayingTrack() != null && (manager.mediaManager.isSongPaused()) ? manager.mediaManager.startSong() : manager.mediaManager.pauseSong())
            imgPlayPause.setImageBitmap((manager.mediaManager.isSongPaused()) ? BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_play) : BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
        else if(v == imgControl)
            showControlPopup(v);
        else if(v == imgSearch)
            txtSongSearch.performClick();
    }

    private void requestSpotify() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(MediaManager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, MediaManager.REDIRECT_URI);
        builder.setScopes(new String[]{"user-library-read", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, MediaManager.REQUEST_CODE, request);
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
                        isConnectingToSpotify = false;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                        error("An unexpected error occurred while trying to connect to Spotify");
                        isConnectingToSpotify = false;
                    }
                });
            }
        } else
            isConnectingToSpotify = false;

    }

    private void voteForSong(final int position) {
        if (manager.currentUser.getChosenTrack().getTrackId().equals(manager.currentChosenTracks.get(position).getTrackId())) {
            error("You can't vote for the song you chose!");
            return;
        }

        final AlertDialog.Builder searchDialog = new AlertDialog.Builder(this);
        searchDialog.setTitle("Are you sure you want to vote for this song");

        // Set up the buttons
        searchDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Thread thread = new Thread() {
                    public void run() {
                        manager.currentUser.setVotedTrack(manager.currentChosenTracks.get(position));
                        manager.sendUserVotedSong(manager.getHostKey(), manager.currentUser);
                        manager.hasUserVotedForSong = true;
                        return;
                    }
                };
                thread.start();
                return;
            }
        });
        searchDialog.setNegativeButton("No", null);

        searchDialog.show();
    }


    private AlertDialog.Builder error(String errorMessage) {
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(errorMessage);
        errorDialog.setPositiveButton("OK", null);
        errorDialog.setNegativeButton("Cancel", null);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorDialog.show();
            }
        });

        return errorDialog;
    }

    private class SongListAdapter extends ArrayAdapter<Track> {

        public SongListAdapter(ArrayList<Track> addapterTracks) {
            super(TuneInSession.this, R.layout.song_view, addapterTracks);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.song_view, parent, false);

            if (manager.hasUserChoseSong) {
                if (manager.currentChosenTracks.size() == 0)
                    return itemView;

                if (position >= manager.currentChosenTracks.size())
                    return itemView;

                Track currentTrack = manager.currentChosenTracks.get(position);

                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
                if (trackIcon.getLayoutParams().width != 250 || trackIcon.getLayoutParams().height != 250)
                    trackIcon.getLayoutParams().width = trackIcon.getLayoutParams().height = 250;

                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText("Votes: " + currentTrack.getVotes());

                if(currentTrack.getUserSubmited() != null){
                    TextView submittedUser = (TextView) itemView.findViewById(R.id.txtSubmitedby);
                    if(submittedUser.getVisibility() == View.INVISIBLE)
                        submittedUser.setVisibility(View.VISIBLE);

                    submittedUser.setText("Chosen by: " + currentTrack.getUserSubmited());
                }
            } else {
                if (searchTracks.size() == 0)
                    return itemView;

                Track currentTrack = searchTracks.get(position);
                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
                if (trackIcon.getLayoutParams().width != 250 || trackIcon.getLayoutParams().height != 250)
                    trackIcon.getLayoutParams().width = trackIcon.getLayoutParams().height = 250;


                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText(currentTrack.getArtistString());
            }
            return itemView;
        }
    }


}
