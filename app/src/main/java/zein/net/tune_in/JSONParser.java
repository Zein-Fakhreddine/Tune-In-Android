package zein.net.tune_in;

import android.util.Log;

/**
 * Created by ZeinF on 3/24/2016.
 */
public class JSONParser {

    private String data;

    public JSONParser(String data){
        this.data = data;
    }

    public String getString(String key){
        if(!data.contains(key)){
            Log.d("TUNEIN", "Could not find the key");
            return "undefned";
        }
        String message =  data.split(key + "\"" + ":" + "\"")[1].split(",")[0];
        Log.d("TUNEIN", "THe message is: " + message);
        return message;
    }
}
