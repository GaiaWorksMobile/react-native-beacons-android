package com.mmazzarolo.beaconsandroid;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BeaconsAndroidModule extends ReactContextBaseJavaModule {
    private static final String LOG_TAG = "BeaconsAndroidModule";
    private ReactApplicationContext mReactContext;
    private Context mApplicationContext;
    private BackgroundPowerSaver backgroundPowerSaver;
    protected BeaconManager mBeaconManager;
    protected BeaconsAndroidConsumer rangingConsumer;
    protected BeaconsAndroidConsumer monitoringConsumer;

    public BeaconsAndroidModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(LOG_TAG, "BeaconsAndroidModule - started");
        this.mReactContext = reactContext;
        this.mApplicationContext = reactContext.getApplicationContext();
        this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
        this.backgroundPowerSaver = new BackgroundPowerSaver(mApplicationContext);
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("SUPPORTED", BeaconTransmitter.SUPPORTED);
        constants.put("NOT_SUPPORTED_MIN_SDK", BeaconTransmitter.NOT_SUPPORTED_MIN_SDK);
        constants.put("NOT_SUPPORTED_BLE", BeaconTransmitter.NOT_SUPPORTED_BLE);
        constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS);
        constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER);
        return constants;
    }

    @ReactMethod
    public void setHardwareEqualityEnforced(Boolean e) {
        Log.d(LOG_TAG, "setHardwareEqualityEnforced - started");
      Beacon.setHardwareEqualityEnforced(e.booleanValue());
    }

    @ReactMethod
    public void addParser(String parser) {
        Log.d(LOG_TAG, "addParser - started");
        if (mBeaconManager == null) {
            this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
        }
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
    }

    @ReactMethod
    public void removeParser(String parser) {
        Log.d(LOG_TAG, "removeParser - started");
        if (mBeaconManager == null) {
            this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
        }
        mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
    }

    @ReactMethod
    public void setBackgroundScanPeriod(int period) {
        Log.d(LOG_TAG, "setBackgroundScanPeriod - started");
        mBeaconManager.setBackgroundScanPeriod((long) period);
    }

    @ReactMethod
    public void setBackgroundBetweenScanPeriod(int period) {
        Log.d(LOG_TAG, "setBackgroundBetweenScanPeriod - started");
        mBeaconManager.setBackgroundBetweenScanPeriod((long) period);
    }

    @ReactMethod
    public void setForegroundScanPeriod(int period) {
        Log.d(LOG_TAG, "setForegroundScanPeriod - started");
        mBeaconManager.setForegroundScanPeriod((long) period);
    }

    @ReactMethod
    public void setForegroundBetweenScanPeriod(int period) {
        Log.d(LOG_TAG, "setForegroundBetweenScanPeriod - started");
        mBeaconManager.setForegroundBetweenScanPeriod((long) period);
    }

    @ReactMethod
    public void checkTransmissionSupported(Callback callback) {
        Log.d(LOG_TAG, "checkTransmissionSupported - started");
        int result = BeaconTransmitter.checkTransmissionSupported(mReactContext);
        callback.invoke(result);
    }

    @ReactMethod
    public void getMonitoredRegions(Callback callback) {
        Log.d(LOG_TAG, "getMonitoredRegions - started");
        WritableArray array = new WritableNativeArray();
        for (Region region: mBeaconManager.getMonitoredRegions()) {
            WritableMap map = new WritableNativeMap();
            map.putString("identifier", region.getUniqueId());
            map.putString("uuid", region.getId1().toString());
            map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
            map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
            array.pushMap(map);
        }
        callback.invoke(array);
    }

    @ReactMethod
    public void getRangedRegions(Callback callback) {
        Log.d(LOG_TAG, "getRangedRegions - started");
        WritableArray array = new WritableNativeArray();
        for (Region region: mBeaconManager.getRangedRegions()) {
            WritableMap map = new WritableNativeMap();
            map.putString("region", region.getUniqueId());
            map.putString("uuid", region.getId1().toString());
            array.pushMap(map);
        }
        callback.invoke(array);
    }

    /***********************************************************************************************
     * Monitoring
     **********************************************************************************************/
    @ReactMethod
    public void startMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {
        Log.d(LOG_TAG, "startMonitoring, regionId: " + regionId + ", beaconUuid: " + beaconUuid + ", minor: " + minor + ", major: " + major);
        try {
            Region region = BeaconsAndroidUtils.createRegion(regionId, beaconUuid, minor, major);
            if (monitoringConsumer == null) {
                monitoringConsumer = createMonitoringConsumer(region);
            }
            mBeaconManager.bind(monitoringConsumer);
            resolve.invoke();
            Log.d(LOG_TAG, "startMonitoring - success");
        } catch (Exception e) {
            Log.d(LOG_TAG, "startMonitoring, error: ", e);
            reject.invoke(e.getMessage());
        }
    }

    private BeaconsAndroidConsumer createMonitoringConsumer(final Region region) {
        return new BeaconsAndroidConsumer(mApplicationContext) {
            @Override
            public void onBeaconServiceConnect() {

                mBeaconManager.addMonitorNotifier(new MonitorNotifier() {
                    @Override
                    public void didEnterRegion(Region region) {
                        Log.d(LOG_TAG, "startMonitoring - didEnterRegion - start");
                        BeaconsAndroidUtils.sendEvent(mReactContext, "regionDidEnter",
                                BeaconsAndroidUtils.createMonitoringResponse(region));
                    }

                    @Override
                    public void didExitRegion(Region region) {
                        Log.d(LOG_TAG, "startMonitoring - didExitRegion - started");
                        BeaconsAndroidUtils.sendEvent(mReactContext, "regionDidExit",
                                BeaconsAndroidUtils.createMonitoringResponse(region));
                    }

                    @Override
                    public void didDetermineStateForRegion(int i, Region region) {

                    }
                });

                try {
                    mBeaconManager.startMonitoringBeaconsInRegion(region);
                    Log.d(LOG_TAG, "monitoringConsumer, called startMonitoringBeaconsInRegion()");
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "startMonitoringBeaconsInRegion error: ", e);
                }

            }
        };
    }

    @ReactMethod
    public void stopMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {
        Log.d(LOG_TAG, "stopMonitoring, regionId: " + regionId + ", beaconUuid: " + beaconUuid + ", minor: " + minor + ", major: " + major);
        try {
            Region region = BeaconsAndroidUtils.createRegion(regionId, beaconUuid, minor, major);
            mBeaconManager.stopMonitoringBeaconsInRegion(region);
            mBeaconManager.unbind(monitoringConsumer);
            monitoringConsumer = null;
            resolve.invoke();
            Log.d(LOG_TAG, "stopMonitoring - success");
        } catch (Exception e) {
            Log.d(LOG_TAG, "stopMonitoring, error: ", e);
            reject.invoke(e.getMessage());
        }
    }

    /***********************************************************************************************
     * Ranging
     **********************************************************************************************/
    @ReactMethod
    public void startRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
        Log.d(LOG_TAG, "startRanging, regionId: " + regionId + ", beaconUuid: " + beaconUuid);
        try {
            Region region = BeaconsAndroidUtils.createRegion(regionId, beaconUuid);
            if (rangingConsumer == null) {
                rangingConsumer = createRangingConsumer(region);
            }
            mBeaconManager.bind(rangingConsumer);
            resolve.invoke();
            Log.d(LOG_TAG, "startRanging, success ");
        } catch (Exception e) {
            Log.d(LOG_TAG, "startRanging, error: ", e);
            reject.invoke(e.getMessage());
        }
    }


    private BeaconsAndroidConsumer createRangingConsumer(final Region region) {
        return new BeaconsAndroidConsumer(mApplicationContext) {

            @Override
            public void onBeaconServiceConnect() {
                if (mBeaconManager == null) {
                    mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
                }
                mBeaconManager.setScannerInSameProcess(true);
                mBeaconManager.addRangeNotifier(new RangeNotifier(){

                    @Override
                    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                        if (beacons.size() > 0) {
                            Log.d(LOG_TAG, "rangingConsumer didRangeBeaconsInRegion, beacons: " + beacons.toString());
                            Log.d(LOG_TAG, "rangingConsumer didRangeBeaconsInRegion, region: " + region.toString());
                            BeaconsAndroidUtils.sendEvent(mReactContext, "beaconsDidRange",
                                    BeaconsAndroidUtils.createRangingResponse(beacons, region));
                        }
                    }
                });

                try {
                    mBeaconManager.startRangingBeaconsInRegion(region);
                    Log.d(LOG_TAG, "rangingConsumer, called startRangingBeaconsInRegion()");
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "startRangingBeaconsInRegion error: ", e);
                }
            }
        };
    }

    @ReactMethod
    public void stopRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
        Log.d(LOG_TAG, "stopRanging, regionId: " + regionId + ", beaconUuid: " + beaconUuid);
        try {
            Region region = BeaconsAndroidUtils.createRegion(regionId, beaconUuid);
            mBeaconManager.stopRangingBeaconsInRegion(region);
            mBeaconManager.unbind(rangingConsumer);
            rangingConsumer = null;
            resolve.invoke();
            Log.d(LOG_TAG, "stopRanging, success ");
        } catch (Exception e) {
            Log.e(LOG_TAG, "stopRanging, error: ", e);
            reject.invoke(e.getMessage());
        }
    }

    /***********************************************************************************************
     * APP的初始化和销毁
     **********************************************************************************************/
    @Override
    public void initialize() {
        // do nothing
        Log.d(LOG_TAG, "initialize - start");
    }

    @Override
    public void onCatalystInstanceDestroy() {
        // do nothing
        Log.d(LOG_TAG, "onCatalystInstanceDestroy - start");
        // 删除所有的notifiers
        mBeaconManager.removeAllMonitorNotifiers();
        mBeaconManager.removeAllRangeNotifiers();
        rangingConsumer = null;
        monitoringConsumer = null;
        mBeaconManager = null;
    }
}
