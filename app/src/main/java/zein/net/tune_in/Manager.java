package zein.net.tune_in;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Zein's on 2/2/2016.
 * Manages everything for the users
 */
public class Manager {

    public static final String serverIP = "https://tuneinbackend.herokuapp.com";
    private final String USER_AGENT = "Mozilla/5.0";
    public static Manager manager;

    public String sessionName;
    public Activity currentActivity;

    public ArrayList<Track> currentSearchTracks = new ArrayList<>();
    public ArrayList<Track> currentChosenTracks = new ArrayList<>();
    public ArrayList<ScUser> currentSeachUsers = new ArrayList<>();
    public Track currentPlayingTrack;
    private String hostKey = "Null";

    public User currentUser;

    public boolean isUserSearching = false;
    public boolean isUserSearchingForUser = false;
    public boolean isUserSearchingForPlaylist = false;
    public boolean hasUserChoseSong = false;
    public boolean hasUserVotedForSong = false;
    public boolean isServer = false;
    public boolean isChoosing = false;
    public boolean updateSearchAdapter = false;
    public boolean restart = false;
    public MediaPlayer mediaPlayer;

    public void setTracks(ArrayList<Track> tracks){
        currentSearchTracks.clear();
        currentSearchTracks.addAll(tracks);
        this.isUserSearching = false;
    }

    public void setUsers(ArrayList<ScUser> users){
        currentSeachUsers.clear();
        currentSeachUsers.addAll(users);
        this.isUserSearchingForUser = false;
        this.isChoosing = true;
    }

    private String convertToSendableString(String toConvert){
        String convertedString = null;
        for(int i = 0; i < toConvert.length(); i++){
            if(toConvert.charAt(i) == ' ')
                convertedString = toConvert.replace(' ', '+');
        }

        if(convertedString == null)
            return toConvert;
        else
            return convertedString;
    }

    public void hostServer(String serverName){
        Log.d("TUNEIN", convertToSendableString(serverName));
        hostKey = getData("/host&name=" + convertToSendableString(serverName)).toString();
    }

    public String sendUser(String serverKey, User user){
       return getData("/user&name=" + convertToSendableString(user.getUserName()) + "&key=" + serverKey).toString();
    }

    public void sendUsersChosenSong(String serverKey, User user){
        sendData("/userschosensong" + "&name=" + convertToSendableString(user.getUserName()) + "&id=" + user.getChosenTrack().getTrackId() + "&key=" + serverKey);
    }

    public boolean checkServerExists(String serverKey){
        return Boolean.valueOf(getData("/servercheck" + "&key=" + serverKey).toString());
    }

    public void sendUserVotedSong(String serverKey, User user){
        sendData("/uservotedsong" + "&name=" + convertToSendableString(user.getUserName()) + "&id=" + user.getVotedTrack().getTrackId() + "&key=" + serverKey);
    }

    public String getChosenSongs(String serverKey){
        return getData("/getchosensongs" + "&key=" + serverKey).toString();
    }

    public String getVotes(String serverKey){
        return getData("/getvotes" + "&key=" + serverKey).toString();
    }

    public String sendRestart(String serverKey, boolean isServer, User user){
        return getData("/restart" + "&name=" + convertToSendableString(user.getUserName()) + "&server=" + String.valueOf(isServer) + "&key=" + serverKey).toString();
    }
    public String getHostKey(){
        return hostKey;
    }

    public boolean isServerOnline(){
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con =
                    (HttpURLConnection) new URL(Manager.serverIP).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Sends data to the serer and reads what is recieved
     * @param data The data you want to send
     * @return The data that is returned from the server
     */
    private StringBuffer getData(String data){
        StringBuffer response = new StringBuffer();
        try{
            URL obj = new URL(Manager.serverIP + data);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;


            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();


        } catch(Exception e){
            e.printStackTrace();
        }
        return response;
    }

    private void sendData(String data){
        try{
            URL obj = new URL(Manager.serverIP + data);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            /*
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();
            */
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setServerKey(String serverKey){
        this.hostKey = serverKey;
    }

}
