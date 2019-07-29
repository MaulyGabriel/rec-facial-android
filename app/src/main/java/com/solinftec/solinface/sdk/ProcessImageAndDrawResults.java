package com.solinftec.solinface.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.luxand.FSDK;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessImageAndDrawResults extends View {

    private static final String TAG = "ProcessImageView";

    public volatile boolean switchingCamera;
    public FSDK.HTracker mTracker;
    public int cameraType;
    public int stopping = 0;
    public int stopped = 0;
    public boolean rotated;

    private final int MAX_FACES = 5;
    private final Lock faceLock = new ReentrantLock();
    private boolean firstFrameSaved;
    private Context mContext;
    private final FaceRectangle[] facePositions = new FaceRectangle[5];
    private float fps = 0.0F;
    private int frameCount = 0;
    private final long[] mIDs = new long[5];
    public int mImageHeight;
    public int mImageWidth;
    private boolean mIsShowingFps = true;
    private Paint mPaintBlue;
    private Paint mPaintBlueTransparent;
    private Paint mPaintGreen;
    public byte[] mRGBData;
    private long mTime = 0L;
    private long mTouchedID;
    private int mTouchedIndex = -1;
    public byte[] mYUVData;
    private boolean showingDialog;
    public static final int WIDTH = 800;
    public static final int HEIGHT = 480;

    public ProcessImageAndDrawResults(Context context) {
        super(context);
        this.mContext = context;
        this.mPaintGreen = new Paint();
        this.mPaintGreen.setStyle(Style.FILL);
        this.mPaintGreen.setColor(Color.WHITE);
        this.mPaintGreen.setTextSize(CameraSettings.density * 18.0F);
        this.mPaintGreen.setTextAlign(Align.CENTER);
        this.mPaintBlue = new Paint();
        this.mPaintBlue.setStyle(Style.FILL);
        this.mPaintBlue.setColor(Color.BLUE);
        this.mPaintBlue.setTextSize(CameraSettings.density * 18.0F);
        this.mPaintBlue.setTextAlign(Align.CENTER);
        this.mPaintBlueTransparent = new Paint();
        this.mPaintBlueTransparent.setStyle(Style.STROKE);
        this.mPaintBlueTransparent.setStrokeWidth(2.0F);
        this.mPaintBlueTransparent.setColor(-16776961);
        this.mPaintBlueTransparent.setTextSize(18.0F * CameraSettings.density);
        this.mYUVData = null;
        this.mRGBData = null;
        this.firstFrameSaved = false;
    }

    public static void decodeYUV420SP(byte[] var0, byte[] var1, int var2, int var3) {
        int var4 = 0;

        for (int var5 = 0; var4 < var3; ++var4) {
            int var6 = (var4 >> 1) * var2 + var2 * var3;
            int var7 = 0;
            int var8 = 0;

            int var12;
            for (int var9 = 0; var9 < var2; var6 = var12) {
                int var10 = (var1[var5] & 255) - 16;
                int var11 = var10;
                if (var10 < 0) {
                    var11 = 0;
                }

                var12 = var6;
                if ((var9 & 1) == 0) {
                    var10 = var6 + 1;
                    byte var15 = var1[var6];
                    byte var14 = var1[var10];
                    var7 = (var15 & 255) - 128;
                    var12 = var10 + 1;
                    var8 = (var14 & 255) - 128;
                }

                var11 = 1192 * var11;
                var6 = 1634 * var7 + var11;
                var10 = var11 - 833 * var7 - 400 * var8;
                int var13 = var11 + 2066 * var8;
                if (var6 < 0) {
                    var11 = 0;
                } else {
                    var11 = var6;
                    if (var6 > 262143) {
                        var11 = 262143;
                    }
                }

                if (var10 < 0) {
                    var6 = 0;
                } else {
                    var6 = var10;
                    if (var10 > 262143) {
                        var6 = 262143;
                    }
                }

                if (var13 < 0) {
                    var10 = 0;
                } else {
                    var10 = var13;
                    if (var13 > 262143) {
                        var10 = 262143;
                    }
                }

                var13 = 3 * var5;
                var0[var13] = (byte) ((byte) (var11 >> 10 & 255));
                var0[var13 + 1] = (byte) ((byte) (var6 >> 10 & 255));
                var0[var13 + 2] = (byte) ((byte) (var10 >> 10 & 255));
                ++var5;
                ++var9;
            }
        }

    }

    private int GetFaceFrame(FSDK.FSDK_Features features, FaceRectangle rectangle) {
        if (features != null && rectangle != null) {
            float x0 = (float) features.features[0].x;
            float y0 = (float) features.features[0].y;
            float x1 = (float) features.features[1].x;
            float y1 = (float) features.features[1].y;
            float x = (x0 + x1) / 2.0F;
            float y = (y0 + y1) / 2.0F;
            x0 = x1 - x0;
            y1 -= y0;
            int p = (int) Math.pow((double) (x0 * x0 + y1 * y1), 0.5D);
            double xd = (double) x;
            double pd = (double) p;
            double z = 1.6D * pd * 0.9D;
            rectangle.x1 = (int) (xd - z);
            double yd = (double) y;
            rectangle.y1 = (int) (yd - 1.1D * pd * 0.9D);
            rectangle.x2 = (int) (xd + z);
            rectangle.y2 = (int) (yd + pd * 2.1D * 0.9D);
            if (rectangle.x2 - rectangle.x1 > rectangle.y2 - rectangle.y1) {
                rectangle.x2 = rectangle.x1 + rectangle.y2 - rectangle.y1;
            } else {
                rectangle.y2 = rectangle.y1 + rectangle.x2 - rectangle.x1;
            }

            return 0;
        } else {
            return -4;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.stopping == 1) {
            this.stopped = 1;
            super.onDraw(canvas);
        } else {
            if (this.switchingCamera) {
                FSDK.SetTrackerParameter(this.mTracker, "VideoFeedDiscontinuity", "0");
                this.switchingCamera = false;
            }

            if (this.mYUVData != null && this.mTouchedIndex == -1) {
                int width = WIDTH;// canvas.getWidth();
                decodeYUV420SP(this.mRGBData, this.mYUVData, this.mImageWidth, this.mImageHeight);
                FSDK.HImage hImage = new FSDK.HImage();
                FSDK.FSDK_IMAGEMODE imagemode = new FSDK.FSDK_IMAGEMODE();
                imagemode.mode = 1;
                FSDK.LoadImageFromBuffer(hImage, this.mRGBData, this.mImageWidth, this.mImageHeight, this.mImageWidth * 3, imagemode);

                if (this.cameraType == 1) {
                    FSDK.MirrorImage(hImage, false);
                }

                FSDK.HImage image = new FSDK.HImage();
                FSDK.CreateEmptyImage(image);
                int mImageWidth = HEIGHT; //this.mImageWidth;
                if (this.rotated) {
                    mImageWidth = this.mImageHeight;
                    if (this.cameraType == 1) {
                        FSDK.RotateImage90(hImage, -1, image);
                    } else {
                        FSDK.RotateImage90(hImage, 1, image);
                    }
                } else {
                    FSDK.RotateImage90(hImage, 2, image);
                }

                //FSDK.CopyImage(hImage, image);

                FSDK.FreeImage(hImage);
                long[] imageFrames = new long[5];
                long[] var12 = new long[1];
                FSDK.FeedFrame(this.mTracker, 0L, image, var12, imageFrames);
                FSDK.FreeImage(image);
                this.faceLock.lock();

                for (int i = 0; i < 5; ++i) {
                    this.facePositions[i] = new FaceRectangle();
                    this.facePositions[i].x1 = 0;
                    this.facePositions[i].y1 = 0;
                    this.facePositions[i].x2 = 0;
                    this.facePositions[i].y2 = 0;
                    this.mIDs[i] = imageFrames[i];
                }

                float widthProp = (float) width * 1.0F / (float) mImageWidth;

                for (mImageWidth = 0; mImageWidth < (int) var12[0]; ++mImageWidth) {
                    FSDK.FSDK_Features var13 = new FSDK.FSDK_Features();
                    FSDK.GetTrackerEyes(this.mTracker, 0L, this.mIDs[mImageWidth], var13);
                    this.GetFaceFrame(var13, this.facePositions[mImageWidth]);
                    FaceRectangle faceRectangle = this.facePositions[mImageWidth];
                    faceRectangle.x1 = (int) ((float) faceRectangle.x1 * widthProp);
                    faceRectangle = this.facePositions[mImageWidth];
                    faceRectangle.y1 = (int) ((float) faceRectangle.y1 * widthProp);
                    faceRectangle = this.facePositions[mImageWidth];
                    faceRectangle.x2 = (int) ((float) faceRectangle.x2 * widthProp);
                    faceRectangle = this.facePositions[mImageWidth];
                    faceRectangle.y2 = (int) ((float) faceRectangle.y2 * widthProp);
                }

                this.faceLock.unlock();
                width = (int) (22.0F * CameraSettings.density);

                for (mImageWidth = 0; (long) mImageWidth < var12[0]; ++mImageWidth) {
                    boolean faceDetected;
                    faceDetection:
                    {
                        canvas.drawRect((float) this.facePositions[mImageWidth].x1, (float) this.facePositions[mImageWidth].y1, (float) this.facePositions[mImageWidth].x2, (float) this.facePositions[mImageWidth].y2, this.mPaintBlueTransparent);
                        if (imageFrames[mImageWidth] != -1L) {
                            String[] faceId = new String[1];
                            FSDK.GetAllNames(this.mTracker, imageFrames[mImageWidth], faceId, 1024L);
                            if (faceId[0] != null && faceId[0].length() > 0) {
                                Log.d(TAG, "Face detected: " + faceId[0]);
                                canvas.drawText(faceId[0], (float) ((this.facePositions[mImageWidth].x1 + this.facePositions[mImageWidth].x2) / 2), (float) (this.facePositions[mImageWidth].y2 + width), this.mPaintBlue);
                                faceDetected = true;
                                //Toast.makeText(getContext(), "Olá " + faceId[0], Toast.LENGTH_SHORT).show();
                                //create communication -> send operator

                                break faceDetection;
                            }
                        }

                        faceDetected = false;
                    }

                    if (!faceDetected) {
                        Log.d(TAG, "No face detected");

                        float x = (float) ((this.facePositions[mImageWidth].x1 + this.facePositions[mImageWidth].x2) / 2);
                        float y = (float) (this.facePositions[mImageWidth].y2 + width);
                        canvas.drawText("Nao cadastrado", x, y, this.mPaintGreen);

                        // create operator -> code-name
                        enterPersonDialog(0);
                    }
                }

                if (this.mIsShowingFps) {
                    ++this.frameCount;
                    long timeNow = System.currentTimeMillis();
                    if (this.mTime == 0L) {
                        this.mTime = timeNow;
                    }

                    timeNow -= this.mTime;
                    if (timeNow >= 3000L) {
                        this.fps = (float) this.frameCount / ((float) timeNow / 1000.0F);
                        this.frameCount = 0;
                        this.mTime = 0L;
                    }

                }

                super.onDraw(canvas);
            } else {
                super.onDraw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == 0) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            this.faceLock.lock();
            FaceRectangle[] faceRectangles = new FaceRectangle[5];
            long[] ids = new long[5];

            int i;
            for (i = 0; i < 5; ++i) {
                faceRectangles[i] = new FaceRectangle();
                faceRectangles[i].x1 = this.facePositions[i].x1;
                faceRectangles[i].y1 = this.facePositions[i].y1;
                faceRectangles[i].x2 = this.facePositions[i].x2;
                faceRectangles[i].y2 = this.facePositions[i].y2;
                ids[i] = this.mIDs[i];
            }

            this.faceLock.unlock();

            for (i = 0; i < 5; ++i) {
                if (faceRectangles[i] != null && faceRectangles[i].x1 <= x && x <= faceRectangles[i].x2 && faceRectangles[i].y1 <= y && y <= faceRectangles[i].y2 + 30) {
                    //this.mTouchedID = ids[i];
                    //this.mTouchedIndex = i;
                    enterPersonDialog(i);
                    break;
                }
            }
        }

        return true;
    }

    private void enterPersonDialog(int index) {
        if (showingDialog) return;
        showingDialog = true;

        this.mTouchedID = this.mIDs[index];
        this.mTouchedIndex = index;
        final EditText editText = new EditText(this.mContext);
        (new AlertDialog.Builder(this.mContext))
                .setMessage("Enter person's name")
                .setView(editText)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FSDK.LockID(ProcessImageAndDrawResults.this.mTracker, ProcessImageAndDrawResults.this.mTouchedID);
                        String name = editText.getText().toString();
                        FSDK.SetName(ProcessImageAndDrawResults.this.mTracker, ProcessImageAndDrawResults.this.mTouchedID, name);
                        if (name.length() <= 0) {
                            FSDK.PurgeID(ProcessImageAndDrawResults.this.mTracker, ProcessImageAndDrawResults.this.mTouchedID);
                        }

                        FSDK.UnlockID(ProcessImageAndDrawResults.this.mTracker, ProcessImageAndDrawResults.this.mTouchedID);
                        ProcessImageAndDrawResults.this.mTouchedIndex = -1;

                        showingDialog = false;
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ProcessImageAndDrawResults.this.mTouchedIndex = -1;

                        showingDialog = false;
                    }
                })
                .setCancelable(false)
                .show();
    }
}
