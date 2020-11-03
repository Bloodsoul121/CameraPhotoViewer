package com.blood.cameraphotoviewer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bloodsoul";

    private static final int CHOOSE_IMAGE = 0x01;
    private static final int CROP_IMAGE = 0x02;

    @BindView(R.id.photo_iv)
    ImageView mPhotoIv;
    @BindView(R.id.crop_iv)
    ImageView mCropIv;

    private boolean mIsNeedCrop;
    private String mPhotoPath;
    private Uri mSavePathUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        requestPermissions();

        // 裁剪之后保存的原文件路径
        mSavePathUri = Uri.fromFile(new File(getExternalFilesDir("crop"), "crop_save.jpg"));
        Log.i(TAG, "SavePathUri -> " + mSavePathUri.toString());
    }

    @SuppressLint("CheckResult")
    private void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        Disposable disposable = rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(accept -> {
                    if (accept) {
                        toast("accept");
                    } else {
                        toast("deny");

                        AlertDialog quitDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("权限异常")
                                .setMessage("请到设置页面，开启权限")
                                .setPositiveButton("确定", (dialogInterface, i) -> finish())
                                .create();
                        quitDialog.show();
                    }
                });
    }

    public void clickBtn1(View view) {
        skipPhotoAlbum();
    }

    public void clickBtn2(View view) {
        skipPhotoCrop();
    }

    private void skipPhotoAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_IMAGE);
    }

    private void skipPhotoCrop() {
        if (TextUtils.isEmpty(mPhotoPath)) {
            toast("请先选择照片");
            skipPhotoAlbum();
            mIsNeedCrop = true;
            return;
        }

        File file = new File(mPhotoPath);
        Uri uri = PhotoUtils.fromFile(this, file);
        Log.i(TAG, "uri -> " + uri.toString());

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");//设置要缩放的图片Uri和类型
        intent.putExtra("crop", "true");//设置显示的VIEW可裁剪
        intent.putExtra("aspectX", 1);//宽度比
        intent.putExtra("aspectY", 1);//高度比
        intent.putExtra("outputX", 600);//输出图片的宽度
        intent.putExtra("outputY", 600);//输出图片的高度
        intent.putExtra("scale", false);//缩放
        intent.putExtra("return-data", true);//当为true的时候就返回缩略图，false就不返回，需要通过Uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mSavePathUri);//设置大图保存到文件
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//保存的图片格式
        intent.putExtra("noFaceDetection", false);//前置摄像头
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 必须加上权限
        startActivityForResult(intent, CROP_IMAGE);//打开剪裁Activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            mIsNeedCrop = false;
            return;
        }
        Uri uri = data.getData();
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CHOOSE_IMAGE:
                    String photoPath = PhotoUtils.getPhotoPath(this, uri);
                    showPhotoImg(photoPath);

                    if (mIsNeedCrop) {
                        skipPhotoCrop();
                    }

                    break;
                case CROP_IMAGE:
                    Bundle bundle = data.getExtras();
                    // 缩略图
                    if (bundle != null) {
                        Bitmap bitmap = bundle.getParcelable("data");
                        mCropIv.setImageBitmap(bitmap);
                        break;
                    }
                    // 保存原图
                    Bitmap bitmap = BitmapFactory.decodeFile(mSavePathUri.getPath());
                    if (bitmap == null) {
                        toast("bitmap is null");
                        break;
                    }
                    mCropIv.setImageBitmap(bitmap);
                    break;
            }
        }
    }

    private void showPhotoImg(String photoPath) {
        if (photoPath == null) {
            toast("photoPath is null");
            return;
        }
        Log.i(TAG, "photoPath -> " + photoPath);
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        if (bitmap == null) {
            toast("bitmap is null");
            return;
        }
        mPhotoIv.setImageBitmap(bitmap);
        mPhotoPath = photoPath;
    }

    private void toast(String msg) {
        Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        toast.setText(msg);
        toast.show();
    }
}
