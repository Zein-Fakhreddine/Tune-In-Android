package zein.net.tune_in;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.media.session.MediaSession;
import android.util.Log;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import static zein.net.tune_in.Manager.manager;


public class PlayAudio extends IntentService {
    public PlayAudio(){
        super("playaudio");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("TUNEIN", "GOT HERE");
        Track trackToPlay = null;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            Track track = manager.currentChosenTracks.get(i);
            Log.d("TUNEIN", "Song:" + track.getTrackTitle() + " has: " + track.getVotes() + "votes");
            if (trackToPlay == null || track.getVotes() > trackToPlay.getVotes())
                trackToPlay = track;
        }
        if(trackToPlay.getTrackType() == Track.TRACK_TYPE.SOUNDCLOUD) {
            try {
                manager.currentPlayingTrack = trackToPlay;
                manager.mediaPlayer.setDataSource(manager.scSearch.getStreamURL(Integer.parseInt(trackToPlay.getTrackId())));
                manager.mediaPlayer.prepare();
                manager.mediaPlayer.start();
            } catch (Exception e) {
                Log.e("TUNEIN", e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else
            if(manager.spotifyPlayer != null)
                manager.spotifyPlayer.play("spotify:track:" + trackToPlay.getTrackId());


        manager.hasUserChoseSong = false;
        manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
        manager.currentChosenTracks.clear();
    }
}
