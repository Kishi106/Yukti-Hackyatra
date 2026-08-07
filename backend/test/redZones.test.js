const { dbscanHaversine, summarizeCluster } = require('../src/redZones');
const { RED_ZONE_MIN_SAMPLES } = require('../src/geoConfig');

const BASE_LAT = 17.6868;
const BASE_LNG = 83.2185;
const METERS_PER_DEG_LAT = 111320;

function offsetPoint(id, dLatMeters, dLngMeters) {
  const cosLat = Math.cos((BASE_LAT * Math.PI) / 180);
  return {
    id,
    lat: BASE_LAT + dLatMeters / METERS_PER_DEG_LAT,
    lng: BASE_LNG + dLngMeters / (METERS_PER_DEG_LAT * cosLat)
  };
}

// 20 points packed into a small grid spanning ~8m x 6m (offset from
// (originLatM, originLngM) meters from BASE) — every pair is well under the
// 20-30m red-zone eps, so this should collapse into one cluster.
function denseCluster(idPrefix, count = 20, originLatM = 0, originLngM = 0) {
  const points = [];
  for (let i = 0; i < count; i++) {
    const row = Math.floor(i / 5);
    const col = i % 5;
    points.push(offsetPoint(`${idPrefix}-${i}`, originLatM + row * 2, originLngM + col * 2));
  }
  return points;
}

describe('dbscanHaversine — red zone clustering', () => {
  test('20 potholes within ~20-30m trigger a red zone (one cluster, all members)', () => {
    const points = denseCluster('dense', 20);
    const clusters = dbscanHaversine(points);

    expect(clusters).toHaveLength(1);
    expect(clusters[0]).toHaveLength(20);
  });

  test('a handful of widely-scattered potholes do not trigger a red zone', () => {
    const points = [
      offsetPoint('a', 0, 0),
      offsetPoint('b', 500, 0),
      offsetPoint('c', 0, 500),
      offsetPoint('d', -500, -500)
    ];
    const clusters = dbscanHaversine(points);
    expect(clusters).toHaveLength(0);
  });

  test('a tight group smaller than the minimum sample size does not trigger a red zone', () => {
    expect(RED_ZONE_MIN_SAMPLES).toBeGreaterThan(3);
    const points = [offsetPoint('a', 0, 0), offsetPoint('b', 2, 0), offsetPoint('c', 0, 2)];
    const clusters = dbscanHaversine(points);
    expect(clusters).toHaveLength(0);
  });

  test('two separate dense groups far apart form two clusters, not one', () => {
    const groupA = denseCluster('A', 20, 0, 0);
    const groupB = denseCluster('B', 20, 0, 2000); // 2km east — nowhere near groupA
    const clusters = dbscanHaversine([...groupA, ...groupB]);

    expect(clusters).toHaveLength(2);
    expect(clusters[0]).toHaveLength(20);
    expect(clusters[1]).toHaveLength(20);
  });
});

describe('summarizeCluster', () => {
  test('risk score and pothole count scale with cluster size', () => {
    const points = denseCluster('dense', 20);
    const summary = summarizeCluster(points);

    expect(summary.potholeCount).toBe(20);
    expect(summary.riskScore).toBeGreaterThan(0);
    expect(summary.memberIds).toHaveLength(20);
  });
});
