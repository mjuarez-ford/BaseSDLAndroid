package com.mjuarez.basesdl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.util.DebugTool;

import java.util.Vector;

public class SdlService extends Service {

    private static final String APP_ID = "8678309";
    private static final String APP_NAME = "Base SDL";
    private static final String ICON_FILENAME = "hello_sdl_icon.png";
    private static final String SDL_IMAGE_FILENAME = "sdl_full_image.png";
    private static final int FOREGROUND_SERVICE_ID = 111;
    private SdlManager sdlManager = null;
    private NotificationChannel channel = null;


    public SdlService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(APP_ID, "SdlService", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Notification serviceNotification = new Notification.Builder(this, channel.getId())
                        .setContentTitle("Connected through SDL")
                        //.setSmallIcon(R.drawable.ic_sdl)
                        .build();
                startForeground(FOREGROUND_SERVICE_ID, serviceNotification);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(notificationManager!=null){ //If this is the only notification on your channel
                notificationManager.deleteNotificationChannel(channel.getId());
            }
            stopForeground(true);
        }
        if (sdlManager != null) {
            sdlManager.dispose();
        }
        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (sdlManager == null) {
            startProxy();
        }
        return START_STICKY;
    }

    private void startProxy() {
        DebugTool.enableDebugTool();
        MultiplexTransportConfig transport = new MultiplexTransportConfig(this, APP_ID, MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF);
        //transport = new TCPTransportConfig(TCP_PORT, DEV_MACHINE_IP_ADDRESS, true); //TCP
        Vector<AppHMIType> appType = new Vector<>();
        appType.add(AppHMIType.DEFAULT);
        SdlManagerListener listener = new SdlManagerListener() {
            @Override
            public void onStart() {
                sdlManager.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
                    @Override
                    public void onNotified(RPCNotification notification) {
                        OnHMIStatus onHMIStatus = (OnHMIStatus)notification;
                        if (onHMIStatus.getHmiLevel() == HMILevel.HMI_FULL && onHMIStatus.getFirstRun()) {
                            sdlManager.getScreenManager().beginTransaction();
                            sdlManager.getScreenManager().setTextField1("Hello, this is MainField1.");
                            sdlManager.getScreenManager().setTextField2("Hello, this is MainField2.");
                            sdlManager.getScreenManager().setTextField3("Hello, this is MainField3.");
                            sdlManager.getScreenManager().setTextField4("Hello, this is MainField4.");
                            sdlManager.getScreenManager().commit(new CompletionListener() {
                                @Override
                                public void onComplete(boolean success) {
                                    System.out.println("Completed");
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void onDestroy() {
                SdlService.this.stopSelf();
            }

            @Override
            public void onError(String info, Exception e) {

            }
            
            @Override
            public LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language) {
                return null;
            }
        };

        SdlArtwork appIcon = new SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, R.mipmap.ic_launcher, true);

        // The manager builder sets options for your session
        SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, APP_NAME, listener);
        builder.setAppTypes(appType);
        builder.setTransportType(transport);
        builder.setAppIcon(appIcon);
        sdlManager = builder.build();
        sdlManager.start();

    }
}
