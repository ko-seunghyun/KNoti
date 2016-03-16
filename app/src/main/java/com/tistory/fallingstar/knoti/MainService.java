package com.tistory.fallingstar.knoti;

import android.accessibilityservice.AccessibilityService;
import android.app.ActionBar;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainService extends Service {

    private static final String TAG = "MainService";

    private MyMediaRecorder myMediaRecorder;

    private View mView;
    private WindowManager mManager;
    private ToggleButton mToggleButton;
    private WindowManager.LayoutParams mParams;

    //알람바 관련 것들.
    private static final String RECORD_START_ACTION = "com.example.packagename.START";
    private static final String RECORD_END_ACTION = "com.example.packagename.END";
    private static final String RECORD_EXIT_ACTION = "com.example.packagename.EXIT";
    private final int NOTIFICATION_ID = 1;

    private RemoteViews contentiew;
    private Notification noti;
    private NotificationManager nm;
    private Notification.Builder builder;

    private boolean m_bRecordFlag = false;

    public MainService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initNotification();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_START_ACTION);
        intentFilter.addAction(RECORD_END_ACTION);
        registerReceiver(buttonBroadcastReceiver, intentFilter);

    }

    public void setToggleBtn(boolean b){
        mToggleButton.setChecked(b);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.stopForeground(true);

        unregisterReceiver(buttonBroadcastReceiver);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initNotification();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_START_ACTION);
        intentFilter.addAction(RECORD_END_ACTION);
        registerReceiver(buttonBroadcastReceiver, intentFilter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }

    //local binder create
    public class MainServiceBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    public interface ICallback {
        public void requestPermission();
    }

    private ICallback mCallback;

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    private final IBinder mBinder = new MainServiceBinder();

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
        //nm.notify(NOTIFICATION_ID, noti);
        this.startForeground(NOTIFICATION_ID, noti);
    }

    BroadcastReceiver buttonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if(RECORD_START_ACTION.equals(action)) {

                if(!m_bRecordFlag){

                    m_bRecordFlag = true;

                    myMediaRecorder.setServiceOnFlag(true);

                    statusBarUp();

                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    Toast.makeText(context, "녹화시작", Toast.LENGTH_SHORT).show();

                    myMediaRecorder.initRecorder();
                    myMediaRecorder.setOutFile();
                    myMediaRecorder.prepareRecorder();

                    if (myMediaRecorder.getMediaProjection() == null) {
                        //권한이 필요함.
                        mCallback.requestPermission();
                    }
                    else {
                        myMediaRecorder.setVirtualDisplay();
                        myMediaRecorder.startRecord();
                    }
                }
;
                contentiew.setTextViewText(R.id.tv_status, "녹화중");
                builder.setContent(contentiew);
                nm.notify(NOTIFICATION_ID, builder.build());


            } else if(RECORD_END_ACTION.equals(action)) {

                if(m_bRecordFlag){

                    m_bRecordFlag = false;

                    myMediaRecorder.setServiceOnFlag(false);

                    statusBarUp();

                    myMediaRecorder.stopRecord();
                    Log.v(TAG, "Recording Stopped");
                    myMediaRecorder.stopScreenSharing();
                    Toast.makeText(context, "녹화종료.", Toast.LENGTH_SHORT).show();
                    myMediaRecorder.scanMedia();
                }

                contentiew.setTextViewText(R.id.tv_status, "녹화종료");
                builder.setContent(contentiew);
                //noti =  builder.build();
                //noti.flags  |= Notification.FLAG_AUTO_CANCEL;
                nm.notify(NOTIFICATION_ID, builder.build());


            } else {
                //?????
            }
        }
    };

    public void statusBarUp(){

        Intent i = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        getApplicationContext().sendBroadcast(i);

    }

    public void initMediaRecorder(Context c, int density){

        //mediarecorder setting
        myMediaRecorder = new MyMediaRecorder(c);

        myMediaRecorder.setScreenDensity(density);

    }

    public MyMediaRecorder getMyMediaRecorder(){
        return myMediaRecorder;
    }

    public void confirmPermission(int resultCode, Intent data){

        myMediaRecorder.createMediaProjection(resultCode, data);

        myMediaRecorder.setVirtualDisplay();

        myMediaRecorder.startRecord();

    }

}
