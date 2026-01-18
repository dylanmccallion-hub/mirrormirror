package com.example.mirrormirrorandroid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
        submitButton = view.findViewById(R.id.submitButton);

        // Initialize DailyStepCounter
        dailyStepCounter = new DailyStepCounter(requireContext());

        // Update UI immediately
        updateHealthUI();

        // Submit button
        submitButton.setOnClickListener(v -> sendDataToMirror());
    }

    private void updateHealthUI() {
        int stepsToday = dailyStepCounter.getStepsToday();
        int caloriesToday = dailyStepCounter.getCaloriesToday();

        stepsText.setText("Steps: " + stepsToday);
        caloriesText.setText("Calories: " + caloriesToday);
    }

    private void sendDataToMirror() {
        int stepsToday = dailyStepCounter.getStepsToday();
        int caloriesToday = dailyStepCounter.getCaloriesToday();

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
        } catch (Exception e) {
            e.printStackTrace();
        }

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
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();

        Toast.makeText(requireContext(), "Health data sent to Mirror", Toast.LENGTH_SHORT).show();
    }
}
