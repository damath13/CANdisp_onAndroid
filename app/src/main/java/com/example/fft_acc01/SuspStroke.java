package com.example.fft_acc01;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SuspStroke extends AppCompatActivity {

    private HashMap<String, Double> latestdata;
    private MyApplication myapp;

    private LineChart mChart_dispFR;
    private LineChart mChart_dispRR;
    private LineChart mChart_velFR;
    private LineChart mChart_velRR;
    private LineDataSet dataset_dispFR;
    private LineDataSet dataset_dispRR;
    private LineDataSet dataset_velFR;
    private LineDataSet dataset_velRR;

    int TIMESPAN_X = 3;         // sec
    double FS = 200.0; // 200Hz
    float cnt = 0;
    private List<Float> xdata; // Float型であることに注意
    private List<Float> ydata; // Float型であることに注意

    Timer timer = new Timer();
    TimerTask updateAccValue;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // このActivityを画面に表示
        setContentView(R.layout.activity_suspstroke);

        Intent intent = getIntent();

        // Appl.クラスを使えるようにする
        myapp = (MyApplication)this.getApplication();

        // スクリーンONを維持. 各アクティビティ内で宣言が必要
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* Chartの初期化----------------------------------------------------*/
        // 共通(x軸)

        // データ長
        int nData = (int) (TIMESPAN_X * FS);

        // xデータは0のみ
        xdata = new ArrayList<>();
        xdata.add((float)0.0);

        /* dispFR ------*/
        mChart_dispFR = findViewById(R.id.chart_dispFR);
        mChart_dispFR.setTouchEnabled(true);
        mChart_dispFR.setDragEnabled(true);
        mChart_dispFR.setScaleEnabled(true);
        mChart_dispFR.setPinchZoom(true);

        // Y軸は0データをセット
        ydata = new ArrayList<>();
        ydata.add((float)0.0);

        // Entry型のListへxdataとydataを代入
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < xdata.size(); i++) {
            entries.add(new Entry(xdata.get(i), ydata.get(i)));
        }

        // グラフのラインになるLineDataSetを作成
        dataset_dispFR = new LineDataSet(entries, "Disp. FR");
        dataset_dispFR.setColor(Color.BLUE);
        dataset_dispFR.setLineWidth(3f);
        dataset_dispFR.setDrawCircles(false);

        //データセットの先頭を削除 なんでだっけ？
        dataset_dispFR.removeFirst();

        // LineDataSsetを使ってLineDataを初期化
        LineData lineData = new LineData(dataset_dispFR);

        // LineChartにLineDataをセット
        mChart_dispFR.setData(lineData);

        // Yaxis
        YAxis leftAxis = mChart_dispFR.getAxisLeft();
        leftAxis.setAxisMinimum(-10f);
        leftAxis.setAxisMaximum(1024f);
        leftAxis.setTextColor(Color.WHITE);
        // Yaxis(右側)
        YAxis rightAxis = mChart_dispFR.getAxisRight();
        rightAxis.setEnabled(false);
        // Xaxis
        XAxis xAxis = mChart_dispFR.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setAxisMinimum(0f);
        //xAxis.setAxisMaximum(3f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setLabelCount(6);

        // グラフの更新
        mChart_dispFR.invalidate();

        // dispRR
        mChart_dispRR = findViewById(R.id.chart_dispRR);
        mChart_dispRR.setTouchEnabled(true);
        mChart_dispRR.setDragEnabled(true);
        mChart_dispRR.setScaleEnabled(true);
        mChart_dispRR.setPinchZoom(true);
        // velFR
        mChart_velFR = findViewById(R.id.chart_velFR);
        mChart_velFR.setTouchEnabled(true);
        mChart_velFR.setDragEnabled(true);
        mChart_velFR.setScaleEnabled(true);
        mChart_velFR.setPinchZoom(true);
        // velRR
        mChart_velRR = findViewById(R.id.chart_velRR);
        mChart_velRR.setTouchEnabled(true);
        mChart_velRR.setDragEnabled(true);
        mChart_velRR.setScaleEnabled(true);
        mChart_velRR.setPinchZoom(true);

        // 受信データを更新するためのタイマー
        timer = new Timer();
        updateAccValue = new SuspStroke.UpdateRecvData();
        timer.schedule(updateAccValue, 1000, 10);

    }

    private class UpdateRecvData extends TimerTask {

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 加速度センサの値とSamplingTimeを取得
                    double suspstroke_fr = myapp.getObj("SUSPSTROKE_FR");
                    double suspstroke_rr = myapp.getObj("SUSPSTROKE_RR");
                    double sampling_time = myapp.getObj("SAMPLINGTIME");

                    // グラフの更新---------------------------------------------
                    LineData lineData = mChart_dispFR.getData();
                    cnt+=1/FS;
                    // 追加描画するデータを追加
                    lineData.addEntry(new Entry(cnt, (float)suspstroke_rr), 0);

                    //  データを追加したら必ずよばないといけない
                    lineData.notifyDataChanged();
                    mChart_dispFR.notifyDataSetChanged();
                    mChart_dispFR.invalidate();

                    // X軸の移動
                    mChart_dispFR.moveViewToX(lineData.getXMax()-3);
                    mChart_dispFR.setVisibleXRangeMaximum(3);
                }
            });
        }
    }
}
