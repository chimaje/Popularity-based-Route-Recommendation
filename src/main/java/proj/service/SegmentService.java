package proj.service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import proj.model.Segment;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SegmentService {

    private static final String BASE_URL = "https://www.strava.com/api/v3";
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String accessToken;

    public SegmentService(String accessToken) {
        this.accessToken = accessToken;
    }

    // -------------------------------------------------------------------------
    // Get a single segment by ID
    // -------------------------------------------------------------------------
    public Segment getSegment(long segmentId) throws IOException {
        String url = BASE_URL + "/segments/" + segmentId;
        String json = get(url);
        return gson.fromJson(json, Segment.class);
    }


    // -------------------------------------------------------------------------
    // Explore segments in a bounding box (lat/lng SW and NE corners)
    // Returns up to 10 segments sorted by effort count
    // -------------------------------------------------------------------------
    public List<Segment> exploreSegments(double swLat, double swLng, double neLat, double neLng,
                                         String activityType) throws IOException {
        String url = String.format(
            "%s/segments/explore?bounds=%f,%f,%f,%f&activity_type=%s",
            BASE_URL, swLat, swLng, neLat, neLng, activityType
        );
        String json = get(url);
        // The response wraps results in a "segments" array
        SegmentExploreResponse response = gson.fromJson(json, SegmentExploreResponse.class);
        return response.segments;
    }

    
    public List<Segment> exploreSegmentsTiled(
        double swLat, double swLng,
        double neLat, double neLng,
        String activityType,
        int gridRows, int gridCols) throws IOException, InterruptedException {

    Map<Long, Segment> unique = new LinkedHashMap<>();
    double latStep = (neLat - swLat) / gridRows;
    double lngStep = (neLng - swLng) / gridCols;

    for (int r = 0; r < gridRows; r++) {
        for (int c = 0; c < gridCols; c++) {
            double tileSW_Lat = swLat + r * latStep;
            double tileSW_Lng = swLng + c * lngStep;
            double tileNE_Lat = tileSW_Lat + latStep;
            double tileNE_Lng = tileSW_Lng + lngStep;

            List<Segment> tile = exploreSegments(
                tileSW_Lat, tileSW_Lng, tileNE_Lat, tileNE_Lng, activityType);
            tile.forEach(s -> unique.put(s.id, s)); // deduplicate by ID

            Thread.sleep(1000); // respect rate limits (100 req/15min)
        }
    }
    return new ArrayList<>(unique.values());
}

    // -------------------------------------------------------------------------
    // Get the authenticated athlete's starred segments
    // -------------------------------------------------------------------------
    public List<Segment> getStarredSegments() throws IOException {
        String url = BASE_URL + "/segments/starred?per_page=30";
        String json = get(url);
        Type listType = new TypeToken<List<Segment>>(){}.getType();
        return gson.fromJson(json, listType);
    }

    // -------------------------------------------------------------------------
    // Internal: shared GET request with Bearer token
    // -------------------------------------------------------------------------
    private String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API error " + response.code() + ": " + response.body().string());
            }
            if (response.body() == null) {
                throw new IOException("Empty response body");
            }
            return response.body().string();
        }
    }

    // Internal wrapper class for explore endpoint
    private static class SegmentExploreResponse {
        List<Segment> segments;
    }
}