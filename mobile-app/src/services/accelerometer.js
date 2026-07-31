// Threshold (in g, above the 1g baseline) that a magnitude spike must cross to count as a possible pothole hit.
// Tune this against real device test data — lower catches more bumps but raises false positives.
export const DETECTION_THRESHOLD = 0.35;

// Minimum GPS speed (m/s) required before we trust accelerometer spikes as road bumps
// rather than the phone being handled, dropped, or set down. ~1.5 m/s is a slow walk.
export const MIN_SPEED_MPS = 1.5;

export const ROLLING_BUFFER_SIZE = 10;

const GRAVITY = 1;

export function computeMagnitude({ x, y, z }) {
  return Math.sqrt(x * x + y * y + z * z);
}

export function computeDelta(rawMagnitude) {
  return Math.abs(rawMagnitude - GRAVITY);
}

export function estimateSeverity(delta) {
  if (delta >= DETECTION_THRESHOLD * 2.5) return 'high';
  if (delta >= DETECTION_THRESHOLD * 1.5) return 'medium';
  return 'low';
}
