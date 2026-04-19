package proj.util;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.annotation.PostConstruct;


@Service
public class RouteGenerate {

    private static final Map<String, String> GRAPH_FILES = Map.of(
        "EFFORT",   "leeds_graph_effort.json",
        "ATHLETE",  "leeds_graph_athlete.json",
        "COMBINED", "leeds_graph_combined.json"
    );

    // Assuming these are class fields; add them if missing
    private final Map<String, SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>> graphs = new HashMap<>();
    private final Map<String, Map<String, double[]>> nodeMeta = new HashMap<>();
    private final Map<String, Map<String, JsonObject>> edgeMeta = new HashMap<>();
    
    @PostConstruct
    public void init() throws IOException {
        loadGraphs();
    }

    private String findNearestNode(double lat, double lng, String weightType) {
        var nodes = nodeMeta.get(weightType);
    if (nodes == null) return null;

    String nearest = null;
    double minDist  = Double.MAX_VALUE;

    for (var entry : nodes.entrySet()) {
        double[] coord = entry.getValue();
        // Haversine approximation — accounts for lng compression at Leeds latitude
        double dLat = Math.toRadians(coord[0] - lat);
        double dLng = Math.toRadians(coord[1] - lng);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat))
                 * Math.cos(Math.toRadians(coord[0]))
                 * Math.sin(dLng/2) * Math.sin(dLng/2);
        double dist = 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        if (dist < minDist) {
            minDist = dist;
            nearest = entry.getKey();
        }
    }
    return nearest;
    }


    public RouteResult generateroute(double startLat, double startLng, double endLat, double endLng, String weightType) {
        System.out.println(startLat+","+startLng+","+endLat+","+endLng);
        String startNode = findNearestNode(startLat, startLng, weightType);
        String endNode = findNearestNode(endLat, endLng, weightType);
            System.out.println(startLat+","+startLng+"Start snapped to: " + startNode);
            System.out.println(endLat+","+endLng+"End snapped to:   " + endNode);
            RouteResult result = new RouteResult();
            var graph = graphs.get(weightType);
            var nodes = nodeMeta.get(weightType);
            var edges = edgeMeta.get(weightType);

            // Return early with error if graph is null
            if (graph == null) {
                result.found = false;
                result.message = "Graph not loaded for: " + weightType;
                return result;
            }

            // Return early with error if vertices don't exist
            if (!graph.containsVertex(startNode) || !graph.containsVertex(endNode)) {
                result.found = false;
                result.message = "Node not found in graph.";
                return result;
            }
            System.out.println("Graph Loaded: " + weightType);
            System.out.println("Graph loaded - vertices: " + graph.vertexSet().size());
            System.out.println("Graph loaded - edges: " + graph.edgeSet().size());
            System.out.println("Start node exists: " + graph.containsVertex(startNode));
            System.out.println("End node exists: " + graph.containsVertex(endNode));
            System.out.println("Start node passed in: '" + startNode + "'");
            System.out.println("End node passed in: '" + endNode + "'");
            
            ConnectivityInspector<String, DefaultWeightedEdge> inspector =
                new ConnectivityInspector<>(graph);

            System.out.println("Components in loaded graph: " + inspector.connectedSets().size());

            List<Set<String>> components = new ArrayList<>(inspector.connectedSets());
            components.sort((a, b) -> b.size() - a.size());

            for (int i = 0; i < Math.min(5, components.size()); i++) {
                System.out.println("Component " + i + ": " + components.get(i).size() + " nodes");
                System.out.println("  Contains start? " + components.get(i).contains(startNode));
                System.out.println("  Contains end?   " + components.get(i).contains(endNode));
            }

            //Calculate populary route using Dijkstra's algorithm
            var dijkstra = new DijkstraShortestPath<>(graph);
            GraphPath<String, DefaultWeightedEdge> path =
                dijkstra.getPath(startNode, endNode);
    
            if (path == null) {
                result.found = false;
                result.message = "No route found — nodes are in different disconnected components.";
                return result;
            }
            List<Map<String, Object>> coords = new ArrayList<>();
            for (String nodeId : path.getVertexList()) {
                double[] latLng = nodes.get(nodeId);
                if (latLng != null) {
                    coords.add(Map.of("nodeId", nodeId, "lat", latLng[0], "lng", latLng[1]));
                }
            }


        List<Map<String, Object>> edgeList = new ArrayList<>();
            double totalDistance = 0;
            double totalEffort   = 0;
    
            for (DefaultWeightedEdge e : path.getEdgeList()) {
                String from = graph.getEdgeSource(e);
                String to   = graph.getEdgeTarget(e);
                JsonObject meta = edges.get(from + "->" + to);
    
                if (meta != null) {
                    int effort   = meta.get("effort_count").getAsInt();
                    double dist  = meta.get("distance_metres").getAsDouble();
                    totalDistance += dist;
                    totalEffort   += effort;
    
                    edgeList.add(Map.of(
                        "fromNode", from,
                        "toNode", to,
                        "segmentName",  meta.get("segment_name").getAsString(),
                        "effortCount",  effort,
                        "athleteCount", meta.get("athlete_count").getAsInt(),
                        "distance",     dist
                    ));
                }
        }
            result.found = true;
            result.weightType = weightType;
            result.startNodeId = startNode;
            result.endNodeId = endNode;
            result.nodeSequence = path.getVertexList();
            result.coordinates = coords.stream()
                .map(m -> new RouteResult.NodeCoord(
                    (String) m.get("nodeId"),
                    (Double) m.get("lat"),
                    (Double) m.get("lng")
                ))
                .toList();
            result.edges = edgeList.stream()
                .map(m -> new RouteResult.EdgeInfo(
                    (String) m.get("fromNode"),
                    (String) m.get("toNode"),
                    (String) m.get("segmentName"),
                    0L, // stravaSegmentId not available
                    (Integer) m.get("effortCount"),
                    (Integer) m.get("athleteCount"),
                    (Double) m.get("distance"),
                    graph.getEdgeWeight(graph.getEdge((String) m.get("fromNode"), (String) m.get("toNode")))
                ))
                .toList();
            result.totalWeight = path.getWeight();
            result.segmentCount = path.getEdgeList().size();
            //response.put("avgEffort",     edgeList.isEmpty() ? 0 : totalEffort / edgeList.size());
            return result;

        }
    public void loadGraphs() throws IOException {
        for (Map.Entry<String, String> entry : GRAPH_FILES.entrySet()) {
            String type = entry.getKey();
            String fileName = entry.getValue();
            Path filePath = Path.of(fileName);

            if (!Files.exists(filePath)) {
                //System.err.println("Skipping missing file: {}" + fileName);
                continue;
            }

            try {
                String content = Files.readString(filePath);
                JsonObject root = JsonParser.parseString(content).getAsJsonObject();

                SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph =
                    new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
                Map<String, double[]> nodes = new HashMap<>();
                Map<String, JsonObject> edges = new HashMap<>();

                // Load nodes
                JsonArray nodesArray = root.getAsJsonArray("nodes");
                if (nodesArray == null) {
                    //System.err.println("Missing 'nodes' array in {}" + fileName);
                    continue;
                }
                for (JsonElement el : nodesArray) {
                    JsonObject n = el.getAsJsonObject();
                    String id = n.get("node_id").getAsString();
                    double lat = n.get("lat").getAsDouble();
                    double lng = n.get("lng").getAsDouble();
                    graph.addVertex(id);
                    nodes.put(id, new double[]{lat, lng});
                }

                // Load edges
                JsonArray edgesArray = root.getAsJsonArray("edges");
                if (edgesArray == null) {
                    //System.err.println("Missing 'edges' array in {}" + fileName);
                    continue;
                }
                for (JsonElement el : edgesArray) {
                    JsonObject e = el.getAsJsonObject();
                    String from = e.get("from_node").getAsString();
                    String to = e.get("to_node").getAsString();
                    double weight = e.get("weight").getAsDouble();

                    if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
                        //System.err.println("Skipping edge from {} to {}: vertices not found");
                        continue;
                    }
                    if (graph.containsEdge(from, to)) {
                        //System.err.println("Skipping duplicate edge from {} to {}");
                        continue;
                    }

                    DefaultWeightedEdge edge = graph.addEdge(from, to);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, weight);
                        edges.put(from + "->" + to, e);
                    }
                }

                graphs.put(type, graph);
                nodeMeta.put(type, nodes);
                edgeMeta.put(type, edges);
                //System.out.println("Loaded {} graph: {} nodes, {} edges", type, graph.vertexSet().size(), graph.edgeSet().size());

            } catch (Exception e) {
                // System.err.println("Error loading graph from {}: {}"+ fileName+ e.getMessage());
                throw new IOException("Failed to load graph: " + fileName, e);
            }
        }

        
    
    }

    public List<Map<String, Object>> getNodes(String weightType) {
    var nodes = nodeMeta.get(weightType);
    var graph = graphs.get(weightType);
    if (nodes == null || graph == null) return List.of();

    // Build component map — nodeId → componentId (sorted largest first)
    ConnectivityInspector<String, DefaultWeightedEdge> inspector =
        new ConnectivityInspector<>(graph);

    List<Set<String>> components = new ArrayList<>(inspector.connectedSets());
    components.sort((a, b) -> b.size() - a.size());

    Map<String, Integer> nodeToComponent = new HashMap<>();
    Map<String, Integer> componentSize   = new HashMap<>();

    for (int i = 0; i < components.size(); i++) {
        int idx = i;
        components.get(i).forEach(nodeId -> {
            nodeToComponent.put(nodeId, idx);
            componentSize.put(nodeId, components.get(idx).size());
        });
    }

    return nodes.entrySet().stream()
        .map(entry -> {
            double[] coord    = entry.getValue();
            String   nodeId   = entry.getKey();
            int      compId   = nodeToComponent.getOrDefault(nodeId, -1);
            int      compSize = componentSize.getOrDefault(nodeId, 0);

            Map<String, Object> map = new HashMap<>();
            map.put("nodeId",         nodeId);
            map.put("lat",            coord[0]);
            map.put("lng",            coord[1]);
            map.put("componentId",    compId);
            map.put("componentSize",  compSize);
            return map;
        })
        .collect(Collectors.toList());
}
}