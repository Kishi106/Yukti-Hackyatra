// Pure geo math — no DB, no I/O. Kept dependency-free (plain haversine)
// rather than pulling in a geo library for two formulas.

const EARTH_RADIUS_METERS = 6371000;

function toRad(deg) {
  return (deg * Math.PI) / 180;
}

// Great-circle distance between two lat/lng points, in meters.
function haversineMeters(lat1, lng1, lat2, lng2) {
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
}

// Rough degree-space bounding box for a given radius in meters, centered on
// (lat, lng). Deliberately approximate (flat-earth at this scale is fine for
// a pre-filter) — every candidate it returns still gets an exact haversine
// check afterwards, so this only needs to avoid *missing* real candidates.
function boundingBoxDegrees(lat, lng, radiusMeters) {
  const latDelta = radiusMeters / 111320;
  const cosLat = Math.cos(toRad(lat));
  const lngDelta = radiusMeters / (111320 * (Math.abs(cosLat) > 1e-6 ? cosLat : 1e-6));
  return {
    minLat: lat - latDelta,
    maxLat: lat + latDelta,
    minLng: lng - lngDelta,
    maxLng: lng + lngDelta
  };
}

module.exports = { haversineMeters, boundingBoxDegrees };
