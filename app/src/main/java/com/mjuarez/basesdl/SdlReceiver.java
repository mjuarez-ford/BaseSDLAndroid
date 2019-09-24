package com.mjuarez.basesdl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.smartdevicelink.transport.SdlBroadcastReceiver;
import com.smartdevicelink.transport.TransportConstants;

public class SdlReceiver extends SdlBroadcastReceiver {
    public static final String RECONNECT_LANG_CHANGE = "RECONNECT_LANG_CHANGE";

    @Override
    public void onSdlEnabled(Context context, Intent intent) {
        //Use the provided intent but set the class to the SdlService
        intent.setClass(context, SdlService.class);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            context.startService(intent);
        }else{
            context.startForegroundService(intent);
        }
    }

    @Override
    public Class<? extends SdlRouterService> defineLocalSdlRouterClass() {
        //Return a local copy of the SdlRouterService located in your project
        return com.mjuarez.basesdl.SdlRouterService.class;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent); // Required if overriding this method

        if (intent != null) {
            String action = intent.getAction();
            if (action != null){
                if(action.equalsIgnoreCase(TransportConstants.START_ROUTER_SERVICE_ACTION)) {
                    if (intent.getBooleanExtra(RECONNECT_LANG_CHANGE, false)) {
                        onSdlEnabled(context, intent);
                    }
                }
            }
        }
    }
}
