package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Created by ZeinF on 4/4/2016.
 */
public class DisplayLikes extends IntentService {
    public DisplayLikes(){
        super("displaylikes");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String data = intent.getDataString();
        int limit = Integer.parseInt(data.split(":")[0]);
        int offset = Integer.parseInt(data.split(":")[1]);
        if(Manager.manager.currentSearchType == Track.TRACK_TYPE.SPOTIFY){
            Log.d("TUNEIN", "This was called");

            try{
                String search = Manager.manager.spSearch.getSavedTracks(offset, limit).toString();
                JSONObject fullJson = new JSONObject(search);
                JSONArray jArray = fullJson.getJSONArray("items");
                for(int i = 0; i < jArray.length(); i++){
                    if(Manager.manager.hasUserChoseSong)
                        break;
                    JSONObject js = jArray.getJSONObject(i);
                    JSONObject track = js.getJSONObject("track");
                    Log.d("TUNEINE", track.toString());
                    if(i == 0 && offset == 0)
                        Manager.manager.currentSearchTracks.clear();
                    Track t = new Track(track, Track.TRACK_TYPE.SPOTIFY);

                    Manager.manager.currentSearchTracks.add(t);
                }

                Manager.manager.doneSearching();
                Manager.manager.isDisplayingSpotifyLikes = true;
            } catch (Exception e){
                e.printStackTrace();
                Manager.manager.doneSearching();
            }
            Manager.manager.isUserSearching = false;
        }
    }
}
