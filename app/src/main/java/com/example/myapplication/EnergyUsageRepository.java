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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EnergyUsageRepository {

    private static EnergyUsageRepository instance;
    private final DatabaseReference databaseRef;
    private final Map<String, Float> dailyTotalsMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Float>> dailyDeviceUsageMap = new HashMap<>();
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

                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
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
                    dailyDeviceUsageMap.put(date, deviceUsage);
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
