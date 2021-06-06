/* SDC_CANdisp
 * @author M.Masuda
 * @version 1.0
 *
 * History
 * v1.0 10/5/2020: 1st edition
 */

package com.example.fft_acc01;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLDouble;

public class SDC_CANdisp extends AppCompatActivity {

    private double SERIALCOMMCYCLE_THOLD = 200; //ms
    private double CANCOMMCYCLE_THOLD = 500; //ms
    private boolean flg_SerialCommStatus = false;
    private boolean flg_CANCommStatus = false;

    private ImageView imageView_Serialcomm;
    private ImageView imageView_CANcomm;
    private TextView textView_SerialCommText;
    private TextView textView_CANCommText;
    private TextView textView_SDC_Mode;
    private TextView textView_SDC_Lamp;
    private TextView textView_SDCfactor_fl;
    private TextView textView_SDCfactor_fr;
    private TextView textView_SDCfactor_rl;
    private TextView textView_SDCfactor_rr;
    private TextView textView_EswayAct;
    private TextView textView_VehSpeed;
    /*
    private PieChart pieChart_SDCFactor_fl;
    private PieChart pieChart_SDCFactor_fr;
    private PieChart pieChart_SDCFactor_rl;
    private PieChart pieChart_SDCFactor_rr;
     */
    private ProgressBar pb_sdcfactor_fl;
    private ProgressBar pb_sdcfactor_fr;
    private ProgressBar pb_sdcfactor_rl;
    private ProgressBar pb_sdcfactor_rr;

    private HashMap<String, Double> latestdata;
    private MyApplication myapp;

    private String enum_sdcmode;
    private String enum_sdclamp;
    private String enum_eswayact;

    private String Spinner_SDCItem = "SDC: 1.Auto";
    private String Spinner_EswayItem = "E-Sway: 1.Auto";

    private BindServiceSensorHandling.MyBinder binder;

    Timer timer = new Timer();
    TimerTask updateAccValue;

    private Handler handler = new Handler();

    private class UpdateAccValue extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {

                    // SerialCommStatusのハンドリング
                    // SerialCommCycle もし200ms以下でシリアル通信されている様だったらインジケータをグリーンにする
                    if(System.currentTimeMillis() - (long)myapp.getObj("SerialCommUpdateTime") < SERIALCOMMCYCLE_THOLD){
                        if(flg_SerialCommStatus == false) {
                            // グレイのインジケータが表示されている場合のみグリーンのインジケータに入れ替える(処理時間を考慮)
                            imageView_Serialcomm.setImageResource(R.drawable.image001_green);
                            flg_SerialCommStatus = true;
                        }
                        textView_SerialCommText.setText(String.format("%s%3d%s", "SerialComm [", (int)myapp.getObj("SerialCommUpdateCycle"), "ms]"));
                    }else{
                        if(flg_SerialCommStatus == true) {
                            // グレイのインジケータが表示されている場合のみグリーンのインジケータに入れ替える(処理時間を考慮)
                            imageView_Serialcomm.setImageResource(R.drawable.image002_grey);
                            flg_SerialCommStatus = false;
                        }
                        textView_SerialCommText.setText("SerialComm [invalid]");
                    }

                    // CANCommStatusのハンドリング
                    if(System.currentTimeMillis() - (long)myapp.getObj("CANCommUpdateTime") <  CANCOMMCYCLE_THOLD){
                        if(flg_CANCommStatus == false) {
                            // グレイのインジケータが表示されている場合のみグリーンのインジケータに入れ替える(処理時間を考慮)
                            imageView_CANcomm.setImageResource(R.drawable.image001_green);
                            flg_CANCommStatus = true;
                        }
                    }else{
                        if(flg_CANCommStatus == true) {
                            // グレイのインジケータが表示されている場合のみグリーンのインジケータに入れ替える(処理時間を考慮)
                            imageView_CANcomm.setImageResource(R.drawable.image002_grey);
                            flg_CANCommStatus = false;
                        }
                    }

