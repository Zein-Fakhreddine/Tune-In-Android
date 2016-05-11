package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;

import static zein.net.tune_in.Manager.manager;


public class PlayAudio extends IntentService {
    public PlayAudio(){
        super("playaudio");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        /*
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
*/
        manager.isTrackPlaying = true;
        manager.hasUserChoseSong = false;
        manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
        manager.currentChosenTracks.clear();
    }
}
