const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { query } = require('../db');

const router = express.Router();

const ROLE_VALUES = ['field_officer', 'commissioner_analyst'];
const SALT_ROUNDS = 10;
const TOKEN_EXPIRY = '7d';

function signToken(official) {
  return jwt.sign(
    { id: official.id, name: official.name, role: official.role },
    process.env.JWT_SECRET,
    { expiresIn: TOKEN_EXPIRY }
  );
}

router.post('/signup', async (req, res) => {
  try {
    const { name, phone, password, role } = req.body;

    if (typeof name !== 'string' || !name.trim()) {
      return res.status(400).json({ error: 'name is required and must be a non-empty string' });
    }
    if (typeof phone !== 'string' || !phone.trim()) {
      return res.status(400).json({ error: 'phone is required and must be a non-empty string' });
    }
    if (typeof password !== 'string' || password.length < 6) {
      return res.status(400).json({ error: 'password is required and must be at least 6 characters' });
    }
    if (!ROLE_VALUES.includes(role)) {
      return res.status(400).json({ error: `role must be one of: ${ROLE_VALUES.join(', ')}` });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    const result = await query(
      `INSERT INTO officials (name, phone, password_hash, role) VALUES ($1, $2, $3, $4) RETURNING id, name, role`,
      [name, phone, passwordHash, role]
    );

    const official = result.rows[0];
    const token = signToken(official);
    res.status(201).json({ id: official.id, name: official.name, role: official.role, token });
  } catch (error) {
    if (error.code === '23505') {
      return res.status(409).json({ error: 'An account already exists for this phone number' });
    }
    res.status(500).json({ error: error.message });
  }
});

router.post('/login', async (req, res) => {
  try {
    const { phone, password } = req.body;

    if (typeof phone !== 'string' || !phone.trim() || typeof password !== 'string' || !password) {
      return res.status(401).json({ error: 'Invalid phone or password' });
    }

    const result = await query('SELECT * FROM officials WHERE phone = $1', [phone]);
    const official = result.rows[0];

    if (!official) {
      return res.status(401).json({ error: 'Invalid phone or password' });
    }

    const matches = await bcrypt.compare(password, official.password_hash);
    if (!matches) {
      return res.status(401).json({ error: 'Invalid phone or password' });
    }

    const token = signToken(official);
    res.json({ id: official.id, name: official.name, role: official.role, token });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/me', (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing or invalid Authorization header' });
  }

  const token = authHeader.slice('Bearer '.length);

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET);
    res.json({ id: payload.id, name: payload.name, role: payload.role });
  } catch (error) {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
});

module.exports = router;
