package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Zein's on 3/19/2016.
 */
public class UserSearch extends IntentService {

    public UserSearch(){
        super("usersearch");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("TUNEIN", "Started user intenet");
        String dataString = workIntent.getDataString();

        Log.d("TUNEIN", "Data:" + dataString);
        ArrayList<String> strings = SoundcloudSearch.getUsers("7c89e606e88c94ff47bfd84357e5e9f4", dataString, 10);
        ArrayList<ScUser> users = new ArrayList<>();
        for(int i = 0; i < strings.size(); i++){
            String s = strings.get(i);
            ScUser user = new ScUser(s);
            users.add(user);
        }

        Manager.manager.setUsers(users);

    }

}
