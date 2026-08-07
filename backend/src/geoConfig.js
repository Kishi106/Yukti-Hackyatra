// Tunable thresholds for Level 1 duplicate-merge and Level 2 red-zone
// clustering. Named here rather than left as magic numbers inline — expect
// these to need retuning against real GPS noise/road-density data.
module.exports = {
  // Level 1: a new detection within this many meters of an existing pothole
  // is treated as the same physical pothole and merged, not a new pin.
  MERGE_RADIUS_METERS: 5,

  // Cheap bounding-box pre-filter radius (in meters) used before the exact
  // haversine check, so merge lookups don't scan the whole potholes table.
  // Wider than MERGE_RADIUS_METERS on purpose (a box, not a circle, needs
  // slack at the corners).
  MERGE_CANDIDATE_BBOX_METERS: 15,

  // Additive confidence bump applied to a pothole each time a new detection
  // merges into it (replaces the old bounding-box hack that bumped nearby
  // rows' confidence without actually merging them).
  MERGE_CONFIDENCE_BONUS: 15,

  // Level 2: DBSCAN epsilon (meters) and minimum cluster size for a group of
  // *potholes* (not raw detections) to be flagged as a RED ZONE.
  RED_ZONE_EPS_METERS: 25,
  RED_ZONE_MIN_SAMPLES: 5,

  // Simple v1 risk score: capped linear function of cluster size. Revisit
  // once we have real severity/traffic-volume data to weight by.
  RED_ZONE_RISK_PER_POTHOLE: 5,
  RED_ZONE_MIN_RADIUS_METERS: 10
};
