const { query } = require('./db');
const { haversineMeters, boundingBoxDegrees } = require('./geo');
const {
  MERGE_RADIUS_METERS,
  MERGE_CANDIDATE_BBOX_METERS,
  MERGE_CONFIDENCE_BONUS
} = require('./geoConfig');

const SEVERITY_RANK = { low: 1, medium: 2, high: 3 };

// Real Postgres-backed implementation. Tests inject a fake implementing the
// same five methods instead of hitting the database.
const defaultRepo = {
  async findCandidatePotholes(lat, lng) {
    const box = boundingBoxDegrees(lat, lng, MERGE_CANDIDATE_BBOX_METERS);
    const result = await query(
      `SELECT * FROM potholes WHERE lat BETWEEN $1 AND $2 AND lng BETWEEN $3 AND $4`,
      [box.minLat, box.maxLat, box.minLng, box.maxLng]
    );
    return result.rows;
  },

  // Most recent raw_detection from the same dashcam session within the
  // merge radius — used to collapse repeated pings from a single pass so
  // one trip over a pothole can't inflate detection_count.
  async findSessionDuplicate(sessionId, lat, lng) {
    if (!sessionId) return null;
    const box = boundingBoxDegrees(lat, lng, MERGE_CANDIDATE_BBOX_METERS);
    const result = await query(
      `SELECT * FROM raw_detections
       WHERE session_id = $1 AND lat BETWEEN $2 AND $3 AND lng BETWEEN $4 AND $5
       ORDER BY detected_at DESC`,
      [sessionId, box.minLat, box.maxLat, box.minLng, box.maxLng]
    );
    return result.rows.find((row) => haversineMeters(row.lat, row.lng, lat, lng) <= MERGE_RADIUS_METERS) || null;
  },

  async insertPothole(fields) {
    const result = await query(
      `INSERT INTO potholes
         (lat, lng, severity, source, ward, ward_no, photo_url, reporter_id,
          confidence_score, detection_count, report_count, last_seen_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 1, $10, NOW())
       RETURNING *`,
      [
        fields.lat,
        fields.lng,
        fields.severity,
        fields.source,
        fields.ward,
        fields.wardNo,
        fields.photo_url,
        fields.reporter_id,
        fields.confidence_score,
        fields.source === 'citizen' ? 1 : 0
      ]
    );
    return result.rows[0];
  },

  async updatePotholeAggregate(id, fields) {
    const result = await query(
      `UPDATE potholes
       SET lat = $1, lng = $2, severity = $3, confidence_score = $4,
           detection_count = $5, report_count = $6, last_seen_at = NOW()
       WHERE id = $7
       RETURNING *`,
      [fields.lat, fields.lng, fields.severity, fields.confidence_score, fields.detection_count, fields.report_count, id]
    );
    return result.rows[0];
  },

  async insertRawDetection(fields) {
    const result = await query(
      `INSERT INTO raw_detections
         (source, session_id, lat, lng, confidence_score, photo_url, reporter_id, matched_pothole_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        fields.source,
        fields.session_id ?? null,
        fields.lat,
        fields.lng,
        fields.confidence_score ?? 0,
        fields.photo_url ?? null,
        fields.reporter_id ?? null,
        fields.matched_pothole_id
      ]
    );
    return result.rows[0];
  }
};

// Pure — picks the closest candidate within MERGE_RADIUS_METERS, or null.
// Exported separately so it's testable without any repo/DB involvement.
function pickClosestMatch(candidates, lat, lng, radiusMeters = MERGE_RADIUS_METERS) {
  let best = null;
  let bestDistance = Infinity;
  for (const candidate of candidates) {
    const distance = haversineMeters(candidate.lat, candidate.lng, lat, lng);
    if (distance <= radiusMeters && distance < bestDistance) {
      best = candidate;
      bestDistance = distance;
    }
  }
  return best;
}

// Pure — folds one new detection into an existing pothole's aggregate
// fields. Running-mean centroid (weighted by detection_count so far), max
// severity seen, additive confidence bonus capped at 100, count increments.
function mergeDetectionInto(existing, detection) {
  const priorCount = existing.detection_count ?? 1;
  const nextCount = priorCount + 1;
  const nextLat = (existing.lat * priorCount + detection.lat) / nextCount;
  const nextLng = (existing.lng * priorCount + detection.lng) / nextCount;
  const nextSeverity =
    (SEVERITY_RANK[detection.severity] || 0) > (SEVERITY_RANK[existing.severity] || 0)
      ? detection.severity
      : existing.severity;
  const nextConfidence = Math.min(100, (existing.confidence_score ?? 0) + MERGE_CONFIDENCE_BONUS);
  const nextReportCount = (existing.report_count ?? 0) + (detection.source === 'citizen' ? 1 : 0);

  return {
    lat: nextLat,
    lng: nextLng,
    severity: nextSeverity,
    confidence_score: nextConfidence,
    detection_count: nextCount,
    report_count: nextReportCount
  };
}

// Orchestrator: match-or-create for one incoming detection. `repo` defaults
// to the real DB-backed implementation; tests pass an in-memory fake.
async function matchOrCreatePothole(detection, repo = defaultRepo) {
  const sessionDuplicate = await repo.findSessionDuplicate(detection.session_id, detection.lat, detection.lng);
  if (sessionDuplicate) {
    // Same dashcam pass already logged a ping here — don't double-count it,
    // and don't insert a second raw_detection for the same pass/location.
    const candidates = await repo.findCandidatePotholes(detection.lat, detection.lng);
    const existing = candidates.find((c) => c.id === sessionDuplicate.matched_pothole_id);
    return { pothole: existing || null, merged: false, collapsedSessionDuplicate: true };
  }

  const candidates = await repo.findCandidatePotholes(detection.lat, detection.lng);
  const match = pickClosestMatch(candidates, detection.lat, detection.lng);

  if (match) {
    const aggregate = mergeDetectionInto(match, detection);
    const pothole = await repo.updatePotholeAggregate(match.id, aggregate);
    await repo.insertRawDetection({ ...detection, matched_pothole_id: pothole.id });
    return { pothole, merged: true, collapsedSessionDuplicate: false };
  }

  const pothole = await repo.insertPothole(detection);
  await repo.insertRawDetection({ ...detection, matched_pothole_id: pothole.id });
  return { pothole, merged: false, collapsedSessionDuplicate: false };
}

module.exports = { matchOrCreatePothole, pickClosestMatch, mergeDetectionInto, defaultRepo };
