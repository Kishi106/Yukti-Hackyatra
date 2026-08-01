require('dotenv').config();
const express = require('express');
const cors = require('cors');
const potholesRoutes = require('./routes/potholes');
const usersRoutes = require('./routes/users');
const uploadsRoutes = require('./routes/uploads');
const wardsRoutes = require('./routes/wards');
const officialsRoutes = require('./routes/officials');

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.get('/health', (_req, res) => res.json({ status: 'ok' }));
app.use('/potholes', potholesRoutes);
app.use('/users', usersRoutes);
app.use('/uploads', uploadsRoutes);
app.use('/wards', wardsRoutes);
app.use('/officials', officialsRoutes);

app.listen(port, () => {
  console.log(`Backend listening on port ${port}`);
});
