require('dotenv').config();
const express = require('express');
const cors = require('cors');
const potholesRoutes = require('./routes/potholes');

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.get('/health', (_req, res) => res.json({ status: 'ok' }));
app.use('/potholes', potholesRoutes);

app.listen(port, () => {
  console.log(`Backend listening on port ${port}`);
});
