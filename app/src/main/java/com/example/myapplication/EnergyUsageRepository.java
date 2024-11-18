package com.example.myapplication;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnergyUsageRepository {

    private static EnergyUsageRepository instance;
    private final DatabaseReference databaseRef;
    private final Map<String, Float> dailyTotalsMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Float>> dailyDeviceUsageMap = new HashMap<>();
    private final Map<String, Map<String, Float>> hourlyDevicesUsageMap = new HashMap<>();
    private final Map<String, Integer> deviceUsageCountMap = new HashMap<>();
    private String userNickname, userEmail, userPhoneNumber, userPhotoUrl;
    private boolean isDataLoaded = false;
    private FirebaseUser user;

    private EnergyUsageRepository() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    public static synchronized EnergyUsageRepository getInstance() {
        if (instance == null) {
            instance = new EnergyUsageRepository();
        }
        return instance;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public String getUserPhotoUrl() {
        return userPhotoUrl;
    }

    public Map<String, Float> getDailyTotalsMap() {
        return dailyTotalsMap;
    }

    public Map<String, Map<String, Float>> getDailyDeviceUsageMap() {
        return dailyDeviceUsageMap;
    }

    public Map<String, Map<String, Float>> getHourlyDevicesUsageMap() {
        return hourlyDevicesUsageMap;
    }

    public Map<String, Float> getWeeklyTotalsMap() {
        Map<String, Float> dailyTotalsMap = getDailyTotalsMap();
        Map<String, Float> weeklyTotalsMap = new LinkedHashMap<>();

        for (Map.Entry<String, Float> entry : dailyTotalsMap.entrySet()) {
            String date = entry.getKey();
            float usage = entry.getValue();
            String weekKey = getWeekKey(date);
            weeklyTotalsMap.put(weekKey, weeklyTotalsMap.getOrDefault(weekKey, 0f) + usage);
        }

        return weeklyTotalsMap;
    }


    public Map<String, Float> getMonthlyTotalsMap() {
        Map<String, Float> dailyTotalsMap = getDailyTotalsMap();
        Map<String, Float> monthlyTotalsMap = new LinkedHashMap<>();

        for (Map.Entry<String, Float> entry : dailyTotalsMap.entrySet()) {
            String date = entry.getKey();
            float usage = entry.getValue();
            String monthKey = getMonthKey(date);
            monthlyTotalsMap.put(monthKey, monthlyTotalsMap.getOrDefault(monthKey, 0f) + usage);
        }

        return monthlyTotalsMap;
    }


    private String getWeekKey(String date) {
        // Example: Assuming date is in "yyyy-MM-dd" format
        // Extract the year and the week number
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(5, 7)) - 1,
                Integer.parseInt(date.substring(8, 10)));
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-W" + weekOfYear;
    }

    private String getMonthKey(String date) {
        // Example: Assuming date is in "yyyy-MM-dd" format
        // Extract the year and the month
        String year = date.substring(0, 4);
        String month = date.substring(5, 7);
        return year + "-" + month;
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    public List<Map<String, Object>> getDeviceStatistics() {
        List<Map<String, Object>> deviceStatisticsList = new ArrayList<>();
        Map<String, Map<String, Float>> dailyDeviceUsageMap = getDailyDeviceUsageMap();

        for (Map.Entry<String, Map<String, Float>> entry : dailyDeviceUsageMap.entrySet()) {
            for (Map.Entry<String, Float> deviceEntry : entry.getValue().entrySet()) {
                String deviceName = deviceEntry.getKey();
                Float usage = deviceEntry.getValue();
                if (usage > 0) {
                    deviceStatisticsList.add(Map.of("device", deviceName, "usage", usage));
                }
            }
        }

        Map<String, List<Float>> deviceUsagesMap = new HashMap<>();
        for (Map<String, Object> entry : deviceStatisticsList) {
            String device = (String) entry.get("device");
            Float usage = (Float) entry.get("usage");
            deviceUsagesMap.computeIfAbsent(device, k -> new ArrayList<>()).add(usage);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : deviceUsagesMap.entrySet()) {
            String device = entry.getKey();
            List<Float> usages = entry.getValue();
            Collections.sort(usages);

            float average = averageConsumption(usages);
            float standardDeviation = standardDeviation(usages, average);
            float min = min(usages);
            float percentile25 = percentile(usages, 25);
            float median = percentile(usages, 50);
            float percentile75 = percentile(usages, 75);
            float max = max(usages);

            result.add(Map.of(
                    "device", device,
                    "count", getDeviceUsageInTimeFormat(device),
                    "average", average,
                    "standardDeviation", standardDeviation,
                    "min", min,
                    "percentile25", percentile25,
                    "median", median,
                    "percentile75", percentile75,
                    "max", max
            ));
        }

        return result;
    }


    private String getDeviceUsageInTimeFormat(String device) {
        // Example: return in "hour:minute" format
        // if count if 1 then it means that device was used for 10 minutes

        int averageCount = (deviceUsageCountMap.getOrDefault(device, 0) / dailyTotalsMap.size());
        int hours = averageCount / 6;
        int minutes = (averageCount % 6) * 10;

        return hours + ":" + (minutes >= 10 ? minutes : minutes + "0");


    }

    private float averageConsumption(List<Float> usages) {
        float sum = 0;
        for (Float usage : usages) {
            sum += usage;
        }
        return sum / usages.size();
    }

    private float standardDeviation(List<Float> usages, float average) {
        float sumSquaredDifferences = 0;
        for (Float usage : usages) {
            sumSquaredDifferences += Math.pow(usage - average, 2);
        }
        return (float) Math.sqrt(sumSquaredDifferences / usages.size());
    }

    private float min(List<Float> usages) {
        return Collections.min(usages);
    }

    private float percentile(List<Float> usages, float percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * usages.size()) - 1;
        return usages.get(index);
    }

    private float max(List<Float> usages) {
        return Collections.max(usages);
    }


    public void loadLastMonth(Runnable onComplete) {
        if (user == null) {
            Log.e("EnergyUsageRepository", "Користувач не авторизований");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        if (isDataLoaded) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        dailyTotalsMap.clear();
        dailyDeviceUsageMap.clear();
        hourlyDevicesUsageMap.clear();

        Query last30DaysQuery = databaseRef.child("users").child(user.getUid()).child("houseEnergyUsage").orderByKey();

        databaseRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userNickname = snapshot.child("nickname").getValue(String.class);
                    userEmail = snapshot.child("email").getValue(String.class);
                    userPhoneNumber = snapshot.child("phone number").getValue(String.class);
                    userPhotoUrl = snapshot.child("photoUrl").getValue(String.class);
                } else {
                    Log.w("EnergyUsageRepository", "User profile data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EnergyUsageRepository", "Failed to load user profile data", error.toException());
            }
        });

        //fetch user data from database
        last30DaysQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    float dailyTotal = 0.0f;

                    Map<String, Float> deviceTotalUsage = new HashMap<>();
                    Map<String, Float> hourlyUsage = new LinkedHashMap<>();

                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey();
                        int hour = Integer.parseInt(time.split(":")[0]); // Отримати годину
                        String hourKey = hour + ":00";

                        float hourlyTotal = 0.0f;

                        for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                            String deviceName  = deviceSnapshot.getKey();
                            Double usage = deviceSnapshot.getValue(Double.class);

                            if (usage != null) {
                                dailyTotal += usage;
                                hourlyTotal += usage.floatValue();
                                if (usage != 0) {
                                    deviceUsageCountMap.put(deviceName, deviceUsageCountMap.getOrDefault(deviceName, 0) + 1);
                                }
                                deviceTotalUsage.put(deviceName, deviceTotalUsage.getOrDefault(deviceName, 0f) + usage.floatValue());
                            }
                        }
                        hourlyUsage.put(hourKey, hourlyUsage.getOrDefault(hourKey, 0f) + hourlyTotal);
                    }

                    dailyTotalsMap.put(date, dailyTotal);
                    dailyDeviceUsageMap.put(date, deviceTotalUsage);
                    hourlyDevicesUsageMap.put(date, hourlyUsage);
                }

                isDataLoaded = true;
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EnergyUsageRepository", "Помилка завантаження даних", error.toException());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
}
