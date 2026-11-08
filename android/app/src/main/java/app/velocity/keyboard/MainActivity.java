package app.velocity.keyboard;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override public void onCreate(Bundle savedInstanceState) {
        registerPlugin(KeyboardBridgePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
