package com.example.mirrormirrorandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MirrorStatusFragment extends Fragment {

    private TextView txtConnectionStatus, txtMirrorId, txtIpAddress, txtUptime, txtModules, txtLastUpdate;
    private ImageView connectionIndicator;
    private ProgressBar loading;
    private ListView listViewMirrors;

    private NsdManager nsdManager;
    private AtomicBoolean mirrorFound = new AtomicBoolean(false);
    private List<String> mirrorNames = new ArrayList<>();
    private List<String> mirrorIps = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private OkHttpClient client = new OkHttpClient();

    private Handler handler = new Handler();
    private String selectedMirrorIp = null;

    private Spinner spinnerCities;
    private final String[] cities = {
            "New York", "Los Angeles", "São Paulo", "Dublin", "Berlin",
            "Moscow", "Tokyo", "Dubai", "Sydney", "Cape Town"
    };

    private static class LatLon {
        final double lat;
        final double lon;
        LatLon(double lat, double lon) { this.lat = lat; this.lon = lon; }
    }

    private final Map<String, LatLon> cityCoordinates = new HashMap<String, LatLon>() {{
        put("New York", new LatLon(40.776676, -73.971321));
        put("Los Angeles", new LatLon(34.052235, -118.243683));
        put("São Paulo", new LatLon(-23.55052, -46.633308));
        put("Dublin", new LatLon(53.349805, -6.26031));
        put("Berlin", new LatLon(52.520008, 13.404954));
        put("Moscow", new LatLon(55.751244, 37.618423));
        put("Tokyo", new LatLon(35.682839, 139.759455));
        put("Dubai", new LatLon(25.276987, 55.296249));
        put("Sydney", new LatLon(-33.86882, 151.209296));
        put("Cape Town", new LatLon(-33.92487, 18.424055));
    }};



    public MirrorStatusFragment() {
        super(R.layout.fragment_mirror_status);
    }

    // Poll selected mirror every 5s to detect disconnects
    private final Runnable statusPoller = new Runnable() {
        @Override
        public void run() {
            if (selectedMirrorIp != null) {
                Request request = new Request.Builder()
                        .url("http://" + selectedMirrorIp + ":8081/status")
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> handleMirrorDisconnect());
                        }
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (!response.isSuccessful() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> handleMirrorDisconnect());
                        }
                    }
                });
            }
            handler.postDelayed(this, 5000);
        }
    };

    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override public void onDiscoveryStarted(String regType) {}
        @Override public void onServiceFound(NsdServiceInfo service) {
            if (service.getServiceType().contains("_magicmirror._tcp")) {
                nsdManager.resolveService(service, resolveListener);
            }
        }
        @Override public void onServiceLost(NsdServiceInfo service) {}
        @Override public void onDiscoveryStopped(String serviceType) {}
        @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) { nsdManager.stopServiceDiscovery(this); }
        @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) { nsdManager.stopServiceDiscovery(this); }
    };

    private final NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}
        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                String mirrorIp = serviceInfo.getHost().getHostAddress();
                String mirrorName = serviceInfo.getServiceName();

                // Add new mirror to list if not already present
                if (!mirrorIps.contains(mirrorIp)) {
                    mirrorNames.add(mirrorName);
                    mirrorIps.add(mirrorIp);
                    adapter.notifyDataSetChanged();
                }

                // Only update status indicator if we’re not already connected
                if (!mirrorIp.equals(selectedMirrorIp)) {
                    txtConnectionStatus.setText("Mirror Found: " + mirrorName);
                    setIndicatorState("searching");
                    mirrorFound.set(true);
                }
            });
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Initialize UI ---
        txtConnectionStatus = view.findViewById(R.id.txtConnectionStatus);
        connectionIndicator = view.findViewById(R.id.connectionIndicator);
        listViewMirrors = view.findViewById(R.id.listViewMirrors);
        spinnerCities = view.findViewById(R.id.spinnerCities);
        loading = view.findViewById(R.id.loading);
        txtMirrorId = view.findViewById(R.id.txtMirrorId);
        txtIpAddress = view.findViewById(R.id.txtIpAddress);
        txtUptime = view.findViewById(R.id.txtUptime);
        txtModules = view.findViewById(R.id.txtModules);
        txtLastUpdate = view.findViewById(R.id.txtLastUpdate);


        // Clear old mirrors and reset flags
        mirrorNames.clear();
        mirrorIps.clear();
        mirrorFound.set(false);
        selectedMirrorIp = null;

        txtConnectionStatus.setText("Status: Disconnected");
        setIndicatorState("disconnected");

        adapter = new ArrayAdapter<>(requireContext(), R.layout.list_items, R.id.txtMirrorName, mirrorNames);
        listViewMirrors.setAdapter(adapter);

        // Start discovery
        nsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);

        txtConnectionStatus.setText("Status: Disconnected");
        setIndicatorState("disconnected");

        listViewMirrors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedMirrorIp = mirrorIps.get(position); // get IP of selected mirror
                String mirrorName = mirrorNames.get(position);
                updateMirrorInfo(selectedMirrorIp, mirrorName); // connect & fetch status
            }
        });


        // --- Restore last selected mirror ---
