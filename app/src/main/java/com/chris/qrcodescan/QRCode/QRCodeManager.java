package com.chris.qrcodescan.QRCode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.chris.qrcodescan.ScanActivity;
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

import rx.Observable;
import rx.android.*;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by xiaopeng on 2017/5/26.
 */

public class QRCodeManager implements SurfaceHolder.Callback, Camera.AutoFocusCallback, Camera.PreviewCallback, ResultPointCallback {
    private final String TAG = ScanActivity.class.getSimpleName();

    private Context mContext;
    private SurfaceView mSurfaceView;
    private Rect mPreviewFrameRect;

    private Camera mCamera;
    private Size screenSize;
    private Size cameraSize;
    private Bitmap mBitmap;
    private PublishSubject<Result> resultSubject;

    private Boolean bStart = false;

    public QRCodeManager(Context context, SurfaceView surfaceView, Rect previewFrameRect) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenSize = new Size(display.getWidth(), display.getHeight());

        mContext = context;
        mSurfaceView = surfaceView;
        mPreviewFrameRect = previewFrameRect;

        surfaceView.getHolder().addCallback(this);
        resultSubject = PublishSubject.create();
    }

    public  Observable startScanQRCode(){
        bStart = true;
        return resultSubject;
    }

    public void stop(){
        mCamera.stopPreview();
    }

    public void start(){
        mCamera.startPreview();
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        Log.d(TAG, "onAutoFocus: " + String.valueOf(b));
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Observable.just(bytes)
                .map(new Func1<byte[], Result>() {
                    @Override
                    public Result call(byte[] bytes) {
//                        Log.d(TAG, "Thread id: " + String.valueOf(Thread.currentThread().getId()));
                        bytes = rotateYUVdata90(bytes);
                        PlanarYUVLuminanceSource luminanceSource = new PlanarYUVLuminanceSource(bytes, cameraSize, mPreviewFrameRect);

                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
                        Hashtable<DecodeHintType, Object> hints = new Hashtable<>(2);
                        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
                        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, QRCodeManager.this);
                        MultiFormatReader multiFormatReader = new MultiFormatReader();
                        multiFormatReader.setHints(hints);
                        long start = System.currentTimeMillis();
                        Result rawResult = null;
                        try {
                            rawResult = multiFormatReader.decodeWithState(bitmap);
                            mBitmap = luminanceSource.renderCroppedGreyScaleBitmap();
                            long end = System.currentTimeMillis();
                            Log.d(TAG, "Decode Success Use " + (end - start) + "ms");

                        } catch (ReaderException re) {
                            long end = System.currentTimeMillis();
                            Log.d(TAG, "Decode ReaderException Use: " + (end - start) + "ms");
                        } finally {
                            if(rawResult == null && mCamera != null){
                                mCamera.setOneShotPreviewCallback(QRCodeManager.this);
                            }
                            multiFormatReader.reset();
                        }

                        return rawResult;
                    }
                })
                .filter(new Func1<Result, Boolean>() {
                    @Override
                    public Boolean call(Result result) {
                        return result != null;
                    }
                })
                .doOnNext(new Action1<Result>() {
                    @Override
                    public void call(Result result) {
                        resultSubject.onNext(result);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            // Config the camera
            configCamera(surfaceHolder);

            // Start Preview
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mSurfaceView.getHolder().getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        try {
            mCamera.unlock();
            mCamera.reconnect();
            mCamera.setOneShotPreviewCallback(this);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint resultPoint) {

    }

    private void configCamera(SurfaceHolder surfaceHolder) throws IOException{
        mCamera = Camera.open(0);
        mCamera.setPreviewDisplay(surfaceHolder);
        mCamera.setOneShotPreviewCallback(this);


        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenSize = new Size(display.getWidth(), display.getHeight());

        Camera.Parameters parameters = mCamera.getParameters();
        cameraSize = getBestPreviewSize(parameters, screenSize);
        parameters.setPreviewSize(cameraSize.height, cameraSize.width);
        parameters.setPreviewFormat(ImageFormat.NV21);//Default
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        if(isAutoFocusSupported(parameters) == true){
            mCamera.autoFocus(this);
        }
    }

    private boolean isAutoFocusSupported(Camera.Parameters params) {
        List<String> modes = params.getSupportedFocusModes();
        return modes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    /**
     * 获取与屏幕大小最相近的预览图像大小
     */
    private Size getBestPreviewSize(Camera.Parameters parameters, Size screenSize) {
        Size size = new Size(screenSize);
        int diff = Integer.MAX_VALUE;
        List<Camera.Size> previewList = parameters.getSupportedPreviewSizes();
        for (Camera.Size previewSize : previewList) {
            // Rotate 90 degrees
            int previewWidth = previewSize.height;
            int previewHeight = previewSize.width;
            int newDiff = Math.abs(previewWidth - screenSize.width) * Math.abs(previewWidth - screenSize.width)
                    + Math.abs(previewHeight - screenSize.height) * Math.abs(previewHeight - screenSize.height);
            if (newDiff == 0) {
                size.width = previewWidth;
                size.height = previewHeight;
                return size;
            } else if (newDiff < diff) {
                diff = newDiff;
                size.width = previewWidth;
                size.height = previewHeight;
            }
        }
        return size;
    }

    private byte[] rotateYUVdata90(byte[] srcData) {
        byte[] desData = new byte[srcData.length];
        int srcWidth = cameraSize.height;
        int srcHeight = cameraSize.width;

        // Only copy Y
        int i = 0;
        for (int x = 0; x < srcWidth; x++) {
            for (int y = srcHeight - 1; y >= 0; y--) {
                desData[i++] = srcData[y * srcWidth + x];
            }
        }

        return desData;
    }
}
