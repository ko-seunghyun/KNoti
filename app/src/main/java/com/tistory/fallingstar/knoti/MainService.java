package com.tistory.fallingstar.knoti;

import android.accessibilityservice.AccessibilityService;
import android.app.ActionBar;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainService extends Service {

    private View mView;
    private WindowManager mManager;
    private ToggleButton mToggleButton;
    private WindowManager.LayoutParams mParams;
    public MainService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.service_ui, null);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,

                //WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                //WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        mParams.alpha = 0.5f;
        //단말 모서리 위치 확인
        //DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        //mParams.x = dm.widthPixels;
        //mParams.y = dm.heightPixels;

        mParams.gravity = Gravity.LEFT | Gravity.BOTTOM;

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        mToggleButton = (ToggleButton) mView.findViewById(R.id.btn_onOff);

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_onOff:
                        if (mCallback == null)
                            return;

                        if (((ToggleButton) v).isChecked()) {
                            mCallback.sendData(true);
                        } else {
                            mCallback.sendData(false);
                        }
                        break;
                }
            }
        });
    }

    public void setToggleBtn(boolean b){
        mToggleButton.setChecked(b);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mManager.removeView(mView);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        public void sendData(boolean f);
    }

    private ICallback mCallback;

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    private final IBinder mBinder = new MainServiceBinder();

}
