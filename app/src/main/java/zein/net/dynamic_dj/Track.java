package zein.net.dynamic_dj;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Zein's on 3/9/2016.
 */
public class Track {

    //Track variables from JSON
    private String trackTitle;
    private ArrayList<String> trackArtists;
    private String artWorkURL;
    private String albumName;
    private String albumId;
    private int duration;
    private String trackId;
    private int votes;
    private Bitmap trackBitMap;
    private String userSubmited;
    private boolean isExplicit;
    public onLoadedArtwork oLA;


    public Track(JSONObject js){
        try{
            votes = 0;
            trackTitle = js.getString("name");
            trackId = js.getString("id");
            duration = js.getInt("duration_ms");
            isExplicit = js.getBoolean("explicit");
            trackArtists = new ArrayList<>();
            JSONArray jArtists = js.getJSONArray("artists");
            for(int i = 0; i < jArtists.length(); i++){
                JSONObject jType = jArtists.getJSONObject(i);
                trackArtists.add(jType.getString("name"));
            }
            JSONObject jAlbum = js.getJSONObject("album");
            albumName = jAlbum.getString("name");
            albumId = jAlbum.getString("id");
            JSONArray jImages = jAlbum.getJSONArray("images");
            for(int i = 0; i < jImages.length(); i++){
                JSONObject jType = jImages.getJSONObject(i);
                if(jType.getInt("width") == 300 || jType.getInt("height") == 300)
                    this.artWorkURL = jType.getString("url");
            }
            loadBitmap();
        } catch (Exception e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating track");
        }
    }


    public void setVote(int votes){
        this.votes = votes;
    }

    public static Track getTrack(String trackId, String username){
            try{
                Track track = new Track(new JSONObject(Manager.manager.mediaManager.getSpotifySearch().getTrack(trackId)));
                track.userSubmited = username;
                return track;
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
    }

    public static Track getTrack(String trackId){
        return getTrack(trackId, null);
    }

    public void loadBitmap(){
        Thread thread = new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL(artWorkURL);
                    trackBitMap =  BitmapFactory.decodeStream(url.openConnection()
                            .getInputStream());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                completedSong(true);

            }
        };
        thread.start();

    }

    public void completedSong(boolean loaded){
        if(oLA != null && this != null)
            oLA.onLoaded(loaded);
    }

    public void setOnLoadedArtworkListener(onLoadedArtwork oLa){
        this.oLA = oLa;
    }

    public String getTrackTitle(){
        return trackTitle;
    }

    public ArrayList<String> getTrackArtist(){
        return trackArtists;
    }

    public String getArtistString(){
        String artists = "";
        for(int i = 0; i < this.getTrackArtist().size(); i++){
            if(i == 0)
                artists += this.getTrackArtist().get(i);
            else
                artists += ", " + this.getTrackArtist().get(i);
        }
        return artists;
    }

    public String getArtWorkURL(){
        return artWorkURL;
    }

    public String getAlbumName(){ return albumName; }


    public int getDuration(){return duration;}

    public String getDurationToTime(){
        double minutes = duration / 60000.0;
        System.out.println("MINUTES: " + minutes);
        double decimal = minutes - ((int) minutes);
        double seconds = decimal * 60;

        return ((int) minutes) + ":" + ((Math.round(seconds) < 10) ? "0" : "") + Math.round(seconds);
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

    public String getUserSubmited(){ return  userSubmited; }

    public boolean isExplicit(){ return isExplicit; }
    public interface onLoadedArtwork{
            void onLoaded(boolean loaded);
    }

}
