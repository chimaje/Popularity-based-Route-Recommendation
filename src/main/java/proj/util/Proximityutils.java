package proj.util;

public class Proximityutils{
    // calulating distance between 2 geo coordinates using Haversine formula
    //
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

         lat1 = Math.toRadians(lat1);
         lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) + 
                   Math.pow(Math.sin(dLon / 2), 2) * 
                   Math.cos(lat1) * 
                   Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c; // Distance in meters
    }

    public static boolean checkThreshold(double lat1, 
    double lng1, double lat2, double lng2,double thresholdMetres){
        return haversine(lat1, lng1, lat2, lng2) <= thresholdMetres;
    }
}