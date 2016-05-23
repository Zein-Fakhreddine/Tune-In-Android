package zein.net.tune_in;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
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
    private int duration;
    private String trackId;
    private int votes;
    private Bitmap trackBitMap;
    private boolean isStreamable;

    private boolean isLoadingBitmap = false;


    public Track(JSONObject js){
        try{
            Log.d("TUNEIN", "this is working");
            trackTitle = js.getString("name");
            JSONObject jAlbum = js.getJSONObject("album");
            JSONArray jImages = jAlbum.getJSONArray("images");
            for(int i = 0; i < jImages.length(); i++){
                JSONObject jType = jImages.getJSONObject(i);
                if(jType.getInt("width") == 300 || jType.getInt("height") == 300)
                    this.artWorkURL = jType.getString("url");
                Log.d("TUNEIN","ARTwork: " + this.artWorkURL);
                playbackCount = -1;
                trackId = js.getString("id");
                duration = js.getInt("duration_ms");
                isStreamable = true;
            }
            loadBitmap();
        } catch (Exception e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating track");
        }
    }


    public void addVote(){
        votes++;
    }


    public void setVote(int votes){
        this.votes = votes;
    }

    public void setIsLoadingBitmap(boolean isLoadingBitmap){this.isLoadingBitmap = isLoadingBitmap;}

    public static Track getTrack(String trackId){
            try{
                return new Track(new JSONObject(Manager.manager.mediaManager.getSpotifySearch().getTrack(trackId)));
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
    }
    public void loadBitmap(){

        Thread thread = new Thread(){
            @Override
            public void run() {
                isLoadingBitmap = true;
                try {
                    Bitmap bmp;
                    URL url = new URL(artWorkURL);
                    bmp =  BitmapFactory.decodeStream(url.openConnection()
                            .getInputStream());

                    trackBitMap = bmp;
                    completedSong();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                isLoadingBitmap = false;
            }
        };
        thread.start();

    }

    public void completedSong(){
        Manager.manager.mediaManager.preparedSong(this);
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

    public int getDuration(){return duration;}

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

    public boolean isLoadingBitmap(){return isLoadingBitmap;}
}
