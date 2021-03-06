package com.example.project_mini_tiktok;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.project_mini_tiktok.model.UploadResponse;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.example.project_mini_tiktok.Constants.STUDENT_ID;
import static com.example.project_mini_tiktok.Constants.USER_NAME;
import static com.example.project_mini_tiktok.Constants.token;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024;
    private static final int REQUEST_CODE_COVER_IMAGE = 101;
    private static final int REQUEST_CODE_VIDEO = 102;
    private static final String COVER_IMAGE_TYPE = "image/*";
    private static final String VIDEO_TYPE = "video/*";
    private IApi api;
    private Uri coverImageUri;
    private Uri videoUri;

    private SimpleDraweeView coverSD;
    private VideoView videoSD;
    private Button btn_submit;
    private Button btn_submit_picture1;
    private Button btn_submit_picture2;
    private Button btn_submit_video1;
    private Button btn_submit_video2;
    private EditText contentEditText;

    private String takeImagePath;
    private String mp4Path = "";

    private int REQUEST_CODE_TAKE_PHOTO = 1001;
    private int REQUEST_CODE_TAKE_PHOTO_PATH = 1002;
    private int PERMISSION_REQUEST_CAMERA_CODE = 1003;
    private int PERMISSION_REQUEST_CAMERA_PATH_CODE = 1004;

    private final static int PERMISSION_REQUEST_CODE = 1005;
    private final static int REQUEST_CODE_RECORD = 1006;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        initNetwork();
        setContentView(R.layout.activity_upload);
        coverSD = findViewById(R.id.sd_cover);
        videoSD = findViewById(R.id.sd_video);
        contentEditText = findViewById(R.id.et_content);
        btn_submit = findViewById(R.id.btn_submit);
        btn_submit_picture1 = findViewById(R.id.btn_submit_picture1);
        btn_submit_picture2 = findViewById(R.id.btn_submit_picture2);
        btn_submit_video1 = findViewById(R.id.btn_submit_video1);
        btn_submit_video2 = findViewById(R.id.btn_submit_video2);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });

        btn_submit_picture2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFile(REQUEST_CODE_COVER_IMAGE, COVER_IMAGE_TYPE, "????????????");
            }
        });

        btn_submit_video2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFile(REQUEST_CODE_VIDEO, VIDEO_TYPE, "????????????");
            }
        });

//        btn_submit_picture1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takePhoto(v);
//            }
//        });
//
//        btn_submit_video1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                record(v);
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_COVER_IMAGE == requestCode) { // ???????????????????????????
            if (resultCode == Activity.RESULT_OK) {
                coverImageUri = data.getData();
                coverSD.setImageURI(coverImageUri);

                if (coverImageUri != null) {
                    Log.d(TAG, "pick cover image " + coverImageUri.toString());
                } else {
                    Log.d(TAG, "uri2File fail " + data.getData());
                }

            } else {
                Log.d(TAG, "file pick fail");
            }
        }else if(REQUEST_CODE_VIDEO == requestCode) { // ???????????????????????????
            if (resultCode == Activity.RESULT_OK) {
                videoUri = data.getData();
                videoSD.setVideoURI(videoUri);
                videoSD.start();

                if (videoUri != null) {
                    Log.d(TAG, "pick video " + videoUri.toString());
                } else {
                    Log.d(TAG, "uri2File fail " + data.getData());
                }
            } else {
                Log.d(TAG, "file pick fail");
            }
        }else if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) { // ???????????????????????????
            // todo 1.2 ??? data ??????????????? bitmap
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap)extras.get("data");
            Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
            coverImageUri = uri;
            coverSD.setImageURI(uri);
        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO_PATH && resultCode == RESULT_OK) { // ?????????????????????????????????????????????
            // todo 1.4 ???????????????????????? bitmap
            // ??????ImageView???????????????
            int targetWidth = coverSD.getWidth();
            int targetHeight = coverSD.getHeight();
            // ??????Options?????????InJustDecodeBounds???true??????????????????????????????
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(takeImagePath, options);
            int photoWidth = options.outWidth;
            int photoHeight = options.outHeight;

            // todo 1.5 ??????????????????
            // ???????????????????????????????????????????????????Options?????????inJustDecodeBounds??????false??????????????????????????????
            int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(takeImagePath, options);
            Bitmap rotateBitmap = PathUtils.rotateImage(bitmap, takeImagePath);
            Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), rotateBitmap, null,null));
            coverImageUri = uri;
            coverSD.setImageBitmap(rotateBitmap);
        }else if(requestCode == REQUEST_CODE_RECORD && resultCode == RESULT_OK){ // ???????????????????????????
            videoUri = data.getData();
            play();
        }
//      else if(requestCode == 1){
//            //???????????????uri???????????????Activity?????????
//            String url = data.getStringExtra("PictureUrl");
//            Uri uri = Uri.parse(url);
//            coverImageUri = uri;
//            ContentResolver cr = this.getContentResolver();
//            try {
//                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
//                /* ???Bitmap?????????ImageView */
//                coverSD.setImageBitmap(bitmap);
//            } catch (FileNotFoundException e) {
//                Log.e("Exception", e.getMessage(),e);
//            }
//        }
    }

    private void play(){
        videoSD.setVideoPath(mp4Path);
        videoSD.start();
    }

