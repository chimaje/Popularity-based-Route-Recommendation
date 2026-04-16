import { useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

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
    distanceMetres: number;
    weight: number;
  }>;
  totalWeight: number;
  segmentCount: number;
}

function MapClickHandler({ onClick }: { onClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click: (e) => {
      onClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

function App() {
  const [startPos, setStartPos] = useState<[number, number] | null>(null);
  const [endPos, setEndPos] = useState<[number, number] | null>(null);
  const [weightType, setWeightType] = useState('');
  const [route, setRoute] = useState<RouteResult | null>(null);
  const [error, setError] = useState('');
  const [settingMode, setSettingMode] = useState<'start' | 'end' | null>(null);

  const handleMapClick = (lat: number, lng: number) => {
    const roundedLat = Math.round(lat * 100000) / 100000;
    const roundedLng = Math.round(lng * 100000) / 100000;
    if (settingMode === 'start') {
      setStartPos([roundedLat, roundedLng]);
      setSettingMode(null);
    } else if (settingMode === 'end') {
      setEndPos([roundedLat, roundedLng]);
      setSettingMode(null);
    }
  };

  const generateRoute = async () => {
    if (!startPos || !endPos) {
      setError('Please set both start and end points');
      return;
    }
    setError('');
    setRoute(null);
    console.log('Generating route with:', { startPos, endPos, weightType });
    const response = await fetch('http://localhost:8080/api/generate-route', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startLat: startPos[0],
        startLng: startPos[1],
        endLat: endPos[0],
        endLng: endPos[1],
        weightType,
      }),
    });

    const data = await response.json();
    if (!response.ok || !data.found) {
      setError(data.message || 'Unable to generate route');
      return;
    }

    setRoute(data);
  };
  return (
    <div style={{ position: 'relative', height: '100vh', width: '100%' }}>
      <MapContainer bounds={[[53.739, -1.620], [53.870, -1.460]]} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapClickHandler onClick={handleMapClick} />
        {startPos && (
          <Marker position={startPos}>
            <Popup>Start Point</Popup>
          </Marker>
        )}
        {endPos && (
          <Marker position={endPos}>
            <Popup>End Point</Popup>
          </Marker>
        )}
        {route?.coordinates.length ? (
          <Polyline
            positions={route.coordinates.map((coord) => [coord.lat, coord.lng] as [number, number])}
            color="blue"
          />
        ) : null}
      </MapContainer>

      <div style={{ position: 'absolute', top: 10, left: 10, zIndex: 1000, padding: 10, background: 'white' }}>
        <div>
          <button onClick={() => setSettingMode('start')} disabled={settingMode === 'start'}>
            {settingMode === 'start' ? 'Click map for start' : 'Set Start Point'}
          </button>
          <button onClick={() => setSettingMode('end')} disabled={settingMode === 'end'}>
            {settingMode === 'end' ? 'Click map for end' : 'Set End Point'}
          </button>
          <button onClick={generateRoute} disabled={!startPos || !endPos}>
            Generate Route
          </button>
        </div>

        <div>
          <span>Route Weighting:</span>
          <button
            onClick={() => setWeightType('ATHLETE')}
            style={{
              backgroundColor: weightType === 'ATHLETE' ? '#4CAF50' : '#f0f0f0',
              color: weightType === 'ATHLETE' ? 'white' : 'black',
              border: '1px solid #ccc',
              padding: '5px 10px',
              margin: '0 5px',
              cursor: 'pointer',
            }}
          >
            Athlete
          </button>
          <button
            onClick={() => setWeightType('EFFORT')}
            style={{
              backgroundColor: weightType === 'EFFORT' ? '#4CAF50' : '#f0f0f0',
              color: weightType === 'EFFORT' ? 'white' : 'black',
              border: '1px solid #ccc',
              padding: '5px 10px',
              margin: '0 5px',
              cursor: 'pointer',
            }}
          >
            Effort
          </button>
          <button
            onClick={() => setWeightType('COMBINED')}
            style={{
              backgroundColor: weightType === 'COMBINED' ? '#4CAF50' : '#f0f0f0',
              color: weightType === 'COMBINED' ? 'white' : 'black',
              border: '1px solid #ccc',
              padding: '5px 10px',
              margin: '0 5px',
              cursor: 'pointer',
            }}
          >
            Combined
          </button>
        </div>

        {error && <div style={{ color: 'red' }}>{error}</div>}
        {route && (
          <div>
            <div>Weight type: {route.weightType}</div>
            <div>Distance segments: {route.segmentCount}</div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;