package zein.net.tune_in;

import android.app.Activity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Manager {

    public static final String serverIP = "https://tuneinbackend.herokuapp.com";

    public static final String USER_AGENT = "Mozilla/5.0";

    public static Manager manager;

    public String sessionName = "unknown";
    public Activity currentActivity;

    public ArrayList<Track> currentChosenTracks = new ArrayList<>();
    private String hostKey = "Null";

    public User currentUser;

    public boolean isUserSearching = false;
    public boolean hasUserChoseSong = false;
    public boolean hasUserVotedForSong = false;
    public boolean isServer = false;
    public boolean isChoosing = false;
    public boolean isLinkedWithSpotify = false;
    public int currentIteration = 0;
    public String spotifyToken = "";
    public boolean isDisplayingSpotifyLikes = false;
    public int currentSpotifyOffset = 0;
    public MediaManager mediaManager = new MediaManager();


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
        currentUser = new User( convertToSendableString(user.getUserName()));
       return getData("/user&name=" +  currentUser.getUserName() + "&key=" + serverKey).toString();
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
        return getData("/restart" + "&name=" + user.getUserName() + "&server=" + String.valueOf(isServer) + "&key=" + serverKey).toString();
    }
    public String getHostKey(){
        return hostKey;
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
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setServerKey(String serverKey){
        this.hostKey = serverKey;
    }

}
