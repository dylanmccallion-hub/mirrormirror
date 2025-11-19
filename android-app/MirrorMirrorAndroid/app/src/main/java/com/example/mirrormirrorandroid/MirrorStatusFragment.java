package com.example.mirrormirrorandroid;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MirrorStatusFragment extends Fragment {

    private TextView txtConnectionStatus, txtMirrorId, txtIpAddress, txtUptime, txtModules, txtLastUpdate;
    private View connectionIndicator;
    private ProgressBar loading;
    private ListView listViewMirrors;

    private NsdManager nsdManager;
    private AtomicBoolean mirrorFound = new AtomicBoolean(false);
    private List<String> mirrorNames = new ArrayList<>();
    private List<String> mirrorIps = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private OkHttpClient client = new OkHttpClient();

    public MirrorStatusFragment() {
        super(R.layout.fragment_mirror_status);
    }

    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override public void onDiscoveryStarted(String regType) {}
        @Override public void onServiceFound(final NsdServiceInfo service) {
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
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                String mirrorIp = serviceInfo.getHost().getHostAddress();

                // Save the discovered IP
                requireActivity().getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("selectedMirrorIp", mirrorIp)
                        .apply();

                // Update UI
                txtConnectionStatus.setText("Mirror Found: " + serviceInfo.getServiceName());
                connectionIndicator.setBackgroundColor(Color.GREEN);

                // Add to list and notify adapter
                mirrorNames.add(serviceInfo.getServiceName());
                mirrorIps.add(mirrorIp);
                adapter.notifyDataSetChanged();

                // Auto-select first discovered mirror
                if (!mirrorFound.getAndSet(true)) {
                    updateMirrorInfo(mirrorIp, serviceInfo.getServiceName());
                }
            });
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtConnectionStatus = view.findViewById(R.id.txtConnectionStatus);
        txtMirrorId = view.findViewById(R.id.txtMirrorId);
        txtIpAddress = view.findViewById(R.id.txtIpAddress);
        txtUptime = view.findViewById(R.id.txtUptime);
        txtModules = view.findViewById(R.id.txtModules);
        txtLastUpdate = view.findViewById(R.id.txtLastUpdate);
        connectionIndicator = view.findViewById(R.id.connectionIndicator);
        loading = view.findViewById(R.id.loading);
        listViewMirrors = view.findViewById(R.id.listViewMirrors);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, mirrorNames);
        listViewMirrors.setAdapter(adapter);

        listViewMirrors.setOnItemClickListener((parent, itemView, position, id) -> {
            String ip = mirrorIps.get(position);
            updateMirrorInfo(ip, mirrorNames.get(position));
        });

        nsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);
        startDiscovery();
    }

    private void startDiscovery() {
        mirrorFound.set(false);
        if (nsdManager != null) {
            nsdManager.discoverServices("_magicmirror._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            txtConnectionStatus.setText("Searching for mirrors...");
            setIndicatorColor(Color.YELLOW);
        }
    }

    private void updateMirrorInfo(String ip, String name) {
        txtConnectionStatus.setText("Mirror Found: " + name);
        setIndicatorColor(Color.GREEN);
        loading.setVisibility(View.VISIBLE);

        // Save selected IP
        requireActivity().getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("selectedMirrorIp", ip)
                .apply();

        // Fetch /status
        Request request = new Request.Builder()
                .url("http://" + ip + ":8081/status")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to fetch mirror status", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
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

                            txtLastUpdate.setText("Last update: " + json.optString("lastUpdate", "-"));

                        } catch (Exception e) {
                            e.printStackTrace();
                            txtModules.setText("-");
                            txtUptime.setText("-");
                            txtLastUpdate.setText("-");
                        } finally {
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopDiscovery();
    }

    private void stopDiscovery() {
        if (nsdManager != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void setIndicatorColor(int color) {
        connectionIndicator.setBackgroundColor(color);
    }
}
