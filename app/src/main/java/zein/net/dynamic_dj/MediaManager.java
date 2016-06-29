package zein.net.dynamic_dj;

import android.media.AudioManager;
import android.util.Log;

import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

import org.json.JSONObject;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;


public class MediaManager {

    //Backup types that can be played if no song is chosen
    public enum BACKUP_TYPE{
        PLAYLIST,
        ALBUM,
        TRACK,
        UNDEFINED
    };

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

    //The thread that handles when a Spotify song ends
    private Thread spotifyThread;

    //Class that holds method to search Spotify
    private SpotifySearch spSearch = new SpotifySearch();

    //The playlist to play from if no song is chosen
    private Playlist backupPlaylist;

    //The Album to play from if no song is chosen
    private  Album backupAlbum;

    //The track to play from if no song is chosen
    private Track backupTrack;

    //Choose randomly from the backup playlist or in order
    private boolean choosingRandomlyFromBp = false;

    //The current song the bp is on
    private int currentBpSong = 0;

    //The current backup type that the user has chose
    private BACKUP_TYPE currentBackupType = BACKUP_TYPE.UNDEFINED;

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
       AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener(){
           @Override
           public void onAudioFocusChange(int focusChange) {
               Log.d("TUNEIN", "Audio focus changed");
               if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                   pauseSong();
               } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                   startSong();
               } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                   pauseSong();
               }
           }
       };

        spotifyThread = new Thread() {
            @Override
            public void run() {
                while (spotifyPlayer != null && !spotifyPlayer.isShutdown()) {
                    if(!isSongPlaying)
                        return;

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

    private boolean playPlaylist() {
        try {
            Track track = new Track(new JSONObject(spSearch.getPlaylistTrack(backupPlaylist.getTrackURL(), (choosingRandomlyFromBp) ? ((int) (Math.random() * backupPlaylist.getTrackCount())) : currentBpSong)));
            if((currentBpSong + 1) >= backupPlaylist.getTrackCount())
                currentBpSong = 0;
            else
                currentBpSong++;
            return playSong(track);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean playAlbum(){
        try {
            int random = ((int) ((backupAlbum.getTrackCount() == -1) ?  Math.random() * 10 : Math.random() * backupAlbum.getTrackCount()));

            Track track = new Track(new JSONObject(spSearch.getAlbumTrack(backupAlbum.getTracksURL(), ((choosingRandomlyFromBp) ?  random : currentBpSong))));
            if((currentBpSong + 1) >= backupAlbum.getTrackCount())
                currentBpSong = 0;
            else
                currentBpSong++;
            return playSong(track);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean playTrack(){
        try {
            return playSong(backupTrack);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean playBackup(){
        try{
            switch (currentBackupType){
                case PLAYLIST:
                    return playPlaylist();
                case ALBUM:
                    return playAlbum();
                case TRACK:
                    return playTrack();
            }
        } catch (Exception e){
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
        currentBpSong = 0;
        currentBackupType = BACKUP_TYPE.PLAYLIST;
    }

    public void setCurrentPlayingTrack(Track currentPlayingTrack){ this.currentPlayingTrack = currentPlayingTrack;}

    public void setBackupAlbum(Album backupAlbum){
        this.backupAlbum = backupAlbum;
        currentBpSong = 0;
        currentBackupType = BACKUP_TYPE.ALBUM;
    }

    public void setBackupTrack(Track backupTrack){
        this.backupTrack = backupTrack;
        currentBpSong = 0;
        currentBackupType = BACKUP_TYPE.TRACK;
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

    public Album getBackupAlbum(){ return backupAlbum; }

    public Track getBackupTrack(){ return backupTrack; }
    public SpotifySearch getSpotifySearch() {
        return spSearch;
    }

    public boolean isChoosingRandomlyFromBp() {
        return choosingRandomlyFromBp;
    }

    public BACKUP_TYPE getCurrentBackupType(){ return currentBackupType; }

    public void setOnFinishedPlaying(onFinishedPlaying oFP) {
        this.oFP = oFP;
    }


    public Track getCurrentPlayingTrack() {
        return currentPlayingTrack;
    }

    /**
     * Stops the music
     * shutdowns the Soundcloud and Spotify players
     */
    public void stop() {
        if (spotifyPlayer != null)
            spotifyPlayer.pause();

    }

    /**
     * Pauses the song
     *
     * @return Returns true if the song was paused correctly
     */
    public boolean pauseSong() {
        if (!isSongPlaying || isSongPaused || currentPlayingTrack == null)
            return false;
        if (spotifyPlayer != null && !spotifyPlayer.isShutdown())
            spotifyPlayer.pause();
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




