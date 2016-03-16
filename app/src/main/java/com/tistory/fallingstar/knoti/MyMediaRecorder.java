package com.tistory.fallingstar.knoti;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ko-seunghyun on 2016-03-16.
 */
public class MyMediaRecorder {

    private static final String TAG = "MyMediaRecorder";

    private MediaRecorder mMediaRecorder;
    private Surface mSurface;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private int mScreenDensity;
    private Context mContext;
    private String mStrAUD, mStrMode, mStrRES, mStrFRA;
    private int mNXRES , mNYRES, mNFRA;
    private String m_strOutPath;
    private boolean mIsRecordOn = false;

    //empty constructor
    public MyMediaRecorder(){

    }

    public MyMediaRecorder(Context c){
        mContext = c;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) c.getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mMediaProjectionCallback = new MediaProjectionCallback();
    }

    public void setScreenDensity(int density){
        mScreenDensity = density;
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mIsRecordOn) {
                mIsRecordOn = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                initRecorder();
                prepareRecorder();
            }
            mMediaProjection = null;
            stopScreenSharing();
            Log.i(TAG, "MediaProjection Stopped");
        }
    }

    //Recorder INIT
    public void initRecorder() {

        SharedPreferences prefs = mContext.getSharedPreferences("KNotiOption", Activity.MODE_PRIVATE);
        mStrRES = prefs.getString("RES", "1280x720");
        mStrFRA = prefs.getString("FRA", "25");
        mStrAUD = prefs.getString("AUD", "MIC");
        mStrMode = prefs.getString("MODE", "가로");

        String[] res = mStrRES.split("x");
        mNXRES = Integer.parseInt(res[1]);
        mNYRES = Integer.parseInt(res[0]);

        mNFRA = Integer.parseInt(mStrFRA);

        mMediaRecorder.reset();

        if(mStrAUD.compareTo("음소거") == 0) {
            //mute
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mMediaRecorder.setVideoEncodingBitRate(7776000);

        if(mStrAUD.compareTo("음소거") == 0) {
            //mute
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if(mStrMode.compareTo("가로")==0){
            mMediaRecorder.setVideoSize(mNYRES, mNXRES);
        }else{
            mMediaRecorder.setVideoSize(mNXRES, mNYRES);
        }
        mMediaRecorder.setVideoFrameRate(mNFRA);

        //설정저장.
        prefs = mContext.getSharedPreferences("KNotiOption", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("RES", mStrRES);
        editor.putString("FRA", mStrFRA);
        editor.putString("AUD", mStrAUD);
        editor.putString("MODE", mStrMode);
        editor.commit();
    }

    public void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "녹화 준비 실패 - 상태이상", Toast.LENGTH_SHORT).show();
            return;
            //finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "녹화 준비 실패 - 파일경로이상", Toast.LENGTH_SHORT).show();
            return;
            //finish();
        }
        mSurface = mMediaRecorder.getSurface();
    }

    public void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        //mMediaRecorder.release();
    }

    public VirtualDisplay createVirtualDisplay() {
        int nX, nY;
        if(mStrMode.compareTo("가로") == 0){
            nX = mNYRES; nY = mNXRES;
        }else {
            nX = mNXRES;
            nY = mNYRES;
        }

        return mMediaProjection.createVirtualDisplay("MainActivity",
                //DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                nX, nY, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface/*mMediaRecorder.getSurface()*/, null /*Callbacks*/, null /*Handler*/);
    }

    public MediaProjection getMediaProjection(){
        return mMediaProjection;
    }

    public MediaProjectionManager getmProjectionManager(){
        return mProjectionManager;
    }

    public void setServiceOnFlag(boolean f){
        mIsRecordOn = f;
    }

    public void setVirtualDisplay(){
        try{
            mVirtualDisplay = createVirtualDisplay();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(mContext, "가상 디스플레이 생성 실패", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void startRecord(){
        mMediaRecorder.start();
    }

    public void stopRecord(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
    }

    public void scanMedia(){
        //미디어 스캐닝
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + m_strOutPath)));

    }
    public void setOutFile(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        Date date = new Date();
        String today = df.format(date);

        //m_strOutPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/KRec/"+today+".mp4";
        m_strOutPath =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + today + ".mp4";
        mMediaRecorder.setOutputFile(m_strOutPath);
    }

    public void createMediaProjection(int resultCode, Intent data){

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

    }

}
