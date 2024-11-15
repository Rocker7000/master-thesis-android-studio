package com.example.myapplication;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private BarChart barChart;
    private TextView tvDailySummary;
    private EnergyUsageRepository energyUsageRepository;
    private TextView btnSelectValue;
    List<String> availableDates;

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

        // Initialize EnergyUsageRepository instance
        energyUsageRepository = EnergyUsageRepository.getInstance();
        availableDates = new ArrayList<>(energyUsageRepository.getDailyTotalsMap().keySet());
        // Initialize views
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
        tvDailySummary = findViewById(R.id.tv_daily_summary);

        // Fetch and display statistics using the repository


        // Initialize the button
        btnSelectValue = findViewById(R.id.btn_select_value);

        //set text to button to yesterday
        if (availableDates.size() >= 2) {
            btnSelectValue.setText(availableDates.get(availableDates.size() - 2));
        }
        updateCharts(availableDates.get(availableDates.size() - 2));
        // Set up click listener to show the popup menu
        btnSelectValue.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View view) {
        // Create a PopupMenu
        PopupMenu popupMenu = new PopupMenu(this, view);
        for (int i = 0; i < availableDates.size(); i++) {
            popupMenu.getMenu().add(0, i, 0, availableDates.get(i));
        }

        // Set click listener for menu items
        popupMenu.setOnMenuItemClickListener(item -> {
            String selectedDate = availableDates.get(item.getItemId());
            btnSelectValue.setText(selectedDate);
            updateCharts(selectedDate);
            return true;
        });

        // Show the popup menu
        popupMenu.show();
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

    private void updateCharts(String selectedDate) {
        // Get the daily totals map from the repository
        LinkedHashMap<String, Float> dailyTotalsMap = new LinkedHashMap<>(energyUsageRepository.getDailyTotalsMap());

        // Get the daily device usage map from the repository
        Map<String, Map<String, Float>> dailyDeviceUsageMap = energyUsageRepository.getDailyDeviceUsageMap();

        // Get the last two days from the LinkedHashMap
        List<String> keys = new ArrayList<>(dailyTotalsMap.keySet());
        if (keys.size() >= 2) {
            String todayDate = keys.get(keys.size()-1);

            // Calculate and display daily summary
            calculateDailySummary(dailyTotalsMap, todayDate, selectedDate);

            // Setup line chart with daily totals
            setupLineChart(dailyTotalsMap);

            // Setup bar chart with today's and yesterday's device usage
            Map<String, Float> todayUsage = dailyDeviceUsageMap.get(todayDate);
            Map<String, Float> yesterdayUsage = dailyDeviceUsageMap.get(selectedDate);
            setupBarChart(todayUsage, yesterdayUsage);
        } else {
            Toast.makeText(this, "Insufficient data for statistics", Toast.LENGTH_SHORT).show();
        }
    }



    private void calculateDailySummary(Map<String, Float> dailyUsageMap, String todayDate, String yesterdayDate) {
        Float todayUsage = dailyUsageMap.get(todayDate);
        Float yesterdayUsage = dailyUsageMap.get(yesterdayDate);

        if (todayUsage != null && yesterdayUsage != null && yesterdayUsage > 0) {
            float difference = ((todayUsage - yesterdayUsage) / yesterdayUsage) * 100;
            boolean isLess = difference < 0;
            String statusText = isLess ? "less" : "higher";

            // Create summary text with both the percentage and status word highlighted
            String summaryText = String.format("Energy usage today is %.1f%% %s than yesterday",
                    Math.abs(difference), statusText);

            Spannable spannable = new SpannableString(summaryText);

            // Highlight percentage and status word
            int percentStart = summaryText.indexOf(String.format("%.1f%%", Math.abs(difference)));
            int percentEnd = percentStart + String.format("%.1f%%", Math.abs(difference)).length();
            int statusStart = summaryText.indexOf(statusText);
            int statusEnd = statusStart + statusText.length();

            int color = isLess ? Color.GREEN : Color.RED;
            spannable.setSpan(new ForegroundColorSpan(color), percentStart, percentEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(color), statusStart, statusEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvDailySummary.setText(spannable);
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
        dataSet.setColor(ColorTemplate.rgb("#000000"));
        dataSet.setValueTextColor(Color.BLACK);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();
    }

    private void setupBarChart(Map<String, Float> todayUsage, Map<String, Float> yesterdayUsage) {
        if (todayUsage == null || yesterdayUsage == null) return;

        List<BarEntry> todayEntries = new ArrayList<>();
        List<BarEntry> yesterdayEntries = new ArrayList<>();

        // Get all unique device names for consistent indexing in the chart
        TreeSet<String> allDevices = new TreeSet<>(todayUsage.keySet());
        allDevices.addAll(yesterdayUsage.keySet());

        int index = 0;
        for (String device : allDevices) {
            todayEntries.add(new BarEntry(index, todayUsage.getOrDefault(device, 0f)));
            yesterdayEntries.add(new BarEntry(index, yesterdayUsage.getOrDefault(device, 0f)));
            index++;
        }

        BarDataSet todayDataSet = new BarDataSet(todayEntries, "Today");
        todayDataSet.setColor(ColorTemplate.rgb("#0000CD"));
        BarDataSet yesterdayDataSet = new BarDataSet(yesterdayEntries, "Yesterday");
        yesterdayDataSet.setColor(ColorTemplate.rgb("#B0E0E6"));

        BarData barData = new BarData(todayDataSet, yesterdayDataSet);
        barData.setBarWidth(0.3f);

        barChart.setData(barData);

        // X-Axis configuration for device names
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new ArrayList<>(allDevices)));

        barChart.groupBars(0f, 0.2f, 0.05f);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.invalidate();
    }
}