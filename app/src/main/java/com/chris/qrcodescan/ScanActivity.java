package com.chris.qrcodescan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.chris.qrcodescan.QRCode.PlanarYUVLuminanceSource;
import com.chris.qrcodescan.QRCode.QRCodeManager;
import com.chris.qrcodescan.QRCode.Size;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import rx.*;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ScanActivity extends Activity {

    private final String TAG = ScanActivity.class.getSimpleName();

    private  QRCodeManager qrCodeManager;

    @BindView(R.id.surfaceView)
    SurfaceView surfaceView;

    @BindView(R.id.cv_capture)
    CaptureView capView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        this.setRequestedOrientation(1);

        qrCodeManager = new QRCodeManager(this, surfaceView, capView.getFrameRect());
        qrCodeManager.startScanQRCode().subscribe(new Action1() {
            @Override
            public void call(Object o) {
                Log.d(TAG, o.toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
