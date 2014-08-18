package com.google.hackathon.helloworld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Random;


public class HelloWorldActivity extends Activity {
    private static final String TAG = HelloWorldActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_CAPTURE = 1;
    private static final String PREFS_SNAP_NUMBER = "snapNumber";
    private int snapNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_world);
        updateState();

        // Try to see if we're shared with first...
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // This was sent to us and it was an image.
        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            Uri uri  = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                ImageView iv = (ImageView) findViewById(R.id.snapped_image);
                iv.setImageURI(uri);
            }
        } else {
            // Otherwise just display the last known image.
            updateView();
        }
    }

    private void updateState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        snapNumber = prefs.getInt(PREFS_SNAP_NUMBER, 0);
    }

    public void onSharePicture(View view) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getSnapFile()));
        startActivity(Intent.createChooser(sharingIntent, "Share Image Using..."));
    }

    public void onSnapPicture(View view) {
        Log.i(TAG, "snapPicture");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        bumpSnapFile();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getSnapFile()));
        startActivityForResult(intent, ACTIVITY_REQUEST_CAPTURE);
    }

    private void bumpSnapFile() {
        snapNumber++;
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREFS_SNAP_NUMBER, snapNumber);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CAPTURE && resultCode == RESULT_OK) {
            updateView();
            return;
        }
        Log.i(TAG, "unknown Activity Request Code:" + requestCode + " Result code: " + resultCode);
    }

    private File getSnapFile() {
        File snapDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.imagePath));
        snapDir.mkdirs();
        File snapFile =  new File(snapDir, String.format("snap-%04d.png", snapNumber));
        Log.i(TAG, "Snapshot path: " + snapFile.getAbsolutePath());
        return snapFile;
    }

    private void updateView() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 2;
        opts.inPurgeable = true;
        Bitmap bmp = null;
        final String snapPath = getSnapFile().getAbsolutePath();
        while (true) {
            try {
                Log.i(TAG, "Downsampling image by: " + opts.inSampleSize + "x");
                bmp = BitmapFactory.decodeFile(snapPath, opts);
                break;
            }
            catch (OutOfMemoryError oom) {
                opts.inSampleSize *= 2;
            }
            catch (Exception ex) {
                Log.i(TAG, "Error loading file: " +  ex.getMessage());
                break;
            }
        }
        if (bmp == null) {
            Log.w(TAG, "failed to load the file: " + snapPath);
            // Nothing could be loaded.
            Context context = getApplicationContext();
            CharSequence text = String.format("File '%s' could not be loaded", snapPath);
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Log.i(TAG, "Image dimensions: " + bmp.getWidth() + "x" +bmp.getHeight());
        ImageView iv = (ImageView) findViewById(R.id.snapped_image);
        iv.setImageBitmap(bmp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hello_world, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
