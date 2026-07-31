require('dotenv').config();
const express = require('express');
const potholesRoutes = require('./routes/potholes');

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());
app.get('/health', (_req, res) => res.json({ status: 'ok' }));
app.use('/potholes', potholesRoutes);

app.listen(port, () => {
  console.log(`Backend listening on port ${port}`);
});
