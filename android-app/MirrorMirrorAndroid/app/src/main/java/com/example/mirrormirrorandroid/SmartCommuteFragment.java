package com.example.mirrormirrorandroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class SmartCommuteFragment extends Fragment {

    private TextView txtEventTitle;
    private TextView txtLocation;
    private TextView txtDistance;
    private TextView txtTravelTime;
    private TextView txtLeaveTime;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    private long currentEventStartMillis = 0;
    private long currentTravelMillis = 0;

    private String currentEventTitle = "";
    private String currentEventLocation = "";
    private String currentDistanceText = "";
    private String currentTravelTimeText = "";

    private Handler leaveTimeHandler = new Handler(Looper.getMainLooper());
    private Runnable leaveTimeRunnable;

    public SmartCommuteFragment() {
        super(R.layout.fragment_smart_commute);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupGoogleSignIn();

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE);
        String accountName = prefs.getString("googleAccountName", null);

        if (accountName != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    Collections.singleton(CalendarScopes.CALENDAR_READONLY)
            );
            credential.setSelectedAccountName(accountName);
            fetchNextEvent(credential);
        } else {
            Toast.makeText(requireContext(),
                    "Please connect Google Calendar in Status tab", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews(View view) {

        txtEventTitle = view.findViewById(R.id.txtEventTitle);
        txtLocation = view.findViewById(R.id.txtLocation);
        txtDistance = view.findViewById(R.id.txtDistance);
        txtTravelTime = view.findViewById(R.id.txtTravelTime);
        txtLeaveTime = view.findViewById(R.id.txtLeaveTime);
    }

    private void fetchDistanceAndTravelTime(String title, String destinationName, long eventStartMillis) {
        if (destinationName == null || destinationName.trim().isEmpty()) return;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                            "Location permission required to calculate commute",
                            Toast.LENGTH_SHORT).show());
            return;
        }

        FusedLocationProviderClient fusedClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        "Current location unknown. Enable location and try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            double originLat = location.getLatitude();
            double originLng = location.getLongitude();

            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext());
                List<Address> addresses = geocoder.getFromLocationName(destinationName, 1);
                if (addresses == null || addresses.isEmpty()) return;

                double destLat = addresses.get(0).getLatitude();
                double destLng = addresses.get(0).getLongitude();

                String apiKey = getString(R.string.google_maps_key);
                String urlStr = "https://maps.googleapis.com/maps/api/directions/json?"
                        + "origin=" + originLat + "," + originLng
                        + "&destination=" + destLat + "," + destLng
                        + "&departure_time=now"
                        + "&key=" + apiKey;

                new Thread(() -> {
                    try {
                        URL url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(15000);
                        conn.connect();

                        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

                        Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                        scanner.close();

                        JSONObject json = new JSONObject(sb.toString());
                        JSONArray routes = json.optJSONArray("routes");
                        if (routes == null || routes.length() == 0) return;

                        JSONObject route = routes.getJSONObject(0);
                        JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                        String distanceText = leg.getJSONObject("distance").optString("text", "N/A");
                        String travelTimeText = leg.has("duration_in_traffic")
                                ? leg.getJSONObject("duration_in_traffic").optString("text")
                                : leg.getJSONObject("duration").optString("text", "N/A");

                        long travelSeconds = leg.has("duration_in_traffic")
                                ? leg.getJSONObject("duration_in_traffic").optLong("value", 0)
                                : leg.getJSONObject("duration").optLong("value", 0);
                        long travelMillis = travelSeconds * 1000;

                        currentEventStartMillis = eventStartMillis;
                        currentTravelMillis = travelMillis;

                        // Calculate leave time based on event start
                        long leaveMillis = eventStartMillis - System.currentTimeMillis() - travelMillis;
                        long leaveMinutes = Math.max(0, leaveMillis / 60000);

                        requireActivity().runOnUiThread(() -> {
                            currentEventTitle = title;
                            currentEventLocation = destinationName;
                            currentDistanceText = distanceText;
                            currentTravelTimeText = travelTimeText;


                            txtDistance.setText("Distance: " + distanceText);
                            txtTravelTime.setText("Travel Time: " + travelTimeText);

                            String friendlyLeaveTime = getFriendlyLeaveTime(leaveMillis);
                            String fullText = "Leave: " + friendlyLeaveTime;

                            SpannableString spannable = new SpannableString(fullText);

                            // Start colouring AFTER "Leave: "
                            int start = "Leave: ".length();
                            int end = fullText.length();

                            int color;
                            if (leaveMinutes <= 0) {
                                color = Color.parseColor("#ff6666"); // red
                            } else if (leaveMinutes <= 15) {
                                color = Color.parseColor("#ffcc33"); // orange
                            } else {
                                color = Color.parseColor("#66ff66"); // green
                            }

                            spannable.setSpan(
                                    new ForegroundColorSpan(color),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );

                            txtLeaveTime.setText(spannable);


                            startLeaveTimeUpdates();

                            sendSmartCommuteToMirror(
                                    currentEventTitle,
                                    currentEventLocation,
                                    currentDistanceText,
                                    currentTravelTimeText,
                                    leaveMinutes
                            );
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startLeaveTimeUpdates() {

        if (leaveTimeRunnable != null) {
            leaveTimeHandler.removeCallbacks(leaveTimeRunnable);
        }

        leaveTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentEventStartMillis == 0) return;

                long leaveMillis = currentEventStartMillis - System.currentTimeMillis() - currentTravelMillis;
                long leaveMinutes = Math.max(0, leaveMillis / 60000);

                String friendlyLeaveTime = getFriendlyLeaveTime(leaveMillis);
                String fullText = "Leave: " + friendlyLeaveTime;

                SpannableString spannable = new SpannableString(fullText);
                int start = "Leave: ".length();
                int end = fullText.length();

                int color;
                if (leaveMinutes <= 0) color = Color.parseColor("#ff6666");
                else if (leaveMinutes <= 15) color = Color.parseColor("#ffcc33");
                else color = Color.parseColor("#66ff66");

                spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                txtLeaveTime.setText(spannable);

                // 🔥 Push updated leave time to mirror
                sendSmartCommuteToMirror(
                        currentEventTitle,
                        currentEventLocation,
                        currentDistanceText,
                        currentTravelTimeText,
                        leaveMinutes
                );

                leaveTimeHandler.postDelayed(this, 60000);
            }
        };

        leaveTimeHandler.post(leaveTimeRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (leaveTimeRunnable != null) {
            leaveTimeHandler.removeCallbacks(leaveTimeRunnable);
        }
    }



    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                txtEventTitle.setText("Connected to Calendar: " + account.getEmail());

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        requireContext(),
                        Collections.singleton(CalendarScopes.CALENDAR_READONLY)
                );
                credential.setSelectedAccount(account.getAccount());

                fetchNextEvent(credential);

                SharedPreferences prefs = requireActivity()
                        .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("googleAccountName", account.getEmail()).apply();
            } else {
                Toast.makeText(requireContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI(String title, String location, String distance, String travelTime, long leaveInMinutes) {
        txtEventTitle.setText("Next Event: " + title);
        txtLocation.setText("Location: " + location);
        txtDistance.setText("Distance: " + distance);
        txtTravelTime.setText("Travel Time: " + travelTime);
        txtLeaveTime.setText("Leave In: " + leaveInMinutes + " mins");
    }

    /** Fetches the next event from Google Calendar */
    private void fetchNextEvent(GoogleAccountCredential credential) {
        new Thread(() -> {
            try {
                Calendar service = new Calendar.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                ).setApplicationName("MirrorMirrorAndroid").build();

                Events events = service.events().list("primary")
                        .setMaxResults(1)
                        .setTimeMin(new com.google.api.client.util.DateTime(System.currentTimeMillis()))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();

                if (!events.getItems().isEmpty()) {
                    Event nextEvent = events.getItems().get(0);
                    String title = nextEvent.getSummary();
                    String location = nextEvent.getLocation() != null ? nextEvent.getLocation() : "N/A";

                    // Show temporary placeholders
                    updateUI(title, location, "Fetching...", "Fetching...", 0);

                    // Pass event start time in millis to calculate leave time correctly
                    long eventStartMillis = nextEvent.getStart().getDateTime() != null
                            ? nextEvent.getStart().getDateTime().getValue()
                            : System.currentTimeMillis();

                    fetchDistanceAndTravelTime(title, location, eventStartMillis);
                }

            } catch (UserRecoverableAuthIOException e) {
                // This is the key: Google requires user consent
                requireActivity().startActivityForResult(e.getIntent(), RC_SIGN_IN);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void applyLeaveTimeColor(long leaveInMinutes) {

        if (leaveInMinutes <= 0) {
            txtLeaveTime.setTextColor(Color.parseColor("#ff6666")); // red
        } else if (leaveInMinutes <= 15) {
            txtLeaveTime.setTextColor(Color.parseColor("#ffcc33")); // orange
        } else {
            txtLeaveTime.setTextColor(Color.parseColor("#66ff66")); // green
        }
    }

    private String getFriendlyLeaveTime(long leaveMillis) {
        if (leaveMillis <= 0) return "Leave now";

        long minutes = leaveMillis / 60000;
        if (minutes < 60) return "in " + minutes + " mins";

        long hours = minutes / 60;
        long remMinutes = minutes % 60;

        if (hours < 24) {
            if (remMinutes == 0) return "in " + hours + " hours";
            return "in " + hours + "h " + remMinutes + "m";
        }

        // Event is more than a day away: show exact time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, hh:mm a");
        return "at " + sdf.format(System.currentTimeMillis() + leaveMillis);
    }

    /** Sends commute info to Mirror */
    /** Sends commute info to Mirror */
    private void sendSmartCommuteToMirror(String title, String location, String distance, String travelTime, long leaveInMinutes) {
        String mirrorIp = requireActivity()
                .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                .getString("selectedMirrorIp", null);

        if (mirrorIp == null) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "No mirror selected", Toast.LENGTH_SHORT).show());
            return;
        }

        // Convert raw minutes to a friendly string
        String friendlyLeaveTime = getFriendlyLeaveTime(leaveInMinutes * 60L * 1000L); // convert back to millis

        JSONObject payload = new JSONObject();
        try {
            payload.put("eventTitle", title);
            payload.put("location", location);
            payload.put("distance", distance);
            payload.put("travelTime", travelTime);
            payload.put("leaveTimeText", friendlyLeaveTime);  // formatted
            payload.put("leaveInMinutes", leaveInMinutes);    // raw number
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + mirrorIp + ":8081/smartcommute");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                System.out.println("SmartCommute POST Response :: " + responseCode);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