//        selectedMirrorIp = prefs.getString("selectedMirrorIp", null);
//
//        if (selectedMirrorIp != null) {
//            // Pre-fill list with placeholder
//            mirrorIps.add(selectedMirrorIp);
//            mirrorNames.add("Loading...");
//            adapter.notifyDataSetChanged();
//
//            // Try to reach the last selected mirror
//            verifyMirrorReachable(selectedMirrorIp);
//        } else {
//            // No saved mirror, start discovery
//            startDiscovery();
//        }
        startDiscovery();

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_dark,  // custom row
                R.id.txtSpinnerItem,
                cities
        );
        cityAdapter.setDropDownViewResource(R.layout.spinner_item_dark);
        spinnerCities.setAdapter(cityAdapter);

// Set a dark semi-transparent background for the popup
        spinnerCities.setPopupBackgroundResource(R.drawable.spinner_popup_background_dark);


        // Restore last selected city
        String lastCity = "New York"; // default
        int position = java.util.Arrays.asList(cities).indexOf(lastCity);
        spinnerCities.setSelection(position);

        spinnerCities.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedCity = cities[pos];

                if (selectedMirrorIp != null) {
                    updateMirrorLocation(selectedMirrorIp, selectedCity); // sends lat/lon and city
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });



        // Start periodic polling
        handler.post(statusPoller);
    }


    private void updateMirrorLocation(String mirrorIp, String city) {
        // Safety check: valid mirror and coordinates exist
        if (mirrorIp == null || !cityCoordinates.containsKey(city)) return;

        loading.setVisibility(View.VISIBLE);

        LatLon coords = cityCoordinates.get(city);

        JSONObject json = new JSONObject();
        try {
            // Send both lat/lon and the city name
            json.put("lat", coords.lat);
            json.put("lon", coords.lon);
            json.put("city", city);  // <-- add this line
        } catch (Exception e) {
            e.printStackTrace();
            loading.setVisibility(View.GONE);
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://" + mirrorIp + ":8081/location") // your server endpoint
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to update location", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Location updated: " + city, Toast.LENGTH_SHORT).show();
                            System.out.println("Location successfully sent: " + json.toString());
                        } else {
                            Toast.makeText(getContext(), "Failed to update location", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }






//    private void verifyMirrorReachable(String ip) {
//        Request request = new Request.Builder()
//                .url("http://" + ip + ":8081/status")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                if (getActivity() != null) {
//                    getActivity().runOnUiThread(() -> {
//                        handleMirrorDisconnect();
//                        mirrorNames.clear();  // remove any placeholder
//                        mirrorIps.clear();
//                        adapter.notifyDataSetChanged();
//                        startDiscovery();    // restart discovery automatically
//                    });
//                }
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) {
//                if (getActivity() != null) {
//                    getActivity().runOnUiThread(() -> {
//                        if (response.isSuccessful()) {
//                            updateMirrorInfo(ip, "Last Selected Mirror");
//                        } else {
//                            handleMirrorDisconnect();
//                            mirrorNames.clear();
//                            mirrorIps.clear();
//                            adapter.notifyDataSetChanged();
//                            startDiscovery();
//                        }
//                    });
//                }
//            }
//        });
//    }


    private void handleMirrorDisconnect() {
        selectedMirrorIp = null;
        SharedPreferences prefs = requireActivity().getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("selectedMirrorIp").remove("selectedMirrorName").apply();

        txtConnectionStatus.setText("Status: Disconnected");
        setIndicatorState("disconnected");
    }

    private void startDiscovery() {
        mirrorFound.set(false);
        if (nsdManager != null) {
            nsdManager.discoverServices("_magicmirror._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }
    }

    private void updateMirrorInfo(String ip, String name) {
        txtConnectionStatus.setText("Connected to: " + name);
        setIndicatorState("connected");
        loading.setVisibility(View.VISIBLE);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("selectedMirrorIp", ip).putString("selectedMirrorName", name).apply();

        Request request = new Request.Builder()
                .url("http://" + ip + ":8081/status")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        handleMirrorDisconnect();
                        Toast.makeText(getContext(), "Failed to fetch mirror status", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String body = response.body() != null ? response.body().string() : "{}";
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(body);
                            txtMirrorId.setText("ID: " + json.optString("id", "-"));
                            txtIpAddress.setText("IP: " + json.optString("ip", ip));
                            txtUptime.setText("Uptime: " + json.optString("uptime", "-"));

                            if (json.has("modules") && json.get("modules") instanceof JSONArray) {
                                JSONArray modules = json.getJSONArray("modules");
                                StringBuilder modulesStr = new StringBuilder();
                                for (int i = 0; i < modules.length(); i++) {
                                    modulesStr.append("• ").append(modules.getString(i)).append("\n");
                                }
                                txtModules.setText(modulesStr.toString().trim());
                            } else {
                                txtModules.setText("-");
                            }

                            txtLastUpdate.setText("Last Update: " + json.optString("lastUpdate", "-"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void setIndicatorState(String state) {
        switch (state) {
            case "disconnected":
                connectionIndicator.setImageResource(R.drawable.indicator_red_gradient);
                break;
            case "searching":
                connectionIndicator.setImageResource(R.drawable.indicator_yellow_gradient);
                break;
            case "connected":
                connectionIndicator.setImageResource(R.drawable.indicator_green_gradient);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopDiscovery();
        handler.removeCallbacks(statusPoller);
    }

    private void stopDiscovery() {
        if (nsdManager != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}