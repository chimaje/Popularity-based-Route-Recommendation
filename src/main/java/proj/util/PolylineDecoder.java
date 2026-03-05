package proj.util;

import com.google.maps.model.*;
import java.util.List;

public class PolylineDecoder {
    /**
     * Decodes a Strava encoded polyline string into a list of LatLng coordinates.
     * Uses Google Maps Services Java library (EncodedPolyline).
     *
     * @param encoded the encoded polyline string from Strava's segment map
     * @return list of LatLng coordinates representing the segment path
     */

    public static List<LatLng> decode(String encoded) {
        return new EncodedPolyline(encoded).decodePath();
    }

}