                    // SDC Lampを値からメッセージに変換
                    if(myapp.getObj("SDC_LAMP") == 0){
                        enum_sdclamp = "SDC_ON";
                        textView_SDC_Lamp.setText(enum_sdclamp);
                        textView_SDC_Lamp.setTextColor(Color.GREEN);
                        textView_SDC_Lamp.setBackgroundColor(Color.parseColor("#2e2e2e"));
                        textView_SDC_Lamp.clearAnimation();
                    }else if (myapp.getObj("SDC_LAMP") == 1){
                        enum_sdclamp = "PASSIVE";
                        textView_SDC_Lamp.setText(enum_sdclamp);
                        textView_SDC_Lamp.setTextColor(Color.BLACK);
                        textView_SDC_Lamp.setBackgroundColor(Color.YELLOW);
                        // テキストの点滅がうまくいかない
                        //blinkText(textView_SDC_Lamp, 10000, 1000);
                    }else if(myapp.getObj("SDC_LAMP") == 2){
                        enum_sdclamp = "SDC_OFF";
                        textView_SDC_Lamp.setText(enum_sdclamp);
                        textView_SDC_Lamp.setTextColor(Color.GRAY);
                        textView_SDC_Lamp.setBackgroundColor(Color.parseColor("#2e2e2e"));
                        textView_SDC_Lamp.clearAnimation();
                    }else if(myapp.getObj("SDC_LAMP") == 3){
                        enum_sdclamp = "STANBY";
                        textView_SDC_Lamp.setText(enum_sdclamp);
                        textView_SDC_Lamp.setTextColor(Color.BLACK);
                        textView_SDC_Lamp.setBackgroundColor(Color.YELLOW);
                        // テキストの点滅がうまくいかない
                        //blinkText(textView_SDC_Lamp, 10000, 1000);
                    }

                    // SDC Modeを値からメッセージに変換
                    if(myapp.getObj("SDC_MODE") == 2){
                        enum_sdcmode = "SDC:Auto\nE-Sway:Auto";
                        textView_SDC_Mode.setTextColor(Color.WHITE);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }else if (myapp.getObj("SDC_MODE") == 3){
                        enum_sdcmode = "SDC:Full soft\nE-Sway:Full soft";
                        textView_SDC_Mode.setTextColor(Color.WHITE);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }else if(myapp.getObj("SDC_MODE") == 4){
                        enum_sdcmode = "SDC:Full soft\nE-Sway:Full firm";
                        textView_SDC_Mode.setTextColor(Color.WHITE);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }else if(myapp.getObj("SDC_MODE") == 5){
                        enum_sdcmode = "SDC:Full firm\nE-Sway:Auto";
                        textView_SDC_Mode.setTextColor(Color.WHITE);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }else if(myapp.getObj("SDC_MODE") == 6){
                        enum_sdcmode = "SDC:Full firm\nE-Sway:Full soft";
                        textView_SDC_Mode.setTextColor(Color.WHITE);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }else if(myapp.getObj("SDC_MODE") == 10){
                        enum_sdcmode = "INIT/FAIL";
                        textView_SDC_Mode.setTextColor(Color.YELLOW);
                        textView_SDC_Mode.setBackgroundColor(Color.parseColor("#2e2e2e"));
                    }
                    textView_SDC_Mode.setText(enum_sdcmode);

                    // E-Sway activation ---------------------------
                    if(myapp.getObj("SDC_MODE") == 0){
                        enum_eswayact = "E-Sway firm";
                        textView_EswayAct.setTextColor(Color.WHITE);
                        textView_EswayAct.setBackgroundColor(Color.parseColor("#2e2e2e"));

                    }else if (myapp.getObj("SDC_MODE") == 1){
                        enum_eswayact = "E-Sway soft";
                        textView_EswayAct.setBackgroundColor(Color.RED);
                    }
                    textView_EswayAct.setText(enum_eswayact);

                    // Vehicle Speed ------------------------------
                    String veh_v = String.format("%.1f", myapp.getObj("VEH_SPEED"));
                    String unit = " km/h";
                    StringBuffer buf = new StringBuffer();
                    buf.append(veh_v);
                    buf.append(unit);
                    textView_VehSpeed.setText(buf.toString());

