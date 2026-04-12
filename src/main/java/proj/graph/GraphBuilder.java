package proj.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import proj.util.Proximityutils;

public class GraphBuilder {
    
    private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> Effortweighted_graph;
    private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> Athleteweighted_graph;
    private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> Calculatedweighted_graph;

    // 15m threshold for considering points as intersecting 
    //Strava GPS accuracy is typically within 5-10 metres in open areas.

    private static final double intersection_metres = 15.0; 

     private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
    private final List<GraphEdge>        edges = new ArrayList<>();

    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> build_Effortweighted_graph() throws IOException {

        nodes.clear();  
        edges.clear();

        Effortweighted_graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        List<SegmentData> segments = loadSegments();

        Map<String, Set<Integer>> intersectionIndices = determineintersections(segments);

        buildGraph(Effortweighted_graph, segments, intersectionIndices, "EFFORT");

        System.out.println("Graph built with effortweight");

        return Effortweighted_graph;
    }

    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> build_Athleteweighted_graph() throws IOException {

        nodes.clear();  
        edges.clear();

        Athleteweighted_graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        List<SegmentData> segments = loadSegments();

        Map<String, Set<Integer>> intersectionIndices = determineintersections(segments);

        buildGraph(Athleteweighted_graph, segments, intersectionIndices, "ATHLETE");

        System.out.println("Graph built with athleteweight");

        return Athleteweighted_graph;
    }

    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> build_Calculatedweighted_graph() throws IOException {

        nodes.clear();  
        edges.clear();

        Calculatedweighted_graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        List<SegmentData> segments = loadSegments();

        Map<String, Set<Integer>> intersectionIndices = determineintersections(segments);

        buildGraph(Calculatedweighted_graph, segments, intersectionIndices, "COMBINED");
        System.out.println("Graph built with combinedweight");
        return Calculatedweighted_graph;
    }

    private List<SegmentData> loadSegments() throws IOException {
        String json = Files.readString(Path.of("leeds_running_segments_from_strava.json"));
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray  segmentArray = root.getAsJsonArray("segments");
 
        List<SegmentData> segments = new ArrayList<>();
        for (JsonElement el : segmentArray) {
            JsonObject s = el.getAsJsonObject();
 
            SegmentData seg = new SegmentData();
            seg.id = s.get("strava_segment_id").getAsLong(); 
            seg.name = s.get("name").getAsString();
            seg.effortCount = s.getAsJsonObject("weight").get("effort_count").getAsInt();
            seg.athleteCount = s.getAsJsonObject("weight").get("athlete_count").getAsInt();
            seg.distanceMetres = s.getAsJsonObject("weight").get("distance_metres").getAsDouble();
 
            // Load all polyline points — using "index" field as per your JSON structure
            JsonArray pts = s.getAsJsonArray("polyline_points");
            seg.points = new ArrayList<>();
            // seg.setPoints(new ArrayList<>());
            for (JsonElement pt : pts) {
                JsonObject p = pt.getAsJsonObject();
                double lat = p.get("lat").getAsDouble();
                double lng = p.get("lng").getAsDouble();
                seg.points.add(new double[]{lat, lng});
            }
 
            segments.add(seg);
        }
        return segments;
    }
    
    private Map<String,Set<Integer>> determineintersections(List<SegmentData> segments){
        Map<String, Set<Integer>> intersections = new HashMap<>();
         for (SegmentData s : segments) {
            intersections.put(String.valueOf(s.id), new TreeSet<>());
        }
 
        int totalComparisons = 0;
        int totalIntersections = 0;
 
        // Compare every point of segment i against every point of segment j
        for (int i = 0; i < segments.size(); i++) {
            for (int j = i + 1; j < segments.size(); j++) {
 
                SegmentData segA = segments.get(i);
                SegmentData segB = segments.get(j);
 
                for (int pi = 0; pi < segA.points.size(); pi++) {
                    for (int pj = 0; pj < segB.points.size(); pj++) {
 
                        totalComparisons++;
 
                        double[] ptA = segA.points.get(pi);
                        double[] ptB = segB.points.get(pj);
 
                        if (Proximityutils.checkThreshold(
                                ptA[0], ptA[1], ptB[0], ptB[1],
                                intersection_metres)) {
 
                            intersections.get(String.valueOf(segA.id)).add(pi);
                            intersections.get(String.valueOf(segB.id)).add(pj);
                            totalIntersections++;
                        }
                    }
                }
            }
        }
        System.out.println("  Intersection points found: " + totalIntersections);
        return intersections;
    }
    
    private static class SegmentData {
        long            id;
        String          name;
        int             effortCount;
        int             athleteCount;
        double          distanceMetres;
        List<double[]>  points;
    }
    private double calculateSubDistance(List<double[]> points, int fromIdx, int toIdx) {
        double total = 0;
        for (int i = fromIdx; i < toIdx; i++) {
            total += Proximityutils.haversine(
                points.get(i)[0],   points.get(i)[1],
                points.get(i+1)[0], points.get(i+1)[1]
            );
        }
        return total;
    }
     
