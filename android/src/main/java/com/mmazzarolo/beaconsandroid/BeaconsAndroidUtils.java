package com.mmazzarolo.beaconsandroid;

import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

/**
 * Created by Andy.Li on 2017/1/20.
 */

public class BeaconsAndroidUtils {

    /**
     * 向js端发送消息
     * @param reactContext
     * @param eventName
     * @param params
     */
    public static void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    /**
     * 获取Region的对象
     * @param regionId
     * @param beaconUuid
     * @return
     */
    public static Region createRegion(String regionId, String beaconUuid) {
        Identifier id1 = (beaconUuid == null) ? null : Identifier.parse(beaconUuid);
        return new Region(regionId, id1, null, null);
    }

    /**
     * 获取Region的对象（含有主要值和次要值）
     * @param regionId
     * @param beaconUuid
     * @param minor
     * @param major
     * @return
     */
    public static Region createRegion(String regionId, String beaconUuid, int minor, int major) {
        Identifier id1 = (beaconUuid == null) ? null : Identifier.parse(beaconUuid);
        return new Region(regionId, id1, Identifier.fromInt(major), Identifier.fromInt(minor));
    }

    /**
     * 返回Rangin扫描的信息
     * @param beacons
     * @param region
     * @return
     */
    public static  WritableMap createRangingResponse(Collection<Beacon> beacons, Region region) {
        WritableMap map = new WritableNativeMap();
        map.putString("identifier", region.getUniqueId());
        map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
        WritableArray a = new WritableNativeArray();
        for (Beacon beacon : beacons) {
            WritableMap b = new WritableNativeMap();
            b.putString("uuid", beacon.getId1().toString());
            b.putInt("major", beacon.getId2().toInt());
            b.putInt("minor", beacon.getId3().toInt());
            b.putInt("rssi", beacon.getRssi());
            b.putDouble("distance", beacon.getDistance());
            b.putString("proximity", getProximity(beacon.getDistance()));
            a.pushMap(b);
        }
        map.putArray("beacons", a);
        return map;
    }

    /**
     * 判断距离
     * @param distance
     * @return
     */
    private static String getProximity(double distance) {
        if (distance == -1.0) {
            return "unknown";
        } else if (distance < 1) {
            return "immediate";
        } else if (distance < 3) {
            return "near";
        } else {
            return "far";
        }
    }


    /**
     * 返回Monitor扫描的信息
     * @param region
     * @return
     */
    public static WritableMap createMonitoringResponse(Region region) {
        WritableMap map = new WritableNativeMap();
        map.putString("identifier", region.getUniqueId());
        map.putString("uuid", region.getId1().toString());
        map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
        map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
        return map;
    }
}
