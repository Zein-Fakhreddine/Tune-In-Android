package zein.net.tune_in;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * Created by Zein's on 3/9/2016.
 */
public class Track {

    //Track variables from JSON
    private String trackTitle;
    private String trackArtist;
    private String artWorkURL;
    private int likeCount;
    private int playbackCount;
    private int trackId;
    private int votes;
    private Bitmap trackBitMap;
    private boolean isStreamable;
    
    public Track(String trackJSON){
        votes = 0;
        try{
            JSONObject obj = new JSONObject(trackJSON);
            trackTitle = obj.getString("title");
            //trackArtist = obj.getString("username");
            artWorkURL = obj.getString("artwork_url");
            playbackCount = obj.getInt("playback_count");
            trackId = obj.getInt("id");
            isStreamable = obj.getBoolean("streamable");
        } catch(JSONException e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating track");
        }
        trackBitMap = getTrackBitMap();
    }

    public void addVote(){
        votes += 1;
    }

    public void removeVote(){
        votes -= 1;
    }

    private Bitmap getTrackBitMap(){
        try {
            Bitmap bmp;
            URL url = new URL(artWorkURL);
            bmp =  BitmapFactory.decodeStream(url.openConnection()
                    .getInputStream());

            return bmp;
        } catch (Exception ex) {
            return null;
        }
    }
    public String getTrackTitle(){
        return trackTitle;
    }

    public String getTrackArtist(){
        return trackArtist;
    }

    public String getArtWorkURL(){
        return artWorkURL;
    }

    public int getLikeCount(){
        return likeCount;
    }

    public int getPlaybackCount(){
        return playbackCount;
    }

    public int getTrackId(){
        return trackId;
    }

    public int getVotes(){
        return votes;
    }

    public Bitmap getTrackBitmap(){
        return trackBitMap;
    }

    public boolean isStreamable(){return isStreamable;}
}
