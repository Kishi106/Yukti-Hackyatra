import { CLOUDINARY_CLOUD_NAME, CLOUDINARY_UPLOAD_PRESET } from '../config';

export async function uploadPhoto(localUri) {
  const form = new FormData();
  form.append('file', {
    uri: localUri,
    type: 'image/jpeg',
    name: 'pothole.jpg'
  });
  form.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);

  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`,
    { method: 'POST', body: form }
  );

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error?.message || 'Photo upload failed');
  }

  const data = await response.json();
  return data.secure_url;
}
