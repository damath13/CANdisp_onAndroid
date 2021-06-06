package com.example.fft_acc01;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class SDC_Control extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdc_control);

        final SeekBar sb0 = findViewById(R.id.seekBar);
        final SeekBar sb1 = findViewById(R.id.seekBar2);
        final SeekBar sb2 = findViewById(R.id.seekBar3);
        final SeekBar sb3 = findViewById(R.id.seekBar4);
        final TextView tv0 = findViewById(R.id.textView4);
        final TextView tv1 = findViewById(R.id.textView5);
        final TextView tv2 = findViewById(R.id.textView6);
        final TextView tv3 = findViewById(R.id.textView7);

        sb0.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //ツマミがドラッグされると呼ばれる
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String str = String.format(Locale.US, "%d %%", i);
                tv0.setText(str);

            }
            //ツマミがタッチされた時に呼ばれる
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            //ツマミがリリースされた時に呼ばれる
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
