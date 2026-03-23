package proj.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * CoordinateCleaner
 *
 * One-time utility to reprocess leeds_running_segments.json,
 * rounding all lat/lng values to 5 decimal places (~1.1m precision).
 *
 * 5dp is sufficient for GPS path representation.
 * Node ID generation in GraphBuilder rounds further to 4dp for
 * connectivity matching (~11m precision to absorb GPS noise).
 *
 * Source: GPS decimal place precision —
 * https://blis.com/precision-matters-critical-importance-decimal-places-five-lowest-go/
 */
public class CoordinateCleaner {

    private static final int    POINT_DECIMAL_PLACES = 5;
    private static final String INPUT_FILE  = "leeds_running_segments_from_strava.json";
    private static final String OUTPUT_FILE = "leeds_running_segments_from_strava.json"; // overwrites in place

    public static void main(String[] args) throws IOException {
        clean();
    }

    public static void clean() throws IOException {
        System.out.println("Loading " + INPUT_FILE + "...");
        String raw  = Files.readString(Path.of(INPUT_FILE));
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();

        JsonArray segments = root.getAsJsonArray("segments");
        int totalPointsRounded = 0;

        for (JsonElement el : segments) {
            JsonObject seg = el.getAsJsonObject();

            // Round start_point
            roundLatLng(seg.getAsJsonObject("start_point"));

            // Round end_point
            roundLatLng(seg.getAsJsonObject("end_point"));

            // Round all polyline_points
            JsonArray points = seg.getAsJsonArray("polyline_points");
            for (JsonElement pt : points) {
                roundLatLng(pt.getAsJsonObject());
                totalPointsRounded++;
            }
        }

        // Save cleaned file
        String cleaned = new GsonBuilder().setPrettyPrinting().create().toJson(root);
        Files.writeString(Path.of(OUTPUT_FILE), cleaned);

        System.out.println("Done. Rounded " + totalPointsRounded + " points to " 
            + POINT_DECIMAL_PLACES + " decimal places.");
        System.out.println("Saved to " + OUTPUT_FILE);
    }

    private static void roundLatLng(JsonObject point) {
        double lat = round(point.get("lat").getAsDouble(), POINT_DECIMAL_PLACES);
        double lng = round(point.get("lng").getAsDouble(), POINT_DECIMAL_PLACES);
        point.addProperty("lat", lat);
        point.addProperty("lng", lng);
    }

    private static double round(double value, int places) {
        return new BigDecimal(value)
            .setScale(places, RoundingMode.HALF_UP)
            .doubleValue();
    }
}