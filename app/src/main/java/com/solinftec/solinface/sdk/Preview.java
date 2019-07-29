package com.solinftec.solinface.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

import java.util.Iterator;

public class Preview extends SurfaceView implements Callback {

    private static final String TAG = "SurfaceView";
    Camera mCamera;
    Context mContext;
    ProcessImageAndDrawResults mDraw;
    boolean mFinished;
    SurfaceHolder mHolder;
    boolean mIsCameraOpen = false;
    boolean mIsPreviewStarted = false;

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    public Preview(Context context, ProcessImageAndDrawResults results) {
        super(context);
        this.mContext = context;
        this.mDraw = results;
        this.mHolder = this.getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setType(3);
    }

    private void makeResizeForCameraAspect(float value) {

        LayoutParams layoutParams = this.getLayoutParams();

        int width = this.getWidth();
        int prop = (int) ((float) width / value);

        if (prop != layoutParams.height) {
            layoutParams.height = prop;
            layoutParams.width = width;
            this.setLayoutParams(layoutParams);
            this.invalidate();
        }

    }

    public void forceStartPreviewOnSurfaceChange() {
        this.mIsPreviewStarted = false;
    }

    public void releaseCallbacks() {
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
        }

        if (this.mHolder != null) {
            this.mHolder.removeCallback(this);
        }

        this.mDraw = null;
        this.mHolder = null;
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        Log.d(TAG, "surfaceChanged");

        if (this.mCamera != null) {
            if (this.mIsPreviewStarted) {
                Log.d(TAG, "surfaceChanged: Preview already started");
            } else {
                Parameters params = this.mCamera.getParameters();
                format = this.getResources().getConfiguration().orientation;
                width = 0;
                if (format != 2) {
                    params.set("orientation", "portrait");
                    this.mCamera.setDisplayOrientation(90);
                    this.mDraw.rotated = true;
                } else {
                    params.set("orientation", "landscape");
                    this.mCamera.setDisplayOrientation(0);
                    this.mDraw.rotated = false;
                }

                Iterator sizeIterator = params.getSupportedPreviewSizes().iterator();
                format = 0;

                Size size;
                while (sizeIterator.hasNext()) {
                    size = (Size) sizeIterator.next();
                    int kWidth = width - WIDTH;
                    height = format - HEIGHT;

                    if (kWidth * kWidth + height * height > (size.width - WIDTH) * (size.width - WIDTH) + (size.height - HEIGHT) * (size.height - HEIGHT)) {
                        width = size.width;
                        format = size.height;
                    }
                }

                label38:
                {
                    boolean success;
                    if (width * format > 0) {
                        try {
                            params.setPreviewSize(width, format);
                        } catch (Exception e) {
                            success = false;
                            break label38;
                        }
                    }

                    try {
                        params.setSceneMode("portrait");
                        params.setFocusMode("continuous-video");
                        this.mCamera.setParameters(params);
                    } catch (Exception e) {
                        success = false;
                    }
                }

                this.mCamera.startPreview();
                this.mIsPreviewStarted = true;
                //size = this.mCamera.getParameters().getPreviewSize();
                //this.makeResizeForCameraAspect(1.0F / ((float) size.width * 1.0F / (float) size.height));


            }
        }
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated");
        if (!this.mIsCameraOpen) {
            this.mIsCameraOpen = true;
            this.mFinished = false;
            this.switchToCamera();
        }
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        this.mFinished = true;
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }

        this.mIsCameraOpen = false;
        this.mIsPreviewStarted = false;
    }

    public void switchToCamera() {
        this.switchToCamera(CameraSettings.currentCamera);
    }

    public void switchToCamera(int cameraType) {
        this.mDraw.cameraType = cameraType;
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }

        this.mDraw.switchingCamera = true;
        CameraInfo info = new CameraInfo();
        int pos = 0;
        byte cameraIndex = 0;

        int i;
        for (i = cameraIndex; pos < Camera.getNumberOfCameras(); ++pos) {
            Camera.getCameraInfo(pos, info);
            if (info.facing == cameraType) {
                cameraIndex = 1;
                i = pos;
            }
        }

        if (cameraIndex != 0) {
            this.mCamera = Camera.open(i);
        } else {
            this.mCamera = Camera.open();
        }

        try {
            this.mCamera.setPreviewDisplay(this.getHolder());
            Camera camera = this.mCamera;
            PreviewCallback previewCallback = new PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (Preview.this.mDraw != null && !Preview.this.mFinished) {
                        if (Preview.this.mDraw.mYUVData == null) {
                            Parameters params = camera.getParameters();
                            Preview.this.mDraw.mImageWidth = params.getPreviewSize().width;
                            Preview.this.mDraw.mImageHeight = params.getPreviewSize().height;
                            Preview.this.mDraw.mRGBData = new byte[3 * Preview.this.mDraw.mImageWidth * Preview.this.mDraw.mImageHeight];
                            Preview.this.mDraw.mYUVData = new byte[data.length];
                        }

                        System.arraycopy(data, 0, Preview.this.mDraw.mYUVData, 0, data.length);
                        Preview.this.mDraw.invalidate();
                    }
                }
            };
            camera.setPreviewCallback(previewCallback);
        } catch (Exception e) {
            (new AlertDialog.Builder(this.mContext))
                    .setMessage("Cannot open cameraType")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Process.killProcess(Process.myPid());
                        }
                    })
                    .show();
            if (this.mCamera != null) {
                this.mCamera.release();
                this.mCamera = null;
            }
        }

    }
}
