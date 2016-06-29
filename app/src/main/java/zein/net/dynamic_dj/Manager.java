package zein.net.dynamic_dj;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Manager {

    public static final String serverIP = "https://tuneinbackend.herokuapp.com";
    public static final String USER_AGENT = "Mozilla/5.0";
    public static final String PREFS_NAME = "SettingsPrefsFile";
    public static final String[] SPOTIFY_SCOPES = {"user-library-read", "streaming"};

    public static Manager manager;

    public String sessionName = "unknown";

    public ArrayList<Track> currentChosenTracks = new ArrayList<>();
    private String hostKey = "Null";

    public User currentUser;

    public boolean filterExplicit = false;

    public boolean isUserSearching = false;
    public boolean hasUserChoseSong = false;
    public boolean hasUserVotedForSong = false;
    public boolean isServer = false;
    public boolean isLinkedWithSpotify = false;
    public int currentIteration = 0;
    public String spotifyToken = null;
    public boolean isDisplayingSpotifyLikes = false;
    public int currentSpotifyOffset = 0;
    public MediaManager mediaManager = new MediaManager();

    private String convertToSendableString(String toConvert){
        return toConvert.replaceAll("\\s+","+");
    }

    public void hostServer(String serverName){
        hostKey = getData("/host&name=" + convertToSendableString(serverName)).toString();
    }

    public String sendUser(String serverKey, User user, boolean isHosting){
        currentUser = new User( convertToSendableString(user.getUserName()));
       return getData("/user&name=" +  currentUser.getUserName()  + "&host=" + isHosting + "&key=" + serverKey).toString();
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

    public String getServerInfo(String serverKey){
        return getData("/serverinfo" + "&key=" + serverKey).toString();
    }

    public String sendRestart(String serverKey, boolean isServer, User user){
        return getData("/restart" + "&name=" + user.getUserName() + "&server=" + String.valueOf(isServer) + "&key=" + serverKey).toString();
    }

    public void sendStopedSession(String serverKey){
        sendData("/stopsession" + "&key=" + serverKey);
    }

    public void sendFilterExplicit(String serverKey){
        sendData("/filter" + "&explicit=" + String.valueOf(filterExplicit) + "&key=" + serverKey);
    }

    public void sendCurrentSong(String serverKey, String currentSongId, boolean currentSongPaused){
        sendData("/currentsong" + "&currentsongid=" + currentSongId + "&currentsongpaused=" + currentSongPaused + "&key=" + serverKey );
    }

    public String getServersOnNetwork(){
        return getData("/serversoninternet").toString();
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

            con.getResponseCode();


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
            con.setRequestMethod("PUT");
            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            con.getResponseCode();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setServerKey(String serverKey){
        this.hostKey = serverKey;
    }

}
