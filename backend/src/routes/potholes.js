const express = require('express');
const { query } = require('../db');
const { SEVERITY_VALUES, SOURCE_VALUES, STATUS_VALUES } = require('../models/pothole');
const { resolveWard } = require('../wardLookup');
const { matchOrCreatePothole } = require('../potholeMerge');
const { recomputeAllRedZones } = require('../redZones');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const { status, ward, ward_no: wardNo } = req.query;
    const conditions = [];
    const params = [];

    if (status) {
      params.push(status);
      conditions.push(`status = $${params.length}`);
    }
    if (ward) {
      params.push(ward);
      conditions.push(`ward = $${params.length}`);
    }
    if (wardNo !== undefined) {
      const parsedWardNo = parseInt(wardNo, 10);
      if (Number.isNaN(parsedWardNo)) {
        return res.status(400).json({ error: 'ward_no must be an integer' });
      }
      params.push(parsedWardNo);
      conditions.push(`ward_no = $${params.length}`);
    }

    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await query(`SELECT * FROM potholes ${where} ORDER BY created_at DESC`, params);
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', async (req, res) => {
  try {
    const { lat, lng, severity, source, ward, photo_url, reporter_id, session_id } = req.body;

    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return res.status(400).json({ error: 'lat and lng are required and must be numbers' });
    }
    if (!SEVERITY_VALUES.includes(severity)) {
      return res.status(400).json({ error: `severity must be one of: ${SEVERITY_VALUES.join(', ')}` });
    }
    if (!SOURCE_VALUES.includes(source)) {
      return res.status(400).json({ error: `source must be one of: ${SOURCE_VALUES.join(', ')}` });
    }

    // The real GPS coordinates are authoritative over any client-supplied ward text.
    let wardName = ward ?? null;
    let wardNo = null;
    const resolvedWard = resolveWard(lat, lng);
    if (resolvedWard) {
      wardNo = resolvedWard.wardNo;
      wardName = resolvedWard.wardName;
    }

    let confidenceScore = source === 'auto' ? 30 : 0;
    if (typeof photo_url === 'string' && photo_url.trim()) {
      confidenceScore += 30;
    }
    confidenceScore = Math.min(confidenceScore, 100);

    // Level 1: match against potholes within MERGE_RADIUS_METERS instead of
    // always inserting a new row — see potholeMerge.js.
    const { pothole, merged, collapsedSessionDuplicate } = await matchOrCreatePothole({
      lat,
      lng,
      severity,
      source,
      ward: wardName,
      wardNo,
      photo_url: photo_url ?? null,
      reporter_id: reporter_id ?? null,
      session_id: session_id ?? null,
      confidence_score: confidenceScore
    });

    if (collapsedSessionDuplicate) {
      // Same dashcam pass re-pinging the same spot — not a new detection.
      return res.status(200).json({ ...pothole, merged: false, collapsedSessionDuplicate: true });
    }

    // Level 2: re-run red-zone clustering now that the pothole set changed.
    // Full-table batch recompute (see redZones.js) — cheap at current scale,
    // and a failure here shouldn't fail the report itself.
    try {
      await recomputeAllRedZones();
    } catch (redZoneError) {
      console.error('Red zone recompute failed:', redZoneError);
    }

    res.status(201).json({ ...pothole, merged, collapsedSessionDuplicate: false });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/:id/detections', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await query(
      `SELECT * FROM raw_detections WHERE matched_pothole_id = $1 ORDER BY detected_at ASC`,
      [id]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.patch('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    if (!STATUS_VALUES.includes(status)) {
      return res.status(400).json({ error: `status must be one of: ${STATUS_VALUES.join(', ')}` });
    }

    const result = await query(
      `UPDATE potholes SET status = $1 WHERE id = $2 RETURNING *`,
      [status, id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Pothole not found' });
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.patch('/:id/confirm', async (req, res) => {
  try {
    const { id } = req.params;

    const result = await query(
      `UPDATE potholes
       SET confidence_score = CASE WHEN user_confirmed = FALSE THEN LEAST(confidence_score + 20, 100) ELSE confidence_score END,
           user_confirmed = TRUE
       WHERE id = $1
       RETURNING *`,
      [id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Pothole not found' });
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.patch('/:id/photo', async (req, res) => {
  try {
    const { id } = req.params;
    const { photo_url } = req.body;

    if (typeof photo_url !== 'string' || !photo_url.trim()) {
      return res.status(400).json({ error: 'photo_url is required and must be a non-empty string' });
    }

    const result = await query(
      `UPDATE potholes
       SET confidence_score = CASE WHEN photo_url IS NULL THEN LEAST(confidence_score + 30, 100) ELSE confidence_score END,
           photo_url = $1
       WHERE id = $2
       RETURNING *`,
      [photo_url, id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Pothole not found' });
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