//    public void systemTakePicture(View view) {
//        Intent intent = new Intent(UploadActivity.this, SystemCameraActivity.class);
//        startActivityForResult(intent, 1);
//    }
//
//    public void systemRecord(View view) {
//        Intent intent = new Intent(UploadActivity.this, SystemRecordActivity.class);
//        startActivityForResult(intent, 2);
//    }

    public void takePhoto(View view) {
        requestCameraPermission();
    }

    public void takePhotoUsePath(View view) {
        requestCameraAndSDCardPermission();
    }

    private void requestCameraAndSDCardPermission() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission) {
            takePhotoUsePathHasPermission();
        } else {
            // ????????????
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CAMERA_PATH_CODE);
        }
    }

    private void takePhotoUsePathHasPermission() {
        // todo 1.3 ???????????? intent ???????????????????????????
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takeImagePath = getOutputMediaPath();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, PathUtils.getUriForFile(this, takeImagePath));
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO_PATH);
        }
    }

    private String getOutputMediaPath() {
        // ?????????????????????????????????/??????
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir, "IMG_" + timeStamp + ".jpg");
        if (!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // ????????????
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CAMERA_CODE);
        } else {
            takePhotoHasPermission();
        }
    }

    private void takePhotoHasPermission() {
        // todo 1.1 ?????????????????? intent
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }


    public void record(View view) {
        requestPermission();
    }

    private void requestPermission() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission && hasAudioPermission) {
            recordVideo();
        } else {
            List<String> permission = new ArrayList<String>();
            if (!hasCameraPermission) {
                permission.add(Manifest.permission.CAMERA);
            }
            if (!hasAudioPermission) {
                permission.add(Manifest.permission.RECORD_AUDIO);
            }
            ActivityCompat.requestPermissions(this, permission.toArray(new String[permission.size()]), PERMISSION_REQUEST_CODE);
        }

    }

    private void recordVideo() {
        // todo 2.1 ?????????????????? intent ?????????????????????
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mp4Path = getOutputMediaPath_video();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, PathUtils.getUriForFile(this, mp4Path));
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,10);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_RECORD);
        }
    }

    private String getOutputMediaPath_video() {
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir, "IMG_" + timeStamp + ".mp4");
        if (!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }

    // ???????????????????????????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoHasPermission();
            } else {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CAMERA_PATH_CODE) {
            boolean hasPermission = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                }
            }
            if (hasPermission) {
                takePhotoUsePathHasPermission();
            } else {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == PERMISSION_REQUEST_CODE){
            boolean hasPermission = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                }
            }
            if (hasPermission) {
                recordVideo();
            } else {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initNetwork() {
        //TODO 3
        // ??????Retrofit??????
        //
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api-android-camp.bytedance.com/zju/invoke/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(IApi.class);
    }

    private void getFile(int requestCode, String type, String title) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_TITLE, title);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
    }

    // ???UI????????????
    public void makeUIToast(Context context, String text, int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    private void submit() {
        byte[] videoData = readDataFromUri(videoUri);
        if(videoData == null || videoData.length == 0){
            Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] coverImageData = readDataFromUri(coverImageUri);
        if (coverImageData == null || coverImageData.length == 0) {
            Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
//        String to = toEditText.getText().toString();
//        if (TextUtils.isEmpty(to)) {
//            Toast.makeText(this, "?????????TA?????????", Toast.LENGTH_SHORT).show();
//            return;
//        }
        String content = contentEditText.getText().toString();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }

        if ( (videoData.length + coverImageData.length) >= MAX_FILE_SIZE) {
            Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        //TODO 5
        // ??????api.submitMessage()??????????????????
        // ???????????????????????????activity???????????????toast
        new Thread(new Runnable() {
            @Override
            public void run() {
                MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", "video.mp4",
                        RequestBody.create(MediaType.parse("multipart/form-data"), videoData));
                MultipartBody.Part coverPart = MultipartBody.Part.createFormData("cover_image", "cover.png",
                        RequestBody.create(MediaType.parse("multipart/form-data"), coverImageData));

                Call<UploadResponse> upResponse = api.submitMessage(STUDENT_ID, USER_NAME,
                        "", coverPart, videoPart, token);
                upResponse.enqueue(new Callback<UploadResponse>(){
                    @Override
                    public void onResponse(final Call<UploadResponse> call, final Response<UploadResponse> response){
                        if(!response.isSuccessful()){
                            makeUIToast(UploadActivity.this, "??????????????????", Toast.LENGTH_SHORT);
                            return;
                        }
                        UploadResponse upResponse = response.body();
                        if(upResponse == null){
                            makeUIToast(UploadActivity.this, "??????????????????", Toast.LENGTH_SHORT);
                            return;
                        }

                        if(upResponse.success){
                            Log.d("UploadResponse", "Success.");
                            makeUIToast(UploadActivity.this, "???????????????", Toast.LENGTH_SHORT);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UploadActivity.this.finish();
                                }
                            });
                        }
                        else{
                            Log.d("UploadResponse Error", upResponse.error);
                            makeUIToast(UploadActivity.this, "????????????: " + upResponse.error, Toast.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(final Call<UploadResponse> call, final Throwable t) {
                        t.printStackTrace();
                        makeUIToast(UploadActivity.this, "????????????" + t.toString(), Toast.LENGTH_SHORT);
                    }

                });
            }
        }).start();
    }


    private byte[] readDataFromUri(Uri uri) {
        byte[] data = null;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            data = Util.inputStream2bytes(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

}
