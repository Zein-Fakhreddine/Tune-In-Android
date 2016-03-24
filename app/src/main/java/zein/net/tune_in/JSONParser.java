package zein.net.tune_in;

import android.util.Log;

/**
 * Created by ZeinF on 3/24/2016.
 */
public class JSONParser {

    public static String getString(String data, String key){
        String message =  data.split(key + "\"" + ":" + "\"")[1].split(",")[0];
        Log.d("TUNEIN", "THe message is: " + message);
        return message;
    }
}
