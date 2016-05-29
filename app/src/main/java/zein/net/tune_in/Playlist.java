package zein.net.tune_in;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class Playlist {

    private String playlistName;
    private String playlistId;
    private String trackURL;
    private Bitmap playlistArtwork;
    private int trackCount;
    public Playlist(JSONObject js){
        try{
            playlistId = js.getString("id");
            playlistName = js.getString("name");
            JSONArray jImages = js.getJSONArray("images");
            playlistArtwork = loadBitmap(jImages.getJSONObject(0).getString("url"));
            trackURL = js.getJSONObject("tracks").getString("href");
            trackCount = js.getJSONObject("tracks").getInt("total");
            Log.d("TUNEIN", "Track url is: " + trackURL);
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    public String getPlaylistName(){ return playlistName; }
    public String getPlaylistId(){ return playlistId; }
    public String getTrackURL(){ return trackURL; }
    public Bitmap getPlaylistArtwork(){ return playlistArtwork; }

    public int getTrackCount(){ return trackCount; }

    public Bitmap loadBitmap(String bitMapurl){
        if(bitMapurl.equals("null"))
            return null;
        Bitmap bmp = null;
        try {
            URL url = new URL(bitMapurl);
            bmp =  BitmapFactory.decodeStream(url.openConnection()
                    .getInputStream());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return bmp;
    }
}
