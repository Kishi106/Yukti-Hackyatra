const express = require('express');
const crypto = require('crypto');
const multer = require('multer');
const { getSupabaseClient } = require('../storage');

const router = express.Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 8 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (!file.mimetype.startsWith('image/')) {
      return cb(new Error('Only image files are allowed'));
    }
    cb(null, true);
  }
});

router.post('/', (req, res) => {
  upload.single('photo')(req, res, async (err) => {
    if (err) {
      return res.status(400).json({ error: err.message });
    }
    if (!req.file) {
      return res.status(400).json({ error: 'photo file is required' });
    }

    try {
      const supabase = getSupabaseClient();
      const bucket = process.env.SUPABASE_STORAGE_BUCKET;
      const filename = `${Date.now()}-${crypto.randomUUID()}.jpg`;

      const { error: uploadError } = await supabase.storage
        .from(bucket)
        .upload(filename, req.file.buffer, { contentType: req.file.mimetype });

      if (uploadError) {
        return res.status(500).json({ error: uploadError.message });
      }

      const { data } = supabase.storage.from(bucket).getPublicUrl(filename);
      res.status(201).json({ url: data.publicUrl });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });
});

module.exports = router;
