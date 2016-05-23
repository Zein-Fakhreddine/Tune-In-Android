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
        manager.hasUserChoseSong = false;
        manager.sendRestart(manager.getHostKey(), manager.isServer, manager.currentUser);
        manager.currentChosenTracks.clear();
    }
}
