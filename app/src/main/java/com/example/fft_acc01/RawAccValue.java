/* RawAccValue
 * @author M.Masuda
 * @version 1.0
 *
 * History
 * v1.0 10/5/2020: 1st edition
 */
package com.example.fft_acc01;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RawAccValue extends AppCompatActivity {

    private HashMap<String, Double> latestdata;
    private MyApplication myapp;

    private TextView textView_rawacc_x;
    private TextView textView_rawacc_y;
    private TextView textView_rawacc_z;
    private LineChart mChart_accraw;
    private LineDataSet dataset;

    int TIMESPAN_X = 3;         // sec
    double UPDATERATE = 0.005; // 200Hz
    private List<Float> xdata; // Float型であることに注意
    private List<Float> ydata; // Float型であることに注意

    Timer timer = new Timer();
    TimerTask updateAccValue;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_acc_value);

        Intent intent = getIntent();
        myapp = (MyApplication)this.getApplication();

        // スクリーンONを維持. 各アクティビティ内で宣言が必要
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // xmlからviewを取得
        textView_rawacc_x = (TextView) findViewById(R.id.textView_accval_x);
        textView_rawacc_y = (TextView) findViewById(R.id.textView_accval_y);
        textView_rawacc_z = (TextView) findViewById(R.id.textView_accval_z);

        /* Chartの初期化----------------------------------------------------*/
        mChart_accraw = findViewById(R.id.chart_accraw);
        mChart_accraw.setTouchEnabled(true);
        mChart_accraw.setDragEnabled(true);
        mChart_accraw.setScaleEnabled(true);
        mChart_accraw.setPinchZoom(true);

        int nData = (int) (TIMESPAN_X / UPDATERATE);

        // X軸は0～500のデータをセット
        xdata = new ArrayList<>();
        for (int i = 0; i < nData; i++) {
            xdata.add((float) i);
        }

        // Y軸は0データをセット(Arrayを全て0で埋める)
        Float[] arr = new Float[nData]; // ArrayListがfloat型ではなくFloat型で定義してるためfloatにするとエラーになる．
        ydata = new ArrayList<>(Arrays.asList(arr));
        Collections.fill(ydata, 0f);

        // Entry型のListへ代入
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < xdata.size(); i++) {
            entries.add(new Entry(xdata.get(i), ydata.get(i)));
        }

        // グラフのラインになるLineDataSetを作成
        dataset = new LineDataSet(entries, "Xaxis");
        dataset.setColor(Color.BLUE);

        //データセットの先頭を削除
        dataset.removeFirst();

        // LineDataSsetを使ってLineDataを初期化
        LineData lineData = new LineData(dataset);

        // LineChartにLineDataをセット
        mChart_accraw.setData(lineData);

        // Yaxis
        YAxis leftAxis = mChart_accraw.getAxisLeft();
        leftAxis.setAxisMinimum(-0f);
        leftAxis.setAxisMaximum(20f);
        // Yaxis(右側)
        YAxis rightAxis = mChart_accraw.getAxisRight();
        rightAxis.setEnabled(false);

        // Xaxis
        XAxis xAxis = mChart_accraw.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // グラフの更新
        mChart_accraw.invalidate();

        /* chartの初期化終わり--------------------------------------*/

        // 加速度表示を更新するためのタイマー
        timer = new Timer();
        updateAccValue = new UpdateAccValue();
        timer.schedule(updateAccValue, 1000, 100);
    }


    private class UpdateAccValue extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    // 加速度センサの値とSamplingTimeを取得
                    double acc_x = myapp.getObj("ACC_X");
                    double acc_y = myapp.getObj("ACC_Y");
                    double acc_z = myapp.getObj("ACC_Z");
                    double sampling_time = myapp.getObj("SAMPLINGTIME");

                    // 加速度センサの値を表示するTextViewを更新
                    textView_rawacc_x.setText(String.format("%.3f", acc_x));
                    textView_rawacc_y.setText(String.format("%.3f", acc_y));
                    textView_rawacc_z.setText(String.format("%.3f", acc_z));

                    // グラフの更新---------------------------------------------
                    // 一旦Yデータを取り除く
                    LineData lineData = mChart_accraw.getData();
                    lineData.removeDataSet(dataset);

                    // センサ値格納リストの長さ文だけydataを先頭から削除 今回は暫定で1データずつ追加
                    List<Float> accValueListX = new ArrayList<Float>(Arrays.<Float>asList((float)acc_z));
                    ydata.subList(0, accValueListX.size()).clear();
                    // ydataにセンサ格納リストを後ろから結合
                    ydata.addAll(accValueListX);

                    // Entry型のListへ代入----
                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < xdata.size(); i++) {
                        entries.add(new Entry(xdata.get(i), ydata.get(i)));
                    }
                    //---ここまでたった2ms…

                    dataset = new LineDataSet(entries, "Xaxis");
                    dataset.setDrawCircles(false);
                    dataset.setDrawCircleHole(false);
                    lineData.addDataSet(dataset);

                    //  データを追加したら必ずよばないといけない
                    mChart_accraw.notifyDataSetChanged();
                    mChart_accraw.invalidate();

                }
            });
        }
    }
}
