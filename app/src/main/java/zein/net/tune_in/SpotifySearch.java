package zein.net.tune_in;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by ZeinF on 3/31/2016.
 */
public class SpotifySearch {

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
           Log.d("TUNEIN", "Response Code : " + responseCode);

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

}
