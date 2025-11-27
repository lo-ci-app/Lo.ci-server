package com.teamloci.loci.global.util;

import com.uber.h3core.H3Core;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GeoUtils {

    private final H3Core h3;

    private static final int BEACON_RESOLUTION = 9;

    public GeoUtils() {
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.H3_INIT_FAILED, e);
        }
    }

    public String latLngToBeaconId(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;

        return h3.latLngToCellAddress(latitude, longitude, BEACON_RESOLUTION);
    }

    public Pair<Double, Double> beaconIdToLatLng(String beaconId) {
        try {
            var latLng = h3.cellToLatLng(beaconId);
            return new Pair<>(latLng.lat, latLng.lng);
        } catch (Exception e) {
            return null;
        }
    }

    public static class Pair<K, V> {
        public final K lat;
        public final V lng;
        public Pair(K lat, V lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

}