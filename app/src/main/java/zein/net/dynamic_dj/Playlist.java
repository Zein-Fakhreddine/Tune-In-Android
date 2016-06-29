package zein.net.dynamic_dj;

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
    private Bitmap playlistArtwork = null;
    private int trackCount;
    private onLoadedArtwork oLa;

    public Playlist(JSONObject js) {
        try {
            playlistId = js.getString("id");
            playlistName = js.getString("name");
            JSONArray jImages = js.getJSONArray("images");
            trackURL = js.getJSONObject("tracks").getString("href");
            trackCount = js.getJSONObject("tracks").getInt("total");
            Log.d("TUNEIN", "Playlist Track count is: " + trackCount);
            loadBitmap(jImages.getJSONObject(0).getString("url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnLoadedArtworkListener(onLoadedArtwork oLa){
        this.oLa = oLa;
    }

    public static Playlist getPlaylist(String playlistId){
        try{
            return new Playlist(new JSONObject(Manager.manager.mediaManager.getSpotifySearch().getPlaylist(playlistId)));
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
    public String getPlaylistName() {
        return playlistName;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getTrackURL() {
        return trackURL;
    }

    public Bitmap getPlaylistArtwork() {
        return playlistArtwork;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void loadBitmap(final String bitMapurl) {
        if (bitMapurl.equals("null"))
            return;

        Thread thread = new Thread(){
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    URL url = new URL(bitMapurl);
                    playlistArtwork = BitmapFactory.decodeStream(url.openConnection()
                            .getInputStream());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if(oLa != null)
                    oLa.onLoaded(true);
            }
        };
        thread.start();
    }

    public interface onLoadedArtwork{
        void onLoaded(boolean loaded);
    }
}
