package zein.net.tune_in;

import android.util.Log;

import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONObject;


public class MediaManager {

    //Static Variables that have regard to Spotify
    public static final String SPOTIFY_CLIENT_ID = "d6d5380ae25943f9ba03d499b0260675";
    public static final String REDIRECT_URI = "tunein://callback";
    public static final int REQUEST_CODE = 1337;

    //If a song is playing is not based on whether or not the song is paused or not
    private boolean isSongPlaying;

    //Used to check if the song is paused or played
    private boolean isSongPaused;

    //The current playing track that is not based on whether the song is paused or not
    private Track currentPlayingTrack;

    private Player spotifyPlayer;

    //Event used to handle when the current playing song has been finished
    private onFinishedPlaying oFP;

    //Event used to handle when the song to play is done
    private onPreparedSong oPS;

    //The thread that handles when a Spotify song ends
    private Thread spotifyThread;

    //Class that holds method to search Spotify
    private SpotifySearch spSearch = new SpotifySearch();

    //The playlist to play from if no song is chosen
    private Playlist backupPlaylist;

    //Choose randomly from the backup playlist or in order
    private boolean choosingRandomlyFromBp = false;

    //The current song the bp is on
    private int currentBpSong = 0;

    public MediaManager() {
        isSongPlaying = false;
    }

    /**
     * Finds out if the song is a Soundcloud or Spotify song
     * Starts playing the song
     * Uses MediaPlayer or Spotify api to see when the song ends
     *
     * @param track The track to play
     * @return Returns True if the song played correctly or false if it did not
     */
    public boolean playSong(Track track) {
        String trackId = track.getTrackId();
        if (spotifyPlayer == null || spotifyPlayer.isShutdown())
            return false;
        spotifyPlayer.play("spotify:track:" + trackId);
        isSongPlaying = true;
        isSongPaused = false;
        currentPlayingTrack = track;
        spotifyThread = new Thread() {
            @Override
            public void run() {
                while (spotifyPlayer != null && !spotifyPlayer.isShutdown()) {
                    if(!isSongPlaying){
                        Log.d("TUNEIN", "So this just happened");
                        return;
                    }

                    spotifyPlayer.getPlayerState(new PlayerStateCallback() {
                        @Override
                        public void onPlayerState(PlayerState playerState) {
                            if(!isSongPlaying)
                                return;
                            if (Math.abs(playerState.positionInMs - playerState.durationInMs) < 700 && (playerState.positionInMs != 0 || playerState.durationInMs != 0)) {
                                completedSong();
                                spotifyThread = null;
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        spotifyThread.start();
        return true;
    }

    public boolean playPlaylist() {
        try {
            currentBpSong = (choosingRandomlyFromBp) ? ((int) (Math.random() * backupPlaylist.getTrackCount())) : ((currentBpSong != backupPlaylist.getTrackCount()) ? currentBpSong++ : 0);
            Track track = new Track(new JSONObject(spSearch.getPlaylistTrack(backupPlaylist.getTrackURL(), currentBpSong)));
            return playSong(track);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Is called when the song is completed
     * calls the on finished playing event
     */
    private void completedSong() {
        isSongPlaying = false;
        isSongPaused = false;
        oFP.finishedPlaying(this);
        currentPlayingTrack = null;
    }


    public boolean isSongPlaying() {
        return isSongPlaying;
    }

    public boolean isSongPaused() {
        return isSongPaused;
    }

    public void setSpotifyPlayer(Player player) {
        this.spotifyPlayer = player;
    }

    public void setBackupPlaylist(Playlist backupPlaylist) {
        this.backupPlaylist = backupPlaylist;
    }

    public void setChoosingRandomlyFromBp(boolean choosingRandomlyFromBp) {
        this.choosingRandomlyFromBp = choosingRandomlyFromBp;
    }

    public Player getSpotifyPlayer() {
        return spotifyPlayer;
    }

    public Playlist getBackupPlaylist() {
        return backupPlaylist;
    }

    public SpotifySearch getSpotifySearch() {
        return spSearch;
    }

    public boolean isChoosingRandomlyFromBp() {
        return choosingRandomlyFromBp;
    }

    public void setOnFinishedPlaying(onFinishedPlaying oFP) {
        this.oFP = oFP;
    }

    public void setOnPreparedSong(onPreparedSong oPS) {
        this.oPS = oPS;
    }

    public void preparedSong(Track track) {
        if(oPS != null)
            oPS.preparedSong(track);
    }

    public Track getCurrentPlayingTrack() {
        return currentPlayingTrack;
    }

    /**
     * Stops the music
     * shutdowns the Soundcloud and Spotify players
     */
    public void stop() {
        if (spotifyPlayer != null) {
            spotifyPlayer.pause();
            spotifyPlayer.shutdown();
            Spotify.destroyPlayer(spotifyPlayer);
            spotifyPlayer = null;
        }
    }

    /**
     * Pauses the song
     *
     * @return Returns true if the song was paused correctly
     */
    public boolean pauseSong() {
        Log.d("TUNEIN", "This was called");
        if (!isSongPlaying || isSongPaused || currentPlayingTrack == null)
            return false;
        if (spotifyPlayer != null && !spotifyPlayer.isShutdown())
            spotifyPlayer.pause();
        Log.d("TUNEIN", "GOT HERE");
        isSongPaused = true;
        return true;
    }

    /**
     * Resumes the song
     *
     * @return Returns true if the song was resumed correctly
     */
    public boolean startSong() {
        if (!isSongPlaying || !isSongPaused || currentPlayingTrack == null)
            return false;

        if (spotifyPlayer != null && !spotifyPlayer.isShutdown())
            spotifyPlayer.resume();
        isSongPaused = false;
        return !isSongPaused;
    }

    /**
     * The on Finished Playing Event
     * This event is called when a song is finished playing
     */
    public interface onFinishedPlaying {
        void finishedPlaying(MediaManager mm);
    }

    /**
     * Called when the song to play is prepared
     * Only really matters when playing a song from a playlist
     */
    public interface onPreparedSong {
        void preparedSong(Track t);
    }

}




