const express = require('express');
const { query } = require('../db');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const result = await query('SELECT * FROM potholes ORDER BY created_at DESC');
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', async (req, res) => {
  try {
    const { latitude, longitude, severity, status, description, image_url } = req.body;
    const result = await query(
      `INSERT INTO potholes (latitude, longitude, severity, status, description, image_url)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [latitude, longitude, severity, status || 'reported', description, image_url]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.patch('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { status, severity } = req.body;
    const result = await query(
      `UPDATE potholes
       SET status = COALESCE($1, status), severity = COALESCE($2, severity), updated_at = NOW()
       WHERE id = $3
       RETURNING *`,
      [status, severity, id]
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
