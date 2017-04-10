package com.tistory.fallingstar.knoti;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {



    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;


    private MainService mService;

    private int m_nXRES , m_nYRES, m_nFRA;
    private String m_strAUD, m_strMode;
    private String m_strOutPath;

    private Context mContext;

    //뒤로가기 두번에 종료.
    private BackPressCloseHandler backPressCloseHandler;

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

        mContext = this.getApplicationContext();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        filePermission();

        //설정 위젯들 초기화
        initOptions();

        //폴더 생성
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/KRec/");
        if (!f.exists()) {
            f.mkdir();
        }

        SharedPreferences prefs = getSharedPreferences("KNotiOption", MODE_PRIVATE);
        mTvRes.setText(prefs.getString( "RES",  "1280x720"));
        mTvFra.setText(prefs.getString("FRE", "15"));
        mTvAud.setText(prefs.getString("AUD", "MIC"));
        mTvMode.setText(prefs.getString("MODE", "가로"));

        String []t1 = mTvRes.getText().toString().split("x");
        m_nXRES  = Integer.parseInt(t1[1]);
        m_nYRES  = Integer.parseInt(t1[0]);

        m_nFRA = Integer.parseInt(mTvFra.getText().toString());

        m_strAUD = mTvAud.getText().toString();
        m_strMode = mTvMode.getText().toString();

        Intent Service = new Intent(this, MainService.class);

        bindService(Service, mConnection, Context.BIND_AUTO_CREATE);

        //화면꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        backPressCloseHandler = new BackPressCloseHandler(this);


    }

    //화면 회전 시 데이터 유실 방지.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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

        unbindService(mConnection);
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

    public void filePermission()
    {
        /* 사용자의 OS 버전이 마시멜로우 이상인지 체크한다. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /* 사용자 단말기의 권한 중 "전화걸기" 권한이 허용되어 있는지 체크한다.
                    *  int를 쓴 이유? 안드로이드는 C기반이기 때문에, Boolean 이 잘 안쓰인다.
                    */
            int permissionResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    /* 권한이 없을 때 */
            // 패키지는 안드로이드 어플리케이션의 아이디다.( 어플리케이션 구분자 )
            if (permissionResult == PackageManager.PERMISSION_DENIED) {


                        /* 사용자가 권한을 한번이라도 거부한 적이 있는 지 조사한다.
                        * 거부한 이력이 한번이라도 있다면, true를 리턴한다.
                        */
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한이 필요합니다.")
                            .setMessage("이 기능을 사용하기 위해서는 단말기의 \"파일 쓰기\" 권한이 필요합니다. 계속하시겠습니까?")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                                    }

                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                    finish();
                                    finishAffinity();
                                    System.exit(0);
                                }
                            })
                            .create()
                            .show();
                }

                //최초로 권한을 요청할 때
                else {
                    // CALL_PHONE 권한을 Android OS 에 요청한다.
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                }

            }


        }
                /* 사용자의 OS 버전이 마시멜로우 이하일 떄
        else {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:010-1111-2222"));
            startActivity(intent);
        }*/

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

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenDensity = metrics.densityDpi;

            //init mediaRecorder
            mService.initMediaRecorder(mContext, mScreenDensity);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private MainService.ICallback mCallback = new MainService.ICallback() {

        public void requestPermission(){
            startActivityForResult(mService.getMyMediaRecorder().getmProjectionManager().createScreenCaptureIntent(), PERMISSION_CODE);
        }

    };

    public void startServiceMethod(View v){

        moveTaskToBack(true);

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
            //권한 수락이 안된 경우.
            mService.confirmPermission(resultCode, data);
            return;
        }
        //권한 요청이 성공한 경우.
        moveTaskToBack(true);

        mService.confirmPermission(resultCode, data);

    }

    private void initRecorder() {

        //설정저장.
        SharedPreferences prefs = getSharedPreferences("KNotiOption" ,MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("RES", mTvRes.getText().toString());
        editor.putString("FRE", mTvFra.getText().toString());
        editor.putString("AUD", mTvAud.getText().toString());
        editor.putString("MODE", mTvMode.getText().toString());
        editor.commit();
    }


}
