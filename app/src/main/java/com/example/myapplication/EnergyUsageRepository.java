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

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnergyUsageRepository {

    private static EnergyUsageRepository instance;
    private final DatabaseReference databaseRef;
    private final Map<String, Float> dailyTotalsMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Float>> dailyDeviceUsageMap = new HashMap<>();
    private final Map<String, Map<String, Float>> hourlyDevicesUsageMap = new HashMap<>();
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

        Query last30DaysQuery = databaseRef.child("users").child(user.getUid()).child("houseEnergyUsage").orderByKey().limitToLast(30);

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
                    Map<String, Float> deviceUsage = new HashMap<>();
                    Map<String, Float> hourlyUsage = new LinkedHashMap<>();

                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey();
                        int hour = Integer.parseInt(time.split(":")[0]); // Отримати годину
                        String hourKey = hour + ":00";

                        float hourlyTotal = 0.0f;

                        for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                            String deviceName = deviceSnapshot.getKey();
                            Double usage = deviceSnapshot.getValue(Double.class);

                            if (usage != null) {
                                dailyTotal += usage;
                                hourlyTotal += usage.floatValue();
                                deviceUsage.put(deviceName, deviceUsage.getOrDefault(deviceName, 0f) + usage.floatValue());
                            }
                        }

                        hourlyUsage.put(hourKey, hourlyUsage.getOrDefault(hourKey, 0f) + hourlyTotal);
                    }

                    dailyTotalsMap.put(date, dailyTotal);
                    dailyDeviceUsageMap.put(date, deviceUsage);
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
