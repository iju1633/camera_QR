package com.example.cameraexample;

// 카메라
import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


import static android.os.Environment.DIRECTORY_PICTURES;

// 주요 기능이 구현되어 있는 클래스로 그 기능에는 권한 체크, 사진 저장, 자동 각도 보정, 뷰에 사진 띄우기가 있다
public class MainActivity extends AppCompatActivity {

    // 카메라 모듈 구현 부분
    private static final int REQUEST_IMAGE_CAPTURE = 672;
    private String imageFilePath;
    private Uri photoUri;


    // 사진 저장 시 갤러리 폴더에 바로 반영사항을 업데이트 시켜주려면 이 것이 필요하다(기술이름 : 미디어 스캐닝)
    private MediaScanner mMediaScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 사진 저장 후 미디어 스캐닝을 돌려줘야 갤러리에 반영됨
        mMediaScanner = MediaScanner.getInstance(getApplicationContext());


        // 권한 체크
        TedPermission.with(getApplicationContext())
                .setPermissionListener(permissionListener)
                .setRationaleMessage("카메라 권한이 필요합니다.") // 권한 체크시 물어볼 메시지
                .setDeniedMessage("거부하셨습니다.") // 거부 시 메시지
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .check();


        findViewById(R.id.btn_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 촬영 버튼을 누르게 되면 카메라 앱을 띄우는 구문
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try { // 파일 쓰기할 때 try-catch 구문을 사용하는 게 필수(exception 처리를 위해)
                        photoFile = createImageFile();
                    } catch (IOException e) {

                    }

                    if (photoFile != null) {
                        photoUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName(), photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        // 촬영이 끝난 후에 이미지를 뷰에 생성하고 디바이스에 저장하기 위해 필요한 코드
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE); 
                    }
                }
            }
        });

    }
    
    // 사진이 중복되지 않도록 사진의 이름을 사진이 찍힌 일시로 저장하고 이미지 파일을 리턴
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEST_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
            ExifInterface exif = null;

            try {
                exif = new ExifInterface(imageFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int exifOrientation;
            int exifDegree;

            if (exif != null) {
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                exifDegree = exifOrientationToDegrees(exifOrientation);
            } else {
                exifDegree = 0;
            }

            String result = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HHmmss", Locale.getDefault() );
            Date curDate   = new Date(System.currentTimeMillis());
            String filename  = formatter.format(curDate);

            String strFolderName = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + File.separator + "HONGDROID" + File.separator;
            File file = new File(strFolderName);
            if( !file.exists() )
                file.mkdirs();

            File f = new File(strFolderName + "/" + filename + ".png");
            result = f.getPath();

            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                result = "Save Error fOut";
            }

            // 비트맵 사진 폴더 경로에 저장
            rotate(bitmap,exifDegree).compress(Bitmap.CompressFormat.PNG, 70, fOut);

            try {
                fOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fOut.close();
                // 방금 저장된 사진을 갤러리 폴더 반영 및 최신화
                mMediaScanner.mediaScanning(strFolderName + "/" + filename + ".png");
            } catch (IOException e) {
                e.printStackTrace();
                result = "File close Error";
            }

            // 이미지 뷰에 비트맵을 set하여 이미지 표현
            ((ImageView) findViewById(R.id.iv_result)).setImageBitmap(rotate(bitmap,exifDegree));

        }
    }

    // 사진 촬영 시, user에 따라 다른 각도로 찍기 때문에 뷰에서 알아서 회전한 채로 보여주기 위함
    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Bitmap rotate(Bitmap bitmap, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() { // 권한 허용 시 이벤트
            Toast.makeText(getApplicationContext(), "권한이 허용됨",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) { // 거부 시 이벤트
            Toast.makeText(getApplicationContext(), "권한이 거부됨",Toast.LENGTH_SHORT).show();
        }
    };
}