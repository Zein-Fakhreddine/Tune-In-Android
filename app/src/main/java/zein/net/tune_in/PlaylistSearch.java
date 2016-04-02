package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by Zein's on 3/20/2016.
 */
public class PlaylistSearch extends IntentService {
    public PlaylistSearch(){
        super("playlistsearch");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String data = intent.getDataString();

        ArrayList<String> strings = Manager.manager.scSearch.getPlaylists(data, 5);
        for(int i = 0; i < strings.size(); i++){
            String s = strings.get(i);
            Playlist p = new Playlist(s);
        }
    }


}
