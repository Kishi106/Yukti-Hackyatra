const { matchOrCreatePothole, pickClosestMatch, mergeDetectionInto } = require('../src/potholeMerge');
const { createFakeRepo } = require('./fakeRepo');

const BASE_LAT = 17.6868;
const BASE_LNG = 83.2185;

function detection(overrides = {}) {
  return {
    lat: BASE_LAT,
    lng: BASE_LNG,
    severity: 'medium',
    source: 'citizen',
    ward: 'Test Ward',
    wardNo: 1,
    photo_url: null,
    reporter_id: null,
    session_id: null,
    confidence_score: 0,
    ...overrides
  };
}

describe('pickClosestMatch (pure)', () => {
  test('returns null when nothing is within radius', () => {
    const candidates = [{ id: 'a', lat: BASE_LAT + 1, lng: BASE_LNG }];
    expect(pickClosestMatch(candidates, BASE_LAT, BASE_LNG, 5)).toBeNull();
  });
});

describe('matchOrCreatePothole — Level 1 merge', () => {
  test('two reports within 5m merge into one pothole record with count=2', async () => {
    const repo = createFakeRepo();

    const first = await matchOrCreatePothole(detection({ lat: BASE_LAT, lng: BASE_LNG }), repo);
    expect(first.merged).toBe(false); // first sighting, nothing to merge into

    // ~2m away (well under the 5m merge radius)
    const second = await matchOrCreatePothole(detection({ lat: BASE_LAT + 0.000018, lng: BASE_LNG }), repo);

    expect(second.merged).toBe(true);
    expect(second.pothole.id).toBe(first.pothole.id);
    expect(repo.potholes).toHaveLength(1);
    expect(repo.potholes[0].detection_count).toBe(2);
  });

  test('a report more than 5m away creates a new record', async () => {
    const repo = createFakeRepo();

    await matchOrCreatePothole(detection({ lat: BASE_LAT, lng: BASE_LNG }), repo);
    // ~20m away — outside both the 5m merge radius and the 15m candidate bbox
    const second = await matchOrCreatePothole(detection({ lat: BASE_LAT + 0.00018, lng: BASE_LNG }), repo);

    expect(second.merged).toBe(false);
    expect(repo.potholes).toHaveLength(2);
    expect(second.pothole.id).not.toBe(repo.potholes[0].id);
  });

  test('4 dashcam detections of the same pothole at slightly different coords merge into one record', async () => {
    const repo = createFakeRepo();
    const offsets = [
      [0, 0],
      [0.00001, 0.00001],
      [-0.00001, 0.00001],
      [0.000005, -0.00001]
    ];

    let potholeId = null;
    for (const [dLat, dLng] of offsets) {
      const result = await matchOrCreatePothole(
        detection({ lat: BASE_LAT + dLat, lng: BASE_LNG + dLng, source: 'auto' }),
        repo
      );
      potholeId = potholeId || result.pothole.id;
      expect(result.pothole.id).toBe(potholeId);
    }

    expect(repo.potholes).toHaveLength(1);
    expect(repo.potholes[0].detection_count).toBe(4);
  });

  test('raw detections are never lost, even after merging', async () => {
    const repo = createFakeRepo();

    await matchOrCreatePothole(detection({ lat: BASE_LAT, lng: BASE_LNG }), repo);
    await matchOrCreatePothole(detection({ lat: BASE_LAT + 0.00001, lng: BASE_LNG }), repo);
    await matchOrCreatePothole(detection({ lat: BASE_LAT + 0.00002, lng: BASE_LNG }), repo);

    expect(repo.potholes).toHaveLength(1);
    // Every one of the 3 incoming detections produced its own raw_detections
    // row, regardless of whether it merged.
    expect(repo.rawDetections).toHaveLength(3);
    expect(repo.rawDetections.every((d) => d.matched_pothole_id === repo.potholes[0].id)).toBe(true);
  });

  test('repeated pings from the same dashcam session within the merge radius collapse into one detection', async () => {
    const repo = createFakeRepo();

    const first = await matchOrCreatePothole(
      detection({ lat: BASE_LAT, lng: BASE_LNG, source: 'auto', session_id: 'trip-1' }),
      repo
    );
    const secondPing = await matchOrCreatePothole(
      detection({ lat: BASE_LAT + 0.00001, lng: BASE_LNG, source: 'auto', session_id: 'trip-1' }),
      repo
    );

    expect(secondPing.collapsedSessionDuplicate).toBe(true);
    expect(repo.potholes).toHaveLength(1);
    expect(repo.potholes[0].detection_count).toBe(1); // not bumped by the collapsed ping
    expect(repo.rawDetections).toHaveLength(1); // second ping never inserted
    expect(secondPing.pothole.id).toBe(first.pothole.id);
  });

  test('a different session at the same spot still merges normally (not collapsed)', async () => {
    const repo = createFakeRepo();

    await matchOrCreatePothole(detection({ lat: BASE_LAT, lng: BASE_LNG, session_id: 'trip-1' }), repo);
    const otherTrip = await matchOrCreatePothole(
      detection({ lat: BASE_LAT + 0.00001, lng: BASE_LNG, session_id: 'trip-2' }),
      repo
    );

    expect(otherTrip.collapsedSessionDuplicate).toBe(false);
    expect(otherTrip.merged).toBe(true);
    expect(repo.potholes[0].detection_count).toBe(2);
  });
});

describe('mergeDetectionInto (pure)', () => {
  test('severity only escalates, never downgrades', () => {
    const existing = { lat: BASE_LAT, lng: BASE_LNG, severity: 'high', confidence_score: 50, detection_count: 1, report_count: 1 };
    const merged = mergeDetectionInto(existing, detection({ severity: 'low' }));
    expect(merged.severity).toBe('high');
  });

  test('confidence is capped at 100', () => {
    const existing = { lat: BASE_LAT, lng: BASE_LNG, severity: 'low', confidence_score: 95, detection_count: 1, report_count: 1 };
    const merged = mergeDetectionInto(existing, detection());
    expect(merged.confidence_score).toBe(100);
  });
});