    private void buildGraph(SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph,List<SegmentData> segments,Map<String, Set<Integer>> intersectionIndices,String weighttype) {

        for (SegmentData seg : segments) {
            // creates nodes for graph
            Set<Integer> splitIndices = intersectionIndices
                .getOrDefault(String.valueOf(seg.id), new TreeSet<>());
            
            splitIndices.add(0);
            splitIndices.add(seg.points.size() - 1);

            List<Integer> orderedIndices = new ArrayList<>(splitIndices);
            Collections.sort(orderedIndices);

            List<String> segmentNodeIds = new ArrayList<>();//creates a node for each split point
            
            for (int idx : orderedIndices) {
                double[] pt = seg.points.get(idx);

                GraphNode.NodeType type;
                if (idx == 0)                          type = GraphNode.NodeType.SEGMENT_START;
                else if (idx == seg.points.size() - 1) type = GraphNode.NodeType.SEGMENT_END;
                else                                   type = GraphNode.NodeType.INTERSECTION;

                GraphNode node = new GraphNode(pt[0], pt[1], type);
                nodes.put(node.nodeId, node);
                graph.addVertex(node.nodeId);
                segmentNodeIds.add(node.nodeId);
            }

            //creates directed edges for graph

            for (int k = 0; k < segmentNodeIds.size() - 1; k++) {
                String fromId = segmentNodeIds.get(k);
                String toId   = segmentNodeIds.get(k + 1);
 
                if (fromId.equals(toId)) continue;
                if (graph.getEdge(fromId, toId) != null) continue;
 
                int fromIdx = orderedIndices.get(k);
                int toIdx   = orderedIndices.get(k + 1);
                double subDist = calculateSubDistance(seg.points, fromIdx, toIdx);
 
                GraphEdge edge = new GraphEdge(
                    fromId, toId,
                    seg.id, seg.name,
                    seg.effortCount, seg.athleteCount,
                    subDist
                );
 
                DefaultWeightedEdge e = graph.addEdge(fromId, toId);
                if (e != null) {
                    switch (weighttype) {
                        case "EFFORT"   -> graph.setEdgeWeight(e, edge.effortWeight);
                        case "ATHLETE"  -> graph.setEdgeWeight(e, edge.athleteWeight);
                        case "COMBINED" -> graph.setEdgeWeight(e, edge.combinedWeight);
                    }
                    edges.add(edge);
                }
            }



        }
        
    }

    public void saveGraph(SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
                      String graphType) throws IOException {

        String outputFile = "leeds_graph_" + graphType.toLowerCase() + ".json";

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();

        // Metadata
        JsonObject metadata = new JsonObject();
        metadata.addProperty("region",                   "Leeds");
        metadata.addProperty("activity_type",            "running");
        metadata.addProperty("weight_type",              graphType);
        metadata.addProperty("intersection_threshold_m", intersection_metres);
        metadata.addProperty("total_nodes",              graph.vertexSet().size());
        metadata.addProperty("total_edges",              graph.edgeSet().size());
        root.add("metadata", metadata);

        // Nodes
        JsonArray nodeArray = new JsonArray();
        for (GraphNode n : nodes.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("node_id", n.nodeId);
            obj.addProperty("lat",     n.lat);
            obj.addProperty("lng",     n.lng);
            obj.addProperty("type",    n.type.toString());
            nodeArray.add(obj);
        }
        root.add("nodes", nodeArray);

        // Edges
       JsonArray edgeArray = new JsonArray();
        for (GraphEdge e : edges) {
            JsonObject obj = new JsonObject();
            obj.addProperty("from_node",         e.fromNodeId);
            obj.addProperty("to_node",           e.toNodeId);
            obj.addProperty("strava_segment_id", e.stravaSegmentId);
            obj.addProperty("segment_name",      e.segmentName);
            obj.addProperty("effort_count",      e.effortCount);
            obj.addProperty("athlete_count",     e.athleteCount);
            obj.addProperty("distance_metres",   e.distanceMetres);

            // Only store the weight relevant to this graph type
            switch (graphType) {
                case "EFFORT"   -> obj.addProperty("weight", e.effortWeight);
                case "ATHLETE"  -> obj.addProperty("weight", e.athleteWeight);
                case "COMBINED" -> obj.addProperty("weight", e.combinedWeight);
            }

            edgeArray.add(obj);
        }
        root.add("edges", edgeArray);

        Files.writeString(Path.of(outputFile), gson.toJson(root));
        System.out.println("Saved " + graphType + " graph to " + outputFile);
    }

    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> loadGraph(String graphType) throws IOException {
        String inputFile = "leeds_graph_" + graphType.toLowerCase() + ".json";
        String json = Files.readString(Path.of(inputFile));
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph = 
            new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        // Load nodes
        JsonArray nodeArray = root.getAsJsonArray("nodes");
        for (JsonElement el : nodeArray) {
            JsonObject nodeObj = el.getAsJsonObject();
            String nodeId = nodeObj.get("node_id").getAsString();
            graph.addVertex(nodeId);
        }

        // Load edges
        JsonArray edgeArray = root.getAsJsonArray("edges");
        for (JsonElement el : edgeArray) {
            JsonObject edgeObj = el.getAsJsonObject();
            String fromNode = edgeObj.get("from_node").getAsString();
            String toNode = edgeObj.get("to_node").getAsString();
            double weight = edgeObj.get("weight").getAsDouble();

            graph.addEdge(fromNode, toNode);
            graph.setEdgeWeight(graph.getEdge(fromNode, toNode), weight);
        }
        return graph;
    }

    public Map<String, GraphNode> getNodes() { return nodes; }
    public List<GraphEdge>        getEdges() { return edges; }

}