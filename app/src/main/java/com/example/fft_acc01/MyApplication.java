/* MyApplication
 * @author M.Masuda
 * @version 1.0
 *
 * History
 * v1.0 10/5/2020: 1st edition
 */

package com.example.fft_acc01;

import android.app.Application;
import android.util.Log;

import java.util.HashMap;

public class MyApplication extends Application {

    private final String TAG = this.getClass().getName();
    public HashMap<String, Double> sensor_data = new HashMap<String, Double>();
    public HashMap<String, BindServiceSensorHandling.MyBinder> binderInst = new HashMap<String, BindServiceSensorHandling.MyBinder>();

    @Override
    public void onCreate() {
        super.onCreate();

        // 各値の初期値
        sensor_data.put("SerialCommUpdateTime", 0.0);
        sensor_data.put("SerialCommUpdateCycle", 0.0);
        sensor_data.put("CANCommUpdateTime", 0.0);
        sensor_data.put("SAMPLINGTIME", 0.0);
        sensor_data.put("SUSPSTROKE_FR", 0.0);
        sensor_data.put("SUSPSTROKE_RR", 0.0);
        sensor_data.put("ACC_X", 0.0);
        sensor_data.put("ACC_Y", 0.0);
        sensor_data.put("ACC_Z", 0.0);
        sensor_data.put("ENGINE_RPM", 0.0);
        sensor_data.put("THROTTLE_POS", 0.0);
        sensor_data.put("VEH_SPEED", 0.0);
        sensor_data.put("WATER_TEMP", 0.0);
        sensor_data.put("CVT_TEMP", 0.0);
        sensor_data.put("GEAR_POS", 0.0);
        sensor_data.put("IMU_YAWRATE", 0.0);
        sensor_data.put("IMU_ACC_Y", 0.0);
        sensor_data.put("IMU_ROLLRATE", 0.0);
        sensor_data.put("IMU_ACC_X", 0.0);
        sensor_data.put("IMU_PITCHRATE", 0.0);
        sensor_data.put("IMU_ACC_Z", 0.0);
        sensor_data.put("SDC_FACTOR_FL", 0.0);
        sensor_data.put("SDC_FACTOR_FR", 0.0);
        sensor_data.put("SDC_FACTOR_RL", 0.0);
        sensor_data.put("SDC_FACTOR_RR", 0.0);
        sensor_data.put("SDC_MODE", 0.0);
        sensor_data.put("SDC_LAMP", 2.0);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void setObj(String key, Double value){
        sensor_data.put(key, value);
    }
    public double getObj(String key){
        return sensor_data.get(key);
    }
    public void setBinder(BindServiceSensorHandling.MyBinder binder){
        binderInst.put("binder", binder);
    }
    public BindServiceSensorHandling.MyBinder getBinder(){
        return binderInst.get("binder");
    }
}
