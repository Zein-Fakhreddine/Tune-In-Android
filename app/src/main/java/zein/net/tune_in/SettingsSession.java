package zein.net.tune_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import static zein.net.tune_in.Manager.manager;
/**
 * Created by Zein's on 3/18/2016.
 */
public class SettingsSession extends Activity implements View.OnClickListener{

    private Button btnLink;
    private ProgressBar pbLoading;
    private TextView soundcloudUser;
    private AlertDialog.Builder chooseDialog;
    private Button btnBackupPlayList;
    private TextView currentPlayList;
    private Switch swChooseRandomly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        initView();
        initManager();
        updateList();
    }

    private void initView(){
        btnLink = (Button) findViewById(R.id.btnLink);
        btnLink.setOnClickListener(this);
        btnBackupPlayList = (Button) findViewById(R.id.btnBackupPlaylist);
        btnBackupPlayList.setOnClickListener(this);
        swChooseRandomly = (Switch) findViewById(R.id.swChooseRandomly);
        currentPlayList = (TextView) findViewById(R.id.txtCurrentPlaylist);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        soundcloudUser = (TextView) findViewById(R.id.txtSoundcloudUsername);
    }

    private void initManager(){
        manager.currentActivity = this;
    }

    private void updateList() {
        final android.os.Handler handler = new android.os.Handler();

        final Runnable r = new Runnable() {
            public void run() {
                if (!manager.isUserSearchingForUser && manager.isChoosing){
                    chooseUser();
                    manager.isChoosing = false;
                    pbLoading.setVisibility(View.INVISIBLE);
                }
                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);
    }

    private void chooseUser(){
        if(manager.currentSeachUsers.size() == 0){
            Log.d("TUNEIN", "No users");
            return;
        }
        chooseDialog = new AlertDialog.Builder(SettingsSession.this);
        chooseDialog.setTitle("Choose your userame")
                .setAdapter(new UserListAdapter(manager.currentSeachUsers), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Log.d("TUNEIN", "Chose user with the name: " + manager.currentSeachUsers.get(which));
                            manager.currentUser.setSoundcloudUser(manager.currentSeachUsers.get(which));
                            soundcloudUser.setText("Current Username: " + manager.currentUser.getSoundcloudUser().getName());
                        } catch (Exception e) {
                            Log.d("TUNEIN","There was an error choosing");
                            e.printStackTrace();
                        }
                    }
                });

        try{
            chooseDialog.show();
        } catch (WindowManager.BadTokenException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        if(btnLink.getId() == v.getId()){
            showSearchDialog();
        }
        if(btnBackupPlayList.getId() == v.getId()){
            showPlaylistSearchDialog();
        }
    }

    private AlertDialog.Builder showSearchDialog(){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for your soundcloud: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    if(search.getText().toString().length() == 0)
                        return false; //TODO: Do Somehting when the user doesnt input anything

                    return true;
                }
                return false;
            }
        });
        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchUser(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        nameDialog.show();

        return nameDialog;
    }

    private AlertDialog.Builder showPlaylistSearchDialog(){
        final AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Search for a Soundcloud playlist: ");

        //Set up the text search
        final EditText search = new EditText(this);

        search.setInputType(InputType.TYPE_CLASS_TEXT);

        nameDialog.setView(search);
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    if(search.getText().toString().length() == 0)
                        return false; //TODO: Do Somehting when the user doesnt input anything

                    return true;
                }
                return false;
            }
        });
        // Set up the buttons
        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchPlaylist(search.getText().toString());
            }
        });
        nameDialog.setNegativeButton("Cancel", null);

        nameDialog.show();

        return nameDialog;
    }

    private void searchUser(String search){
        Intent userSearch = new Intent(this, UserSearch.class);
        userSearch.setData(Uri.parse(search));
        this.startService(userSearch);
        manager.isUserSearchingForUser = true;
        pbLoading.setVisibility(View.VISIBLE);
    }

    private void searchPlaylist(String search){
        Intent playlistSearch = new Intent(this, PlaylistSearch.class);
        playlistSearch.setData(Uri.parse(search));
        this.startService(playlistSearch);
        manager.isUserSearchingForPlaylist = true;
        pbLoading.setVisibility(View.VISIBLE);
    }

    private class UserListAdapter extends ArrayAdapter<ScUser> {
        public UserListAdapter(ArrayList<ScUser> users){
            super(SettingsSession.this, R.layout.user_view, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.user_view,
                        parent, false);
            }

            ScUser currentUser = manager.currentSeachUsers.get(position);

            ImageView avatar = (ImageView) itemView.findViewById(R.id.imgProfile);
            avatar.setImageBitmap(currentUser.getUserBitMap());
            avatar.getLayoutParams().width = 250;
            avatar.getLayoutParams().height = 250;

            TextView txtName = (TextView) itemView.findViewById(R.id.txtUserName);
            txtName.setText(currentUser.getName());
            return itemView;
        }
    }
}
