/* BindServiceSensorHandling
* @author M.Masuda
* @version 1.0
*
* History
* v1.0 10/5/2020: 1st edition
*/

package com.example.fft_acc01;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.BuildConfig;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class BindServiceSensorHandling extends Service implements SerialInputOutputManager.Listener{

    private TextView mTextView;

    // debug
    private static final String TAG = "USBSerial";

    // Serialデータ受信関連
    private long timeSerialRecv_prev;

    // Sensor data
    public HashMap<String, Double> receivedata;
    private MyApplication myapp;
    private StringBuilder buf_rawdata;

    // Sensor 将来的にはこの設定部分だけを別ファイルに切り出す
    private float samplingtime_ms;
    private double acc_x_ms2;
    private double acc_y_ms2;
    private double acc_z_ms2;
    private double engine_rpm;
    private double throttle_pos_norm;
    private double veh_speed_ms;
    private double water_temp;
    private double cvt_temp;
    private double gear_pos;
    private double imu_yawrate_degs;
    private double imu_rollrate_degs;
    private double imu_pitchrate_degs;
    private double imu_acc_x_ms2;
    private double imu_acc_y_ms2;
    private double imu_acc_z_ms2;
    private double str_angle_deg;
    private double sdc_factor_fl;
    private double sdc_factor_fr;
    private double sdc_factor_rl;
    private double sdc_factor_rr;
    private double sdc_mode;
    private double sdc_lamp;
    // CAN ID number
    private int SAMPLINGTIME = 0;
    private int ADC1_1 = 0;
    private int ADC1_2 = 1;
    private int ADC1_3 = 2;
    private int ADC2_1 = 3;
    private int ADC2_2 = 4;
    private int ADC2_3 = 5;
    private int ID_ENGINE = 0x100;
    private int ID_VEH_SPEED = 0x113;
    private int ID_TEMP = 0x120;
    private int ID_GEAR_POS = 0x121;
    private int ID_IMU_INFO1 = 0x174;
    private int ID_IMU_INFO2 = 0x178;
    private int ID_IMU_INFO3 = 0x17C;
    private int ID_EPS = 0x209;
    private int ID_LWS_Standard = 0x2B0;
    private int ID_SDC_INFO = 0x218;
    private int ID_ESP_DATA = 0x250;
    private int ID_SDC_LAMP = 0x401;
    // Coefficients [Factor, offset]
    private double BITLEN_ADC_G = 1024;
    private double REFV_ADC_G = 5;
    private double RES_ADC_X_G = 1.996;
    private double RES_ADC_Y_G = 2.024;
    private double RES_ADC_Z_G = 1.988;
    private double OFF_ADC_X_G = -4.697;
    private double OFF_ADC_Y_G = -4.789;
    private double OFF_ADC_Z_G = -4.722;
    private double[] COE_ENGINE_RPM = {1, 0};
    private double[] COE_THROTTEL_POS = {1, 0};
    private double[] COE_VEHSPEED = {400.0/32768, 0};
    private double[] COE_VEHICLESPEED = {0.05625, 0};
    private double[] COE_WATER_TEMP = {0.5, 0};
    private double[] COE_CVT_TEMP = {1, 0};
    private double[] COE_GEAR_POS = {1, 0};
    private double[] COE_IMU_YAWRATE = {0.005, 0};
    private double[] COE_IMU_ROLLRATE = {0.005, 0};
    private double[] COE_IMU_PITCHRATE = {0.005, 0};
    private double[] COE_IMU_ACC_X = {0.00125, -40.957};
    private double[] COE_IMU_ACC_Y = {0.00125, -40.957};
    private double[] COE_IMU_ACC_Z = {0.00125, -40.957};
    private double[] COE_STRANGLE = {0.1, 0};
    private double[] COE_LWS_ANGLE = {0.1, 0};
    private double[] COE_SDC_FACTOR_FL = {0.5, 0};
    private double[] COE_SDC_FACTOR_FR = {0.5, 0};
    private double[] COE_SDC_FACTOR_RL = {0.5, 0};
    private double[] COE_SDC_FACTOR_RR = {0.5, 0};
    private double[] COE_SDC_MODE = {1, 0};
    private double[] COE_SDC_LAMP = {1, 0};

    // ストロークセンサのADC値をストロークに変換するmap
    private int[] X_MAP_STROKESENSOR_FR = {0, 250, 500, 750, 1000};
    private int[] Y_MAP_STROKESENSOR_FR = {1, 1, 1, 1};
    private int[] X_MAP_STROKESENSOR_RR = {0, 250, 500, 750, 1000};
    private int[] Y_MAP_STROKESENSOR_RR = {1, 1, 1, 1};
    
    // USB関連
    public SerialInputOutputManager usbIoManager;
    private UsbDeviceConnection usbConnection;
    private UsbSerialDriver usbdriver;
    public UsbSerialPort usbSerialPort;
    private enum UsbPermission {Unknown, Requested, Granted, Denied}
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final int WRITE_WAIT_MILLIS = 2000;

    private boolean quit;

    // onBind()で返されるBinder
    private MyBinder binder;

    /*
    最初のbindService呼び出しのみ、システムにIBinderインターフェースを渡すために呼ばれる。
     */
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("Service is binded");
        binder = new MyBinder();
        return binder;
    }

    /*
     Serviceのインスタンスがない状態で、クライアントがstartServiceまたはbindServiceを呼んだ時に
     *Serviceのインスタンス生成で呼ばれる。すでにインスタンスが存在している場合は呼ばれない。
     */
    @Override
    public void onCreate() {
        System.out.println("Service is created");
        // コンストラクタのような感じで各変数を初期化
        receivedata = new HashMap<String, Double>();
        myapp = (MyApplication) this.getApplication();
        buf_rawdata = new StringBuilder();

        while(true){
            if(connect()){
                // USBのConnectionが成功した場合にToastを表示
                Context context = getApplicationContext();
                Toast.makeText(context, "USB connection succeeded", Toast.LENGTH_LONG).show();
                break;
            }
            else{
                // USBのConnectionが上手く行かないときのエラーを実装
                Context context = getApplicationContext();
                Toast.makeText(context, "USB connection error", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    /*
     onBind()で返されるbindeオブジェクト. ここに処理やデータを記述する。
     */
    public class MyBinder extends Binder {
        // 第一引数は数値(文字) Byteコードではないので注意

        boolean sendRequest(int req){
            // 送信されるのは文字列なのでここで変換
            String buf = String.valueOf(req);
            try {
                usbSerialPort.write(buf.getBytes(), WRITE_WAIT_MILLIS);
                return true;
            }catch (IOException e){
                Log.e(TAG, "Display mode request error");
                return false;
            }
        }
        // 第2引数を使う場合は文字、第1引数は無視される
        boolean sendRequest(int req, char req_str){
            // charが渡されてくるのでStringにコピー
            String buf = String.valueOf(req_str);
            try {
                usbSerialPort.write(buf.getBytes(), WRITE_WAIT_MILLIS);
                return true;
            }catch (IOException e){
                Log.e(TAG, "Display mode request error");
                return false;
            }
        }
    }

    @Override
    public void onRunError(Exception e) {
        Log.d(TAG, "Runner stopped");
    }

    /*バインドしているクライアントが「全て」いなくなったときに呼ばれる。そのためunbindServiceが呼ばれても、
     *ほかにバインドしているクライアントが存在した場合、onUnbindは呼ばれない。
     */
    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        System.out.println("System is unbined");
        return true;
    }

    //バインドされたクライアントがなくなって、onUnbindが呼ばれたあとに呼ばれる
    @Override
    public void onDestroy() {
        System.out.println("Service is destroyed");
        this.quit = true;
    }

    // データを受信したときにイベントが発生する
    // ここでは受信したデータをバッファに入れ、メッセージ(CAN ID)単位にして次の処理クラスへ渡す
    // バッファが無くなるまでメッセージ単位でデータを切り出す
    @Override
    public void onNewData(byte[] bytes) {

        // Serialデータを受信したら前回受信時の時間との差を取ってその時間をアプリケーションクラスへ登録
        long timeSerialRecv = System.currentTimeMillis();
        long durationSerialComm = timeSerialRecv - timeSerialRecv_prev;
        myapp.setObj("SerialCommUpdateTime", (double)timeSerialRecv);
        myapp.setObj("SerialCommUpdateCycle", (double)durationSerialComm);
        timeSerialRecv_prev = timeSerialRecv;
        Log.i(TAG, "Serial update cycle: " + String.format("%03d", durationSerialComm));

        // 受信データ[byte]を文字列にする
        for (int i = 0; i < bytes.length; i++) {
            /* Data format
            "\t ID1_Data1-1_Data1-2... \t "ID2"_"Data2-1"... \t "ID3"... \r\n
            */
            buf_rawdata.append(new String(new byte[]{bytes[i]}));
        }

        // For debug
        Log.i(TAG, "received data length: " + String.valueOf(bytes.length));
        Log.d(TAG, "DataFromArduino: " + buf_rawdata);

        // rawdata内の改行(\r\n)の位置を検索, 改行コードが無くなるまで削除する
        while (true) {
            int ind_lf = buf_rawdata.indexOf("\r\n");
            if (ind_lf != -1) {
                // 改行コード(\r\n)がある場合は削除 改行コードはシリアルモニタ上でのデバッグ時に見やすくするためで、データ処理には不要
                buf_rawdata = buf_rawdata.delete(ind_lf, ind_lf + 2);
            } else {
                break;
            }
        }
        // rawdata内のタブ(\t)の位置を検索
        while (true) {

            int ind_tab = buf_rawdata.indexOf("\t");
            /** タブの位置が先頭にある場合 **/
            if (ind_tab == 0) {
                // タブの位置が先頭にあればそこからMessageが格納されているはず
                // 先頭のタブを除いて再度タブの位置を確認
                int ind_tab2 = buf_rawdata.indexOf("\t", 1);
                if (ind_tab2 == -1) {
                    // 末尾に\tがない場合はMessageの後半がバッファにない場合。データが追加されるのを待つ
                    break;
                } else {
                    // Messageの抜き出し(末尾の\tは含まない)
                    String indata = buf_rawdata.substring(1, ind_tab2);
                    // データ判別ルーチンへ渡す
                    data_sourter(indata);
                    // 送信した文字列は変数rawdataから削除(末尾の\tは削除しない)
                    buf_rawdata = buf_rawdata.delete(0, ind_tab2);
                }
            }
            /** 先頭に\tが無くて途中に\tがある場合 **/
            else if (ind_tab != -1) {
                // 先頭に\tが無くて途中に\tがある場合はMessageの前半が欠損している可能性あり。破棄する。\tは削除しない
                buf_rawdata = buf_rawdata.delete(0, ind_tab);
                break;
            }
            /** \tが1つもない場合　**/
            else {
                // なにもしない。データが追加されるのを待つ
                break;
            }
        }
    }

    /*
     USB connectiong
     */
    private boolean connect() {
        // Find all available drivers from attached devices.
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            Log.e(TAG, "There is no available USB devices");
            return false;
        }

        // Open a connection to the first available driver.
        usbdriver = availableDrivers.get(0);
        if (usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(usbdriver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(BindServiceSensorHandling.this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(usbdriver.getDevice(), usbPermissionIntent);
            // USB Permissionを確認した時はUSB Connectionをやり直すためにfalseを返す
            return false;
        }
        usbConnection = usbManager.openDevice(usbdriver.getDevice());

        if (usbConnection == null) {
            if (!usbManager.hasPermission(usbdriver.getDevice()))
                Log.e(TAG, "connection failed: permission denied");
            else
                Log.e(TAG, "connection failed: open failed");
            return false;
        }

        // Open USB port
        usbSerialPort = usbdriver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbSerialPort.setDTR(true);
            // for debug
            byte[] buffer = new byte[8];
            int len = usbSerialPort.read(buffer, READ_WAIT_MILLIS);
            System.out.println("Acc data length at 1st time :" + String.valueOf(len));
            System.out.println("Acc data at 1st time :" + new String(buffer));
            System.out.println("USB port has opened");
        } catch (IOException e) {
            try {
                usbSerialPort.close();
            }catch(IOException e2){
                // Nothing
            }
            Log.e(TAG, "Establishment of USB port was failued");
            e.printStackTrace();
        }

        // Listenerクラスをセット.非同期処理を開始
        usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
        Executors.newSingleThreadExecutor().submit(usbIoManager);

        return true;
    }

    /*
    * USB disconnection
     */
    private void disconnect(){
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
                usbSerialPort = null;
                Log.i(TAG, "USB serial port was closed");
            } catch (IOException e) {
                Log.e(TAG, "USB serial port was not closed");
            }
        }
        if(usbIoManager != null)
            usbIoManager.stop();
        usbIoManager = null;
        Log.i(TAG, "USB IO manager was stopped");
    }

    /*
     受信MessageをIDごとに処理. ここで各バイトの結合と分解能/オフセットから物理値変換まで行う.(将来的にはハードコーディングやめたい)
     */
    private void data_sourter(String msg) {

        // この関数が動作するということはCANを受信出来ているということになる
        // この関数が呼ばれる度にその時間をAppl.クラスに保存する。表示クラスで一定時間以上CANが受信出来ていない場合のハンドリングを行う
        // Serialデータを受信したら前回受信時の時間との差を取ってその時間をアプリケーションクラスへ登録
        myapp.setObj("CANCommUpdateTime", (double)System.currentTimeMillis());

        /* Data format
        "ID1"_"Data1-1"_"Data1-2" ....
        msg_arr = ["ID1"_"Data1-1"_"Data1-2", "ID2"_"Data2-1"];
         */

        // 改行コードでデータを分割する
        String[] msg_arr = msg.split(" ");

        //for (int j = 0; j < msg_arr.length; j++) {

            Log.i(TAG,"ID: " + msg_arr);

            try {
                if (msg_arr[0].equals(String.format("%x", SAMPLINGTIME))) {
                    // 単位をmicro sec から milli secに変えて小数点第1で四捨五入
                    samplingtime_ms = Math.round(Float.parseFloat(msg_arr[1]) / 1000);
                }
                else if (msg_arr[0].equals(String.format("%x", ADC1_1))) {
                    /* 加速度センサの場合 */
                    // m/s^2に変換
                    acc_x_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_X_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_X_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_X", acc_x_ms2);
                }
                else if (msg_arr[0].equals(String.format("%x", ADC1_2))) {
                    // m/s^2に変換
                    acc_y_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_Y_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_Y_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_Y", acc_y_ms2);
                }
                else if (msg_arr[0].equals(String.format("%x", ADC1_3))) {
                    /* 加速度センサの場合 */
                    // m/s^2に変換
                    acc_z_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_Z_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_Z_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_Z", acc_z_ms2);

                    /* ストロークセンサの場合 */
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("SUSPSTROKE_FR", (double)Integer.parseInt(msg_arr[1]));
                }
                else if (msg_arr[0].equals(String.format("%x", ADC2_1))) {
                    /* 加速度センサの場合 */
                    // m/s^2に変換
                    acc_x_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_X_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_X_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_X", acc_x_ms2);
                }
                else if (msg_arr[0].equals(String.format("%x", ADC2_2))) {
                    // m/s^2に変換
                    acc_y_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_Y_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_Y_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_Y", acc_y_ms2);
                }
                else if (msg_arr[0].equals(String.format("%x", ADC2_3))) {
                    /* 加速度センサの場合 */
                    // m/s^2に変換
                    acc_z_ms2 = (Integer.parseInt(msg_arr[1]) * RES_ADC_Z_G * REFV_ADC_G / BITLEN_ADC_G + OFF_ADC_Z_G) * 9.81;
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ACC_Z", acc_z_ms2);

                    /* ストロークセンサの場合 */
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("SUSPSTROKE_RR", (double)Integer.parseInt(msg_arr[1]));
                }
                // ENGINE
                else if (msg_arr[0].equals(String.format("%x", ID_ENGINE))) {
                    engine_rpm =
                            ((Integer.parseInt(msg_arr[1], 16) << 8) + Integer.parseInt(msg_arr[2], 16))
                                    * COE_ENGINE_RPM[0] + COE_ENGINE_RPM[1];
                    throttle_pos_norm =
                            ((Integer.parseInt(msg_arr[3], 16) << 8) + (Integer.parseInt(msg_arr[4])))
                                    * COE_THROTTEL_POS[0] + COE_THROTTEL_POS[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("ENGINE_RPM", engine_rpm);
                    myapp.setObj("THROTTLE_POS", throttle_pos_norm);
                }
                // VEH_SPEED
                else if (msg_arr[0].equals(String.format("%x", ID_VEH_SPEED))) {
                    veh_speed_ms =
                            ((Integer.parseInt(msg_arr[1], 16) << 8) + Integer.parseInt(msg_arr[2], 16))
                                    * COE_VEHSPEED[0] + COE_VEHSPEED[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("VEH_SPEED", veh_speed_ms);
                }
                // ESP, INTEL
                else if (msg_arr[0].equals(String.format("%x", ID_ESP_DATA))) {
                    veh_speed_ms =
                            ((Integer.parseInt(msg_arr[2], 16) << 8) + Integer.parseInt(msg_arr[1], 16))
                                    * COE_VEHICLESPEED[0] + COE_VEHICLESPEED[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("VEH_SPEED", veh_speed_ms);
                }
                // TEMP
                else if (msg_arr[0].equals(String.format("%x", ID_TEMP))) {
                    water_temp = ((Integer.parseInt(msg_arr[1], 16) << 8) + (Integer.parseInt(msg_arr[2], 16))) * COE_WATER_TEMP[0] + COE_WATER_TEMP[1];
                    cvt_temp = Integer.parseInt(msg_arr[5], 16) * COE_CVT_TEMP[0] + COE_CVT_TEMP[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("WATER_TEMP", water_temp);
                    myapp.setObj("CVT_TEMP", cvt_temp);
                }
                // GEAR POS
                else if (msg_arr[0].equals(String.format("%x", ID_GEAR_POS))) {
                    gear_pos = Integer.parseInt(msg_arr[1], 16 ) * COE_GEAR_POS[0] + COE_GEAR_POS[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("GEAR_POS", gear_pos);
                }
                // IMU INFO1
                else if (msg_arr[0].equals(String.format("%x", ID_IMU_INFO1))) {
                    imu_yawrate_degs = ((Integer.parseInt(msg_arr[1], 16) << 8) + Integer.parseInt(msg_arr[2], 16)) * COE_IMU_YAWRATE[0] + COE_IMU_YAWRATE[1];
                    imu_acc_y_ms2 = ((Integer.parseInt(msg_arr[5], 16) << 8) + Integer.parseInt(msg_arr[6], 16)) * COE_IMU_ACC_Y[0] + COE_IMU_ACC_Y[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("IMU_YAWRATE", imu_yawrate_degs);
                    myapp.setObj("IMU_ACC_Y", imu_acc_y_ms2);
                }
                // IMU INFO2
                else if (msg_arr[0].equals(String.format("%x", ID_IMU_INFO2))) {
                    imu_rollrate_degs = ((Integer.parseInt(msg_arr[1], 16) << 8) + Integer.parseInt(msg_arr[2], 16)) * COE_IMU_ROLLRATE[0] + COE_IMU_ROLLRATE[1];
                    imu_acc_x_ms2 = ((Integer.parseInt(msg_arr[5], 16) << 8) + (Integer.parseInt(msg_arr[6], 16))) * COE_IMU_ACC_X[0] + COE_IMU_ACC_X[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("IMU_ROLLRATE", imu_rollrate_degs);
                    myapp.setObj("IMU_ACC_X", imu_acc_x_ms2);
                }
                // IMU_INFO3
                else if (msg_arr[0].equals(String.format("%x", ID_IMU_INFO3))) {
                    imu_pitchrate_degs = ((Integer.parseInt(msg_arr[1], 16) << 8) + Integer.parseInt(msg_arr[2], 16)) * COE_IMU_PITCHRATE[0] + COE_IMU_PITCHRATE[1];
                    imu_acc_z_ms2 = ((Integer.parseInt(msg_arr[5], 16) << 8) + Integer.parseInt(msg_arr[6], 16)) * COE_IMU_ACC_Z[0] + COE_IMU_ACC_Z[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("IMU_PITCHRATE", imu_pitchrate_degs);
                    myapp.setObj("IMU_ACC_Z", imu_acc_z_ms2);
                }
                // EPS
                else if (msg_arr[0].equals(String.format("%x", ID_EPS))) {
                    str_angle_deg = ((Integer.parseInt(msg_arr[2], 16) << 8) + Integer.parseInt(msg_arr[3], 16)) * COE_STRANGLE[0] + COE_STRANGLE[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("STR_ANGLE", str_angle_deg);
                }
                // SDC INFO
                else if (msg_arr[0].equals(String.format("%x", ID_SDC_INFO))) {
                    sdc_factor_fl = Integer.parseInt(msg_arr[1], 16) * COE_SDC_FACTOR_FL[0] + COE_SDC_FACTOR_FL[1];
                    sdc_factor_fr = Integer.parseInt(msg_arr[2], 16) * COE_SDC_FACTOR_FR[0] + COE_SDC_FACTOR_FR[1];
                    sdc_factor_rl = Integer.parseInt(msg_arr[3], 16) * COE_SDC_FACTOR_RL[0] + COE_SDC_FACTOR_RL[1];
                    sdc_factor_rr = Integer.parseInt(msg_arr[4], 16) * COE_SDC_FACTOR_RR[0] + COE_SDC_FACTOR_RR[1];
                    sdc_mode = Integer.parseInt(msg_arr[5], 16) * COE_SDC_MODE[0] + COE_SDC_MODE[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("SDC_FACTOR_FL", sdc_factor_fl);
                    myapp.setObj("SDC_FACTOR_FR", sdc_factor_fr);
                    myapp.setObj("SDC_FACTOR_RL", sdc_factor_rl);
                    myapp.setObj("SDC_FACTOR_RR", sdc_factor_rr);
                    myapp.setObj("SDC_MODE", sdc_mode);
                }
                // SDC LAMP
                else if (msg_arr[0].equals(String.format("%x", ID_SDC_LAMP))) {
                    sdc_lamp = Integer.parseInt(msg_arr[1], 16) * COE_SDC_LAMP[0] + COE_SDC_LAMP[1];
                    // Applクラスへ登録(HashMap)
                    myapp.setObj("SDC_LAMP", sdc_lamp);
                }
            }
            catch(Exception e){
                Log.i(TAG, "Array is empty");
            }
        //}
    }

    /*
    角度センサの角度をストロークに変換する関数
     */
    private float angle2stroke(int adc, int[] map_x, float[] map_y){
        // map_xとmap_yの配列の長さをチェック、同じでない場合はエラーを送出
        if(map_x.length != map_y.length){
            try {
                throw new Exception("Array length of Map_x and Map_y are not corresponded");
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        float y = 0f;
        for(int idx=1; idx<=map_x.length-1; idx++) {
            if (adc < map_x[idx]) {
                float a = (map_y[idx] - map_y[idx - 1]) / (map_x[idx] - map_x[idx - 1]);
                y = a * adc + (map_y[idx - 1] - a * map_x[idx - 1]);
            } else {
                float a = (map_y[map_y.length - 1] - map_y[map_y.length - 2]) / (map_x[map_x.length - 1] - map_x[map_x.length - 2]);
                y = a * adc + (map_y[map_y.length - 2] - a * map_x[map_x.length - 2]);
            }
        }
        return(y);
    }
}

