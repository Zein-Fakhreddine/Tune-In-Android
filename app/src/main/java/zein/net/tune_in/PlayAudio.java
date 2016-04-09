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

        if(Manager.manager.currentPlayingTrack.getTrackType() == Track.TRACK_TYPE.SOUNDCLOUD) {
            try {
                manager.mediaPlayer.setDataSource(manager.scSearch.getStreamURL(Integer.parseInt(Manager.manager.currentPlayingTrack.getTrackId())));
                manager.mediaPlayer.prepare();
                manager.mediaPlayer.start();
            } catch (Exception e) {
                Log.e("TUNEIN", e.getLocalizedMessage());
                e.printStackTrace();
            }
        }


        manager.isTrackPlaying = true;
        manager.hasUserChoseSong = false;
        manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
        manager.currentChosenTracks.clear();
    }
}
