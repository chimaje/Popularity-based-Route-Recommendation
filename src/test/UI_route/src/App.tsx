import { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMapEvents, CircleMarker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

interface GraphNode {
  nodeId: string;
  lat: number;
  lng: number;
  componentId: number;
  componentSize: number;
}

interface NodeCoord {
  nodeId: string;
  lat: number;
  lng: number;
}

interface RouteResult {
  found: boolean;
  message?: string;
  weightType: string;
  startNodeId: string;
  endNodeId: string;
  nodeSequence: string[];
  coordinates: NodeCoord[];
  edges: Array<{
    fromNode: string;
    toNode: string;
    segmentName: string;
    effortCount: number;
    athleteCount: number;
    distance: number;
    weight: number;
  }>;
  totalWeight: number;
  segmentCount: number;
}

// Colours per component — largest components get distinct colours, rest are grey
const COMPONENT_COLOURS: Record<number, string> = {
  0: '#22c55e',
  1: '#3b82f6',
  2: '#f59e0b',
  3: '#ec4899',
  4: '#8b5cf6',
  5: '#14b8a6',
};

function getComponentColour(componentId: number): string {
  return COMPONENT_COLOURS[componentId] ?? '#94a3b8';
}

function MapClickHandler({ onClick }: { onClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click: (e) => onClick(e.latlng.lat, e.latlng.lng),
  });
  return null;
}

