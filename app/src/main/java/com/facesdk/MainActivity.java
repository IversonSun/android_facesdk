package com.facesdk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private static final int SELECT_IMAGE = 1;
    static final String TAG = "MainActivity";

    private TextView infoResult;
    private ImageView imageView;
    private Bitmap yourSelectedImage = null;

    private static FaceSDKNative faceSDKNative = new FaceSDKNative();
    private static boolean sdk_init_ok = false;
    private static boolean sdk_model_ok = true;
    private static boolean sdk_permission_ok = true;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;

    //Check Permissions
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        //if (currentapiVersion >= Build.VERSION_CODES.M) {
        sdk_permission_ok = verifyPermissions(this);
        //}

        //copy model
        try {
            copyBigDataToSD("det1.bin");
            copyBigDataToSD("det2.bin");
            copyBigDataToSD("det3.bin");
            copyBigDataToSD("det1.param");
            copyBigDataToSD("det2.param");
            copyBigDataToSD("det3.param");
        } catch (IOException e) {
            e.printStackTrace();
            sdk_model_ok = false;
        }

        File sdDir = Environment.getExternalStorageDirectory();//get model store dir
        String sdPath = sdDir.toString() + "/facesdk/";
        sdk_init_ok = faceSDKNative.FaceDetectionModelInit(sdPath);


        infoResult = (TextView) findViewById(R.id.infoResult);
        imageView = (ImageView) findViewById(R.id.imageView);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null) {
                    infoResult.setText("no image found");
                    return;
                }
                int width = yourSelectedImage.getWidth();
                int height = yourSelectedImage.getHeight();
                byte[] imageDate = getPixelsRGBA(yourSelectedImage);

                long timeDetectFace = System.currentTimeMillis();
                //do FaceDetect
                int faceInfo[] =  faceSDKNative.FaceDetect(imageDate, width, height,4);
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;

                //Get Results
               if (faceInfo!=null && faceInfo.length>1) {
                   int faceNum = faceInfo[0];
                   infoResult.setText("detect time："+timeDetectFace+"ms,   face number：" + faceNum);
                   Log.i(TAG, "detect time："+timeDetectFace);
                   Log.i(TAG, "face num：" + faceNum );

                   Bitmap drawBitmap = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                   for (int i=0; i<faceNum; i++) {
                       int left, top, right, bottom;
                       Canvas canvas = new Canvas(drawBitmap);
                       Paint paint = new Paint();
                       left = faceInfo[1+14*i];
                       top = faceInfo[2+14*i];
                       right = faceInfo[3+14*i];
                       bottom = faceInfo[4+14*i];
                       paint.setColor(Color.RED);
                       paint.setStyle(Paint.Style.STROKE);
                       paint.setStrokeWidth(5);
                       //crop image
                       //Bitmap cropBmp = faceSDKNative.CropImage(drawBitmap, left, top, right-left, bottom-top);

                       //just for debug
                       //faceSDKNative.SaveImage(cropBmp);

                       //encode img to base64
                       //String encodeImg = faceSDKNative.EncodeBase64(cropBmp);

                       //Draw rect
                       canvas.drawRect(left, top, right, bottom, paint);

                       //Draw landmark
                       for (int j=0; j<5; j++) {
                           int pointX = faceInfo[5+j+14*i];
                           int pointY = faceInfo[5+j+5+14*i];
                           canvas.drawCircle(pointX, pointY, 2, paint);
                       }


                   }
                   imageView.setImageBitmap(drawBitmap);
                }else{
                   String debug = "sdk error: no face found, \n";
                   if(!sdk_init_ok) {
                       debug += "sdk_init_error, may models not flush, please restart apk\n";
                   }
                   if(!sdk_model_ok) {
                       debug +="sdk_model_copy, may not have write sdcar permission, please restart apk\n";
                   }
                   if(!sdk_permission_ok) {
                       debug += "should access permission, please restart apk\n";
                   }
                   infoResult.setText(debug);
               }

            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try {
                if (requestCode == SELECT_IMAGE) {
                    Bitmap bitmap = decodeUri(selectedImage);

                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                    yourSelectedImage = rgba;

                    imageView.setImageBitmap(yourSelectedImage);
                }
            } catch (FileNotFoundException e) {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        //o.inSampleSize = 1;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        //// Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private boolean copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//get root dir
        File file = new File(sdDir.toString()+"/facesdk/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/facesdk/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return true;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/facesdk/"+ strOutFileName);
        myInput = this.getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);
        return true;
    }

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_permission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

}
