package zein.net.dynamic_dj;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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

import static zein.net.dynamic_dj.Manager.manager;

public class DynamicDjSession extends Activity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener {
    private ListView trackView;
    private ArrayList<Track> searchTracks;

    private ProgressBar pbSearching;
    private ImageView imgSong, imgPlayPause, imgControl, imgDisplayLikes;
    private EditText txtSongSearch;

    //Tracks
    private ArrayAdapter<Track> adapter;
    private boolean runThread, isConnectingToSpotify, doubleBackToExitPressedOnce;
    private int preLast;

    private NotificationManager notificationManager;

    private AlertDialog.Builder songInfo;

    private ArrayList<String> usernames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_menu);
        searchTracks = new ArrayList<>();
        initManager();
        initView();
        if (manager.isServer)
            requestSpotify();

        if (manager.mediaManager.getBackupPlaylist() == null) {
            SharedPreferences settings = getSharedPreferences(Manager.PREFS_NAME, 0);
            String backupplaylist = settings.getString("backupplaylist", "none");
            if (!backupplaylist.equals("none"))
                setPlaylist(backupplaylist);
        }

        update();
    }

    private void initManager() {
        TextView txtSessionName = (TextView) findViewById(R.id.txtSessionName);
        txtSessionName.append(" " + manager.sessionName);
    }

    private void initView() {

        trackView = (ListView) findViewById(R.id.lstTracks);
        if(Build.VERSION.SDK_INT >= 16){
            LayoutTransition l = new LayoutTransition();
            l.enableTransitionType(LayoutTransition.CHANGING);
            trackView.setLayoutTransition(l);
        }
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
        String key = getString(R.string.keyhint) + " " + manager.getHostKey();
        txtKey.setText(key);
        imgSong = (ImageView) findViewById(R.id.imgSong);
        imgSong.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (imgSong.getVisibility() == View.INVISIBLE)
                    return false;
                songInfo = new AlertDialog.Builder(DynamicDjSession.this).setTitle("Song Info:");
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.info_view, null);
                songInfo.setView(dialogView);

                Track clickedTrack = manager.mediaManager.getCurrentPlayingTrack();
                if (clickedTrack == null || clickedTrack.getTrackBitmap() == null)
                    return false;
                ImageView imgArtwork = (ImageView) dialogView.findViewById(R.id.imgPlaylistArtwork);
                imgArtwork.setImageBitmap(clickedTrack.getTrackBitmap());
                TextView txtName = (TextView) dialogView.findViewById(R.id.txtPlaylistName);
                String name = getString(R.string.trackname) + clickedTrack.getTrackTitle();
                txtName.setText(name);
                TextView txtArtists = (TextView) dialogView.findViewById(R.id.txtPlaylistArtists);
                String artists = getString(R.string.artists) + clickedTrack.getArtistString();
                txtArtists.setText(artists);
                TextView txtAlbumName = (TextView) dialogView.findViewById(R.id.txtAlbumName);
                String album = getString(R.string.Album) + clickedTrack.getAlbumName();
                txtAlbumName.setText(album);
                TextView txtDuration = (TextView) dialogView.findViewById(R.id.txtPlaylistDuration);
                String duration = getString(R.string.duration) + clickedTrack.getDurationToTime();
                txtDuration.setText(duration);

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchTrack(txtSongSearch.getText().toString());
            }
        });
        imgDisplayLikes = (ImageView) findViewById(R.id.imgDisplayLikes);
        imgDisplayLikes.setOnClickListener(this);
        if (!manager.isServer)
            imgPlayPause.setVisibility(View.INVISIBLE);
        adapter = new SongListAdapter(searchTracks);
        trackView.setAdapter(adapter);

        // buildIcon();
    }

    private void update() {
        runThread = true;
        Thread thread = new Thread() {
            public void run() {
                while (runThread && manager != null) {
                    if (manager.isServer && (manager.mediaManager.getSpotifyPlayer() == null || manager.mediaManager.getSpotifyPlayer().isShutdown()) && !isConnectingToSpotify) {
                        if (!isConnectingToSpotify)
                            requestSpotify();
                        isConnectingToSpotify = true;
                    }

                    try {
                        JSONObject serverInfo = new JSONObject(manager.getServerInfo(manager.getHostKey()));

                        if (!manager.isServer && runThread) {
                            if (serverInfo.getBoolean("stoped"))
                                leaveSession("host");
                            if (serverInfo.getInt("serverIteration") != manager.currentIteration) {
                                manager.currentIteration = serverInfo.getInt("serverIteration");
                                restartClient();
                            }
                            if(!serverInfo.getString("currentPlayingSongId").equals("-1")){
                                if(manager.mediaManager.getCurrentPlayingTrack() == null || !serverInfo.getString("currentPlayingSongId").equals(manager.mediaManager.getCurrentPlayingTrack().getTrackId())){
                                    manager.mediaManager.setCurrentPlayingTrack(Track.getTrack(serverInfo.getString("currentPlayingSongId")));
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            imgSong.setVisibility(View.VISIBLE);
                                            imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
                                            if(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap() != null)
                                                imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                                            else
                                                manager.mediaManager.getCurrentPlayingTrack().setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                                                    @Override
                                                    public void onLoaded(boolean loaded) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                imgSong.setImageBitmap(manager.mediaManager.getCurrentPlayingTrack().getTrackBitmap());
                                                            }
                                                        });
                                                    }
                                                });
                                        }
                                    });
                                }
                                final boolean isPaused = serverInfo.getBoolean("currentSongPaused");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imgPlayPause.setVisibility(View.VISIBLE);
                                        imgPlayPause.setImageBitmap((isPaused) ? BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_play) : BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_pause));
                                    }
                                });
                            }
                        }
                        manager.filterExplicit = serverInfo.getBoolean("filterExplicit");
                        JSONArray users = serverInfo.getJSONArray("users");
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            final String username = user.getString("userName");
                            if (!usernames.contains(username)) {
                                if (usernames.size() != 0) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(DynamicDjSession.this, username + " has joined", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                usernames.add(username);
                            }

                            String chosenSongId = user.getString("chosenSongId");
                            if (!chosenSongId.equals("-1")) {
                                int currentTrackIteration;
                                try {
                                    currentTrackIteration = Integer.parseInt(chosenSongId.split("ITE")[1]);
                                    if (currentTrackIteration == manager.currentIteration) {
                                        String trackId = chosenSongId.split("ITE")[0];
                                        if (!trackId.equals("-1") && !hasTrackId(trackId) && runThread) {
                                            boolean add = true;
                                            final Track addTrack = Track.getTrack(trackId, username);
                                            for (int x = 0; x < manager.currentChosenTracks.size(); x++) {
                                                Track testTrack = manager.currentChosenTracks.get(x);
                                                if (testTrack.getUserSubmited() == null)
                                                    continue;
                                                assert addTrack != null;
                                                if (testTrack.getUserSubmited().equals(addTrack.getUserSubmited())) {
                                                    add = false;
                                                    break;
                                                }

                                            }
                                            if (add) {
                                                manager.currentChosenTracks.add(addTrack);
                                                assert addTrack != null;
                                                addTrack.setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                                                    @Override
                                                    public void onLoaded(boolean loaded) {
                                                        DynamicDjSession.this.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                adapter.notifyDataSetChanged();
                                                                if (manager.mediaManager.isSongPlaying() && manager.mediaManager.getCurrentPlayingTrack() == addTrack)
                                                                    imgSong.setImageBitmap(addTrack.getTrackBitmap());
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                            if (!runThread)
                                                manager.currentChosenTracks.clear();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
                            Track track = manager.currentChosenTracks.get(i);
                            int vCount = 0;
                            for (int x = 0; x < users.length(); x++) {
                                JSONObject user = users.getJSONObject(x);
                                try {
                                    String voteId = user.getString("votedSongId");
                                    if (voteId.equals(track.getTrackId()))
                                        vCount++;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (track.getVotes() != vCount) {
                                track.setVote(vCount);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }

                    } catch (Exception e) {
                        leaveSession("server");
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
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            String chosenTrackId = manager.currentChosenTracks.get(i).getTrackId();
            if (trackId.equals(chosenTrackId))
                return true;
        }
        return false;
    }

    private void setPlaylist(final String playlistId) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                manager.mediaManager.setBackupPlaylist(Playlist.getPlaylist(playlistId));
            }
        };
        thread.start();
    }

    private void buildIcon() {
        Intent buttonsIntent = new Intent(this, NotifyActivityHandler.class);
        PendingIntent bIntent = PendingIntent.getActivity(this, 0, buttonsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, NotifyActivityHandler.class);
        pauseIntent.setAction("pause");
        PendingIntent pPauseIntent = PendingIntent.getActivity(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Invoking the default notification service */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("New Message");
        mBuilder.setContentText("You've received new message.");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentIntent(pPauseIntent);
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


    private void searchTrack(final String search) {
        if (manager.hasUserChoseSong)
            return;
        if (search.length() == 0) {
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
        Thread searchTrackThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    if (!search.equals(txtSongSearch.getText().toString()))
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

                                    if (!search.equals(txtSongSearch.getText().toString()))
                                        return;

                                    JSONObject js = jArray.getJSONObject(i);
                                    if (i == 0)
                                        searchTracks.clear();
                                    Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                                    Track t = new Track(js);
                                    if (manager.hasUserChoseSong)
                                        break;

                                    if(t.isExplicit() && manager.filterExplicit)
                                        continue;

                                    t.setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                                        @Override
                                        public void onLoaded(boolean loaded) {
                                            if (!loaded)
                                                return;
                                            DynamicDjSession.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    });
                                    searchTracks.add(t);

                                    adapter.notifyDataSetChanged();
                                }
                                Manager.manager.isDisplayingSpotifyLikes = false;
                                manager.currentSpotifyOffset = 0;

                                pbSearching.setVisibility(View.INVISIBLE);
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                                error("An error ocured while searching for songs");
                            }

                            manager.isUserSearching = false;
                            pbSearching.setVisibility(View.INVISIBLE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pbSearching.setVisibility(View.INVISIBLE);
                        }
                    });
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
        if (manager.isServer) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    manager.sendStopedSession(manager.getHostKey());
                }
            });
            thread.start();
        }

        if (manager.mediaManager != null)
            manager.mediaManager.stop();
        this.startActivity(new Intent(this, MainMenu.class).putExtra("reason", reason));
        manager = new Manager();
        manager.currentChosenTracks.clear();
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
        } else {
            final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
            errorDialog.setTitle("You need to link your Spotify account");
            errorDialog.setPositiveButton("Link", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(DynamicDjSession.this, SettingsSession.class).putExtra("reason", "link"));
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

        Thread playSongThread = new Thread() {
            @Override
            public void run() {
                if (manager.mediaManager.isSongPlaying()) {
                    error("A song is already playing");
                    return;
                }
                if (count == 0 && manager.mediaManager.getCurrentBackupType() == MediaManager.BACKUP_TYPE.UNDEFINED) {
                    error("No chosen songs to play");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_play));
                        }
                    });
                    return;
                }
                if (manager.mediaManager.getCurrentBackupType() != MediaManager.BACKUP_TYPE.UNDEFINED && count == 0) {
                    if (!manager.mediaManager.playBackup()) {
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
                        startService(new Intent(DynamicDjSession.this, PlayAudio.class).setData(Uri.parse("")));
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
                    } else {
                        manager.currentIteration++;
                        startService(new Intent(DynamicDjSession.this, PlayAudio.class).setData(Uri.parse("")));
                    }
                }
                manager.sendCurrentSong(manager.getHostKey(), manager.mediaManager.getCurrentPlayingTrack().getTrackId(), manager.mediaManager.isSongPaused());
                manager.mediaManager.setOnFinishedPlaying(new MediaManager.onFinishedPlaying() {
                    @Override
                    public void finishedPlaying(MediaManager mm) {
                        if (!manager.mediaManager.isSongPlaying())
                            startSong();
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count != 0 || manager.mediaManager.getCurrentBackupType() == MediaManager.BACKUP_TYPE.TRACK) {
                            finishSongLoading(manager.mediaManager.getCurrentPlayingTrack());
                        } else {
                            manager.mediaManager.getCurrentPlayingTrack().setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                                @Override
                                public void onLoaded(boolean loaded) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                       finishSongLoading(manager.mediaManager.getCurrentPlayingTrack());
                                        }
                                    });
                                }
                            });
                        }

                    }
                });
            }
        };
        playSongThread.start();
    }

    private void finishSongLoading(Track t){
        if(imgSong == null)
            return;
        imgSong.setImageBitmap(t.getTrackBitmap());
        imgSong.getLayoutParams().width = imgSong.getLayoutParams().height = 200;
        imgSong.setVisibility(View.VISIBLE);
        imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_pause));
        Thread thread  = new Thread(new Runnable() {
            @Override
            public void run() {
                manager.sendCurrentSong(manager.getHostKey(), manager.mediaManager.getCurrentPlayingTrack().getTrackId(), manager.mediaManager.isSongPaused());
            }
        });
        thread.start();
        pbSearching.setVisibility(View.INVISIBLE);
        restart();
    }

    private void displayLikes(final int limit, final int offset) {
        if (manager.isUserSearching || manager.hasUserChoseSong)
            return;
        adapter.notifyDataSetChanged();
        manager.isUserSearching = true;

        pbSearching.setVisibility(View.VISIBLE);
        Thread displayLikesThread = new Thread() {
            @Override
            public void run() {
                try {
                    String search = manager.mediaManager.getSpotifySearch().getSavedTracks(offset, limit);
                    JSONObject fullJson = new JSONObject(search);
                    JSONArray jArray = fullJson.getJSONArray("items");
                    for (int i = 0; i < jArray.length(); i++) {

                        if (manager.hasUserChoseSong)
                            break;
                        JSONObject js = jArray.getJSONObject(i);
                        final JSONObject track = js.getJSONObject("track");
                        if (i == 0 && offset == 0)
                            searchTracks.clear();

                        if (manager.hasUserChoseSong)
                            break;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Track t = new Track(track);
                                t.setOnLoadedArtworkListener(new Track.onLoadedArtwork() {
                                    @Override
                                    public void onLoaded(boolean loaded) {
                                        if (!loaded)
                                            return;
                                        DynamicDjSession.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                });
                                searchTracks.add(t);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }

                    Manager.manager.isDisplayingSpotifyLikes = true;
                } catch (Exception e) {
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
                songInfo = new AlertDialog.Builder(DynamicDjSession.this).setTitle("Song Info:");
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.info_view, null);
                songInfo.setView(dialogView);

                final AlertDialog shown = songInfo.show();
                Track clickedTrack = (manager.hasUserChoseSong) ? manager.currentChosenTracks.get(position) : searchTracks.get(position);
                if (clickedTrack == null || clickedTrack.getTrackBitmap() == null)
                    return false;
                ImageView imgArtwork = (ImageView) dialogView.findViewById(R.id.imgPlaylistArtwork);
                imgArtwork.setImageBitmap(clickedTrack.getTrackBitmap());
                TextView txtName = (TextView) dialogView.findViewById(R.id.txtPlaylistName);
                txtName.setText("Name: " + clickedTrack.getTrackTitle());
                TextView txtArtists = (TextView) dialogView.findViewById(R.id.txtPlaylistArtists);

                txtArtists.setText("Artists: " + clickedTrack.getArtistString());
                TextView txtAlbumName = (TextView) dialogView.findViewById(R.id.txtAlbumName);
                txtAlbumName.setText("Album: " + clickedTrack.getAlbumName());
                txtAlbumName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shown.dismiss();
                    }
                });
                TextView txtDuration = (TextView) dialogView.findViewById(R.id.txtPlaylistDuration);
                txtDuration.setText("Duration: " + clickedTrack.getDurationToTime());

                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == imgPlayPause && manager.mediaManager.getCurrentPlayingTrack() != null) {
            if (manager.mediaManager.isSongPaused())
                manager.mediaManager.startSong();
            else
                manager.mediaManager.pauseSong();
            imgPlayPause.setImageBitmap((manager.mediaManager.isSongPaused()) ? BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_play) : BitmapFactory.decodeResource(DynamicDjSession.this.getResources(), R.mipmap.ic_pause));
            Thread thread  = new Thread(new Runnable() {
                @Override
                public void run() {
                    manager.sendCurrentSong(manager.getHostKey(), manager.mediaManager.getCurrentPlayingTrack().getTrackId(), manager.mediaManager.isSongPaused());
                }
            });
            thread.start();
        } else if (v == imgControl)
            showControlPopup(v);
        else if (v == imgDisplayLikes)
            loadFavorites();
    }

    private void requestSpotify() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(MediaManager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, MediaManager.REDIRECT_URI);
        builder.setScopes(Manager.SPOTIFY_SCOPES);
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
            Toast.makeText(this, "You can not vote for the song you chose!", Toast.LENGTH_SHORT).show();
            return;
        }

        Thread thread = new Thread() {
            public void run() {
                manager.currentUser.setVotedTrack(manager.currentChosenTracks.get(position));
                manager.sendUserVotedSong(manager.getHostKey(), manager.currentUser);
                manager.hasUserVotedForSong = true;
            }
        };
        thread.start();
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

        private SongListAdapter(ArrayList<Track> addapterTracks) {
            super(DynamicDjSession.this, R.layout.song_view, addapterTracks);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.song_view, parent, false);

            if ((manager.currentChosenTracks.size() == 0 && manager.hasUserChoseSong) || (searchTracks.size() == 0 && !manager.hasUserChoseSong) || (position >= manager.currentChosenTracks.size() && manager.hasUserChoseSong) || (position >= searchTracks.size() && !manager.hasUserChoseSong))
                return itemView;

            Track currentTrack = (manager.hasUserChoseSong) ? manager.currentChosenTracks.get(position) : searchTracks.get(position);
            TextView txtExplicit = (TextView) itemView.findViewById(R.id.txtExplicit);
            txtExplicit.setText("");
            ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
            trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
            if (trackIcon.getLayoutParams().width != 250 || trackIcon.getLayoutParams().height != 250)
                trackIcon.getLayoutParams().width = trackIcon.getLayoutParams().height = 250;

            TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
            trackName.setText(currentTrack.getTrackTitle());
            TextView trackInfo = (TextView) itemView.findViewById(R.id.txtTime);
            trackInfo.setText((manager.hasUserChoseSong) ? "Votes: " + currentTrack.getVotes() : currentTrack.getDurationToTime());

            if (currentTrack.isExplicit())
                txtExplicit.setText(R.string.explicit);

            if (currentTrack.getUserSubmited() != null && manager.hasUserChoseSong) {
                TextView submittedUser = (TextView) itemView.findViewById(R.id.txtSubmitedby);
                if (submittedUser.getVisibility() == View.INVISIBLE)
                    submittedUser.setVisibility(View.VISIBLE);
                String chosenBy = getString(R.string.chosenby) + currentTrack.getUserSubmited();
                submittedUser.setText(chosenBy);
            }

            return itemView;
        }

    }


}
