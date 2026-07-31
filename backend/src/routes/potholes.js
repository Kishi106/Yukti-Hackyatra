const express = require('express');
const { query } = require('../db');
const { SEVERITY_VALUES, SOURCE_VALUES, STATUS_VALUES } = require('../models/pothole');

const router = express.Router();

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
    const { lat, lng, severity, source, ward, photo_url } = req.body;

    if (typeof lat !== 'number' || typeof lng !== 'number') {
      return res.status(400).json({ error: 'lat and lng are required and must be numbers' });
    }
    if (!SEVERITY_VALUES.includes(severity)) {
      return res.status(400).json({ error: `severity must be one of: ${SEVERITY_VALUES.join(', ')}` });
    }
    if (!SOURCE_VALUES.includes(source)) {
      return res.status(400).json({ error: `source must be one of: ${SOURCE_VALUES.join(', ')}` });
    }

    const result = await query(
      `INSERT INTO potholes (lat, lng, severity, source, ward, photo_url)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [lat, lng, severity, source, ward ?? null, photo_url ?? null]
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

module.exports = router;
