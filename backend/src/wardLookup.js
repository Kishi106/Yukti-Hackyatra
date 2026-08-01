const { query } = require('./db');
const { booleanPointInPolygon } = require('@turf/boolean-point-in-polygon');
const { point, polygon, multiPolygon } = require('@turf/helpers');

let wardCache = [];

function buildTurfGeometry(geometry) {
  if (!geometry || !geometry.type || !geometry.coordinates) return null;
  try {
    if (geometry.type === 'Polygon') {
      return polygon(geometry.coordinates);
    }
    if (geometry.type === 'MultiPolygon') {
      return multiPolygon(geometry.coordinates);
    }
  } catch (error) {
    console.error('Skipping ward boundary with invalid geometry:', error.message);
  }
  return null;
}

async function refreshWardCache() {
  const result = await query('SELECT ward_no, zone_id, ward_name, geometry FROM ward_boundaries');
  wardCache = result.rows
    .map((row) => ({
      wardNo: row.ward_no,
      zoneId: row.zone_id,
      wardName: row.ward_name,
      geometry: buildTurfGeometry(row.geometry)
    }))
    .filter((ward) => ward.geometry !== null);
  return wardCache;
}

// Warm the cache once on module load. Fire-and-forget: resolveWard() simply has
// nothing to match against until this resolves (a negligible window right after
// server startup), rather than blocking the whole module system on a DB round trip.
refreshWardCache().catch((error) => {
  console.error('Failed to load ward boundary cache:', error.message);
});

function resolveWard(lat, lng) {
  const pt = point([lng, lat]);
  for (const ward of wardCache) {
    try {
      if (booleanPointInPolygon(pt, ward.geometry)) {
        return { wardNo: ward.wardNo, zoneId: ward.zoneId, wardName: ward.wardName };
      }
    } catch (error) {
      // Skip a ward whose geometry fails the point-in-polygon check at runtime.
    }
  }
  return null;
}

module.exports = { resolveWard, refreshWardCache };
