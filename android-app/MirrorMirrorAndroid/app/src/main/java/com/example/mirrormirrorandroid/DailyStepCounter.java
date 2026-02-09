package com.example.mirrormirrorandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Calendar;

public class DailyStepCounter implements SensorEventListener {
    private Context context;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "StepPrefs";
    private static final String KEY_LAST_SENSOR = "last_sensor_value";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_LAST_DAY = "last_day";

    public DailyStepCounter(Context ctx) {
        context = ctx;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor == null) {
            Log.e("DailyStepCounter", "Step counter sensor not available!");
        }
    }

    public boolean hasSensor() {
        return stepCounterSensor != null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int totalStepsSinceReboot = (int) event.values[0];

        int lastDay = prefs.getInt(KEY_LAST_DAY, -1);
        int today = java.util.Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

        int stepsToday = prefs.getInt(KEY_STEPS_TODAY, 0);
        int lastSensor = prefs.getInt(KEY_LAST_SENSOR, totalStepsSinceReboot);

        // reset daily steps if day changed
        if (lastDay != today) {
            stepsToday = 0;
            lastSensor = totalStepsSinceReboot;
        }

        // add steps since last reading
        int delta = totalStepsSinceReboot - lastSensor;
        if (delta > 0) stepsToday += delta;

        // save updated values
        prefs.edit()
                .putInt(KEY_STEPS_TODAY, stepsToday)
                .putInt(KEY_LAST_SENSOR, totalStepsSinceReboot)
                .putInt(KEY_LAST_DAY, today)
                .apply();

        // optional: calculate calories
        int calories = (int) (stepsToday * 0.04); // rough estimate: 0.04 kcal per step

        Log.d("DailyStepCounter", "Steps today: " + stepsToday + ", Calories: " + calories);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public void start() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }


    public int getStepsToday() {
        return prefs.getInt(KEY_STEPS_TODAY, 0);
    }

    public int getCaloriesToday(float weightKg, float heightCm) {
        int steps = getStepsToday();

        // Approximate step length in meters
        double stepLengthMeters = heightCm * 0.415 / 100.0;
        double distanceKm = stepLengthMeters * steps / 1000.0;

        double walkingSpeedKmh = 5.0; // average walking speed
        double durationHours = distanceKm / walkingSpeedKmh;

        double met = 3.5; // average MET for walking
        double calories = met * weightKg * durationHours;

        return (int) calories;
    }

}
