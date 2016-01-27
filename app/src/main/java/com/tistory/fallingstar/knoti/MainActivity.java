package com.tistory.fallingstar.knoti;

import android.app.AlertDialog;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import android.view.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static int DISPLAY_WIDTH;
    private static int DISPLAY_HEIGHT;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private boolean mServiceOnOff = false;
    private MediaRecorder mMediaRecorder;
    private MainService mService;

    //list alert data
    private final CharSequence[] resolutionItems = {
            "1920x1080", "1280x720", "854x480", "640x360", "426x240"
    };

    private final CharSequence[] framerateItems = {
            "30 FPS", "25 FPS", "15 FPS"
    };

    private final CharSequence[] audioItems = {
            "MIC","INTERNAL"
    };

    private final CharSequence[] titleItems = {
            "해상도 선택", "프레임 레이트 선택", "오디오 소스 선택"
    };

    private TextView mTvRes, mTvFra, mTvAud;
    public AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
		//					 WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        DISPLAY_WIDTH = 480;//480;//metrics.widthPixels;
        DISPLAY_HEIGHT = 640;//640;//metrics.heightPixels;

        mMediaRecorder = new MediaRecorder();
        initRecorder();
        prepareRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mMediaProjectionCallback = new MediaProjectionCallback();

        //
        //SharedPreferences prefs = getSharedPreferences("KNotiOption" ,MODE_PRIVATE);
        //prefs.getString( "RES",  "");
        //prefs.getInt( "FRE",  0);

        //설정 터치시에
        initOptions();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initOptions(){
        mTvRes = (TextView)findViewById(R.id.tv_set_res);
        mTvFra = (TextView)findViewById(R.id.tv_set_fra);
        mTvAud = (TextView)findViewById(R.id.tv_set_aud);

        mTvRes.setOnTouchListener(tvTouchListener);
        mTvFra.setOnTouchListener(tvTouchListener);
        mTvAud.setOnTouchListener(tvTouchListener);
    }

    private View.OnTouchListener tvTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction() == MotionEvent.ACTION_DOWN){
                if(mTvRes.getId() == v.getId()){
                    myDialog(0);
                }else if(mTvFra.getId() == v.getId()){
                    myDialog(1);
                }else if(mTvAud.getId() == v.getId()){
                    myDialog(2);
                }
            }
            return true;
        }
    };

    public void myDialog(final int idx){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);     // 여기서 this는 Activity의 this

        CharSequence[] ref = null;
        if(idx == 0) ref = resolutionItems;
        else if(idx == 1) ref = framerateItems;
        else if(idx == 2) ref = audioItems;

        // 여기서 부터는 알림창의 속성 설정
        builder.setTitle(titleItems[idx])        // 제목 설정
                .setSingleChoiceItems(ref, -1, new DialogInterface.OnClickListener() {
                    // 목록 클릭시 설정
                    public void onClick(DialogInterface dialog, int index) {
                        //Toast.makeText(getApplicationContext(), resolutionItems[index], Toast.LENGTH_SHORT).show();
                        if (idx == 0) {
                            mTvRes.setText(resolutionItems[index]);
                        } else if (idx == 1) {
                            mTvFra.setText(framerateItems[index]);
                        } else if (idx == 2) {
                            mTvAud.setText(audioItems[index]);
                        }
                        mDialog.dismiss();
                    }
                });

        mDialog = builder.create();    // 알림창 객체 생성
        mDialog.show();    // 알림창 띄우기
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            MainService.MainServiceBinder binder = (MainService.MainServiceBinder) service;
            mService = binder.getService();
            mService.registerCallback(mCallback);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private MainService.ICallback mCallback = new MainService.ICallback() {

        public void sendData(boolean flag) {

            mServiceOnOff = flag;

            if(flag == true){
                shareScreen();
            }
            else{
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                stopScreenSharing();
                initRecorder();
                prepareRecorder();
            }
        }
    };

    public void startServiceMethod(View v){
        Intent Service = new Intent(this, MainService.class);
        //startService(Service);
        bindService(Service, mConnection, Context.BIND_AUTO_CREATE);

        moveTaskToBack(false);
    }

    /*
    public void endServiceMethod(View v){
        Intent Service = new Intent(this, MainService.class);
        stopService(Service);
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if(mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        //앱 종료시 서비스도 같이 종료.
        if(mService != null) {
            Intent Service = new Intent(MainActivity.this, MainService.class);
            unbindService(mConnection);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            //mToggleButton.setChecked(false);
            mService.setToggleBtn(false);

            Intent Service = new Intent(MainActivity.this, MainService.class);
            unbindService(mConnection);

            return;
        }
        //권한 요청이 성공한 경우.
        moveTaskToBack(false);
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        //mMediaRecorder.release();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mServiceOnOff) {
                mServiceOnOff = false;
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

    private void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void initRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(7776000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(480, 640);
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        mMediaRecorder.setOutputFile("/sdcard/capture.mp4");
    }


}
