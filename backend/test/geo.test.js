const { haversineMeters, boundingBoxDegrees } = require('../src/geo');

describe('haversineMeters', () => {
  test('same point is zero distance', () => {
    expect(haversineMeters(17.6868, 83.2185, 17.6868, 83.2185)).toBeCloseTo(0, 3);
  });

  test('two points ~5m apart at Visakhapatnam latitude', () => {
    // ~0.000045 deg latitude ~= 5m
    const distance = haversineMeters(17.6868, 83.2185, 17.68684, 83.2185);
    expect(distance).toBeGreaterThan(3);
    expect(distance).toBeLessThan(7);
  });

  test('two points ~1km apart', () => {
    const distance = haversineMeters(17.6868, 83.2185, 17.6958, 83.2185);
    expect(distance).toBeGreaterThan(950);
    expect(distance).toBeLessThan(1050);
  });
});

describe('boundingBoxDegrees', () => {
  test('box contains the center point with margin on all sides', () => {
    const box = boundingBoxDegrees(17.6868, 83.2185, 20);
    expect(box.minLat).toBeLessThan(17.6868);
    expect(box.maxLat).toBeGreaterThan(17.6868);
    expect(box.minLng).toBeLessThan(83.2185);
    expect(box.maxLng).toBeGreaterThan(83.2185);
  });
});
