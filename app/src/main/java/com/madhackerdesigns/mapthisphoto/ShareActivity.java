package com.madhackerdesigns.mapthisphoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class ShareActivity extends Activity {
    private static String TAG = "ShareActivity";
    private static String FILENAME = "temp.jpg";

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageUri = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        Log.v(TAG, imageUri.toString());
        new MapPhotoTask().execute();
        finish();
    }

    private Double degreesFor(String data, String ref) {
        String[] results = data.split("[/,]");
        Double degrees = Double.valueOf(results[0]) / Double.valueOf(results[1]);
        Double minutes = Double.valueOf(results[2]) / Double.valueOf(results[3]);
        Double seconds = Double.valueOf(results[4]) / Double.valueOf(results[5]);

        Double coordinate = degrees + (minutes * 60.0 + seconds)/3600.0;
        if (ref.equals("S") || ref.equals("W")) coordinate *= -1.0;
        return coordinate;
    }

    private void downloadImageFile() throws IOException {
        final InputStream imageInput = getContentResolver().openInputStream(imageUri);
        try {
            final FileOutputStream tempFile = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            try {
                final byte[] buffer = new byte[1024];
                int read;

                while ((read = imageInput.read(buffer)) != -1)
                    tempFile.write(buffer, 0, read);

                tempFile.flush();
            } finally {
                tempFile.close();
            }
        } finally {
            imageInput.close();
        }
    }

    private String imagePath() throws IOException {
        return getFileStreamPath(FILENAME).getAbsolutePath();
    }

    private Uri buildMapUri() {
        Uri mapUri = null;

        try {
            downloadImageFile();
            ExifInterface exif = new ExifInterface(imagePath());

            String latData = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longData = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String longRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (latData != null) {
                Double latitude = degreesFor(latData, latRef);
                Double longitude = degreesFor(longData, longRef);
                mapUri = Uri.parse("geo:0,0?q=" + latitude.toString() + "," + longitude.toString() + "&z=11");
                Log.v(TAG, mapUri.toString());
            }

            deleteFile(FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mapUri;
    }

    private void showImageLocationOnMap(Uri mapUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            toast(R.string.finding_location);
            startActivity(intent);
        } else toast(R.string.no_map_app);
    }

    private void toast(Integer resourceId) {
        Toast.makeText(this, resourceId, Toast.LENGTH_LONG).show();
    }

    private class MapPhotoTask extends AsyncTask<Void, Void, Uri> {
        @Override
        protected Uri doInBackground(Void... voids) {
            return buildMapUri();
        }

        @Override
        protected void onPostExecute(Uri mapUri) {
            if (mapUri != null) showImageLocationOnMap(mapUri);
            else toast(R.string.no_geotag);
        }
    }
}
