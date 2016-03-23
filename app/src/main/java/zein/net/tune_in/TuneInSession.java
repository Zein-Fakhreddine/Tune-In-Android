package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;

/**
 * Created by Zein's on 2/6/2016.
 */
public class TuneInSession extends Activity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener {


    private ListView trackView;
    private ProgressBar pbSearching;
    private ImageView imgSong, imgPlayPause;

    //Tracks
    private ArrayAdapter<Track> adapter;
    private boolean doubleBackToExitPressedOnce;

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
        if(!manager.isServer)
            imgPlayPause.setVisibility(View.INVISIBLE);
        adapter = new SongListAdapter(Manager.manager.currentSearchTracks);
        trackView.setAdapter(adapter);
    }

    private void update() {
        final android.os.Handler handler = new android.os.Handler();

        final Runnable r = new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
                if (!manager.isUserSearching)
                    pbSearching.setVisibility(View.INVISIBLE);
                if (manager.hasUserChoseSong)
                    pbSearching.setVisibility((manager.currentChosenTracks.size() == 0) ? View.VISIBLE : View.INVISIBLE);

                if(manager.mediaPlayer != null){
                    if(manager.mediaPlayer.isPlaying())

                    imgPlayPause.setImageBitmap((manager.mediaPlayer.isPlaying()) ? BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_pause) : BitmapFactory.decodeResource(TuneInSession.this.getResources(), R.mipmap.ic_play));
                }

                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    if (manager.hasUserChoseSong) {
                        if (!manager.isServer) {
                            String message = manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
                            Log.d("TUNEIN", "The message is: " + message);
                            if (message.equals("restart"))
                                restartClient();
                        }
                        String[] info = manager.getChosenSongs(manager.getHostKey()).split(",");

                        for (int i = 0; i < info.length; i++) {
                            int trackId = Integer.parseInt(info[i]);
                            Log.d("TUNEIN", "Found track with id: " + trackId);
                            if (!(trackId == -1 || trackId == 0)) {
                                if (!hasTrackId(trackId)) {
                                    manager.currentChosenTracks.add(new Track(SoundcloudSearch.getTrack("7c89e606e88c94ff47bfd84357e5e9f4", trackId)));
                                }
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


    private boolean hasTrackId(int trackId) {
        boolean hasTrackId = false;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            int chosenTrackId = manager.currentChosenTracks.get(i).getTrackId();
            if (trackId == chosenTrackId) {
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
        // Set up the buttons
        searchDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
        trackSearch.setData(Uri.parse(search));
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

                if (manager.isServer) {
                    startSong();
                } else
                    error("You have to be the host to start the session!");
                break;
            case R.id.settings_id:
                this.startActivity(new Intent(this, SettingsSession.class));
        }
        return false;
    }

    private void loadFavorites() {
        if (manager.currentUser.getSoundcloudUser() == null) {
            error("You have not linked your Soundcloud acount yet");
            return;
        }
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
        manager.mediaPlayer = new MediaPlayer();
        manager.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                startSong();
            }
        });
        manager.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (manager.currentPlayingTrack != null) {
                    imgSong.setImageBitmap(manager.currentPlayingTrack.getTrackBitmap());
                    imgSong.getLayoutParams().width = 200;
                    imgSong.getLayoutParams().height = 200;
                    imgSong.setVisibility(View.VISIBLE);
                }
            }
        });
        Intent playAudio = new Intent(this, PlayAudio.class);

        playAudio.setData(Uri.parse(""));
        this.startService(playAudio);

        pbSearching.setVisibility(View.VISIBLE);

        restart();
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
        this.runOnUiThread(new Runnable() {
            public void run() {
                manager.currentChosenTracks.clear();
                manager.hasUserChoseSong = false;
                manager.hasUserVotedForSong = false;
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
            if(manager.mediaPlayer != null){
                if(manager.mediaPlayer.isPlaying())
                    manager.mediaPlayer.pause();
                else
                    manager.mediaPlayer.start();
            }
        }
    }

    private void voteForSong(final int position) {
        if (manager.currentUser.getChosenTrack().getTrackId() == manager.currentChosenTracks.get(position).getTrackId()) {
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

                Track currentTrack = Manager.manager.currentChosenTracks.get(position);

                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
                trackIcon.getLayoutParams().width = 250;
                trackIcon.getLayoutParams().height = 250;

                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText("3");
            } else {
                if (Manager.manager.currentSearchTracks.size() == 0)
                    return itemView;

                Track currentTrack = Manager.manager.currentSearchTracks.get(position);

                ImageView trackIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
                trackIcon.setImageBitmap(currentTrack.getTrackBitmap());
                trackIcon.getLayoutParams().width = 250;
                trackIcon.getLayoutParams().height = 250;

                TextView trackName = (TextView) itemView.findViewById(R.id.txtSongName);
                trackName.setText(currentTrack.getTrackTitle());
                TextView trackTime = (TextView) itemView.findViewById(R.id.txtTime);
                trackTime.setText("2");
            }
            return itemView;
        }
    }

}
