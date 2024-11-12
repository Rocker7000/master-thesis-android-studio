package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("houseEnergyUsage");

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Energy Monitoring");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        pieChart = findViewById(R.id.pieChart);
        recyclerView = findViewById(R.id.rvDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch and display data from Firebase
        fetchAndDisplayData();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate to MainActivity
            Intent intent = new Intent(MonitoringActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close MonitoringActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void fetchAndDisplayData() {
        String dateKey = "2024-11-04";  // Define the date key to fetch data for
        Map<String, Float> deviceUsageMap = new HashMap<>();  // Accumulator for each device's total usage

        // Fetch data for the specified date
        database.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                        // Loop through each time entry
                        for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                            String device = deviceSnapshot.getKey();
                            Float usage = deviceSnapshot.getValue(Float.class);
                            if (device != null && usage != null) {
                                // Accumulate usage for each device
                                deviceUsageMap.put(device, deviceUsageMap.getOrDefault(device, 0f) + usage);
                            }
                        }
                    }

                    // Calculate total usage to determine percentages
                    float totalUsage = 0f;
                    for (Float usage : deviceUsageMap.values()) {
                        totalUsage += usage;
                    }

                    // Prepare list of DeviceData for adapter and pie chart
                    List<DeviceData> deviceDataList = new ArrayList<>();
                    for (Map.Entry<String, Float> entry : deviceUsageMap.entrySet()) {
                        float usagePercentage = (entry.getValue() / totalUsage) * 100;
                        deviceDataList.add(new DeviceData(entry.getKey(), usagePercentage));
                    }

                    // Update PieChart and RecyclerView
                    setupPieChart(deviceDataList);
                    deviceAdapter = new DeviceAdapter(deviceDataList);
                    recyclerView.setAdapter(deviceAdapter);
                } else {
                    Toast.makeText(MonitoringActivity.this, "No data found for date: " + dateKey, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MonitoringActivity", "Failed to read data from Firebase", error.toException());
            }
        });
    }

    private void setupPieChart(List<DeviceData> devices) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (DeviceData device : devices) {
            entries.add(new PieEntry(device.getUsagePercentage(), device.getName()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Device Usage");

        colors.add(ColorTemplate.rgb("#000080"));
        colors.add(ColorTemplate.rgb("#0000CD"));
        colors.add(ColorTemplate.rgb("#0000FF"));
        colors.add(ColorTemplate.rgb("#1E90FF"));
        colors.add(ColorTemplate.rgb("#00BFFF"));
        colors.add(ColorTemplate.rgb("#87CEFA"));
        colors.add(ColorTemplate.rgb("#B0E0E6"));

        dataSet.setColors(colors);
        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setEntryLabelColor(Color.parseColor("#FFA500"));
        pieChart.setDescription(null);
        pieChart.setCenterText("Energy Usage");
        pieChart.animateY(1000);
        pieChart.invalidate(); // Refresh chart
    }
}
