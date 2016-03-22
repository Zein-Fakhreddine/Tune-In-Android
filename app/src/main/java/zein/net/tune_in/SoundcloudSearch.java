package zein.net.tune_in;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Zein's on 2/7/2016.
 */
public class SoundcloudSearch {

    public static ArrayList<String> getTracks(String clientId, String search, int limit){
        return getData("http://api.soundcloud.com/tracks.json?client_id=CLIENT_ID&q=SEARCH&limit=LIMIT", clientId, "{\"download_url\"", search, limit);
    }

    public static ArrayList<String> getUsers(String clientId, String search, int limit){
        return getData("http://api.soundcloud.com/users.json?client_id=CLIENT_ID&q=SEARCH&limit=LIMIT",clientId, "{\"playlist_count\"",search, limit);
    }

    public static ArrayList<String> getPlaylists(String clientId, String search, int limit){
        return getData("http://api.soundcloud.com/playlists.json?client_id=CLIENT_ID&q=SEARCH&limit=LIMIT", clientId, "{\"duration\"", search, limit);
    }
    /*
    public static ArrayList<String> getUserFavirotes(String clientId, String userId, int limit){
        String searchURL = "https://api.soundcloud.com/users/USERID/favorites?client_id=CLIENT_ID?";
        limit += 1;

        searchURL = searchURL.replace("CLIENT_ID", clientId).replace("USERID", userId);
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        try{
            String jsonContent = "";
            url = new URL(searchURL);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                jsonContent += line;

            jsonContent = jsonContent.replace("[", "").replace("]", "");
            //"\\{\"download_url\""
            String[] jsonContents = jsonContent.split("\\" + key);
            for(int i = 0; i < jsonContents.length; i++){
                String track = jsonContents[i];
                if(!track.equals("")){
                    track = key + track;
                    track = track.substring(0, track.length() - 1) + track.substring(track.length());
                    if(i == 1) track += "}";
                    if(track.charAt(track.length() - 1) == '}')
                        result.add(track);
                }
            }
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

*/
    public static String getTrack(String clientId, int trackId){
        String searchURL = "http://api.soundcloud.com/tracks/TRACK_ID?client_id=CLIENT_ID";
        searchURL = searchURL.replace("CLIENT_ID", clientId).replace("TRACK_ID", String.valueOf(trackId));
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String jsonContent = "";
        try{
            url = new URL(searchURL);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                jsonContent += line;

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
        return jsonContent;
    }


    public static String getStreamURL(String client_id, int trackId){
        String streamURL = null;
        String searchURL = "https://api.soundcloud.com/i1/tracks/" + trackId + "/streams?client_id=" + client_id;
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        String jsonContent = "";
        try{
            url = new URL(searchURL);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                jsonContent += line;

            streamURL = new JSONObject(jsonContent).getString("http_mp3_128_url");
        } catch(MalformedURLException mue){
            mue.printStackTrace();
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        catch (JSONException je){
            je.printStackTrace();
        }finally{
            try{
                if (is != null) is.close();
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        return streamURL;
    }


    private static ArrayList<String> getData(String searchURL, String clientId,String key, String search, int limit){
        limit += 1;
        String[] spaces = search.split(" ");
        ArrayList<String> result = new ArrayList<>();
        for(int i = 0; i < spaces.length; i++)
            search = search.replace(" ", "%20");

        searchURL = searchURL.replace("CLIENT_ID", clientId).replace("LIMIT", String.valueOf(limit)).replace("SEARCH", search);
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        try{
            String jsonContent = "";
            url = new URL(searchURL);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
                jsonContent += line;

            jsonContent = jsonContent.replace("[", "").replace("]", "");
            //"\\{\"download_url\""
            String[] jsonContents = jsonContent.split("\\" + key);
            for(int i = 0; i < jsonContents.length; i++){
                String track = jsonContents[i];
                if(!track.equals("")){
                    track = key + track;
                    track = track.substring(0, track.length() - 1) + track.substring(track.length());
                    if(i == 1) track += "}";
                    if(track.charAt(track.length() - 1) == '}')
                        result.add(track);
                }
            }
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
