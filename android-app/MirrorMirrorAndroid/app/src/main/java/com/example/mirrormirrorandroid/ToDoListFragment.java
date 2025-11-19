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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ToDoListFragment extends Fragment {

    private Button btnAdd, submitListButton;
    private ListView listView;
    private ArrayList<ToDoItem> items;
    private ArrayAdapter<ToDoItem> adapter;

    public ToDoListFragment() {
        super(R.layout.fragment_todo_list);
    }

    // --- Simple class to hold item text + optional due date ---
    private static class ToDoItem {
        String title;
        @Nullable
        String dueDate;

        ToDoItem(String title, @Nullable String dueDate) {
            this.title = title;
            this.dueDate = dueDate;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnAdd = view.findViewById(R.id.btnAdd);
        submitListButton = view.findViewById(R.id.submitListButton);
        listView = view.findViewById(R.id.listView);

        items = new ArrayList<>();

        adapter = new ArrayAdapter<ToDoItem>(requireContext(), R.layout.list_item, items) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View itemView = convertView;
                if (itemView == null) {
                    itemView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
                }

                ToDoItem item = getItem(position);

                TextView title = itemView.findViewById(R.id.itemTitle);
                TextView due = itemView.findViewById(R.id.itemDueDate);

                title.setText(item.title);

                if (item.dueDate != null && !item.dueDate.isEmpty()) {
                    due.setText("Due Date: " + item.dueDate);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        Date dueDate = sdf.parse(item.dueDate);
                        Date today = sdf.parse(sdf.format(new Date())); // today with time stripped

                        // calculate tomorrow by adding one day in milliseconds
                        Date tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);

                        if (dueDate != null) {
                            if (!dueDate.after(today)) {
                                // due today or past -> red
                                due.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                            } else if (dueDate.equals(tomorrow)) {
                                // due tomorrow -> yellow
                                due.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                            } else {
                                // future beyond tomorrow -> white
                                due.setTextColor(getResources().getColor(R.color.white));
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        due.setTextColor(getResources().getColor(R.color.white));
                    }
                } else {
                    due.setText("");
                }


                return itemView;
            }
        };


        listView.setAdapter(adapter);

        // --- Add button launches floating input dialog ---
        btnAdd.setOnClickListener(v -> {
            FloatingInputDialog dialog = new FloatingInputDialog((text, dueDate) -> {
                items.add(new ToDoItem(text, dueDate));
                adapter.notifyDataSetChanged();
            });
            dialog.show(getParentFragmentManager(), "floating_input");
        });

        submitListButton.setOnClickListener(v -> {
            // Get selected mirror IP from SharedPreferences
            String mirrorIp = requireActivity()
                    .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                    .getString("selectedMirrorIp", null);

            if (mirrorIp == null) {
                Toast.makeText(requireContext(), "No mirror selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare JSON array of tasks
            JSONArray jsonArray = new JSONArray();
            for (ToDoItem item : items) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("title", item.title);
                    obj.put("dueDate", item.dueDate != null ? item.dueDate : JSONObject.NULL);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(obj);
            }

            JSONObject payload = new JSONObject();
            try {
                payload.put("list", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Send REST POST to the discovered mirror
            new Thread(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("http://" + mirrorIp + ":8081/todolist");
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

            Toast.makeText(requireContext(), "List sent to Mirror", Toast.LENGTH_SHORT).show();
        });


        // --- Long press to delete items ---
        listView.setOnItemLongClickListener((parent, v1, position, id) -> {
            ToDoItem removed = items.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "Deleted: " + removed.title, Toast.LENGTH_SHORT).show();
            return true;
        });
    }
}
