package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//TODO
// 1.setup lineChart peak usage times for each device
// 2.connect chatGpt Response to the app
// 3. setup the chatGpt response to be displayed in the Tips and Notifications CardViews


public class AnalysisActivity extends AppCompatActivity {

    private TextView usagePerDayTextView;
    private TextView averageUsagePerDayTextView;
    private BarChart barChart;
    private TableLayout tableLayout;
    private EnergyUsageRepository energyUsageRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        usagePerDayTextView = findViewById(R.id.usage_per_day);
        averageUsagePerDayTextView = findViewById(R.id.average_usage_per_day);
        barChart = findViewById(R.id.barChart);

        tableLayout = findViewById(R.id.tableLayout);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Energy Analysis");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        energyUsageRepository = EnergyUsageRepository.getInstance();

        List<Map<String, Object>> deviceStatisticsList = energyUsageRepository.getDeviceStatistics();
        populateTable(deviceStatisticsList);

        updateUI();
        setupBarChart();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate to MainActivity
            Intent intent = new Intent(AnalysisActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close MonitoringActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateUI() {
        Map<String, Float> dailyTotalsMap = energyUsageRepository.getDailyTotalsMap();
        float totalUsage = 0;
        int daysCount = dailyTotalsMap.size();

        for (float usage : dailyTotalsMap.values()) {
            totalUsage += usage;
        }

        totalUsage /= 1000; // Convert to kWh
        float averageUsage = (daysCount > 0 ? totalUsage / daysCount : 0);

        String mostRecentDate = dailyTotalsMap.keySet().stream().reduce((first, second) -> second).orElse("");
        float mostRecentUsage = (dailyTotalsMap.getOrDefault(mostRecentDate, 0f)/ 1000);

        usagePerDayTextView.setText(String.format("%.2f kWh", mostRecentUsage));
        averageUsagePerDayTextView.setText(String.format("%.2f kWh", averageUsage));

    }

    private void populateTable(List<Map<String, Object>> deviceStatisticsList) {
        // Clear existing rows
        tableLayout.removeAllViews();

        // Add header row
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));

        addTextView(headerRow, "Device", 1);
        addTextView(headerRow, "Avarage Time", 1);
        addTextView(headerRow, "Average (Vt)", 1);
        addTextView(headerRow, "Standard Deviation", 1);
        addTextView(headerRow, "Min (Vt)", 1);
        addTextView(headerRow, "25th Percentile", 1);
        addTextView(headerRow, "Median (Vt)", 1);
        addTextView(headerRow, "75th Percentile", 1);
        addTextView(headerRow, "Max (Vt)", 1);

        tableLayout.addView(headerRow);

        // Add data rows
        for (int i = 0; i < deviceStatisticsList.size(); i++) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            Map<String, Object> deviceStatistics = deviceStatisticsList.get(i);
            addTextView(row, (String) deviceStatistics.get("device"), i);
            addTextView(row, deviceStatistics.get("count").toString(), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("average")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("standardDeviation")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("min")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("percentile25")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("median")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("percentile75")), i);
            addTextView(row, String.format("%.2f", deviceStatistics.get("max")), i);

            tableLayout.addView(row);
        }
    }

    private void addTextView(TableRow row, String text, int index) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        textView.setPadding(8, 8, 8, 8);

        if (index % 2 == 0) {
            textView.setBackground(getResources().getDrawable(R.drawable.table_style_background_gray));

        } else {
            textView.setBackground(getResources().getDrawable(R.drawable.table_style_background_white));
        }

        textView.setText(text);
        row.addView(textView);
    }

    private void setupBarChart() {
        Map<String, Map<String, Float>> hourlyDevicesUsageMap = energyUsageRepository.getHourlyDevicesUsageMap();
        List<BarEntry> entries = new ArrayList<>();
        List<String> DeviceNames = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Map<String, Float>> entry : hourlyDevicesUsageMap.entrySet()) {
            String hour = entry.getKey();
            Map<String, Float> deviceUsageMap = entry.getValue();
            float[] usages = new float[5];
            int deviceIndex = 0;

            for (Map.Entry<String, Float> deviceEntry : deviceUsageMap.entrySet()) {
                if (deviceIndex < 5) {
                    usages[deviceIndex] = deviceEntry.getValue();
                    deviceIndex++;
                }
            }

            entries.add(new BarEntry(index++, usages));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Device Usage");
        dataSet.setColors(new int[]{
                Color.parseColor("#8cebff"),
                Color.parseColor("#c6ff8c"),
                Color.parseColor("#ffd38c"),
                Color.parseColor("#fff78c"),
                Color.parseColor("#ff8c8c")
        });

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate();
    }
}