package proj;

import java.util.ArrayList;
import java.util.List;

import com.google.maps.model.LatLng;

import proj.auth.Stravaauth;
import proj.model.Segment;
import proj.service.SegmentService;
import proj.util.PolylineDecoder;

public class Main {

    public static void main(String[] args) throws Exception {
        Stravaauth auth = new Stravaauth();

        // Run with --auth to get your access token setup instructions
        if (args.length > 0 && args[0].equals("--auth")) {
            auth.printAuthorizationUrl();
            return;
        }

        SegmentService segment = new SegmentService(auth.getAccessToken());

        // -----------------------------------------------------------------------
        // Example 1: Explore segments near Leeds
        // Change these coordinates to your area of interest!
        // -----------------------------------------------------------------------
        System.out.println("=== Exploring segments near Leeds ===");
        List<Segment> segments = segment.exploreSegments(
            53.739, -1.620,   // SW corner (roughly Morley/Beeston)
            53.870, -1.460,   // NE corner (roughly Roundhay/Seacroft)
            "running"
        );
        segments.forEach(System.out::println);

        System.out.println();

        // for (Segment s : segments) {
        //     Segment full = segment.getSegment(s.id);
        //     System.out.println("RAW: " + new com.google.gson.Gson().toJson(full));
        //     Thread.sleep(500);
        // }


        // -----------------------------------------------------------------------
        //  Explore segments in Leeds using tiled approach (4x4 grid = 16 API calls)
        // -----------------------------------------------------------------------
        System.out.println("=== Exploring segments in Leeds using tiled approach ===");
        List<Segment> allSegments = segment.exploreSegmentsTiled(
                53.739, -1.620, 53.870, -1.460, "running", 2, 2  // 16 API calls
            );

        allSegments.forEach(System.out::println);

        System.out.println();
        // -----------------------------------------------------------------------
        // Example 2: Get a segments with polyline data
        // -----------------------------------------------------------------------
            System.out.println("=== Getting segment with polyline data ===");
            List<Segment> detailed = new ArrayList<>();
            for (Segment s : allSegments) {
                detailed.add(segment.getSegment(s.id));
                Thread.sleep(500);
            }
            detailed.forEach(System.out::println);

        System.out.println();

        //for decoding polylines
        // List<LatLng> path = PolylineDecoder.decode(segment.map.polyline);
            for (Segment s : detailed) {
            if (s.map != null && s.map.polyline != null) {
                List<LatLng> path = PolylineDecoder.decode(s.map.polyline);
                System.out.println("Segment: " + s.name);
                System.out.println("  Points: " + path);
                 }
            }
    }

}