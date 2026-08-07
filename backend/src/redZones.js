const { query, pool } = require('./db');
const { haversineMeters } = require('./geo');
const {
  RED_ZONE_EPS_METERS,
  RED_ZONE_MIN_SAMPLES,
  RED_ZONE_RISK_PER_POTHOLE,
  RED_ZONE_MIN_RADIUS_METERS
} = require('./geoConfig');

// Pure DBSCAN over { id, lat, lng } points using haversine distance.
// O(n^2) region queries — fine at the current scale (hundreds of potholes,
// recomputed as a full batch, see recomputeAllRedZones below). If this ever
// needs to scale to tens of thousands of rows, that's the point to move to
// PostGIS (ST_ClusterDBSCAN) instead of hand-rolling it further.
function dbscanHaversine(points, epsMeters = RED_ZONE_EPS_METERS, minSamples = RED_ZONE_MIN_SAMPLES) {
  const n = points.length;
  const labels = new Array(n).fill(undefined); // undefined = unvisited, null = noise, number = cluster index

  function regionQuery(idx) {
    const p = points[idx];
    const neighbors = [];
    for (let j = 0; j < n; j++) {
      if (j === idx) continue;
      if (haversineMeters(p.lat, p.lng, points[j].lat, points[j].lng) <= epsMeters) {
        neighbors.push(j);
      }
    }
    return neighbors;
  }

  const clusters = [];
  for (let i = 0; i < n; i++) {
    if (labels[i] !== undefined) continue;
    const neighbors = regionQuery(i);
    if (neighbors.length + 1 < minSamples) {
      labels[i] = null;
      continue;
    }
    const clusterIndex = clusters.length;
    clusters.push([i]);
    labels[i] = clusterIndex;

    const queue = [...neighbors];
    while (queue.length) {
      const j = queue.shift();
      if (labels[j] === null) {
        labels[j] = clusterIndex;
        clusters[clusterIndex].push(j);
      }
      if (labels[j] !== undefined) continue;
      labels[j] = clusterIndex;
      clusters[clusterIndex].push(j);
      const jNeighbors = regionQuery(j);
      if (jNeighbors.length + 1 >= minSamples) {
        queue.push(...jNeighbors);
      }
    }
  }

  return clusters.map((idxs) => idxs.map((i) => points[i]));
}

// Pure — centroid + radius (max member distance from centroid, floored) for
// one cluster, plus a simple size-based risk score.
function summarizeCluster(members) {
  const centroidLat = members.reduce((sum, m) => sum + m.lat, 0) / members.length;
  const centroidLng = members.reduce((sum, m) => sum + m.lng, 0) / members.length;
  const maxDistance = members.reduce(
    (max, m) => Math.max(max, haversineMeters(centroidLat, centroidLng, m.lat, m.lng)),
    0
  );
  return {
    centroidLat,
    centroidLng,
    radiusM: Math.max(RED_ZONE_MIN_RADIUS_METERS, maxDistance),
    potholeCount: members.length,
    riskScore: Math.min(100, members.length * RED_ZONE_RISK_PER_POTHOLE),
    memberIds: members.map((m) => m.id)
  };
}

const defaultRepo = {
  async findAllPotholes() {
    const result = await query('SELECT id, lat, lng FROM potholes');
    return result.rows;
  },

  // Full replace, in a transaction: clear old zones/memberships, insert the
  // freshly computed ones. Simple v1 approach — see recomputeAllRedZones.
  async replaceRedZones(clusterSummaries) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      await client.query('UPDATE potholes SET red_zone_id = NULL WHERE red_zone_id IS NOT NULL');
      await client.query('DELETE FROM red_zones');

      const zones = [];
      for (const summary of clusterSummaries) {
        const result = await client.query(
          `INSERT INTO red_zones (centroid_lat, centroid_lng, radius_m, pothole_count, risk_score)
           VALUES ($1, $2, $3, $4, $5)
           RETURNING *`,
          [summary.centroidLat, summary.centroidLng, summary.radiusM, summary.potholeCount, summary.riskScore]
        );
        const zone = result.rows[0];
        zones.push(zone);

        for (const potholeId of summary.memberIds) {
          await client.query(
            `INSERT INTO red_zone_members (red_zone_id, pothole_id) VALUES ($1, $2)`,
            [zone.id, potholeId]
          );
          await client.query('UPDATE potholes SET red_zone_id = $1 WHERE id = $2', [zone.id, potholeId]);
        }
      }

      await client.query('COMMIT');
      return zones;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
};

// v1 tradeoff, flagged as such per the spec: a full batch recompute over
// every pothole rather than a true incrementally-scoped one. At the current
// data volume (hundreds of rows) this is fast (single-digit milliseconds to
// low tens of ms) and avoids the correctness pitfalls of a naive
// neighborhood-only DBSCAN (clusters splitting/merging at the boundary of
// whatever "local" window was chosen). Revisit if the table grows enough
// for O(n^2) to matter — candidate fix is a real incremental/windowed
// DBSCAN, or moving clustering into PostGIS.
async function recomputeAllRedZones(repo = defaultRepo) {
  const potholes = await repo.findAllPotholes();
  const points = potholes.map((p) => ({ id: p.id, lat: p.lat, lng: p.lng }));
  const clusters = dbscanHaversine(points);
  const summaries = clusters.map(summarizeCluster);
  return repo.replaceRedZones(summaries);
}

module.exports = { dbscanHaversine, summarizeCluster, recomputeAllRedZones, defaultRepo };
