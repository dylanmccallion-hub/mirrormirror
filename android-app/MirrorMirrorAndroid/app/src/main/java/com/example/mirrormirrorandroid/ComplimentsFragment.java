package com.example.mirrormirrorandroid;

import android.content.Context;
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

    private Button btnAdd, submitButton;
    private ListView listView;
    private ArrayList<String> compliments;
    private ArrayAdapter<String> adapter;

    public ComplimentsFragment() {
        super(R.layout.fragment_compliments);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnAdd = view.findViewById(R.id.btnAdd);
        submitButton = view.findViewById(R.id.submitButton);
        listView = view.findViewById(R.id.listView);

        compliments = new ArrayList<>();

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

        // Add new compliment
        btnAdd.setOnClickListener(v -> {
            FloatingInputDialog dialog = new FloatingInputDialog((text, ignored) -> {
                compliments.add(text);
                adapter.notifyDataSetChanged();
            }, R.layout.floating_input_maps);
            dialog.show(getParentFragmentManager(), "floating_input_maps");
        });

        // Submit to Mirror
        submitButton.setOnClickListener(v -> {
            String mirrorIp = requireActivity()
                    .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                    .getString("selectedMirrorIp", null);

            if (mirrorIp == null) {
                Toast.makeText(requireContext(), "No mirror selected", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray jsonArray = new JSONArray();
            for (String comp : compliments) {
                jsonArray.put(comp);
            }

            JSONObject payload = new JSONObject();
            try {
                payload.put("list", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // POST to server
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

            Toast.makeText(requireContext(), "Compliments sent to Mirror", Toast.LENGTH_SHORT).show();
        });

        // Long press to delete
        listView.setOnItemLongClickListener((parent, v1, position, id) -> {
            String removed = compliments.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "Deleted: " + removed, Toast.LENGTH_SHORT).show();
            return true;
        });
    }
}