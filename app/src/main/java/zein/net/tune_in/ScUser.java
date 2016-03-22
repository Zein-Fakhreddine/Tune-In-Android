package zein.net.tune_in;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * Created by Zein's on 3/19/2016.
 */
public class ScUser {
    private String avatarURL;
    private Bitmap userBitMap;
    private String name;
    private int id;

    public ScUser(String userJson){
        try{
            JSONObject obj = new JSONObject(userJson);
            avatarURL = obj.getString("avatar_url");
            name = obj.getString("username");
            id = obj.getInt("id");
        } catch(JSONException e){
            e.printStackTrace();
            Log.d("TUNEIN", "error creating user");
        }

        userBitMap = getTrackBitMap();
    }


    private Bitmap getTrackBitMap(){
        try {
            Bitmap bmp;
            URL url = new URL(avatarURL);
            bmp =  BitmapFactory.decodeStream(url.openConnection()
                    .getInputStream());

            return bmp;
        } catch (Exception ex) {
            return null;
        }
    }

    public Bitmap getUserBitMap(){
        return userBitMap;
    }

    public String getName(){
        return  name;
    }

    public int id(){
        return id;
    }
}
