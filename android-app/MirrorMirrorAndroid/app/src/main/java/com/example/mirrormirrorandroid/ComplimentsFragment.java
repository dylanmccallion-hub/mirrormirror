package com.example.mirrormirrorandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ComplimentsFragment extends Fragment {

    private Button btnAdd;
    private ListView listView;
    private ArrayList<String> compliments;
    private ArrayAdapter<String> adapter;

    private SharedPreferences prefs;

    public ComplimentsFragment() {
        super(R.layout.fragment_compliments);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnAdd = view.findViewById(R.id.btnAdd);
        listView = view.findViewById(R.id.listView);
        prefs = requireActivity().getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE);

        // --- Load saved compliments ---
        compliments = new ArrayList<>();
        String savedJson = prefs.getString("complimentsList", null);
        if (savedJson != null) {
            try {
                JSONArray array = new JSONArray(savedJson);
                for (int i = 0; i < array.length(); i++) {
                    compliments.add(array.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        adapter = new ArrayAdapter<String>(requireContext(), R.layout.list_item_compliments, compliments) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View itemView = convertView;
                if (itemView == null) {
                    itemView = getLayoutInflater().inflate(R.layout.list_item_compliments, parent, false);
                }
                TextView title = itemView.findViewById(R.id.itemTitle);
                title.setText(compliments.get(position));
                return itemView;
            }
        };

        listView.setAdapter(adapter);

        // --- Add new compliment ---
        btnAdd.setOnClickListener(v -> {
            FloatingInputDialog dialog = new FloatingInputDialog((text, ignored) -> {
                compliments.add(text);
                adapter.notifyDataSetChanged();
                saveCompliments(); // save immediately
                sendToMirror();    // send immediately
            }, R.layout.floating_input_maps);
            dialog.show(getParentFragmentManager(), "floating_input_maps");
        });

        // --- Long press to delete ---
        listView.setOnItemLongClickListener((parent, v1, position, id) -> {
            String removed = compliments.remove(position);
            adapter.notifyDataSetChanged();
            saveCompliments();
            sendToMirror();
            Toast.makeText(requireContext(), "Deleted: " + removed, Toast.LENGTH_SHORT).show();
            return true;
        });

        // --- Send saved list to mirror immediately on launch ---
        sendToMirror();
    }

    private void saveCompliments() {
        JSONArray array = new JSONArray();
        for (String comp : compliments) array.put(comp);
        prefs.edit().putString("complimentsList", array.toString()).apply();
    }

    private void sendToMirror() {
        String mirrorIp = prefs.getString("selectedMirrorIp", null);
        if (mirrorIp == null || compliments.isEmpty()) return;

        JSONArray jsonArray = new JSONArray();
        for (String comp : compliments) jsonArray.put(comp);

        JSONObject payload = new JSONObject();
        try { payload.put("list", jsonArray); } catch (JSONException e) { e.printStackTrace(); }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + mirrorIp + ":8081/compliments");
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
    }
}
