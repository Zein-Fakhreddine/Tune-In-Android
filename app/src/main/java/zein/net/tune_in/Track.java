package zein.net.tune_in;

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
    private int duration;
    private String trackId;
    private int votes;
    private Bitmap trackBitMap;
    private String userSubmited;

    private boolean isLoadingBitmap = false;


    public Track(JSONObject js){
        try{
            votes = 0;
            trackTitle = js.getString("name");
            trackId = js.getString("id");
            duration = js.getInt("duration_ms");
            trackArtists = new ArrayList<>();
            JSONArray jArtists = js.getJSONArray("artists");
            for(int i = 0; i < jArtists.length(); i++){
                JSONObject jType = jArtists.getJSONObject(i);
                trackArtists.add(jType.getString("name"));
            }
            JSONObject jAlbum = js.getJSONObject("album");
            albumName = jAlbum.getString("name");
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


    public void addVote(){
        votes++;
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
}
