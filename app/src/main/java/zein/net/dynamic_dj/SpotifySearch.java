package zein.net.dynamic_dj;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static zein.net.dynamic_dj.Manager.manager;

public class SpotifySearch {

    /***
     * Searches uses the Spotify http api a set of tracks based off a search String
     *
     * @param search The search String
     * @param offset The offset of the search
     * @param limit  The limit of the search
     * @return Returns the track info in JSON format
     */
    public String searchTracks(String search, int offset, int limit) {
        return getData("https://api.spotify.com/v1/search?q=" + search + "&type=track&limit=" + limit + "&offset=" + offset);
    }

    /***
     * Gets information on a track based on a track id
     * @param trackId The track id to get information of
     * @return Returns the track information in JSON format
     */
    public String getTrack(String trackId) {
        return getData("https://api.spotify.com/v1/tracks/" + trackId);
    }

    /**
     * Sends data to the serer and reads what is received
     *
     * @param offset The offset of the tracks
     * @param limit  the limit of how many tracks to get
     * @return The data that is returned from the server
     */
    public String getSavedTracks(int offset, int limit) {
        return getData("https://api.spotify.com/v1/me/tracks?limit=" + limit + "&offset=" + offset);
    }

    public String searchPlaylists(String search, int offset, int limit) {
        return getData("https://api.spotify.com/v1/search?q=" + search + "&type=playlist&limit=" + limit + "&offset=" + offset);
    }

    public String getPlaylist(String playlistId) {
        return getData("https://api.spotify.com/v1/playlists/" + playlistId);
    }


    public String getPlaylistTrack(String href, int which) {
        try {
            JSONObject fullJSON = new JSONObject(getData(href));
            JSONArray itemJSON = fullJSON.getJSONArray("items");
            return itemJSON.getJSONObject(which).getJSONObject("track").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String searchAlbums(String search, int offset, int limit) {
        return getData("https://api.spotify.com/v1/search?q=" + search + "&type=album&limit=" + limit + "&offset=" + offset);
    }


    public String getAlbumTrack(String href, int wich) {
        try {
            JSONObject fullJSON = new JSONObject(getData(href));
            if(manager.mediaManager.getBackupAlbum().getTrackCount() == -1)
                manager.mediaManager.getBackupAlbum().setTrackCount(fullJSON.getInt("total"));
                if(wich > manager.mediaManager.getBackupAlbum().getTrackCount())
                    wich = (int) (Math.random() * manager.mediaManager.getBackupAlbum().getTrackCount());

            return getData(fullJSON.getJSONArray("items").getJSONObject(wich).getString("href"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getData(String href){
        href = href.replaceAll("\\s+","%20");

        StringBuilder response = new StringBuilder();
        try {
            URL obj = new URL(href);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", Manager.USER_AGENT);
            con.setRequestProperty("Accept", "application/json");
            if (manager.spotifyToken != null)
                con.setRequestProperty("Authorization", "Bearer " + manager.spotifyToken);
            con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response.toString();
    }

}
