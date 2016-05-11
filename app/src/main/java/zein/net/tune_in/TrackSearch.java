package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;

/**
 * Created by Zein's on 2/7/2016.
 */
public class TrackSearch extends IntentService {

    public TrackSearch(){
        super("tracksearch");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        String search = workIntent.getDataString();

        if(Manager.manager.currentSearchType == Track.TRACK_TYPE.SOUNDCLOUD){
            ArrayList<String> strings = Manager.manager.scSearch.getTracks(search, 10);

            ArrayList<Track> tracks = new ArrayList<>();
            for(int i = 0; i < strings.size(); i++){
                if(i == 0)
                    Manager.manager.currentSearchTracks.clear();
                String s = strings.get(i);
                Track t = new Track(s, Track.TRACK_TYPE.SOUNDCLOUD);
                if(t.isStreamable())
                    Manager.manager.currentSearchTracks.add(t);
                tracks.add(t);
            }

            Manager.manager.doneSearching();
        } else if(Manager.manager.currentSearchType == Track.TRACK_TYPE.SPOTIFY){
            try{
                String string = Manager.manager.spSearch.searchTracks(search, 0, 10);
                JSONObject fullJson = new JSONObject(string);
                JSONObject trackJson = fullJson.getJSONObject("tracks");
                JSONArray jArray = trackJson.getJSONArray("items");

                ArrayList<Track> tracks = new ArrayList<>();
                for(int i = 0; i < jArray.length(); i++){
                    JSONObject js = jArray.getJSONObject(i);
                    if(i == 0)
                        Manager.manager.currentSearchTracks.clear();
                    Log.d("TUNEIN", "JSON: " + js.getString("uri"));
                    Track t = new Track(js, Track.TRACK_TYPE.SPOTIFY);

                    Manager.manager.currentSearchTracks.add(t);

                   tracks.add(t);
                }

                Manager.manager.doneSearching();
                Manager.manager.isDisplayingSpotifyLikes = false;
                manager.currentSpotifyOffset = 0;
            } catch (Exception e){
                e.printStackTrace();
                Manager.manager.doneSearching();
            }
        }
    }

}
