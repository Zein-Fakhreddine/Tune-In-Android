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
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
    private ImageView imgSong, imgPlayPause, imgControl;

    //Tracks
    private ArrayAdapter<Track> adapter;
    private boolean runThread, isConnectingToSpotify, doubleBackToExitPressedOnce;
    private int preLast;

    NotificationManager notificationManager;
    private Thread playSongThread, searchTrackThread, displayLikesThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_menu);
        searchTracks = new ArrayList<>();
        initManager();
        initView();
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
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if (lastItem == totalItemCount) {
                    if (preLast != lastItem) { //to avoid multiple calls for last item
                        Log.d("Last", "Last");
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
        imgPlayPause = (ImageView) findViewById(R.id.imgPlayPause);
        imgPlayPause.setOnClickListener(this);
        imgControl = (ImageView) findViewById(R.id.imgControl);
        imgControl.setOnClickListener(this);
        imgControl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAddSongPopup(v);
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
        /*
        final Runnable r = new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
                if (!manager.isUserSearching)
                    pbSearching.setVisibility(View.INVISIBLE);
                if (manager.hasUserChoseSong)
                    pbSearching.setVisibility((manager.currentChosenTracks.size() == 0) ? View.VISIBLE : View.INVISIBLE);

                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);
*/
        Thread thread = new Thread() {
            public void run() {
                while (runThread && manager != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                adapter.notifyDataSetChanged();
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });

                    if (manager.isServer && manager.mediaManager.getSpotifyPlayer().isShutdown() && !isConnectingToSpotify) {
                        if (!isConnectingToSpotify)
                            requestSpotify();
                        isConnectingToSpotify = true;
                    }
                    String[] info = manager.getChosenSongs(manager.getHostKey()).split(",");
                    if (!manager.isServer && manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser).equals("restart"))
                        restartClient();

                    for (int i = 0; i < info.length; i++) {
                        if (!info[i].equals("-1")) {
                            Log.d("TUNEIN", "Info: " + info[i]);
                            int currentTrackIteration = Integer.parseInt(info[i].split("ITE")[1]);
                            if (currentTrackIteration == manager.currentIteration) {
                                Log.d("TUNEIN", "Adding track with iteration: " + currentTrackIteration + " and manager iteration: " + manager.currentIteration);
                                String trackId = info[i].split("ITE")[0];
                                Log.d("TUNEIN", "Found track with id: " + trackId);
                                if (!(trackId.equals("-1") || trackId.equals("0"))) {
                                    if (!hasTrackId(trackId)) {
                                        manager.currentChosenTracks.add(Track.getTrack(trackId));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                    String[] votesIds = manager.getVotes(manager.getHostKey()).split(",");
                    for (int i = 0; i < votesIds.length; i++)
                        try {
                            String voteId = votesIds[i];
                            for (int y = 0; y < manager.currentChosenTracks.size(); y++) {
                                Track track = manager.currentChosenTracks.get(y);
                                track.setVote(0);
                            }
                            for (int x = 0; x < manager.currentChosenTracks.size(); x++) {
                                Track track = manager.currentChosenTracks.get(x);
                                Log.d("TUNEIN", "Vote id: " + voteId);
                                if (track.getTrackId().equals(voteId))
                                    track.addVote();
                            }
                        } catch (Exception e) {
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
        buttonsIntent.putExtra("Pause", true);

        PendingIntent bIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), buttonsIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        /* Invoking the default notification service */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("New Message");
        mBuilder.setContentText("You've received new message.");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.addAction(R.mipmap.ic_pause, "Pause", bIntent);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

   /* notificationID allows you to update the notification later on. */
        notificationManager.notify(2119, mBuilder.build());
    }

    public void showAddSongPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.addsongpopup, popup.getMenu());
        popup.show();
    }

    private void showSongSearch() {
        if (manager.hasUserChoseSong) {
            error("You have already chosen a song");
            return;
        }

        final AlertDialog.Builder searchDialog = new AlertDialog.Builder(this);
        searchDialog.setTitle("Search for a song");
        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    searchTrack(search.getText().toString());
                    return true;
                }
                return false;
            }
        });
        searchDialog.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    searchTrack(search.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    dialog.cancel();
                }
            }
        });
        searchDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        searchDialog.show();
    }

    private void searchTrack(final String search) {
        if (manager.isUserSearching)
            return;

        adapter.notifyDataSetChanged();
        manager.isUserSearching = true;
        pbSearching.setVisibility(View.VISIBLE);

        searchTrackThread = new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject fullJson = new JSONObject(Manager.manager.mediaManager.getSpotifySearch().searchTracks(search, 0, 10));
                    JSONObject trackJson = fullJson.getJSONObject("tracks");
                    JSONArray jArray = trackJson.getJSONArray("items");
                    loadSongs(jArray);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        searchTrackThread.start();
    }

    private void loadSongs(final JSONArray jArray) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < jArray.length(); i++) {
                        if (manager.hasUserChoseSong)
                            break;
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
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.song_id:
                showSongSearch();
                break;
            case R.id.playlist_id:

                break;
            case R.id.favorites_id:
                loadFavorites();
                break;
            case R.id.start_id:
                if (manager.currentChosenTracks.size() == 0) {
                    error("No one has chose a song yet");
                    return true;
                } else if (manager.isServer) {
                    startSong();
                    return true;
                } else
                    error("You have to be the host to start the session!");
                break;

            case R.id.end_id:
                //manager.sendExit(manager.getHostKey()); // TODO: Tell the server that you have ended the session
                endSession();
                break;
            case R.id.settings_id:
                this.startActivity(new Intent(this, SettingsSession.class));
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
                leaveSession();
            }
        });
        errorDialog.setNegativeButton("No", null);
        errorDialog.show();
    }

    private void leaveSession() {
        manager.mediaManager.stop();
        this.startActivity(new Intent(this, MainMenu.class));
        runThread = false;
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
        } else
            error("You need to link your Spotify account");

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
                if (manager.mediaManager.getBackupPlaylist() != null && count == 0){
                    if (!manager.mediaManager.playPlaylist()){
                        error("Could not play the song");
                        return;
                    } else
                        manager.currentIteration++;
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
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbSearching.setVisibility(View.VISIBLE);
                            }
                        });
                        manager.currentIteration++;
                    }
                }

                manager.mediaManager.setOnFinishedPlaying(new MediaManager.onFinishedPlaying() {
                    @Override
                    public void finishedPlaying(MediaManager mm) {
                        Log.d("TUNEIN", "This is what is happening you idiot");
                        startSong();
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(count != 0){
                            imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                            imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
                            imgSong.setVisibility(View.VISIBLE);
                            imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
                            restart();
                        }
                        else{
                            manager.mediaManager.setOnPreparedSong(new MediaManager.onPreparedSong() {
                                @Override
                                public void preparedSong(Track t) {
                                    if(t == manager.mediaManager.getCurrentPlayingTrack()){
                                        imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                                        imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
                                        imgSong.setVisibility(View.VISIBLE);
                                        imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
                                        restart();
                                        pbSearching.setVisibility(View.INVISIBLE);
                                    }

                                }
                            });
                        }

                    }
                });
            }
        };
        playSongThread.start();

        this.startService(new Intent(this, PlayAudio.class).setData(Uri.parse("")));
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
                        JSONObject track = js.getJSONObject("track");
                        Log.d("TUNEINE", track.toString());
                        if (i == 0 && offset == 0)
                            searchTracks.clear();
                        Track t = new Track(track);
                        if (manager.hasUserChoseSong)
                            break;
                        searchTracks.add(t);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
        displayLikesThread.run();

    }

    private void restart() {
        manager.hasUserChoseSong = false;
        manager.hasUserVotedForSong = false;
        adapter = new SongListAdapter(
                searchTracks);
        trackView.setAdapter(adapter);
        registerListCallBack();
    }

    private void restartClient() {
        manager.currentIteration++;
        manager.currentChosenTracks.clear();
        manager.hasUserChoseSong = false;
        manager.hasUserVotedForSong = false;
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
                    Thread thread = new Thread() {
                        public void run() {
                            manager.currentUser.setChosenTrack(searchTracks.get(position));
                            manager.hasUserChoseSong = true;
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
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == imgPlayPause.getId() && manager.mediaManager.getCurrentPlayingTrack() != null && (manager.mediaManager.isSongPaused()) ? manager.mediaManager.startSong() : manager.mediaManager.pauseSong())
            imgPlayPause.setImageBitmap((manager.mediaManager.isSongPaused()) ? BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_play) : BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
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
            Log.d("TUNEIN", "GOODIN");
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.d("TUNEIN", "Best");
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
                    }
                };
                thread.start();
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
                trackTime.setText("Playback count: " + currentTrack.getPlaybackCount());
            }
            return itemView;
        }
    }
}
