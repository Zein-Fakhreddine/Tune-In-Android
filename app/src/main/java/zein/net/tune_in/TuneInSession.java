package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;

public class TuneInSession extends Activity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener {

    private ListView trackView;
    private ProgressBar pbSearching;
    private ImageView imgSong, imgPlayPause, imgSpotify, imgSoundcloud;

    //Tracks
    private ArrayAdapter<Track> adapter;
    private boolean doubleBackToExitPressedOnce;

    private boolean runThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_menu);
        initManager();
        initView();
        update();
    }

    private void initManager() {
        TextView txtSessionName = (TextView) findViewById(R.id.txtSessionName);
        txtSessionName.append(" " + manager.sessionName);
    }

    private void initView() {
        trackView = (ListView) findViewById(R.id.lstTracks);
        registerListCallBack();
        pbSearching = (ProgressBar) findViewById(R.id.pbSearching);
        TextView txtKey = (TextView) findViewById(R.id.txtKey);
        txtKey.setText("KEY: " + manager.getHostKey());
        imgSong = (ImageView) findViewById(R.id.imgSong);
        imgPlayPause = (ImageView) findViewById(R.id.imgPlayPause);
        imgPlayPause.setOnClickListener(this);
        imgSpotify = (ImageView) findViewById(R.id.imgSpotify);
        imgSpotify.setOnClickListener(this);
        imgSoundcloud = (ImageView)findViewById(R.id.imgSouncloud);
        imgSoundcloud.setOnClickListener(this);

        if(manager.currentSearchType == Track.TRACK_TYPE.SOUNDCLOUD){
            imgSpotify.setAlpha(.5f);
            imgSoundcloud.setAlpha(1f);
        } else{
            imgSoundcloud.setAlpha(.5f);
            imgSpotify.setAlpha(1f);
        }
        if(!manager.isServer)
            imgPlayPause.setVisibility(View.INVISIBLE);
        adapter = new SongListAdapter(Manager.manager.currentSearchTracks);
        trackView.setAdapter(adapter);
    }

    private void update() {
        runThread = true;
        final android.os.Handler handler = new android.os.Handler();

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

        Thread thread  = new Thread() {
            public void run() {
                while (runThread && manager != null) {
                    if (manager.hasUserChoseSong || manager.isServer) {
                        String[] info = manager.getChosenSongs(manager.getHostKey()).split(",");
                        if (!manager.isServer) {
                            String message = manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
                            Log.d("TUNEIN", "The message is: " + message);
                            if (message.equals("restart"))
                                restartClient();
                        }
                        for (int i = 0; i < info.length; i++) {
                            if(!info[i].equals("-1")) {
                                Log.d("TUNEIN", "Info: "+ info[i]);
                                int currentTrackIteration = Integer.parseInt(info[i].split("ITE")[1]);
                                if (currentTrackIteration == manager.currentIteration) {
                                    Log.d("TUNEIN", "Adding track with iteration: " + currentTrackIteration + " and manager iteration: " + manager.currentIteration);
                                    String trackId = info[i].split("ITE")[0];
                                    Log.d("TUNEIN", "Found track with id: " + trackId);
                                    if (!(trackId == "-1" || trackId == "0")) {
                                        if (!hasTrackId(trackId)){
                                            if(isInteger(trackId))
                                                manager.currentChosenTracks.add(Track.getTrack(trackId, Track.TRACK_TYPE.SOUNDCLOUD));
                                            else
                                                manager.currentChosenTracks.add(Track.getTrack(trackId, Track.TRACK_TYPE.SPOTIFY));
                                        }

                                    }
                                }
                            }
                        }
                        String[] votesIds = manager.getVotes(manager.getHostKey()).split(",");
                        for (int i = 0; i < votesIds.length; i++) {
                            try{
                                int voteId = Integer.parseInt(votesIds[i]);
                                for (int x = 0; x < manager.currentChosenTracks.size(); x++) {
                                    Track track = manager.currentChosenTracks.get(x);
                                    track.setVote(0);
                                    if (track.getTrackId() == String.valueOf(voteId))
                                        track.addVote();
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try{
                        Thread.sleep(500);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    public  boolean isInteger(String str) {
        if (str == null)
            return false;
        int length = str.length();
        if (length == 0)
            return false;
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1)
                return false;
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
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
                        Log.d("TUNEIN", "Connected");
                        manager.isLinkedWithSpotify = true;
                        manager.currentSearchType = Track.TRACK_TYPE.SPOTIFY;
                        imgSoundcloud.setAlpha(.5f);
                        imgSpotify.setAlpha(1f);
                        pbSearching.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("TUNEIN", "Could not initialize player: " + throwable.getMessage());
                        error("Unexpected error occurred while connecting to Spotify");
                    }
                });
            }
        }
    }
    private boolean hasTrackId(String trackId) {
        boolean hasTrackId = false;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            String chosenTrackId = manager.currentChosenTracks.get(i).getTrackId();
            Log.d("TUNEIN", "The track id is: " + trackId + " and the chosen track id is: " + chosenTrackId);
            if (trackId.equals(chosenTrackId)){
                hasTrackId = true;
                break;
            }
        }
        return hasTrackId;
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

    private void searchTrack(String search) {
        Intent trackSearch;
        Log.d("TUNEIN", "Called with search term: " + search);

        trackSearch = new Intent(this, TrackSearch.class);
        trackSearch.setData(Uri.parse(search + ":" + ((manager.currentSearchType == Track.TRACK_TYPE.SOUNDCLOUD) ? "sc" : "sp")));
        this.startService(trackSearch);

        manager.isUserSearching = true;
        pbSearching.setVisibility(View.VISIBLE);
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
                }
                else if (manager.isServer)
                    startSong();
                else
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

    private void endSession(){
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

    private void leaveSession(){
        if(manager.mediaPlayer != null){
            manager.mediaPlayer.stop();
            manager.mediaPlayer.release();
            manager.mediaPlayer = null;
        }

        this.startActivity(new Intent(this, MainMenu.class));
        runThread = false;
        manager = new Manager();
        finish();
    }

    private void loadFavorites() {
        if (manager.currentUser.getSoundcloudUser() == null) {
            AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
            errorDialog.setTitle("You have not linked your Soundcloud account yet.");
            errorDialog.setPositiveButton("Link", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    loadSettings();
                }
            });
            errorDialog.setNegativeButton("Cancel", null);
            errorDialog.show();
        }
    }

    private void loadSettings(){
        Intent settingsSession = new Intent(this, SettingsSession.class);
        settingsSession.putExtra("Link", true);
        this.startActivity(settingsSession);
    }

    private void startSong() {
        if (manager.currentChosenTracks.size() == 0) {
            error("No chosen songs to play");
            return;
        }
        if (manager.mediaPlayer != null && manager.mediaPlayer.isPlaying()) {
            error("A song is already playing");
            return;
        }

        manager.currentIteration++;

        Track trackToPlay = null;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            Track track = manager.currentChosenTracks.get(i);
            Log.d("TUNEIN", "Song:" + track.getTrackTitle() + " has: " + track.getVotes() + "votes");
            if (trackToPlay == null || track.getVotes() > trackToPlay.getVotes())
                trackToPlay = track;
        }
        manager.currentPlayingTrack = trackToPlay;

        if(trackToPlay.getTrackType() == Track.TRACK_TYPE.SOUNDCLOUD) {
            manager.mediaPlayer = new MediaPlayer();
            manager.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    startSong();
                }
            });
        } else{
            //TODO: Add a way to detect when the Spotify song finishes
            replaySpotifySong(manager.currentPlayingTrack.getDuration());
        }
        imgSong.setImageBitmap(manager.currentPlayingTrack.getTrackBitmap());
        imgSong.getLayoutParams().width = 200;
        imgSong.getLayoutParams().height = 200;
        imgSong.setVisibility(View.VISIBLE);
        imgPlayPause.setImageBitmap(BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause));
        Intent playAudio = new Intent(this, PlayAudio.class);

        playAudio.setData(Uri.parse(""));
        this.startService(playAudio);

        pbSearching.setVisibility(View.VISIBLE);

        restart();
    }

    private void replaySpotifySong(int waitTime){
        new CountDownTimer(waitTime, 1000) {
            int pausedTime = 0;
            public void onTick(long millisUntilFinished) {
                if(!manager.isTrackPlaying)
                    pausedTime++;
            }

            public void onFinish() {
                Log.d("TUNEIN", "Finished");
                if(pausedTime == 0)
                    startSong();
                else
                    replaySpotifySong(pausedTime);
            }
        }.start();

    }

    private void restart() {
        manager.hasUserChoseSong = false;
        manager.hasUserVotedForSong = false;
        adapter = new SongListAdapter(
                manager.currentSearchTracks);
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
                        manager.currentSearchTracks);
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
                    Thread thread = new Thread() {
                        public void run() {
                            manager.currentUser.setChosenTrack(manager.currentSearchTracks.get(position));
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
        if(v.getId() == imgPlayPause.getId()){
            if(manager.currentPlayingTrack != null){
                if(manager.mediaPlayer != null && manager.currentPlayingTrack.getTrackType() == Track.TRACK_TYPE.SOUNDCLOUD){
                    if(manager.mediaPlayer.isPlaying()){
                        manager.mediaPlayer.pause();
                        manager.isTrackPlaying = false;
                    }
                    else{
                        manager.mediaPlayer.start();
                        manager.isTrackPlaying = true;
                    }
                } else if(manager.currentPlayingTrack.getTrackType() == Track.TRACK_TYPE.SPOTIFY && manager.spotifyPlayer != null){
                    if(manager.isTrackPlaying){
                        manager.spotifyPlayer.pause();
                        manager.isTrackPlaying = false;
                    } else{
                        manager.spotifyPlayer.resume();
                        manager.isTrackPlaying = true;
                    }
                }

                imgPlayPause.setImageBitmap((manager.isTrackPlaying) ? BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause) : BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_play));
            }

        }
        if(v.getId() == imgSoundcloud.getId()){
            if(manager.currentSearchType != Track.TRACK_TYPE.SOUNDCLOUD){
                manager.currentSearchType = Track.TRACK_TYPE.SOUNDCLOUD;
                imgSpotify.setAlpha(.5f);
                imgSoundcloud.setAlpha(1f);
            }

        }
        if(v.getId() == imgSpotify.getId()){
            if(manager.currentSearchType != Track.TRACK_TYPE.SPOTIFY){
                if(manager.spotifyPlayer == null){
                    pbSearching.setVisibility(View.VISIBLE);
                    AuthenticationRequest.Builder builder =
                            new AuthenticationRequest.Builder(manager.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, manager.REDIRECT_URI);
                    builder.setScopes(new String[]{"user-library-read", "streaming"});
                    AuthenticationRequest request = builder.build();

                    AuthenticationClient.openLoginActivity(this, manager.REQUEST_CODE, request);
                } else{
                    manager.currentSearchType = Track.TRACK_TYPE.SPOTIFY;
                    imgSoundcloud.setAlpha(.5f);
                    imgSpotify.setAlpha(1f);
                }

            }
        }
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
        AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(errorMessage);
        errorDialog.setPositiveButton("OK", null);
        errorDialog.setNegativeButton("Cancel", null);
        errorDialog.show();
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

                if(position >= manager.currentChosenTracks.size())
                    return itemView;

                Track currentTrack = manager.currentChosenTracks.get(position);

                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
                if(trackIcon.getLayoutParams().width != 250 || trackIcon.getLayoutParams().height != 250) {
                    trackIcon.getLayoutParams().width = 250;
                    trackIcon.getLayoutParams().height = 250;
                }
                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText("Votes: "+ currentTrack.getVotes());
            } else {
                if (Manager.manager.currentSearchTracks.size() == 0)
                    return itemView;

                Track currentTrack = Manager.manager.currentSearchTracks.get(position);

                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());

                if(trackIcon.getLayoutParams().width != 250 || trackIcon.getLayoutParams().height != 250) {
                    trackIcon.getLayoutParams().width = 250;
                    trackIcon.getLayoutParams().height = 250;
                }

                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText("Playback count: " + currentTrack.getPlaybackCount());
            }
            return itemView;
        }
    }

}
