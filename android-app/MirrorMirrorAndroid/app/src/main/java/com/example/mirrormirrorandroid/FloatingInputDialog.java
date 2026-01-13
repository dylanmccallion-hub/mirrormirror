package com.example.mirrormirrorandroid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class FloatingInputDialog extends DialogFragment {

    public interface OnSubmitListener {
        void onSubmit(String text, @Nullable String dueDate);
    }

    private OnSubmitListener listener;
    private String selectedDueDate = null;
    private int layoutResId; // <-- NEW

    // Constructor with layout
    public FloatingInputDialog(OnSubmitListener listener, int layoutResId) {
        this.listener = listener;
        this.layoutResId = layoutResId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate the layout you pass in
        View view = inflater.inflate(layoutResId, null);

        final EditText editText = view.findViewById(R.id.editTextItem);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);

        // Only try to find btnSetDueDate if it exists
        Button btnSetDueDate = view.findViewById(R.id.btnSetDueDate);
        if (btnSetDueDate != null) {
            btnSetDueDate.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                        R.style.DatePickerLight,
                        (dpView, year, month, dayOfMonth) -> {
                            selectedDueDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            });
        }

        btnSubmit.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty() && listener != null) {
                listener.onSubmit(text, selectedDueDate);
            }
            dismiss();
        });

        builder.setView(view);
        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        editText.requestFocus();
        editText.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }
}

