package proj.graph;

/**
 * GraphEdge
 *
 * Represents a directed weighted edge between two nodes.
 * Each edge corresponds to a sub-segment — either a full Strava segment
 * or a portion of one split at an intersection point.
 */
public class GraphEdge {

    public final String   fromNodeId;
    public final String   toNodeId;
    public final long     stravaSegmentId;    
    public final String   segmentName;
    public final int      effortCount;        
    public final int      athleteCount;       
    public final double   distanceMetres;     
    public final double effortWeight; 
    public final double athleteWeight;  
    public final double combinedWeight;

    public GraphEdge(String fromNodeId, String toNodeId,
                     long stravaSegmentId, String segmentName,
                     int effortCount, int athleteCount,
                     double distanceMetres) {

        this.fromNodeId      = fromNodeId;
        this.toNodeId        = toNodeId;
        this.stravaSegmentId = stravaSegmentId;
        this.segmentName     = segmentName;
        this.effortCount     = effortCount;
        this.athleteCount    = athleteCount;
        this.distanceMetres  = distanceMetres;

        // Invert effort_count so Dijkstra's "shortest path" = most popular path
        // Adding 1 avoids division by zero for segments with 0 efforts
        this.effortWeight   = 1.0 / (effortCount + 1);
        this.athleteWeight  = 1.0 / (athleteCount + 1);
        this.combinedWeight = 1.0 / ((effortCount * 0.7) + (athleteCount * 0.3) + 1);
    }

    @Override
    public String toString() {
        return String.format(
            "GraphEdge[%s → %s | segment='%s' | efforts=%d | dist=%.0fm]",
            fromNodeId, toNodeId, segmentName, effortCount, distanceMetres
        );
    }
}