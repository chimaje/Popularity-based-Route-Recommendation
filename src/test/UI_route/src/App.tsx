import React from 'react';
import { MapContainer, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css'; // Required for Leaflet styles

function App() {
  return (
    <div style={{ position: 'relative', height: '100vh', width: '100%' }}>
      <MapContainer center={[53.7965, -1.5479]} zoom={12} style={{ height: '100%', width: '100%' }} dragging={false}
        scrollWheelZoom={false}
        boxZoom={false}
        keyboard={false}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
      </MapContainer>
      <div style={{
        position: 'absolute',
        top: '10px',
        left: '10px',
        zIndex: 1000,
        display: 'flex',
        flexDirection: 'row',
        padding: '10px',
        borderRadius: '5px',
        marginLeft: '25px',
        gap: '15rem'
      }}>
        <div style={{ display: 'flex', flexDirection: 'row', marginRight: '20px' }}>
          <input type="text" placeholder="Enter start location" style={{ marginRight: '10px' }} />
          <input type="text" placeholder="Enter end location" style={{ marginRight: '10px' }} />
          <button>Search</button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <div style={{color: 'black'}}>Route Weighting Method:</div>
          <button>Athlete Weight</button>
          <button>Effort Weight</button>
          <button>Combined Weight</button>
        </div>
      </div>
    </div>
  );
}

export default App;