                    /* PieChartではなくprogress barを使っての表示に変更
                    float sdc_factor_fl = (float)myapp.getObj("SDC_FACTOR_FL");
                    float sdc_factor_fl_remain = 100f - sdc_factor_fl;
                    List<Float> values = Arrays.asList(sdc_factor_fl, sdc_factor_fl_remain);
                    List<PieEntry> entries = new ArrayList<>();
                    for(int i = 0; i < values.size(); i++ ){
                        entries.add(new PieEntry(values.get(i), i));
                    }
                    PieDataSet dataset = new PieDataSet(entries, "SDC Factor FL");
                    dataset.setDrawValues(true);
                    PieData pieData = new PieData(dataset);
                    pieChart_SDCFactor_fl.setData(pieData);
                    pieChart_SDCFactor_fl.notifyDataSetChanged();
                    pieChart_SDCFactor_fl.invalidate();
                     */
                    // Progress barを更新
                    pb_sdcfactor_fl.setProgress((int)myapp.getObj("SDC_FACTOR_FL"));
                    pb_sdcfactor_fr.setProgress((int)myapp.getObj("SDC_FACTOR_FR"));
                    pb_sdcfactor_rl.setProgress((int)myapp.getObj("SDC_FACTOR_RL"));
                    pb_sdcfactor_rr.setProgress((int)myapp.getObj("SDC_FACTOR_RR"));

