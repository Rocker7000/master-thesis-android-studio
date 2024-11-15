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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private EnergyUsageRepository energyUsageRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        // Initialize EnergyUsageRepository
        energyUsageRepository = EnergyUsageRepository.getInstance();

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

        // Load data through the repository and update UI
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
        // Get the daily totals map from the repository
        Map<String, Float> dailyTotalsMap = energyUsageRepository.getDailyTotalsMap();

        // Convert the map to a LinkedHashMap to maintain insertion order
        LinkedHashMap<String, Float> linkedDailyTotalsMap = new LinkedHashMap<>(dailyTotalsMap);

        // Get the last day from the LinkedHashMap
        List<String> keys = new ArrayList<>(linkedDailyTotalsMap.keySet());
        if (!keys.isEmpty()) {
            String lastDate = keys.get(keys.size() - 1);
            Map<String, Float> deviceUsageMap = energyUsageRepository.getDailyDeviceUsageMap().get(lastDate);

            if (deviceUsageMap != null) {
                List<DeviceData> devices = new ArrayList<>();
                float totalUsage = linkedDailyTotalsMap.get(lastDate);

                for (Map.Entry<String, Float> entry : deviceUsageMap.entrySet()) {
                    String deviceName = entry.getKey();
                    Float usage = entry.getValue();
                    if (usage != null && totalUsage != 0) {
                        float usagePercentage = (usage / totalUsage) * 100;
                        devices.add(new DeviceData(deviceName, usagePercentage));
                    }
                }

                setupPieChart(devices);
                setupRecyclerView(devices);
            } else {
                Toast.makeText(this, "No data available for the last day", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView(List<DeviceData> devices) {
        deviceAdapter = new DeviceAdapter(devices, "percentage");
        recyclerView.setAdapter(deviceAdapter);
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
