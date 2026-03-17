package proj.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.google.gson.*;
import com.google.maps.model.LatLng;

import proj.model.Segment;



public class Segmentdatastore {

    private static final String OUTPUT_FILE = "leeds_running_segments_from_strava.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public void save(List<Segment> segments, String region, String activityType) throws IOException {

        JsonObject root = new JsonObject();

        // Metadata
        JsonObject metadata = new JsonObject();
        metadata.addProperty("region",          region);
        metadata.addProperty("activity_type",   activityType);
        metadata.addProperty("collected_at",    LocalDate.now().toString());
        metadata.addProperty("total_segments",  segments.size());
        root.add("metadata", metadata);

        // Segments
        JsonArray segmentArray = new JsonArray();

        for (Segment s : segments) {

            JsonObject seg = new JsonObject();

            seg.addProperty("strava_segment_id", s.id);
            seg.addProperty("name",s.name);

            // start point for segment
            LatLng startPoint = s.map != null && s.map.path != null && !s.map.path.isEmpty() ? s.map.path.get(0) : null;
            JsonObject start_point = new JsonObject();

            if (startPoint != null) {
                start_point.addProperty("lat", startPoint.lat);
                start_point.addProperty("lng", startPoint.lng);
                seg.add("start_point", start_point);
            } else {
                continue;
            }

            // end point for segment
            LatLng endPoint = s.map != null && s.map.path != null && !s.map.path.isEmpty() ? s.map.path.get(s.map.path.size() - 1) : null;
            JsonObject end_point = new JsonObject();

            if (endPoint != null) {
                end_point.addProperty("lat", endPoint.lat);
                end_point.addProperty("lng", endPoint.lng);
                seg.add("end_point", end_point);
            } else {
                continue;
            }

            // Weight — popularity and distance metrics only
            JsonObject weight = new JsonObject();
            weight.addProperty("effort_count",    s.effortCount);
            weight.addProperty("athlete_count",   s.athleteCount);
            weight.addProperty("distance_metres", s.distance);
            seg.add("weight", weight);

            // All decoded polyline points with sequence index
            JsonArray polylineArray = new JsonArray();
            if (s.map != null && s.map.path != null) {
                int index = 0;
                for (int i = 0; i < s.map.path.size(); i++) {
                    JsonObject pt = new JsonObject();
                    pt.addProperty("index", index++);
                    pt.addProperty("lat", s.map.path.get(i).lat);
                    pt.addProperty("lng", s.map.path.get(i).lng);
                    polylineArray.add(pt);
                }
                seg.add("polyline_points", polylineArray);
                seg.addProperty("point_count", s.map.path.size());
            }

            segmentArray.add(seg);


        }
        root.add("segments", segmentArray);


        // Write to file
        String json = gson.toJson(root);
        Files.writeString(Path.of(OUTPUT_FILE), json);
        System.out.println("Saved " + segmentArray.size() + " segments to " + OUTPUT_FILE);

    }

    // -------------------------------------------------------------------------
    // Load previously saved segments from JSON (avoids re-calling the API)
    // -------------------------------------------------------------------------
    public JsonObject load() throws IOException {
        if (!Files.exists(Path.of(OUTPUT_FILE))) {
            throw new IOException("No cached data found at " + OUTPUT_FILE +
                ". Run the API fetch first.");
        }
        String json = Files.readString(Path.of(OUTPUT_FILE));
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public boolean cacheExists() {
        return Files.exists(Path.of(OUTPUT_FILE));
    }

    public String getOutputFile() {
        return OUTPUT_FILE;
    }

}