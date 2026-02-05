package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class DashboardFragment extends Fragment {

    private TextView tvStreakTimer, restoresLeftView;
    private ProgressBar streakProgressBar;
    private Button btnRestore;
    private Spinner spinnerStreaks;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvStreakTimer = view.findViewById(R.id.tvStreakTimer);
        streakProgressBar = view.findViewById(R.id.streakProgressBar);
        btnRestore = view.findViewById(R.id.btnRestore);
        restoresLeftView = view.findViewById(R.id.restoresLeft);
        spinnerStreaks = view.findViewById(R.id.spinnerStreaks);

        int restoresLeft = 0; // Sample value
        updateRestoreUI(restoresLeft);

        // Spinner selection listener
        spinnerStreaks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                Toast.makeText(getContext(), "Showing: " + selected + " streak", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Countdown timer
        long totalTime = 24 * 60 * 60 * 1000; // 24 hours
        new CountDownTimer(totalTime, 1000) {
            public void onTick(long millisUntilFinished) {
                int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                int seconds = (int) ((millisUntilFinished / 1000) % 60);
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                tvStreakTimer.setText(time);
                streakProgressBar.setProgress((int) (millisUntilFinished * 100 / totalTime));
            }
            public void onFinish() {
                tvStreakTimer.setText("00:00:00");
                streakProgressBar.setProgress(0);
            }
        }.start();

        // Restore button
        btnRestore.setOnClickListener(v -> {
            if (btnRestore.isEnabled()) {
                Toast.makeText(getContext(), "Gym streak restored successfully! 🔥", Toast.LENGTH_SHORT).show();
                updateRestoreUI(0);
            } else {
                Toast.makeText(getContext(), "No restores left this month!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateRestoreUI(int restoresLeft) {
        if (restoresLeft > 0) {
            btnRestore.setEnabled(true);
            btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.holo_green_dark));
            restoresLeftView.setText("Restores left for this month: " + restoresLeft);
            restoresLeftView.setTextColor(getContext().getColor(android.R.color.holo_green_dark));
        } else {
            btnRestore.setEnabled(false);
            btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.darker_gray));
            restoresLeftView.setText("Restores left for this month: 0");
            restoresLeftView.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
        }
    }
}
