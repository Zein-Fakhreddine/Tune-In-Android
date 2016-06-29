package zein.net.dynamic_dj;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class Album {

    private String albumName;
    private String albumId;
    private String tracksURL;
    private Bitmap albumArtowrk = null;
    private int trackCount = -1;
    private onLoadedArtwork oLa;

    public Album(JSONObject js) {
        try {
            albumId = js.getString("id");
            albumName = js.getString("name");
            tracksURL = "https://api.spotify.com/v1/albums/" + albumId + "/tracks";
            JSONArray jImages = js.getJSONArray("images");
            Log.d("TUNEIN", "Playlist Track count is: " + trackCount);
            loadBitmap(jImages.getJSONObject(0).getString("url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnLoadedArtworkListener(onLoadedArtwork oLa) {
        this.oLa = oLa;
    }

    public static Album getAlbum(String albumId) {
        try {
            //TODO: Do this
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getTracksURL(){ return tracksURL; }

    public String getAlbumId() {
        return albumId;
    }

    public Bitmap getAlbumArtowrk() {
        return albumArtowrk;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount){ this.trackCount = trackCount; }
    public void loadBitmap(final String bitMapurl) {
        if (bitMapurl.equals("null"))
            return;

        Thread thread = new Thread() {
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    URL url = new URL(bitMapurl);
                    albumArtowrk = BitmapFactory.decodeStream(url.openConnection()
                            .getInputStream());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (oLa != null)
                    oLa.onLoaded(true);
            }
        };
        thread.start();
    }

    public interface onLoadedArtwork {
        void onLoaded(boolean loaded);
    }
}
