package zein.net.dynamic_dj;

/**
 * Created by Zein's on 3/9/2016.
 */
public class User {

    private Track chosenTrack;

    private String userName;
    private Track votedTrack;
    public User(String username) {
        this.userName = username;
    }

    public void setChosenTrack(Track chosenTrack) {
        this.chosenTrack = chosenTrack;
    }

    public void setVotedTrack(Track votedTrack) {
        this.votedTrack = votedTrack;
    }

    public String getUserName() {
        return userName;
    }

    public Track getChosenTrack() {
        return chosenTrack;
    }

    public Track getVotedTrack() {
        return votedTrack;
    }

}
