package zein.net.tune_in;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

}
