const SEVERITY_VALUES = ['low', 'medium', 'high'];
const SOURCE_VALUES = ['auto', 'citizen'];
const STATUS_VALUES = ['new', 'in_progress', 'fixed'];

class Pothole {
  constructor({
    id,
    lat,
    lng,
    severity,
    source,
    photo_url,
    status,
    ward,
    created_at,
    reporter_id,
    confidence_score,
    user_confirmed
  }) {
    this.id = id;
    this.lat = lat;
    this.lng = lng;
    this.severity = severity;
    this.source = source;
    this.photo_url = photo_url;
    this.status = status;
    this.ward = ward;
    this.created_at = created_at;
    this.reporter_id = reporter_id;
    this.confidence_score = confidence_score;
    this.user_confirmed = user_confirmed;
  }
}

module.exports = { Pothole, SEVERITY_VALUES, SOURCE_VALUES, STATUS_VALUES };
