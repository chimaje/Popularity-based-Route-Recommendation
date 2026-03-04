package com;

import java.util.List;

import com.auth.Stravaauth;
import com.model.Segment;
import com.service.SegmentService;

public class Main {

    public static void main(String[] args) throws Exception {
        Stravaauth auth = new Stravaauth();

        // Run with --auth to get your access token setup instructions
        if (args.length > 0 && args[0].equals("--auth")) {
            auth.printAuthorizationUrl();
            return;
        }

        SegmentService segments = new SegmentService(auth.getAccessToken());

        // -----------------------------------------------------------------------
        // Example 1: Explore segments near London (Hyde Park area)
        // Change these coordinates to your area of interest!
        // -----------------------------------------------------------------------
        System.out.println("=== Exploring segments near London ===");
        List<Segment> nearby = segments.exploreSegments(
                51.490, -0.200,   // SW corner
                51.520,  0.180,   // NE corner
                "running"         // "running" or "cycling"
        );
        nearby.forEach(System.out::println);

        System.out.println();

        // -----------------------------------------------------------------------
        // Example 2: Look up a specific segment by ID + its leaderboard
        // This is the famous "Box Hill" cycling segment (ID: 4241449)
        // Change to any segment ID you're interested in!
        // -----------------------------------------------------------------------
        long segmentId = 4241449L;
        System.out.println("=== Fetching segment: " + segmentId + " ===");
        Segment seg = segments.getSegment(segmentId);
        System.out.println(seg);

        System.out.println();

        // -----------------------------------------------------------------------
        // Example 3: Your starred segments
        // -----------------------------------------------------------------------
        System.out.println("=== Your Starred Segments ===");
        List<Segment> starred = segments.getStarredSegments();
        if (starred.isEmpty()) {
            System.out.println("(No starred segments — star some on Strava first!)");
        } else {
            starred.forEach(System.out::println);
        }
    }
}