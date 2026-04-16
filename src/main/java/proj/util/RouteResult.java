package proj.util;

import java.util.List;

public class RouteResult {

    public boolean         found;          
    public String          weightType;      
    public String          startNodeId;
    public String          endNodeId;
    public List<String>    nodeSequence;    
    public List<NodeCoord> coordinates;     
    public List<EdgeInfo>  edges;           // segment detail per edge
    public double          totalWeight;     // Dijkstra path weight
    public int             segmentCount;    // number of edges in the route
    public String          message;         // error message if route not found

    // -------------------------------------------------------------------------
    // Nested: coordinate point for Leaflet
    // -------------------------------------------------------------------------
    public static class NodeCoord {
        public String nodeId;
        public double lat;
        public double lng;

        public NodeCoord(String nodeId, double lat, double lng) {
            this.nodeId = nodeId;
            this.lat    = lat;
            this.lng    = lng;
        }
    }

    public static class EdgeInfo {
        public String fromNode;
        public String toNode;
        public String segmentName;
        public long   stravaSegmentId;
        public int    effortCount;
        public int    athleteCount;
        public double distanceMetres;
        public double weight;

        public EdgeInfo(String fromNode, String toNode,
                        String segmentName, long stravaSegmentId,
                        int effortCount, int athleteCount,
                        double distanceMetres, double weight) {
            this.fromNode        = fromNode;
            this.toNode          = toNode;
            this.segmentName     = segmentName;
            this.stravaSegmentId = stravaSegmentId;
            this.effortCount     = effortCount;
            this.athleteCount    = athleteCount;
            this.distanceMetres  = distanceMetres;
            this.weight          = weight;
        }
    }
}