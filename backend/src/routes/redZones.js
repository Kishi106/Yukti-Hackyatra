const express = require('express');
const { query } = require('../db');
const { recomputeAllRedZones } = require('../redZones');

const router = express.Router();

router.get('/', async (_req, res) => {
  try {
    const result = await query('SELECT * FROM red_zones ORDER BY risk_score DESC');
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Manual full recompute — the same batch DBSCAN pass that already runs
// automatically after every POST /potholes (see potholeMerge.js). Useful
// after a bulk data change or to recover from a failed automatic run.
router.post('/recompute', async (_req, res) => {
  try {
    const zones = await recomputeAllRedZones();
    res.json(zones);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
