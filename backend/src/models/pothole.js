class Pothole {
  constructor({ id, latitude, longitude, severity, status, description, image_url }) {
    this.id = id;
    this.latitude = latitude;
    this.longitude = longitude;
    this.severity = severity;
    this.status = status;
    this.description = description;
    this.image_url = image_url;
  }
}

module.exports = Pothole;
