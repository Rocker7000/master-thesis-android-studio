package com.example.myapplication;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class EnergyUsageRepository {

    private static EnergyUsageRepository instance;
    private final DatabaseReference databaseRef;
    private final Map<String, Float> dailyTotalsMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Float>> dailyDeviceUsageMap = new HashMap<>();
    private int daysLoaded = 0;

    private EnergyUsageRepository() {
        databaseRef = FirebaseDatabase.getInstance().getReference("houseEnergyUsage");
    }

    public static synchronized EnergyUsageRepository getInstance() {
        if (instance == null) {
            instance = new EnergyUsageRepository();
        }
        return instance;
    }

    public Map<String, Float> getDailyTotalsMap() {
        return dailyTotalsMap;
    }

    public Map<String, Map<String, Float>> getDailyDeviceUsageMap() {
        return dailyDeviceUsageMap;
    }

    public void loadAvailableDays(@NonNull Consumer<Integer> onDaysCountLoaded) {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int availableDays = (int) snapshot.getChildrenCount();
                int numDays = Math.min(availableDays, 30);  // Cap at 30 days
                onDaysCountLoaded.accept(numDays);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EnergyUsageRepository", "Failed to load days count", error.toException());
                onDaysCountLoaded.accept(0);  // If there’s an error, default to 0 days
            }
        });
    }

    public void loadLastNDaysData(int numDays, Runnable onComplete) {
        daysLoaded = 0;
        dailyTotalsMap.clear();
        dailyDeviceUsageMap.clear();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 0; day < numDays; day++) {
            String date = dateFormat.format(calendar.getTime());
            dailyTotalsMap.put(date, 0.0f);
            dailyDeviceUsageMap.put(date, new HashMap<>());

            databaseRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    float dailyTotal = 0.0f;
                    Map<String, Float> deviceUsage = dailyDeviceUsageMap.get(date);

                    if (snapshot.exists()) {
                        for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                            for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                                String deviceName = deviceSnapshot.getKey();
                                Double usage = deviceSnapshot.getValue(Double.class);

                                if (usage != null) {
                                    dailyTotal += usage;
                                    deviceUsage.put(deviceName, deviceUsage.getOrDefault(deviceName, 0f) + usage.floatValue());
                                }
                            }
                        }
                        dailyTotalsMap.put(date, dailyTotal);
                    }

                    daysLoaded++;
                    if (daysLoaded == numDays && onComplete != null) {
                        onComplete.run();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("EnergyUsageRepository", "Помилка завантаження даних", error.toException());
                }
            });

            calendar.add(Calendar.DATE, -1);
        }
    }
}