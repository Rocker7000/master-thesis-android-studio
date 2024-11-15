package com.example.myapplication;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BarChart barChart;
    private View loadingView, contentLayout;
    private static final int TWO_WEEK_DAYS = 14;
    private EnergyUsageRepository energyUsageRepository;
    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        initViewComponents();

        energyUsageRepository = EnergyUsageRepository.getInstance();
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        saveUserToDatabaseIfNotExists(currentUser);
        setupNavigationButtons();
        observeData();
    }

    private void redirectToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void initViewComponents() {
        loadingView = findViewById(R.id.loading_view);
        contentLayout = findViewById(R.id.contentLayout);
        barChart = findViewById(R.id.barChart);

        loadingView.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void setupNavigationButtons() {
        findViewById(R.id.monitoring_button).setOnClickListener(v -> startActivity(new Intent(this, MonitoringActivity.class)));
        findViewById(R.id.statics_button).setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        findViewById(R.id.nav_home).setOnClickListener(v -> recreate());
        findViewById(R.id.nav_user).setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }


    private void observeData() {
        mainViewModel.getDailyTotalsMap().observe(this, new Observer<LinkedHashMap<String, Float>>() {
            @Override
            public void onChanged(LinkedHashMap<String, Float> dailyTotalsMap) {
                displayBarChart(dailyTotalsMap);
            }
        });

        loadingView.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        mainViewModel.loadLastMonthData();
    }

    private void saveUserToDatabaseIfNotExists(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("nickname", user.getDisplayName());
                    userData.put("email", user.getEmail());
                    userData.put("phone number", user.getPhoneNumber());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                    userRef.setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("MainActivity", "User data saved successfully.");
                        } else {
                            Log.e("MainActivity", "Error saving user data", task.getException());
                        }
                    });
                } else {
                    Log.d("MainActivity", "User already exists in the database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to read user data", error.toException());
            }
        });
    }

    private void displayBarChart(LinkedHashMap<String, Float> dailyTotalsMap) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();
        int index = 0;

        int size = dailyTotalsMap.size();
        int start = Math.max(0, size - TWO_WEEK_DAYS);

        ArrayList<String> keys = new ArrayList<>(dailyTotalsMap.keySet());
        ArrayList<Float> values = new ArrayList<>(dailyTotalsMap.values());

        for (int i = start; i < size; i++) {
            String date = keys.get(i);
            date = date.substring(8, 10) + "-" + date.substring(5, 7);
            Float value = values.get(i);
            if (value > 0) {
                barEntries.add(new BarEntry(index++, value));
                dateLabels.add(date);
            }
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "Total energy consumption (W)");
        barDataSet.setColor(0xFF2C7CD3);
        barDataSet.setValueTextSize(10f);

        barChart.setData(new BarData(barDataSet));
        barChart.invalidate();
        configureChart(dateLabels);

        loadingView.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void configureChart(ArrayList<String> dateLabels) {
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dateLabels.get((int) value);
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxis = barChart.getAxisLeft();
        YAxis yAxisRight = barChart.getAxisRight();
        yAxisRight.setEnabled(false);
        yAxis.setAxisMinimum(0f);
    }


}