function App() {
  const [graphNodes,   setGraphNodes]   = useState<GraphNode[]>([]);
  const [startPos,     setStartPos]     = useState<[number, number] | null>(null);
  const [endPos,       setEndPos]       = useState<[number, number] | null>(null);
  const [startNodeId,  setStartNodeId]  = useState<string | null>(null);
  const [endNodeId,    setEndNodeId]    = useState<string | null>(null);
  const [weightType,   setWeightType]   = useState('EFFORT');
  const [route,        setRoute]        = useState<RouteResult | null>(null);
  const [error,        setError]        = useState('');
  const [warning,      setWarning]      = useState('');
  const [settingMode,  setSettingMode]  = useState<'start' | 'end' | null>(null);
  const [coordInput,   setCoordInput]   = useState('');

  // Load nodes once on mount with default weight type EFFORT
  // Changing weight type only affects route generation, not node display
  useEffect(() => {
    fetch('http://localhost:8080/api/nodes?weightType=EFFORT')
      .then(r => r.json())
      .then((data: GraphNode[]) => setGraphNodes(Array.isArray(data) ? data : []))
      .then(() => setWeightType(''))
      .catch(() => setError('Failed to load nodes. Is the Java server running?'));
  }, []);

  // Warn if selected nodes are in different components
  useEffect(() => {
    if (!startNodeId || !endNodeId) { setWarning(''); return; }
    const s = graphNodes.find(n => n.nodeId === startNodeId);
    const e = graphNodes.find(n => n.nodeId === endNodeId);
    if (s && e && s.componentId !== e.componentId) {
      setWarning(
        `Start (component ${s.componentId}) and end (component ${e.componentId}) ` +
        `are in different disconnected components. No route exists between them.`
      );
    } else {
      setWarning('');
    }
  }, [startNodeId, endNodeId, graphNodes]);

  const handleMapClick = (lat: number, lng: number) => {
    const roundedLat = Math.round(lat * 100000) / 100000;
    const roundedLng = Math.round(lng * 100000) / 100000;
    if (settingMode === 'start') {
      setStartPos([roundedLat, roundedLng]);
      setStartNodeId(null);
      setSettingMode(null);
    } else if (settingMode === 'end') {
      setEndPos([roundedLat, roundedLng]);
      setEndNodeId(null);
      setSettingMode(null);
    }
  };

  const handleNodeClick = (node: GraphNode) => {
    if (settingMode === 'start') {
      setStartPos([node.lat, node.lng]);
      setStartNodeId(node.nodeId);
      setRoute(null);
      setError('');
      setSettingMode(null);
    } else if (settingMode === 'end') {
      setEndPos([node.lat, node.lng]);
      setEndNodeId(node.nodeId);
      setRoute(null);
      setError('');
      setSettingMode(null);
    }
  };

  const setPointFromInput = () => {
    const parts = coordInput.split(',').map(s => s.trim());
    if (parts.length !== 2) { setError('Invalid format. Use: lat, lng'); return; }
    const lat = parseFloat(parts[0]);
    const lng = parseFloat(parts[1]);
    if (isNaN(lat) || isNaN(lng)) { setError('Invalid coordinates'); return; }
    const roundedLat = Math.round(lat * 100000) / 100000;
    const roundedLng = Math.round(lng * 100000) / 100000;
    if (settingMode === 'start') {
      setStartPos([roundedLat, roundedLng]);
      setStartNodeId(null);
      setSettingMode(null);
    } else if (settingMode === 'end') {
      setEndPos([roundedLat, roundedLng]);
      setEndNodeId(null);
      setSettingMode(null);
    } else {
      setError('Please select start or end mode first');
    }
    setCoordInput('');
  };

  const generateRoute = async () => {
    if (!startPos || !endPos) { setError('Please set both start and end points'); return; }
    if (!weightType)          { setError('Please select a weight type'); return; }
    if (warning)              { setError(warning); return; }

    setError('');
    setRoute(null);

    try {
      const response = await fetch('http://localhost:8080/api/generate-route', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startLat: startPos[0], startLng: startPos[1],
          endLat:   endPos[0],   endLng:   endPos[1],
          weightType,
        }),
      });
      const data = await response.json();
      if (!response.ok || !data.found) {
        setError(data.message || 'Unable to generate route');
        return;
      }
      setRoute(data);
    } catch {
      setError('Failed to connect to API. Is the Java server running?');
    }
  };

  const getNodeColour = (node: GraphNode): string => {
    if (node.nodeId === startNodeId) return '#f97316';
    if (node.nodeId === endNodeId)   return '#a855f7';
    return getComponentColour(node.componentId);
  };

  const routePositions = route?.coordinates?.map(
    c => [c.lat, c.lng] as [number, number]
  ) || [];

  // Build unique component list for legend
  const componentLegend = Array.from(
    new Map(graphNodes.map(n => [n.componentId, n.componentSize])).entries()
  ).sort((a, b) => b[1] - a[1]);

  return (
    <div style={{ position: 'relative', height: '100vh', width: '100%' }}>

      {/* Map */}
      <MapContainer
        bounds={[[53.739, -1.620], [53.870, -1.460]]}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapClickHandler onClick={handleMapClick} />

        {/* Graph nodes coloured by component */}
        {graphNodes.map(node => (
          <CircleMarker
            key={node.nodeId}
            center={[node.lat, node.lng]}
            radius={node.nodeId === startNodeId || node.nodeId === endNodeId ? 9 : 5}
            pathOptions={{
              color:       getNodeColour(node),
              fillColor:   getNodeColour(node),
              fillOpacity: 0.85,
              weight:      2,
            }}
            eventHandlers={{ click: () => handleNodeClick(node) }}
          >
            <Popup>
              <div style={{ fontSize: '12px', lineHeight: '1.6' }}>
                <strong>{node.nodeId}</strong><br />
                Component: {node.componentId} ({node.componentSize} nodes)<br />
                Lat: {node.lat.toFixed(5)}, Lng: {node.lng.toFixed(5)}
              </div>
            </Popup>
          </CircleMarker>
        ))}

        {startPos && !startNodeId && (
          <Marker position={startPos}><Popup>Start Point</Popup></Marker>
        )}
        {endPos && !endNodeId && (
          <Marker position={endPos}><Popup>End Point</Popup></Marker>
        )}

        {routePositions.length > 1 && (
          <Polyline
            positions={routePositions}
            pathOptions={{ color: '#f97316', weight: 4, opacity: 0.9 }}
          />
        )}
      </MapContainer>

      {/* Control panel */}
      <div style={{
        position: 'absolute', top: 10, left: 10, zIndex: 1000,
        padding: '10px', background: 'white', borderRadius: '4px',
        boxShadow: '0 1px 5px rgba(0,0,0,0.3)', fontSize: '13px',
        maxWidth: '410px'
      }}>

        {/* Controls row */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '8px', alignItems: 'center' }}>
          <button
            onClick={() => { setSettingMode('start'); setError(''); }}
            disabled={settingMode === 'start'}
            style={{ padding: '5px 10px', cursor: 'pointer' }}
          >
            {settingMode === 'start' ? 'Click map or node...' : 'Set Start Point'}
          </button>
          <button
            onClick={() => { setSettingMode('end'); setError(''); }}
            disabled={settingMode === 'end'}
            style={{ padding: '5px 10px', cursor: 'pointer' }}
          >
            {settingMode === 'end' ? 'Click map or node...' : 'Set End Point'}
          </button>
          <input
            type="text"
            value={coordInput}
            onChange={e => setCoordInput(e.target.value)}
            placeholder="lat, lng"
            style={{ width: '120px', padding: '4px' }}
          />
          <button
            onClick={setPointFromInput}
            disabled={!settingMode}
            style={{ padding: '5px 10px', cursor: 'pointer' }}
          >
            Set from Input
          </button>
          <button
            onClick={generateRoute}
            disabled={!startPos || !endPos}
            style={{ padding: '5px 10px', cursor: 'pointer' }}
          >
            Generate Route
          </button>
        </div>

        {/* Weight type row */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
          <span style={{ color: '#555' }}>Route Weighting:</span>
          {['ATHLETE', 'EFFORT', 'COMBINED',].map(type => (
    <button key={type} onClick={() => setWeightType(type)} style={{
        backgroundColor: weightType === type ? '#4CAF50' : '#f0f0f0',
        color: weightType === type ? 'white' : 'black',
        border: '1px solid #ccc', padding: '4px 10px', cursor: 'pointer',
    }}>
        {type}
    </button>
))}
        </div>

        {/* Warning */}
        {warning && (
          <div style={{ color: '#92400e', background: '#fef3c7', padding: '6px 8px', borderRadius: '3px', fontSize: '12px', marginBottom: '6px' }}>
            {warning}
          </div>
        )}

        {/* Error */}
        {error && (
          <div style={{ color: 'red', fontSize: '12px', marginBottom: '6px' }}>{error}</div>
        )}

        {/* Route result */}
        {route && (
          <div style={{ fontSize: '12px', borderTop: '1px solid #eee', paddingTop: '6px', marginBottom: '6px' }}>
            <div><strong>Weight type:</strong> {route.weightType}</div>
            <div><strong>Segments used:</strong> {route.segmentCount}</div>
            <div><strong>Nodes in path:</strong> {route.nodeSequence?.length}</div>
            {route.edges?.length > 0 && (
              <div style={{ marginTop: '4px' }}>
                <strong>Segments:</strong>
                {route.edges?.map((e, i) => (
    <div key={i} style={{ paddingLeft: '10px', color: '#444' }}>
        {weightType === 'ATHLETE'  && `- ${e.segmentName} (${e.athleteCount?.toLocaleString()} athletes)`}
        {weightType === 'EFFORT'   && `- ${e.segmentName} (${e.effortCount?.toLocaleString()} efforts)`}
        {weightType === 'COMBINED' && `- ${e.segmentName} (${Math.round((e.effortCount * 0.7) + (e.athleteCount * 0.3)).toLocaleString()} combined score)`}
        
    </div>
))}
              </div>
            )}
          </div>
        )}

        {/* Legend */}
        {graphNodes.length > 0 && (
          <div style={{ borderTop: '1px solid #eee', paddingTop: '6px', fontSize: '11px', color: '#555' }}>
            <div style={{ marginBottom: '3px', fontWeight: 'bold' }}>Components:</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              {/* Only show named components for those with more than 2 nodes */}
              {componentLegend.filter(([, size]) => size > 2).map(([compId, size]) => (
                <div key={compId} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <div style={{
                    width: 9, height: 9, borderRadius: '50%',
                    background: getComponentColour(compId as number),
                    flexShrink: 0
                  }} />
                  <span>{compId} ({size} nodes)</span>
                </div>
              ))}
              {/* Group all 2-node isolated segments together */}
              {componentLegend.some(([, size]) => size <= 2) && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <div style={{ width: 9, height: 9, borderRadius: '50%', background: '#94a3b8', flexShrink: 0 }} />
                  <span>Isolated ({componentLegend.filter(([, size]) => size <= 2).length} segments)</span>
                </div>
              )}
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <div style={{ width: 9, height: 9, borderRadius: '50%', background: '#f97316', flexShrink: 0 }} />
                <span>Start</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <div style={{ width: 9, height: 9, borderRadius: '50%', background: '#a855f7', flexShrink: 0 }} />
                <span>End</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;