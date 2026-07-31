const express = require('express');
const { query } = require('../db');

const router = express.Router();

router.post('/', async (req, res) => {
  try {
    const { name, phone, ward } = req.body;

    if (typeof name !== 'string' || !name.trim()) {
      return res.status(400).json({ error: 'name is required and must be a non-empty string' });
    }
    if (typeof phone !== 'string' || !phone.trim()) {
      return res.status(400).json({ error: 'phone is required and must be a non-empty string' });
    }

    const result = await query(
      `INSERT INTO users (name, phone, ward) VALUES ($1, $2, $3) RETURNING *`,
      [name, phone, ward ?? null]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    if (error.code === '23505') {
      const existing = await query('SELECT id FROM users WHERE phone = $1', [req.body.phone]);
      return res.status(409).json({
        error: 'Account already exists for this phone number',
        existingUserId: existing.rows[0]?.id
      });
    }
    res.status(500).json({ error: error.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await query('SELECT * FROM users WHERE id = $1', [id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.patch('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone, ward } = req.body;

    const fields = [];
    const params = [];

    if (name !== undefined) {
      params.push(name);
      fields.push(`name = $${params.length}`);
    }
    if (phone !== undefined) {
      params.push(phone);
      fields.push(`phone = $${params.length}`);
    }
    if (ward !== undefined) {
      params.push(ward);
      fields.push(`ward = $${params.length}`);
    }

    if (fields.length === 0) {
      const existing = await query('SELECT * FROM users WHERE id = $1', [id]);
      if (existing.rowCount === 0) {
        return res.status(404).json({ error: 'User not found' });
      }
      return res.json(existing.rows[0]);
    }

    params.push(id);
    const result = await query(
      `UPDATE users SET ${fields.join(', ')} WHERE id = $${params.length} RETURNING *`,
      params
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(result.rows[0]);
  } catch (error) {
    if (error.code === '23505') {
      return res.status(409).json({ error: 'Account already exists for this phone number' });
    }
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