                    // SDCFactorのパーセンテージを更新
                    textView_SDCfactor_fl.setText(String.format("%d%%", (int)myapp.getObj("SDC_FACTOR_FL")));
                    textView_SDCfactor_fr.setText(String.format("%d%%", (int)myapp.getObj("SDC_FACTOR_FR")));
                    textView_SDCfactor_rl.setText(String.format("%d%%", (int)myapp.getObj("SDC_FACTOR_RL")));
                    textView_SDCfactor_rr.setText(String.format("%d%%", (int)myapp.getObj("SDC_FACTOR_RR")));
                }
            });

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // このActivityを画面に表示
        setContentView(R.layout.activity_sdc_can_disp);

        // スクリーンONを維持. 各アクティビティ内で宣言が必要
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 受信データを格納しているAppクラスを使用できるようにする
        myapp = (MyApplication)this.getApplication();
        latestdata = new HashMap<String, Double>();
        // CANデータを送信するためのBinderクラスを取得
        binder = myapp.getBinder();

        // xmlから各viewを取得
        imageView_Serialcomm = findViewById(R.id.imageView_SerialComm);
        imageView_CANcomm = findViewById(R.id.imageView_CANcomm);
        textView_SerialCommText = findViewById(R.id.textView_SerialCommText);
        textView_CANCommText = findViewById(R.id.textView_CANCommText);
        textView_SDC_Mode = findViewById(R.id.textView_selectedmode);
        textView_SDC_Lamp = findViewById(R.id.textView_sdc_lamp);

        // ProgressBarを使ってSDC Factorのインジケータを表示
        pb_sdcfactor_fl = findViewById(R.id.progressBar_sdcfactor_fl);
        pb_sdcfactor_fr = findViewById(R.id.progressBar_sdcfactor_fr);
        pb_sdcfactor_rl = findViewById(R.id.progressBar_sdcfactor_rl);
        pb_sdcfactor_rr = findViewById(R.id.progressBar_sdcfactor_rr);
        // SDC Factorを表示するためのtext IDを取得
        textView_SDCfactor_fl = findViewById(R.id.textView_sdcfactor_fl);
        textView_SDCfactor_fr = findViewById(R.id.textView_sdcfactor_fr);
        textView_SDCfactor_rl = findViewById(R.id.textView_sdcfactor_rl);
        textView_SDCfactor_rr = findViewById(R.id.textView_sdcfactor_rr);
        // E-SwayAct用のtext IDを取得
        textView_EswayAct = findViewById(R.id.textView_eswayact);
        // VehicleSpeed用
        textView_VehSpeed = findViewById(R.id.textView_vehspeed);

        /** SDC ModeをHMIから選択して送信するためのSpinnerの準備 */
        ArrayAdapter<String> adapter_sdcmode = new ArrayAdapter<String>(this, R.layout._cutom_spinner_item);
        adapter_sdcmode.setDropDownViewResource(R.layout._custom_spinner_dropdown);
        adapter_sdcmode.add("SDC: 1.Auto");
        adapter_sdcmode.add("SDC: 2.Full soft");
        adapter_sdcmode.add("SDC: 3.Full firm or Fixed");
        final Spinner spinner_sdcmode = (Spinner) findViewById(R.id.spinner_sdcmode);
        spinner_sdcmode.setAdapter(adapter_sdcmode);
        // リスナーを登録
        spinner_sdcmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner)adapterView;
                Spinner_SDCItem = (String)spinner.getSelectedItem();
                // 得られたSDC modeを元にECUへ信号送出
                sendModeSigToECU();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /** E-Sway ModeをHMIから選択して送信するためのSpinnerの準備 */
        ArrayAdapter<String> adapter_eswaymode = new ArrayAdapter<String>(this, R.layout._cutom_spinner_item);
        adapter_eswaymode.setDropDownViewResource(R.layout._custom_spinner_dropdown);
        adapter_eswaymode.add("E-Sway: 1.Auto");
        adapter_eswaymode.add("E-Sway: 2.Full soft");
        adapter_eswaymode.add("E-Sway: 3.Full firm");
        Spinner spinner_eswaymode = (Spinner) findViewById(R.id.spinner_eswaymode);
        spinner_eswaymode.setAdapter(adapter_eswaymode);
        // リスナーを登録
        spinner_eswaymode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner)adapterView;
                Spinner_EswayItem = (String)spinner.getSelectedItem();
                // 得られたE-Sway modeを元にECUへ信号送出
                sendModeSigToECU();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // もしUSBが接続されていないようであればデモモードに入る
        // Matファイルを読み込む
        //MatFileReader mfr = new MatFileReader(new File("asset"))


        // 画面表示を更新するためのタイマー これがないと画面が更新されない このタイマーで画面更新メソッドを呼び出し
        timer = new Timer();
        updateAccValue = new SDC_CANdisp.UpdateAccValue();
        //timer.schedule(updateAccValue, 1000, 50);
        timer.schedule(updateAccValue, 1000, 20);
    }

    // SDCModeとE-SwayModeからECUに送る値を決定するための関数
    private void sendModeSigToECU(){
        if(Spinner_SDCItem.equals("SDC: 1.Auto") && Spinner_EswayItem.equals("E-Sway: 1.Auto")){
            int sendSDCmode = 1;
            if(binder != null){
                binder.sendRequest(sendSDCmode);
            }
        }else if(Spinner_SDCItem.equals("SDC: 2.Full soft") && Spinner_EswayItem.equals("E-Sway: 2.Full soft")){
            int sendSDCmode = 2;
            if(binder != null){
                binder.sendRequest(sendSDCmode);
            }
        }else if(Spinner_SDCItem.equals("SDC: 2.Full soft") && Spinner_EswayItem.equals("E-Sway: 3.Full firm")){
            int sendSDCmode = 3;
            if(binder != null){
                binder.sendRequest(sendSDCmode);
            }
        }else if(Spinner_SDCItem.equals("SDC: 3.Full firm or Fixed") && Spinner_EswayItem.equals("E-Sway: 1.Auto")){
            int sendSDCmode = 5;
            if(binder != null){
                binder.sendRequest(sendSDCmode);
            }
        }else if(Spinner_SDCItem.equals("SDC: 3.Full firm or Fixed") && Spinner_EswayItem.equals("E-Sway: 2.Full soft")) {
            int sendSDCmode = 0;
            if(binder != null){
                binder.sendRequest(sendSDCmode);
            }
        }else{
            Context context = getApplicationContext();
            Toast.makeText(context, "Error. Sorry but no such combination mode", Toast.LENGTH_LONG).show();
        }
    }

    // TextViewを点滅させるための関数
    private void blinkText(TextView txtView, long duration, long offset){
        Animation anm = new AlphaAnimation(0.0f, 1.0f);
        anm.setDuration(duration);
        anm.setStartOffset(offset);
        anm.setRepeatMode(Animation.REVERSE);
        anm.setRepeatCount(Animation.INFINITE);
        txtView.startAnimation(anm);
    }

}
