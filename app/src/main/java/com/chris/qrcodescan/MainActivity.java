package com.chris.qrcodescan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private static final int TAKE_PICKTURE_RESULT = 1000;

    @BindView(R.id.imv)
    ImageView imv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.scanButton)
    void scanClicked(){
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, ScanActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.camera)
    void cameraClicked(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File uri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, TAKE_PICKTURE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == TAKE_PICKTURE_RESULT){
            Bundle bundle = data.getExtras();
            Bitmap bm = (Bitmap) bundle.get("data");
            if (bm != null)
                bm.recycle();
            bm = (Bitmap) data.getExtras().get("data");
            if(bm != null){
                imv.setImageBitmap(bm);
            }
        }
    }
}
