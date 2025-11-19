package com.example.mirrormirrorandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class MirrorSettingsFragment extends Fragment {

    private ListView listView;
    private ArrayList<MirrorDevice> devices;
    private ArrayAdapter<MirrorDevice> adapter;
    private SharedPreferences prefs;

    public MirrorSettingsFragment() {
        super(R.layout.fragment_mirror_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.deviceList);
        prefs = requireActivity().getSharedPreferences("mirrors", Context.MODE_PRIVATE);

        // --- Mock devices for now ---
        devices = new ArrayList<>();
        devices.add(new MirrorDevice("MM-12345", "Living Room Mirror", "192.168.3.159", true));
        devices.add(new MirrorDevice("MM-67890", "Office Mirror", "192.168.3.160", false));

        adapter = new ArrayAdapter<MirrorDevice>(
                requireContext(),
                R.layout.list_item_mirror,
                R.id.mirrorName,
                devices
        );
        listView.setAdapter(adapter);

        // --- Handle mirror selection ---
        listView.setOnItemClickListener((parent, v, position, id) -> {
            MirrorDevice selected = devices.get(position);

            prefs.edit()
                    .putString("selectedMirrorId", selected.id)
                    .putString("selectedMirrorIp", selected.ip)
                    .apply();

            Toast.makeText(requireContext(),
                    "Connected to: " + selected.name + " (" + selected.id + ")",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
