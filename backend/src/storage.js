require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');

// Created lazily so the server can still boot (and every non-upload endpoint keep
// working) when Supabase env vars aren't configured yet — only /uploads needs this.
let supabase = null;

function getSupabaseClient() {
  if (!supabase) {
    if (!process.env.SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
      throw new Error('SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set to use file uploads');
    }
    // Server-side client using the service role key (not the anon key), since this
    // process needs permission to upload into the storage bucket on the caller's behalf.
    supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);
  }
  return supabase;
}

module.exports = { getSupabaseClient };
