package zein.net.tune_in;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ZeinF on 3/31/2016.
 */
public class SpotifySearch {

    /***
     * Searches uses the Spotify http api a set of tracks based off a search String
     * @param search The search String
     * @param offset The offset of the search
     * @param limit The limit of the search
     * @return Returns the track info in JSON format
     */
    public String searchTracks(String search, int offset, int limit){
        String[] spaces = search.split(" ");
        for(int i = 0; i < spaces.length; i++)
            search = search.replace(" ", "%20");
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String result = "";
        try{
            url = new URL("https://api.spotify.com/v1/search?q=" + search + "&type=track&limit=" + limit +  "&offset=" + offset);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                result += line;

        } catch(MalformedURLException mue){
            mue.printStackTrace();
        } catch(IOException ioe){
            ioe.printStackTrace();
        } finally{
            try{
                if (is != null) is.close();
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        return result;
    }

    /***
     * Gets information on a track based on a track id
     * @param trackId The track id to get information of
     * @return Returns the track information in JSON format
     */
    public String getTrack(String trackId){
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String result = "";
        try{
            url = new URL("https://api.spotify.com/v1/tracks/" + trackId);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                result += line;

        } catch(MalformedURLException mue){
            mue.printStackTrace();
        } catch(IOException ioe){
            ioe.printStackTrace();
        } finally{
            try{
                if (is != null) is.close();
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Sends data to the serer and reads what is recieved
     * @param  offset The offset of the tracks
     * @param limit the limit of how many tracks to get
     * @return The data that is returned from the server
     */
    public StringBuffer getSavedTracks(int offset, int limit){
        StringBuffer response = new StringBuffer();
        try{
            URL obj = new URL("https://api.spotify.com/v1/me/tracks?limit=" + limit + "&offset=" + offset);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", Manager.USER_AGENT);
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Authorization", "Bearer " + Manager.manager.spotifyToken);
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

        } catch(Exception e){
            e.printStackTrace();
        }

        System.out.println(response);
        return response;
    }

    public String searchPlaylists(String search, int offset, int limit){
        String[] spaces = search.split(" ");
        for(int i = 0; i < spaces.length; i++)
            search = search.replace(" ", "%20");
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String result = "";
        try{
            url = new URL("https://api.spotify.com/v1/search?q=" + search + "&type=playlist&limit=" + limit +  "&offset=" + offset);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                result += line;

        } catch(MalformedURLException mue){
            mue.printStackTrace();
        } catch(IOException ioe){
            ioe.printStackTrace();
        } finally{
            try{
                if (is != null) is.close();
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        return result;
    }

    public String getPlaylistTracks(String href){
        StringBuffer response = new StringBuffer();
        try{
            Log.d("TUNEIN", "The playlist track url is: " + href);
            URL obj = new URL(href);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", Manager.USER_AGENT);
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Authorization", "Bearer " + Manager.manager.spotifyToken);
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

        } catch(Exception e){
            e.printStackTrace();
        }

        System.out.println(response);
        return response.toString();
    }

    public String getPlaylistTrack(String href, int wich){
        try{
            JSONObject fullJSON = new JSONObject(getPlaylistTracks(href));
            JSONArray itemJSON = fullJSON.getJSONArray("items");
            return itemJSON.getJSONObject(wich).getJSONObject("track").toString();
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

}
