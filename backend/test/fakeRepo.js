const { haversineMeters, boundingBoxDegrees } = require('../src/geo');
const { MERGE_CANDIDATE_BBOX_METERS, MERGE_RADIUS_METERS } = require('../src/geoConfig');

// In-memory stand-in for potholeMerge.js's DB-backed repo, implementing the
// same five methods with the same bbox-prefilter shape as the real
// Postgres queries — so these tests exercise real matching behavior, not a
// hollowed-out mock.
function createFakeRepo() {
  const potholes = [];
  const rawDetections = [];
  let nextId = 1;

  function inBbox(row, lat, lng, radiusMeters) {
    const box = boundingBoxDegrees(lat, lng, radiusMeters);
    return row.lat >= box.minLat && row.lat <= box.maxLat && row.lng >= box.minLng && row.lng <= box.maxLng;
  }

  return {
    potholes,
    rawDetections,

    async findCandidatePotholes(lat, lng) {
      return potholes.filter((p) => inBbox(p, lat, lng, MERGE_CANDIDATE_BBOX_METERS));
    },

    async findSessionDuplicate(sessionId, lat, lng) {
      if (!sessionId) return null;
      return (
        rawDetections
          .filter((d) => d.session_id === sessionId && inBbox(d, lat, lng, MERGE_CANDIDATE_BBOX_METERS))
          .find((d) => haversineMeters(d.lat, d.lng, lat, lng) <= MERGE_RADIUS_METERS) || null
      );
    },

    async insertPothole(fields) {
      const pothole = {
        id: `pothole-${nextId++}`,
        lat: fields.lat,
        lng: fields.lng,
        severity: fields.severity,
        source: fields.source,
        confidence_score: fields.confidence_score,
        detection_count: 1,
        report_count: fields.source === 'citizen' ? 1 : 0
      };
      potholes.push(pothole);
      return pothole;
    },

    async updatePotholeAggregate(id, fields) {
      const pothole = potholes.find((p) => p.id === id);
      Object.assign(pothole, fields);
      return pothole;
    },

    async insertRawDetection(fields) {
      const detection = { id: `detection-${nextId++}`, ...fields };
      rawDetections.push(detection);
      return detection;
    }
  };
}

module.exports = { createFakeRepo };
