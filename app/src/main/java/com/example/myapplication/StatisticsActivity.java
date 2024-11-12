package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private BarChart barChart;
    private TextView tvDailySummary;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Set up the toolbar with title and back navigation
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Energy Statistics");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("houseEnergyUsage");

        // Initialize views
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
        tvDailySummary = findViewById(R.id.tv_daily_summary);

        // Fetch and display statistics from Firebase
        fetchAndDisplayStatistics();
    }

    // Handle toolbar back button click to navigate to MainActivity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(StatisticsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchAndDisplayStatistics() {
        // Get today's date and yesterday's date
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = today.format(formatter);
        String yesterdayDate = yesterday.format(formatter);

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    TreeMap<String, Float> dailyUsageMap = new TreeMap<>();
                    Map<String, Float> todayDeviceUsage = new TreeMap<>();
                    Map<String, Float> yesterdayDeviceUsage = new TreeMap<>();

                    for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                        String date = daySnapshot.getKey();
                        float totalUsageForDay = 0f;
                        Map<String, Float> deviceUsageMap = date.equals(todayDate) ? todayDeviceUsage : date.equals(yesterdayDate) ? yesterdayDeviceUsage : null;

                        for (DataSnapshot timeSnapshot : daySnapshot.getChildren()) {
                            for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                                String device = deviceSnapshot.getKey();
                                Float usage = deviceSnapshot.getValue(Float.class);
                                if (usage != null) {
                                    totalUsageForDay += usage;
                                    if (deviceUsageMap != null) {
                                        deviceUsageMap.put(device, deviceUsageMap.getOrDefault(device, 0f) + usage);
                                    }
                                }
                            }
                        }
                        dailyUsageMap.put(date, totalUsageForDay);
                    }

                    calculateDailySummary(dailyUsageMap);
                    setupLineChart(dailyUsageMap);
                    setupBarChart(todayDeviceUsage, yesterdayDeviceUsage);
                } else {
                    Toast.makeText(StatisticsActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StatisticsActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateDailySummary(Map<String, Float> dailyUsageMap) {
        if (dailyUsageMap.size() >= 2) {
            List<Float> usageValues = new ArrayList<>(dailyUsageMap.values());
            float todayUsage = usageValues.get(usageValues.size() - 1);
            float yesterdayUsage = usageValues.get(usageValues.size() - 2);

            float difference = ((todayUsage - yesterdayUsage) / yesterdayUsage) * 100;
            String summaryText = String.format("Energy usage today is %.1f%% %s than yesterday",
                    Math.abs(difference), difference < 0 ? "lower" : "higher");
            tvDailySummary.setText(summaryText);
        } else {
            tvDailySummary.setText("Insufficient data for comparison");
        }
    }

    private void setupLineChart(Map<String, Float> dailyUsageMap) {
        List<Entry> entries = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : dailyUsageMap.entrySet()) {
            entries.add(new Entry(index++, entry.getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Energy Usage");
        dataSet.setColor(ColorTemplate.rgb("#00000"));
        dataSet.setValueTextColor(android.graphics.Color.BLACK);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();
    }

    private void setupBarChart(Map<String, Float> todayUsage, Map<String, Float> yesterdayUsage) {
        List<BarEntry> todayEntries = new ArrayList<>();
        List<BarEntry> yesterdayEntries = new ArrayList<>();

        // Get all unique device names to ensure both today and yesterday are included
        TreeSet<String> allDevices = new TreeSet<>(todayUsage.keySet());
        allDevices.addAll(yesterdayUsage.keySet());

        int index = 0;
        for (String device : allDevices) {
            // Create entries, using 0f for any missing device data
            todayEntries.add(new BarEntry(index, todayUsage.getOrDefault(device, 0f)));
            yesterdayEntries.add(new BarEntry(index, yesterdayUsage.getOrDefault(device, 0f)));
            index++;
        }



        BarDataSet todayDataSet = new BarDataSet(todayEntries, "Today");
        todayDataSet.setColor(ColorTemplate.rgb("#0000CD"));
        BarDataSet yesterdayDataSet = new BarDataSet(yesterdayEntries, "Yesterday");
        yesterdayDataSet.setColor(ColorTemplate.rgb("#B0E0E6"));

        BarData barData = new BarData(todayDataSet, yesterdayDataSet);
        barData.setBarWidth(0.3f);  // Width for each bar
        barData.setValueTextSize(10f);
        // Configure chart to show groups for each device
        barChart.setData(barData);

        // X-Axis configuration for labels and positioning
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);  // Ensure each device gets a label
        xAxis.setCenterAxisLabels(true);  // Center labels between groups
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new ArrayList<>(allDevices)));

        // Group the bars (0f starting group, 0.4f group space, 0.05f bar space)
        barChart.groupBars(0f, 0.2f, 0.05f);

        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.invalidate();  // Refresh the chart
    }
}