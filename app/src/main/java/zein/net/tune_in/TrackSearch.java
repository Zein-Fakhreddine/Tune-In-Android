package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Zein's on 2/7/2016.
 */
public class TrackSearch extends IntentService {

    public TrackSearch(){
        super("tracksearch");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        Log.d("TUNEIN", "Started intent");
        String dataString = workIntent.getDataString();

        Log.d("TUNEIN", "Data: " + dataString);
        ArrayList<String> strings = SoundcloudSearch.getTracks("7c89e606e88c94ff47bfd84357e5e9f4", dataString, 10);
        ArrayList<Track> tracks = new ArrayList<>();
        for(int i = 0; i < strings.size(); i++){
            String s = strings.get(i);
            Track t = new Track(s);
            if(t.isStreamable())
                tracks.add(t);
        }
        Manager.manager.setTracks(tracks);

    }

}
