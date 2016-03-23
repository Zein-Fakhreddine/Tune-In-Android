package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import static zein.net.tune_in.Manager.manager;

/**
 * Created by Zein's on 3/18/2016.
 */
public class PlayAudio extends IntentService {
    public PlayAudio(){
        super("playaudio");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("TUNEIN", "GOT HERE");
        startSong();
    }

    private void startSong(){
        Log.d("TUNEIN", "Starting new song");
        String[] votesIds = manager.getVotes(manager.getHostKey()).split(",");
        for (int i = 0; i < votesIds.length; i++) {
            try{
                int voteId = Integer.parseInt(votesIds[i]);
                for (int x = 0; x < manager.currentChosenTracks.size(); x++) {
                    Track track = manager.currentChosenTracks.get(x);
                    if (track.getTrackId() == voteId)
                        track.addVote();
                }
            } catch(Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        Track trackToPlay = null;
        for (int i = 0; i < manager.currentChosenTracks.size(); i++) {
            Track track = manager.currentChosenTracks.get(i);
            Log.d("TUNEIN", "Song:" + track.getTrackTitle() + " has: " + track.getVotes() + "votes");
            if (trackToPlay == null || track.getVotes() > trackToPlay.getVotes())
                trackToPlay = track;
        }

        try {
            manager.currentPlayingTrack = trackToPlay;
            manager.mediaPlayer.setDataSource(SoundcloudSearch.getStreamURL("7c89e606e88c94ff47bfd84357e5e9f4", trackToPlay.getTrackId()));
            manager.mediaPlayer.prepare();
            manager.mediaPlayer.start();
        } catch (Exception e) {
            Log.e("TUNEIN", e.getLocalizedMessage());
            e.printStackTrace();
        }

        manager.hasUserChoseSong = false;
        manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
        manager.currentChosenTracks.clear();
    }
}
