package proj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.model.LatLng;

import proj.graph.GraphBuilder;
import proj.model.Segment;
import proj.service.SegmentService;
import proj.util.PolylineDecoder;
import proj.util.Segmentdatastore;

/**
 * Fetches popular running segments from Strava API, caches them locally,
 * and decodes polyline data for route visualization.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        
        // Stravaauth auth = new Stravaauth();

        // if (args.length > 0 && args[0].equals("--auth")) {
        //     auth.printAuthorizationUrl();
        //     return;
        // }

        // SegmentService segmentService = new SegmentService(auth.getAccessToken());
        // Segmentdatastore dataStore = new Segmentdatastore();

        // if (dataStore.cacheExists()) {
        //     loadAndDisplayCache(dataStore);
        //     // CoordinateCleaner.clean(); one-time utility to round lat/lng values in the cached JSON
        //     GraphBuilder builder = new GraphBuilder();

        //     SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> effortGraph = builder.build_Effortweighted_graph();
        //     builder.saveGraph(effortGraph, "EFFORT");

        //     SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> athleteGraph = builder.build_Athleteweighted_graph();
        //     builder.saveGraph(athleteGraph, "ATHLETE");
        
        //     SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> combinedGraph = builder.build_Calculatedweighted_graph();
        //     builder.saveGraph(combinedGraph, "COMBINED");
        // } else {
        //     fetchAndProcessSegments(segmentService, dataStore);
        // }
        GraphBuilder builder = new GraphBuilder();

            SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> efforts = builder.loadGraph("effort");

        ConnectivityInspector<String, DefaultWeightedEdge> inspector = 
            new ConnectivityInspector<>(efforts);

        System.out.println("Is fully connected: " + inspector.isConnected());
        System.out.println("Number of components: " + inspector.connectedSets().size());
    }

    /**
     * Loads and displays cached segment data.
     */
    private static void loadAndDisplayCache(Segmentdatastore dataStore) throws IOException {
        System.out.println("=== Loading segments from cache ===");
        JsonObject cached = dataStore.load();
        JsonObject metadata = cached.getAsJsonObject("metadata");
        JsonArray segs = cached.getAsJsonArray("segments");

        System.out.println("Region:         " + metadata.get("region").getAsString());
        System.out.println("Activity type:  " + metadata.get("activity_type").getAsString());
        System.out.println("Collected at:   " + metadata.get("collected_at").getAsString());
        System.out.println("Total segments: " + segs.size());

        if (segs.size() > 0) {
            System.out.println("\n=== Sample segment ===");
            System.out.println(segs.get(0));
        }
    }

    /**
     * Fetches segments from Strava API, processes them, and saves to cache.
     */
    private static void fetchAndProcessSegments(SegmentService segmentService, Segmentdatastore dataStore) throws InterruptedException, IOException {
        System.out.println("=== Fetching from Strava API ===");

        List<Segment> allSegments = exploreSegmentsNearLeeds(segmentService);
        List<Segment> detailed = fetchDetailedSegments(segmentService, allSegments);
        decodePolylines(detailed);

        System.out.println("=== Displaying segments with decoded polyline data ===");
        detailed.forEach(System.out::println);

        dataStore.save(detailed, "Leeds", "running");

        System.out.println("\nData collection complete.");
        System.out.println("Saved to: " + dataStore.getOutputFile());
    }

    /**
     * Explores segments in Leeds using a 2x2 tiled grid approach.
     */
    private static List<Segment> exploreSegmentsNearLeeds(SegmentService segmentService) throws IOException, InterruptedException {
        System.out.println("=== Exploring segments in Leeds using tiled approach ===");
        List<Segment> allSegments = segmentService.exploreSegmentsTiled(
            53.739, -1.620, 53.870, -1.460, "running", 2, 2
        );
        // allSegments.forEach(System.out::println);
        // System.out.println();
        return allSegments;
    }

    /**
     * Fetches detailed segment information including polyline data.
     * Includes 500ms delay between API calls for rate limiting.
     */
    private static List<Segment> fetchDetailedSegments(SegmentService segmentService, List<Segment> allSegments) throws InterruptedException, IOException {
        System.out.println("=== Getting segment with polyline data ===");
        List<Segment> detailed = new ArrayList<>();
        for (Segment s : allSegments) {
            detailed.add(segmentService.getSegment(s.id));
            Thread.sleep(500);
        }
        detailed.forEach(System.out::println);
        System.out.println();
        return detailed;
    }

    /**
     * Decodes polyline strings into coordinate paths for route visualization.
     */
    private static void decodePolylines(List<Segment> detailed) {
        for (Segment s : detailed) {
            if (s.map != null && s.map.polyline != null) {
                List<LatLng> path = PolylineDecoder.decode(s.map.polyline);
                System.out.println("Segment: " + s.name);
                System.out.println("  Points: " + path);
                s.map.path = path;
                s.map.polyline = null;
            }
        }
    }
}