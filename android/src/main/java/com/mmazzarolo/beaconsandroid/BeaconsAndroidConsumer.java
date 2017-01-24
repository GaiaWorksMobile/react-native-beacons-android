package com.mmazzarolo.beaconsandroid;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import org.altbeacon.beacon.BeaconConsumer;


/**
 * Created by Andy.Li on 2017/1/20.
 */

public class BeaconsAndroidConsumer implements BeaconConsumer {

    public interface Callback {
        void onBeaconServiceConnect();
    }

    Context applicationContext;

    public BeaconsAndroidConsumer(Context context) {
        this.applicationContext = context;
    }

    @Override
    public void onBeaconServiceConnect() {
        ((Callback) applicationContext).onBeaconServiceConnect();
    }

    @Override
    public Context getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        applicationContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return applicationContext.bindService(intent, serviceConnection, i);
    }
}
