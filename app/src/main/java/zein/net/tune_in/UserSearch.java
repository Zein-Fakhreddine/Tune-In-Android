package zein.net.tune_in;

import android.app.IntentService;
import android.content.Intent;

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
        ArrayList<String> strings = Manager.manager.scSearch.getUsers(workIntent.getDataString(), 10);
        ArrayList<ScUser> users = new ArrayList<>();
        for(int i = 0; i < strings.size(); i++)
            users.add(new ScUser(strings.get(i)));

        Manager.manager.setUsers(users);
    }
}
