package zein.net.tune_in;

import android.media.MediaPlayer;
import android.util.Log;

import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

import static zein.net.tune_in.Manager.manager;

/**
 * Created by Zein's on 5/2/2016.
 */
public class MediaManager {

    //If the song is playing this is only based of if the song is paused or being played
    private boolean isSongPlaying;

    private boolean isSongPaused;

    private Track.TRACK_TYPE trackType;
    private Player spotifyPlayer;
    private MediaPlayer soundcloudPlayer;
    private onFinishedPlaying oFP;
    private Thread spotifyThread;
    public MediaManager(){
        isSongPlaying = false;
        soundcloudPlayer = new MediaPlayer();
    }

    public boolean playSong(String songId){

        if(isInteger(songId)){
            //Is a soundcloud song
            try{
                soundcloudPlayer.setDataSource(manager.scSearch.getStreamURL(Integer.parseInt(Manager.manager.currentPlayingTrack.getTrackId())));
                soundcloudPlayer.prepare();
                soundcloudPlayer.start();
                soundcloudPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        completedSong();
                    }
                });
                isSongPlaying = true;
                isSongPaused = false;
                trackType = Track.TRACK_TYPE.SOUNDCLOUD;
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }

        } else{
            if(spotifyPlayer == null || spotifyPlayer.isShutdown())
                return false;
            spotifyPlayer.play("spotify:track:" + songId);
            isSongPlaying = true;
            isSongPaused = false;
            spotifyThread = new Thread(){
                @Override
                public void run() {
                    while(spotifyPlayer != null && !spotifyPlayer.isShutdown()){
                        if(isSongPaused)
                            continue;
                        spotifyPlayer.getPlayerState(new PlayerStateCallback() {
                            @Override
                            public void onPlayerState(PlayerState playerState) {
                                if(Math.abs(playerState.positionInMs - playerState.durationInMs) < 700 || playerState.positionInMs == playerState.durationInMs){
                                    isSongPlaying = false;
                                    Log.d("TUNEIN", "It is done");
                                    completedSong();
                                    spotifyThread = null;
                                }
                            }
                        });

                        try{
                            Thread.sleep(500);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            };
            spotifyThread.start();

            Log.d("TUNEIN", "It worked");

            trackType = Track.TRACK_TYPE.SPOTIFY;
        }

        return true;
    }

    private void completedSong(){
        oFP.finishedPlaying(this);
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

    public boolean isSongPlaying(){return isSongPlaying;}
    public boolean isSongPaused(){return isSongPaused;}
    public void setSpotifyPlayer(Player player){
        this.spotifyPlayer = player;
    }
    public Player getSpotifyPlayer(){return  spotifyPlayer;}
    public void setOnFinishedPlaying(onFinishedPlaying oFP){
        this.oFP = oFP;
    }

    public void stop(){
        if(spotifyPlayer != null || !spotifyPlayer.isShutdown()){
            spotifyPlayer.pause();
            spotifyPlayer.shutdown();
            spotifyPlayer = null;
        }
        if(soundcloudPlayer != null){
            soundcloudPlayer.stop();
            soundcloudPlayer.release();
            soundcloudPlayer = null;
        }
    }

    public void pauseSong() {
        if (!isSongPlaying || isSongPaused)
            return;
        if (trackType == Track.TRACK_TYPE.SPOTIFY) {
            if (spotifyPlayer != null && !spotifyPlayer.isShutdown())
                spotifyPlayer.pause();
        } else if (trackType == Track.TRACK_TYPE.SOUNDCLOUD)
            soundcloudPlayer.pause();
        isSongPaused = true;
    }

    public void startSong(){
        if(!isSongPlaying || !isSongPaused)
            return;

        if (manager.currentPlayingTrack.getTrackType() == Track.TRACK_TYPE.SPOTIFY) {
            if (spotifyPlayer != null && !spotifyPlayer.isShutdown())
                spotifyPlayer.resume();
        } else if (manager.currentPlayingTrack.getTrackType() == Track.TRACK_TYPE.SOUNDCLOUD)
            soundcloudPlayer.start();
        isSongPaused = false;
    }
    public interface onFinishedPlaying{
        void finishedPlaying(MediaManager mm);
    }
}




