const express = require('express');
const { query } = require('../db');
const { SEVERITY_VALUES, SOURCE_VALUES, STATUS_VALUES } = require('../models/pothole');

const router = express.Router();

// Duplicate-detection radius: ~30m in each direction at typical GVMC latitudes.
const DUPLICATE_LAT_DELTA = 0.00027;
const DUPLICATE_LNG_DELTA = 0.0003;

router.get('/', async (req, res) => {
  try {
    const { status, ward } = req.query;
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

    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await query(`SELECT * FROM potholes ${where} ORDER BY created_at DESC`, params);
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', async (req, res) => {
  try {
    const { lat, lng, severity, source, ward, photo_url, reporter_id } = req.body;

    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return res.status(400).json({ error: 'lat and lng are required and must be numbers' });
    }
    if (!SEVERITY_VALUES.includes(severity)) {
      return res.status(400).json({ error: `severity must be one of: ${SEVERITY_VALUES.join(', ')}` });
    }
    if (!SOURCE_VALUES.includes(source)) {
      return res.status(400).json({ error: `source must be one of: ${SOURCE_VALUES.join(', ')}` });
    }

    let confidenceScore = source === 'auto' ? 30 : 0;

    const duplicates = await query(
      `SELECT id FROM potholes
       WHERE ABS(lat - $1) <= $2
         AND ABS(lng - $3) <= $4
         AND created_at >= NOW() - INTERVAL '30 days'`,
      [lat, DUPLICATE_LAT_DELTA, lng, DUPLICATE_LNG_DELTA]
    );

    if (duplicates.rowCount > 0) {
      confidenceScore += 20;
      await query(
        `UPDATE potholes SET confidence_score = LEAST(confidence_score + 20, 100) WHERE id = ANY($1)`,
        [duplicates.rows.map((row) => row.id)]
      );
    }

    if (typeof photo_url === 'string' && photo_url.trim()) {
      confidenceScore += 30;
    }

    confidenceScore = Math.min(confidenceScore, 100);

    const result = await query(
      `INSERT INTO potholes (lat, lng, severity, source, ward, photo_url, reporter_id, confidence_score)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [lat, lng, severity, source, ward ?? null, photo_url ?? null, reporter_id ?? null, confidenceScore]
    );
    res.status(201).json(result.rows[0]);
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
