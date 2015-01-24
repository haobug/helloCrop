package com.example.hellocrop;

import android.app.Activity;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;


public class VideoCropActivity extends Activity implements TextureView.SurfaceTextureListener {
 // Log tag
    private static final String TAG = VideoCropActivity.class.getName();

    // Asset video file name
    private static final String FILE_NAME = "/sdcard/DCIM/Camera/VID_20150124_170119.3gp";

    // MediaPlayer instance to control playback of video file.
    private MediaPlayer mMediaPlayer;
    private TextureView mTextureView;
 // Original video size, in our case 640px / 360px
    private float mVideoWidth;
    private float mVideoHeight;

    private Activity mMovieActivity;
    ScaleGestureDetector mSGD;

    private int mScreenWidth;
    private int mScreenHeight;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.texture_video_crop);
        
        calculateVideoSize();
        initView();
    }
    
    private void initView() {
        mTextureView = (TextureView) findViewById(R.id.textureView);
        // SurfaceTexture is available only after the TextureView
        // is attached to a window and onAttachedToWindow() has been invoked.
        // We need to use SurfaceTextureListener to be notified when the SurfaceTexture
        // becomes available.
        mTextureView.setSurfaceTextureListener(this);
        
        FrameLayout rootView = (FrameLayout) findViewById(R.id.rootView);
        mMovieActivity = this;
        
        mSGD = new ScaleGestureDetector(
                mMovieActivity, new ScaleGestureDetector.OnScaleGestureListener() {
                    float mFacttor;

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        android.util.Log.e(TAG, "onScale");
                        mFacttor = detector.getScaleFactor();
                        zoomView(mFacttor);
                        return true;
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        Log.e(TAG, "onScaleBegin");
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        Log.e(TAG, "onScaleEnd");
                        //zoomView(mFacttor);
                    }

                });
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getPointerCount() == 2){
                    mSGD.onTouchEvent(motionEvent);
                    return true;
                }
                /*switch (motionEvent.getAction()) {
                   case MotionEvent.ACTION_UP:
                        updateTextureViewSize((int) motionEvent.getX(), (int) motionEvent.getY(), 1);
                        
                        break;
                }*/
                return true;
            }
        });
    }
    
    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }
    private void zoomView(float factor){
        FrameLayout.LayoutParams layoutParams = (LayoutParams) mTextureView.getLayoutParams();
        
        layoutParams.width *= factor;
        layoutParams.height *= factor;
        mTextureView.setLayoutParams(layoutParams);
        //after resize
        layoutParams = (LayoutParams) mTextureView.getLayoutParams();
        Log.e("LOG_TAG", String.format("l:%5dt:%5dw:%5d,h:%5d",
                mTextureView.getLeft(), mTextureView.getTop(),
                layoutParams.width, layoutParams.height));
        mTextureView.setLeft(mScreenWidth/2 - layoutParams.width);
        mTextureView.setTop(mScreenHeight/2 - layoutParams.height);
        Log.e("LOG_TAG", String.format("l:%5dt:%5dw:%5d,h:%5d",
                mTextureView.getLeft(), mTextureView.getTop(),
                layoutParams.width, layoutParams.height));
    }
    private void updateTextureViewSize(int viewWidth, int viewHeight, float factor) {
        float scaleX = factor;
        float scaleY = factor;
        Log.e(TAG, "factor:" + factor);

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);

        mTextureView.setTransform(matrix);
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            // Make sure we stop video and release resources when activity is destroyed.
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Surface surface = new Surface(surfaceTexture);

        try {
            /*AssetFileDescriptor afd = getAssets().openFd(FILE_NAME);*/
            mMediaPlayer = new MediaPlayer();
            /*mMediaPlayer
                    .setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());*/
            mMediaPlayer.setDataSource(FILE_NAME);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
    private void initScreenSize() {
        try {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            mMovieActivity.getWindowManager().getDefaultDisplay()
                    .getMetrics(displaymetrics);
            mScreenWidth = displaymetrics.widthPixels;
            mScreenHeight = displaymetrics.heightPixels;
        } catch (Exception e) {
            Log.e(TAG, "Error calculateVideoSize " + e.getMessage());
        }
    }
    
    private void calculateVideoSize() {
        try {
            /*AssetFileDescriptor afd = getAssets().openFd(FILE_NAME);*/
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            /*metaRetriever.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());*/
            metaRetriever.setDataSource(FILE_NAME);
            String height = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            mVideoHeight = Float.parseFloat(height);
            mVideoWidth = Float.parseFloat(width);

        } catch (Exception e) {
            Log.e(TAG, "Error calculateVideoSize " + e.getMessage());
        }
    }
}
