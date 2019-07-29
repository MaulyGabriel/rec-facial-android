package com.solinftec.solinface;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.luxand.FSDK;
import com.solinftec.solinface.sdk.CameraSettings;
import com.solinftec.solinface.sdk.Preview;
import com.solinftec.solinface.sdk.ProcessImageAndDrawResults;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    public static final int MEMORY_LIMIT = 2000;
    public static String TAG;
    private final String databaseFile = "faces.dat";
    private ProcessImageAndDrawResults mDraw;
    private boolean mIsFailed = false;
    private FrameLayout mLayout;
    private Preview mPreview;
    //private PowerManager.WakeLock mWakeLock;
    private boolean wasStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        CameraSettings.density = this.getResources().getDisplayMetrics(

        ).scaledDensity;
        int activation = FSDK.ActivateLibrary(FSDK.key);
        if (activation != 0) {
            this.mIsFailed = true;
            this.showErrorAndClose("FaceSDK activation failed", activation);
        } else {
            FSDK.Initialize();
            this.getWindow().setFlags(1024, 1024);
            this.requestWindowFeature(1);
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            this.mLayout = new FrameLayout(this);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -1);
            this.mLayout.setLayoutParams(layoutParams);
            this.setContentView(this.mLayout);
            this.checkCameraPermissionsAndOpenCamera();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        if (!this.mIsFailed) {
            /*if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }*/

            if (this.mDraw != null) {
                this.pauseProcessingFrames();
                String file = this.getApplicationInfo().dataDir +
                        File.separator +
                        databaseFile;
                FSDK.SaveTrackerMemoryToFile(this.mDraw.mTracker, file);
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (!this.mIsFailed) {
            /*this.mWakeLock = ((PowerManager) this.getSystemService("power")).newWakeLock(10, "Luxand");
            if (!this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            }*/

            this.resumeProcessingFrames();
            StringBuilder message = new StringBuilder();
            message.append("mDraw: ");
            String valid;
            if (this.mDraw != null) {
                valid = "valid";
            } else {
                valid = "null";
            }

            message.append(valid);
            Log.d(TAG, message.toString());
            message = new StringBuilder();
            message.append("mPreview: ");
            if (this.mPreview != null) {
                valid = "valid";
            } else {
                valid = "null";
            }

            message.append(valid);
            Log.d(TAG, message.toString());
        }
    }

    @Override
    protected void onStart() {
        String message = "onStart, wasStopped:" + this.wasStopped;
        Log.d(TAG, message);
        super.onStart();

        if (!this.mIsFailed) {
            if (this.wasStopped && this.mDraw == null) {
                Log.d(TAG, "openCamera");
                this.checkCameraPermissionsAndOpenCamera();
                this.wasStopped = false;
            }

        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (!this.mIsFailed) {
            if (this.mDraw != null || this.mPreview != null) {
                this.mPreview.setVisibility(View.GONE);
                this.mLayout.setVisibility(View.GONE);
                this.mLayout.removeAllViews();
                this.mPreview.releaseCallbacks();
                this.mPreview = null;
                this.mDraw = null;
                this.wasStopped = true;
            }
        }
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        /*
        if (view.getId() == R.id.clearButton) {
            (new AlertDialog.Builder(this)).setMessage("Are you sure to clear the memory?").setPositiveButton("Ok", new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.pauseProcessingFrames();
                    FSDK.ClearTracker(MainActivity.this.mDraw.mTracker);
                    MainActivity.this.resetTrackerParameters();
                    MainActivity.this.resumeProcessingFrames();
                }
            }).setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).setCancelable(false).show();
        } else if (view.getId() == R.id.cameraButton) {
            if (CameraSettings.currentCamera == 1) {
                CameraSettings.currentCamera = 0;
            } else {
                CameraSettings.currentCamera = 1;
            }

            this.mPreview.switchToCamera(CameraSettings.currentCamera);
            this.mPreview.forceStartPreviewOnSurfaceChange();
            this.mPreview.surfaceChanged(null, 0, 0, 0);
        }
        */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            this.openCamera();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public static void alert(Context context, final Runnable runnable, String message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage(message);
        dialogBuilder.setNegativeButton("Ok", new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if (runnable != null) {
            dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    runnable.run();
                }
            });
        }

        dialogBuilder.show();
    }

    private void checkCameraPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.CAMERA")) {
                alert(this, new Runnable() {
                    public void run() {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.CAMERA"}, 1);
                    }
                }, "The application processes frames from camera.");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 1);
            }
        } else {
            this.openCamera();
        }
    }

    private void openCamera() {
        View view = new View(this);
        view.setBackgroundColor(-16777216);
        this.mDraw = new ProcessImageAndDrawResults(this);
        this.mPreview = new Preview(this, this.mDraw);
        this.mDraw.mTracker = new FSDK.HTracker();
        String file = this.getApplicationInfo().dataDir + File.separator + databaseFile;
        if (FSDK.LoadTrackerMemoryFromFile(this.mDraw.mTracker, file) != 0) {
            int tracker = FSDK.CreateTracker(this.mDraw.mTracker);
            if (tracker != 0) {
                this.showErrorAndClose("Error creating tracker", tracker);
            }
        }

        this.resetTrackerParameters();

        this.getWindow().setBackgroundDrawable(new ColorDrawable());
        this.mLayout.setVisibility(View.VISIBLE);
        this.addContentView(view, new ViewGroup.LayoutParams(-1, -1));
        this.addContentView(this.mPreview, new ViewGroup.LayoutParams(-1, -1));
        this.addContentView(this.mDraw, new ViewGroup.LayoutParams(-2, -2));
       /*
        view = ((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_menu, null);
        view.findViewById(R.id.container).setOnClickListener(this);
        view.findViewById(R.id.cameraButton).setOnClickListener(this);
        view.findViewById(R.id.clearButton).setOnClickListener(this);
        this.addContentView(view, new ViewGroup.LayoutParams(-1, -1));
        */
    }

    private void pauseProcessingFrames() {
        if (this.mDraw != null) {
            this.mDraw.stopping = 1;

            for (int i = 0; i < 100 && this.mDraw.stopped == 0; ++i) {
                try {
                    Thread.sleep(10L);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void resetTrackerParameters() {
        int[] params = new int[1];
        FSDK.SetTrackerMultipleParameters(this.mDraw.mTracker,"ContinuousVideoFeed=true;FacialFeatureJitterSuppression=0;RecognitionPrecision=1;Threshold=0.996;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=2000;HandleArbitraryRotations=true;DetermineFaceRotationAngle=true;InternalResizeWidth=70;FaceDetectionThreshold=3;", params);
        //FSDK.SetTrackerMultipleParameters(this.mDraw.mTracker, "ContinuousVideoFeed=true;FacialFeatureJitterSuppression=0;RecognitionPrecision=1;Threshold=0.996;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=2000;HandleArbitraryRotations=false;DetermineFaceRotationAngle=false;InternalResizeWidth=70;FaceDetectionThreshold=3;", params);
        if (params[0] != 0) {
            this.showErrorAndClose("Error setting tracker parameters, position", params[0]);
        }

    }

    private void resumeProcessingFrames() {
        if (this.mDraw != null) {
            this.mDraw.stopped = 0;
            this.mDraw.stopping = 0;
        }

    }

    public void showErrorAndClose(String error, int message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        StringBuilder str = new StringBuilder();
        str.append(error);
        str.append(": ");
        str.append(message);
        dialogBuilder.setMessage(str.toString()).setPositiveButton("Ok", new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Process.killProcess(Process.myPid());
            }
        }).show();
    }

    public void showMessage(String message) {
        (new AlertDialog.Builder(this)).setMessage(message).setPositiveButton("Ok", new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).setCancelable(false).show();
    }
}
