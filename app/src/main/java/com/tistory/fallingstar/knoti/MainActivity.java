package com.tistory.fallingstar.knoti;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private Surface mSurface;

    private int m_nXRES , m_nYRES, m_nFRA;
    private String m_strAUD, m_strMode;
    private String m_strOutPath;

    //뒤로가기 두번에 종료.
    private BackPressCloseHandler backPressCloseHandler;

    private static final String RECORD_START_ACTION = "com.example.packagename.START";
    private static final String RECORD_END_ACTION = "com.example.packagename.END";
    private static final String RECORD_EXIT_ACTION = "com.example.packagename.EXIT";

    private final int NOTIFICATION_ID = 1;

    private boolean m_bRecordFlag = false;

    //list alert data
    private final CharSequence[] resolutionItems = {
            "1920x1080", "1280x720", "854x480", "640x360", "426x240"
    };

    private final CharSequence[] framerateItems = {
            "30", "25", "15"
    };

    private final CharSequence[] audioItems = {
            "MIC","음소거"
    };

    private final CharSequence[] modeItems = {
            "가로","세로"
    };

    private final CharSequence[] titleItems = {
            "해상도 선택", "프레임 레이트 선택", "오디오 소스 선택", "녹화 모드 선택"
    };

    private TextView mTvRes, mTvFra, mTvAud, mTvMode;
    public AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
		//					 WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


        //설정 위젯들 초기화
        initOptions();

        //폴더 생성
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/KRec/");
        if (!f.exists()) {
            f.mkdir();
        }

        SharedPreferences prefs = getSharedPreferences("KNotiOption", MODE_PRIVATE);
        mTvRes.setText(prefs.getString( "RES",  "1280x720"));
        mTvFra.setText(prefs.getString("FRE", "25"));
        mTvAud.setText(prefs.getString("AUD", "MIC"));
        mTvMode.setText(prefs.getString("MODE", "가로"));

        String []t1 = mTvRes.getText().toString().split("x");
        m_nXRES  = Integer.parseInt(t1[1]);
        m_nYRES  = Integer.parseInt(t1[0]);

        m_nFRA = Integer.parseInt(mTvFra.getText().toString());

        m_strAUD = mTvAud.getText().toString();
        m_strMode = mTvMode.getText().toString();


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        //DISPLAY_WIDTH = m_nXRES; //metrics.widthPixels;
        //DISPLAY_HEIGHT = m_nYRES;//metrics.heightPixels;


        mMediaRecorder = new MediaRecorder();
        initRecorder();
        //prepareRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        mMediaProjectionCallback = new MediaProjectionCallback();

        //화면꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        backPressCloseHandler = new BackPressCloseHandler(this);

        initNotification();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_START_ACTION);
        intentFilter.addAction(RECORD_END_ACTION);
        registerReceiver(buttonBroadcastReceiver, intentFilter);
    }

    private RemoteViews contentiew;
    private Notification noti;
    private NotificationManager nm;
    private Notification.Builder builder;
    public void initNotification(){
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);

        contentiew = new RemoteViews(getPackageName(), R.layout.remoteview);

        Intent intent_ = new Intent(RECORD_START_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent_,
                PendingIntent.FLAG_UPDATE_CURRENT);
        contentiew.setOnClickPendingIntent(R.id.btn_noti_start, pendingIntent);

        Intent intent_1 = new Intent(RECORD_END_ACTION);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(this, 1, intent_1,
                PendingIntent.FLAG_UPDATE_CURRENT);
        contentiew.setOnClickPendingIntent(R.id.btn_noti_end, pendingIntent1);

        //contentiew.setOnClickPendingIntent(R.id.button, pendingIntent);
        builder.setContent(contentiew);
        noti = builder.build();
        //noti.contentView = contentiew;
        noti.flags  |= Notification.FLAG_NO_CLEAR;
        nm.notify(NOTIFICATION_ID, noti);
    }

    BroadcastReceiver buttonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if(RECORD_START_ACTION.equals(action)) {

                if(!m_bRecordFlag){

                    //statusBarUp();

                    Toast.makeText(context, "녹화시작", Toast.LENGTH_SHORT).show();
                    if (mMediaProjection != null) {
                        initRecorder();
                        setOutFile();
                        prepareRecorder();
                        shareScreen();
                    }
                    else {
                        shareScreen();
                    }
                }

                //TextView tvStatus = (TextView) findViewById(R.id.tv_status);
                //tvStatus.setText("녹화중");
                contentiew.setTextViewText(R.id.tv_status, "녹화중");
                builder.setContent(contentiew);
                nm.notify(NOTIFICATION_ID, builder.build());
                m_bRecordFlag = true;

            } else if(RECORD_END_ACTION.equals(action)) {
                if(m_bRecordFlag){
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    Log.v(TAG, "Recording Stopped");
                    stopScreenSharing();
                    Toast.makeText(context, "녹화종료.", Toast.LENGTH_SHORT).show();
                    //미디어 스캐닝
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + m_strOutPath)));
                }

                contentiew.setTextViewText(R.id.tv_status, "녹화종료");
                builder.setContent(contentiew);
                //noti =  builder.build();
                //noti.flags  |= Notification.FLAG_AUTO_CANCEL;
                nm.notify(NOTIFICATION_ID, builder.build());

                m_bRecordFlag = false;
            } else {
                //?????
            }
        }
    };

    public void statusBarUp(){
        try{
            Object sbservice = getApplication().getSystemService("statusbar");  // statusbar 시스템 서비스 객체를 가져온다.
            Class<?> statusbarManager;
            statusbarManager = Class.forName("android.app.StatusBarManager");   // StatusBarManager 클래스의 정보를 담은 Class 객체를 가져온다.
            //Method showsb = statusbarManager.getMethod("expand");               // 상태바를 내리는 expand 메서드를 가져온 뒤,
            Method showsb = statusbarManager.getMethod("collapse");               // 상태바를 내리는 expand 메서드를 가져온 뒤,
            showsb.invoke(sbservice);                                           // 위에서 얻은 statusbar 시스템 서비스 객체를 대상으로, expand 메서드 호출!
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

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

        if(nm != null){
            nm.cancel(NOTIFICATION_ID);
        }

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
            this.finish();
            //return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    private void initOptions(){
        mTvRes = (TextView)findViewById(R.id.tv_set_res);
        mTvFra = (TextView)findViewById(R.id.tv_set_fra);
        mTvAud = (TextView)findViewById(R.id.tv_set_aud);
        mTvMode =(TextView)findViewById(R.id.tv_set_mode);

        mTvRes.setOnTouchListener(tvTouchListener);
        mTvFra.setOnTouchListener(tvTouchListener);
        mTvAud.setOnTouchListener(tvTouchListener);
        mTvMode.setOnTouchListener(tvTouchListener);
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
                }else if(mTvMode.getId() == v.getId()){
                    myDialog(3);
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
        else if(idx == 3) ref = modeItems;

        // 여기서 부터는 알림창의 속성 설정
        builder.setTitle(titleItems[idx])        // 제목 설정
                .setSingleChoiceItems(ref, -1, new DialogInterface.OnClickListener() {
                    // 목록 클릭시 설정
                    public void onClick(DialogInterface dialog, int index) {
                        //Toast.makeText(getApplicationContext(), resolutionItems[index], Toast.LENGTH_SHORT).show();
                        if (idx == 0) {
                            mTvRes.setText(resolutionItems[index]);
                            String[] res = resolutionItems[index].toString().split("x");
                            m_nXRES = Integer.parseInt(res[1]);
                            m_nYRES = Integer.parseInt(res[0]);
                            //DISPLAY_WIDTH = m_nXRES;
                            //DISPLAY_HEIGHT = m_nYRES;
                        } else if (idx == 1) {
                            mTvFra.setText(framerateItems[index]);
                            m_nFRA = Integer.parseInt(framerateItems[index].toString());
                        } else if (idx == 2) {
                            mTvAud.setText(audioItems[index]);
                            m_strAUD = audioItems[index].toString();
                        } else if (idx == 3) {
                            mTvMode.setText(modeItems[index]);
                            m_strMode = modeItems[index].toString();
                        }

                        mDialog.dismiss();

                        initRecorder(); //옵션 재설정.
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
                if (mMediaProjection != null) {
                    initRecorder();
                    setOutFile();
                    prepareRecorder();
                    shareScreen();
                }
                else {
                    shareScreen();
                }

                Toast.makeText(getApplicationContext(), "빨간버튼 클릭시 녹화 중지.", Toast.LENGTH_SHORT).show();
            }
            else{
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                stopScreenSharing();

                Toast.makeText(getApplicationContext(), "회색버튼 클릭시 녹화 시작.", Toast.LENGTH_SHORT).show();
                //미디어 스캐닝
                //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + "폴더위치" + "파일이름" + ".파일확장자")));
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + m_strOutPath)));

            }
        }
    };

    public void startServiceMethod(View v){
        //Intent Service = new Intent(this, MainService.class);
        //startService(Service);
        //bindService(Service, mConnection, Context.BIND_AUTO_CREATE);

        moveTaskToBack(true);

        //Toast.makeText(getApplicationContext(), "회색버튼 클릭시 녹화 시작.", Toast.LENGTH_SHORT).show();
    }

    /*
    public void endServiceMethod(View v){
        Intent Service = new Intent(this, MainService.class);
        stopService(Service);
    }*/



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
        moveTaskToBack(true);

        initRecorder();
        setOutFile();
        prepareRecorder();

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        try{
            mVirtualDisplay = createVirtualDisplay();
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        mMediaRecorder.start();

        //액티비티 생명 주기에 따라 서비스가 종료되니 다시 되살림.
        /*if(mService == null) {
            Intent Service = new Intent(this, MainService.class);
            bindService(Service, mConnection, Context.BIND_AUTO_CREATE);

        }*/

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
        int nX, nY;
        if(m_strMode.compareTo("가로") == 0){
            nX = m_nYRES; nY = m_nXRES;
        }else {
            nX = m_nXRES;
            nY = m_nYRES;
        }

        return mMediaProjection.createVirtualDisplay("MainActivity",
                //DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                nX, nY, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface/*mMediaRecorder.getSurface()*/, null /*Callbacks*/, null /*Handler*/);
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
        mSurface = mMediaRecorder.getSurface();
    }

    private void initRecorder() {

        mMediaRecorder.reset();

        if(m_strAUD.compareTo("음소거") == 0) {
            //mute
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mMediaRecorder.setVideoEncodingBitRate(7776000);

        if(m_strAUD.compareTo("음소거") == 0) {
            //mute
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if(m_strMode.compareTo("가로")==0){
            mMediaRecorder.setVideoSize(m_nYRES, m_nXRES);
        }else{
            mMediaRecorder.setVideoSize(m_nXRES, m_nYRES);
        }
        mMediaRecorder.setVideoFrameRate(m_nFRA);

        //mMediaRecorder.setVideoSize(m_nXRES, m_nYRES);

        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));


        //설정저장.
        SharedPreferences prefs = getSharedPreferences("KNotiOption" ,MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("RES", mTvRes.getText().toString());
        editor.putString("FRE", mTvFra.getText().toString());
        editor.putString("AUD", mTvAud.getText().toString());
        editor.putString("MODE", mTvMode.getText().toString());
        editor.commit();
    }

    public void setOutFile(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        Date date = new Date();
        String today = df.format(date);

        //m_strOutPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/KRec/"+today+".mp4";
        m_strOutPath =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + today + ".mp4";
        mMediaRecorder.setOutputFile(m_strOutPath);
    }


}
