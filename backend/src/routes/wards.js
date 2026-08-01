const express = require('express');
const { query } = require('../db');

const router = express.Router();

router.get('/boundaries', async (req, res) => {
  try {
    const { ward_no } = req.query;
    const conditions = [];
    const params = [];

    if (ward_no) {
      params.push(ward_no);
      conditions.push(`ward_no = $${params.length}`);
    }

    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await query(
      `SELECT ward_no, zone_id, ward_name, geometry FROM ward_boundaries ${where} ORDER BY ward_no`,
      params
    );

    const features = result.rows.map((row) => ({
      type: 'Feature',
      geometry: row.geometry,
      properties: {
        ward_no: row.ward_no,
        zone_id: row.zone_id,
        ward_name: row.ward_name
      }
    }));

    if (ward_no) {
      if (features.length === 0) {
        return res.status(404).json({ error: 'Ward not found' });
      }
      return res.json(features[0]);
    }

    res.json({ type: 'FeatureCollection', features });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q || !q.trim()) {
      return res.json([]);
    }

    const result = await query(
      `SELECT ward_no, zone_id, ward_name FROM ward_boundaries WHERE ward_name ILIKE $1 ORDER BY ward_name`,
      [`%${q.trim()}%`]
    );

    res.json(
      result.rows.map((row) => ({
        ward_no: row.ward_no,
        zone_id: row.zone_id,
        ward_name: row.ward_name
      }))
    );
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
