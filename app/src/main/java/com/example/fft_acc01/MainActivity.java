/* CAN disp & FFT for SxS
* @author M.Masuda
* @version 1.0
*
*  history:
*  v1.0 10/5/2020: 1st edition
*
*  Structure of classes:
*  MainActivity.java : メインメニューのActivity
*  BindServiceSensorHandling.java : BindServiceを使ったUSB Serialの接続とデータ取得
*  MyApplication.java : BindServiceSensorHandlingクラスで取得したデータを格納.各アクティビティはここにアクセスしてデータを取得
*  RawAccValue.java : 加速度センサの生値を表示
*  AccFFT : 加速度センサのFFT結果を表示. 未実装
*  SDC_CANdisp : SDC関連CANデータの表示
*
*/

// TODO: 2020/05/13 Androidでの受信文字列が改行コードまで送られない場合を考慮しておいた方が良さそう(挙動が不安定)
// TODO: 2020/06/14 Android側からMCP2515のフィルタとマスク値を送るようにしてCAN IDの動的な変更に対応する
// TODO: 2020/05/10 加速度センサ生値のグラフの横軸が正確なサンプリング時間になっていない点の修正
// TODO: 2020/05/10 加速度センサ生値のグラフ,X/Y/Zの表示に対応
// TODO: 2020/05/10 Arduino側の加速度センサ値送信周期を5mse刻みに直す
// TODO: 2020/05/10 FFTの実装
// TODO: 2020/05/10 CAN表示、他のCAN信号をリストで表示(SDC Factorの画面からスワイプして表示がいいかも)

package com.example.fft_acc01;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "USBSerial";
    private BindServiceSensorHandling mSensorHandling;
    // BindService class of my making. Bindされた後に返されるクラス。中身にデータが格納される
    BindServiceSensorHandling.MyBinder binder;

    // センサ値やクラス間共通の値を保持しておくためのApplicationクラス
    private MyApplication myapp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_mainを画面に表示
        setContentView(R.layout.activity_main);

        // スクリーンONを維持. 各アクティビティ内で宣言が必要
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Applicationクラスの何か
        myapp = (MyApplication) this.getApplication();

        // Register click listener for button of RawAccValue
        findViewById(R.id.button_rawacc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ArduinoにACCデータ取得リクエスト(a)を送る
                binder.sendRequest(-1, 'a');

                final Intent intent = new Intent(MainActivity.this, RawAccValue.class);
                startActivity(intent);
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            }
        });

        /** Register click listener for button of RawAccValue */
        findViewById(R.id.button_fft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(MainActivity.this, AccFFT.class);
                startActivity(intent);
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            }
        });

        /** Register click listener for button of CAN */
        findViewById(R.id.button_SDCdisp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ArduinoにCANデータ取得リクエスト("c")を送る
                if(binder != null) {
                    boolean flg_SendRequest = binder.sendRequest(-1, 'c');
                }

                final Intent intent = new Intent(MainActivity.this, SDC_CANdisp.class);
                startActivity(intent);

                //無くても動く 何をしているのかは要チェック
                //ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            }
        });

        /** Register click listener for button of SuspStroke */
        findViewById(R.id.button_SuspStroke).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ArduinoにADCデータ取得リクエスト(a)を送る
                if(binder != null) {
                    binder.sendRequest(-1, 'a');
                }

                final Intent intent = new Intent(MainActivity.this, SuspStroke.class);
                startActivity(intent);
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            }
        });

        /** Register click listener for button of SDC control */
        findViewById(R.id.button_SDCcontrol).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Intent intent = new Intent(MainActivity.this, SDC_Control.class);
                startActivity(intent);
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            }
        });

        /** Register click listener for buttons to connect USB */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //サービスがすでに動いているかをチェック
                ActivityManager manage = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for(ActivityManager.RunningServiceInfo serviceInfo : manage.getRunningServices(Integer.MAX_VALUE))
                    if(BindServiceSensorHandling.class.getName().equals(serviceInfo.service.getClassName())){
                        // 動いている場合は一回unBindする
                        unbindService(conn);
                    }
                // Bind BindService
                // Intent for MainActivity with Bind service
                final Intent intent = new Intent(MainActivity.this, BindServiceSensorHandling.class);
                bindService(intent, conn, Service.BIND_AUTO_CREATE);


            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // スクリーンONを維持
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStop(){
        super.onStop();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    // ServiceConnection object class
    private ServiceConnection conn = new ServiceConnection() {
        // callback for connection successful
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            System.out.println("Service connected");
            // onBindされたときにbinderが返される。このbinderにサービス側で処理やデータを格納しておいて使う側が呼び出す
            binder = (BindServiceSensorHandling.MyBinder) iBinder;

            // Appicationクラスにbinderのインスタンスを登録
            myapp.setBinder(binder);

        }
        // callback for connection unsuccessful
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            System.out.println("Service disconnected");
        }
    };
}




