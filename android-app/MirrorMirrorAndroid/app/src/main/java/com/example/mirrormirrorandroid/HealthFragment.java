package com.example.mirrormirrorandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HealthFragment extends Fragment {

    private TextView stepsText, caloriesText;
    private EditText heightInput, weightInput;
    private Button submitButton;

    private DailyStepCounter dailyStepCounter;

    public HealthFragment() {
        super(R.layout.fragment_health);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stepsText = view.findViewById(R.id.stepsText);
        caloriesText = view.findViewById(R.id.caloriesText);
        heightInput = view.findViewById(R.id.heightInput);
        weightInput = view.findViewById(R.id.weightInput);
        submitButton = view.findViewById(R.id.submitButton);

        dailyStepCounter = new DailyStepCounter(requireContext());

        if (!dailyStepCounter.hasSensor()) {
            Toast.makeText(requireContext(),
                    "Step counter not supported on this device",
                    Toast.LENGTH_LONG).show();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},
                        101
                );
            }
        }

        setupTextWatchers();
        updateHealthUI();

        submitButton.setOnClickListener(v -> sendDataToMirror());
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateHealthUI();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        heightInput.addTextChangedListener(watcher);
        weightInput.addTextChangedListener(watcher);
    }

    private void updateHealthUI() {
        int stepsToday = dailyStepCounter.getStepsToday();

        float height = 170f, weight = 70f;
        try { height = Float.parseFloat(heightInput.getText().toString()); } catch (Exception ignored){}
        try { weight = Float.parseFloat(weightInput.getText().toString()); } catch (Exception ignored){}

        int caloriesToday = dailyStepCounter.getCaloriesToday(weight, height);

        stepsText.setText("Steps Today: " + stepsToday);
        caloriesText.setText("Calories: " + caloriesToday);
    }

    private void sendDataToMirror() {
        int stepsToday = dailyStepCounter.getStepsToday();

        float height = 170f, weight = 70f;
        try { height = Float.parseFloat(heightInput.getText().toString()); } catch (Exception ignored){}
        try { weight = Float.parseFloat(weightInput.getText().toString()); } catch (Exception ignored){}

        int caloriesToday = dailyStepCounter.getCaloriesToday(weight, height);

        String mirrorIp = requireActivity()
                .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                .getString("selectedMirrorIp", null);

        if (mirrorIp == null) {
            Toast.makeText(requireContext(), "No mirror selected", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("steps", stepsToday);
            payload.put("calories", caloriesToday);
            payload.put("height", height);  // <-- send height
            payload.put("weight", weight);  // <-- send weight
        } catch (Exception e) { e.printStackTrace(); }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + mirrorIp + ":8081/health");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                System.out.println("POST Response Code :: " + responseCode);
            } catch (Exception e) { e.printStackTrace(); }
            finally { if (conn != null) conn.disconnect(); }
        }).start();

        Toast.makeText(requireContext(), "Health data sent to Mirror", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        dailyStepCounter.start();
        handler.post(updateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        dailyStepCounter.stop();
        handler.removeCallbacks(updateRunnable);
    }

    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateHealthUI();
            handler.postDelayed(this, 1000);
        }
    };
}
