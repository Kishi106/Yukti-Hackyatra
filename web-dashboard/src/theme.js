// Shared design tokens for the dashboard shell (header, sidebar, right column,
// auth screens). Mirrors the light-government palette already used by
// MapView.jsx so the choropleth/markers and the surrounding shell read as one
// system.
export const INK = '#12212F';
export const INK_LO = '#5B6B7C';
export const LINE = '#D8E0EA';
export const GOV = '#12518A';
export const AMBER = '#B07800';
export const GREEN = '#1B7F5A';
export const RED = '#C0392B';
export const BG = '#F4F6F9';

// Brand colors sampled directly from the GVMC logo.
export const ORANGE = '#F2603D';
export const CHARCOAL = '#1F2023';

export const STATUS_COLORS = {
  new: '#5B7DB1', // neutral blue/gray
  in_progress: ORANGE,
  fixed: CHARCOAL
};

export const MUTED_BAR = '#D8DDE4';

export const STATUS_LABELS = {
  new: 'New',
  in_progress: 'In Progress',
  fixed: 'Fixed'
};

export const ROLE_LABELS = {
  field_officer: 'Field Officer',
  commissioner_analyst: 'Commissioner Analyst'
};

export const inter = { fontFamily: 'Inter, system-ui, sans-serif' };
export const display = { fontFamily: "'Barlow Condensed',sans-serif", fontWeight: 700 };
export const mono = { fontFamily: "'JetBrains Mono',monospace" };
