package zein.net.tune_in;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
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
    private String trackId;
    private int votes;
    private Bitmap trackBitMap;
    private boolean isStreamable;
    private TRACK_TYPE trackType;

    public static enum TRACK_TYPE{
        SPOTIFY,
        SOUNDCLOUD
    }

    public Track(String trackJSON, TRACK_TYPE type){
        this.trackType = type;
        if(type == TRACK_TYPE.SOUNDCLOUD)
            initSoundcloud(trackJSON);
    }

    public Track(JSONObject js, TRACK_TYPE type){
        this.trackType = type;
        if(type == TRACK_TYPE.SPOTIFY){
            initSpoify(js);
        }
    }
    public void initSoundcloud(String trackJSON){
        try{
            JSONObject obj = new JSONObject(trackJSON);
            trackTitle = obj.getString("title");
            artWorkURL = obj.getString("artwork_url");
            playbackCount = obj.getInt("playback_count");
            trackId = String.valueOf(obj.getInt("id"));
            isStreamable = obj.getBoolean("streamable");
            votes = 0;
        } catch(JSONException e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating track");
        }
        trackBitMap = getTrackBitMap();
    }

    public void initSpoify(JSONObject js){
        try{
            Log.d("TUNEIN", "this is working");
            trackTitle = js.getString("name");
            JSONObject jAlbum = js.getJSONObject("album");
            JSONArray jImages = jAlbum.getJSONArray("images");
            for(int i = 0; i < jImages.length(); i++){
                JSONObject jType = jImages.getJSONObject(i);
                if(jType.getInt("height") == 300)
                    this.artWorkURL = jType.getString("url");
                Log.d("TUNEIN","ARTwork: " + this.artWorkURL);
                trackBitMap = getTrackBitMap();
                playbackCount = -1;
                trackId = js.getString("id");
                isStreamable = true;
            }

        } catch (Exception e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating track");
        }

    }


    public void addVote(){
        votes += 1;
    }


    public void setVote(int votes){
        this.votes = votes;
    }

    public static Track getTrack(String trackId, TRACK_TYPE type){
        if(type == TRACK_TYPE.SOUNDCLOUD)
            return new Track(Manager.manager.scSearch.getTrack(Integer.parseInt(trackId)), type);
        else{
            try{
                return new Track(new JSONObject(Manager.manager.spSearch.getTrack(trackId)), type);
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
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

    public TRACK_TYPE getTrackType(){return trackType;}
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

    public String getTrackId(){
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
