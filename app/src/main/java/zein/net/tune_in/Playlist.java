package zein.net.tune_in;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Zein's on 3/20/2016.
 */
public class Playlist {

    private String permalink_url;
    private String creator;
    private String name;
    private ArrayList<String> tracks;

    public Playlist(String playlistJson){
        tracks = new ArrayList<>();
        try{
            JSONObject obj = new JSONObject(playlistJson);
            permalink_url = obj.getString("permalink_url");

            Log.d("TUNEIN", "got here");
            JSONArray tracks = obj.getJSONArray("tracks");
            for(int i = 0; i < tracks.length(); i++){
                Log.d("TUNEIN", tracks.getJSONObject(i).toString());
            }
        } catch (Exception e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating playlist");
        }
    }
}
