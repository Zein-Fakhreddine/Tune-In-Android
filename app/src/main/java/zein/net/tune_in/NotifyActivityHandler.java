package zein.net.tune_in;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Zein's on 5/15/2016.
 */
public class NotifyActivityHandler extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TUNEIN", "Clicked");
        if (getIntent() != null && getIntent().getBooleanExtra("Pause", false)) {
            // Do your onclick code.\
            Log.d("TUNEIN", "No it was realy clicked");
        }
    }
}
