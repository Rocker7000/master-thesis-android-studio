package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {


    private ImageView profileImageView;
    private TextView nicknameTextView, emailTextView, numberTextView;
    private EnergyUsageRepository userData ;
    private View contentLayout;
    private RecyclerView devicesRecyclerView;
    private DeviceAdapter deviceAdapter;
    private List<DeviceData> deviceList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);


        profileImageView = findViewById(R.id.profileImage);
        nicknameTextView = findViewById(R.id.nicknameText);
        emailTextView = findViewById(R.id.emailText);
        numberTextView = findViewById(R.id.phoneNumber);
        contentLayout = findViewById(R.id.contentLayout);
        devicesRecyclerView = findViewById(R.id.devicesListContainer);

        userData = EnergyUsageRepository.getInstance();
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, "kWt");
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesRecyclerView.setAdapter(deviceAdapter);

        loadUserData();
        loadDevicesData();

        findViewById(R.id.nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        });

        findViewById(R.id.nav_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });
    }

    private void loadUserData() {
        contentLayout.setVisibility(View.GONE);

        // Отримуємо дані користувача
        String nickname = userData.getUserNickname();
        String email = userData.getUserEmail();
        String photoUrl = userData.getUserPhotoUrl();
        String phoneNumber = userData.getUserPhoneNumber();

            // Відображаємо дані
        nicknameTextView.setText(nickname != null ? nickname : "Noname");
        emailTextView.setText(email != null ? "Email: " + email : " ");
        numberTextView.setText(phoneNumber != null ? "Phone: " + phoneNumber : " ");

        if (photoUrl != null) {
            // Завантажуємо фото профілю з використанням Glide
            Glide.with(UserActivity.this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile_placeholder) // Зображення за замовчуванням
                    .circleCrop()
                    .into(profileImageView);
        }
        showContentWithAnimation();
        if(nickname == null && email == null){
            Toast.makeText(UserActivity.this, "UserData is not loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDevicesData() {
        // Get daily device usage data from EnergyUsageRepository
        Map<String, Map<String, Float>> dailyDeviceUsageMap = userData.getDailyDeviceUsageMap();
        deviceList.clear();

        // Map to accumulate total usage per device
        Map<String, Float> totalUsageMap = new HashMap<>();
        int numberOfDays = dailyDeviceUsageMap.size(); // Total days in the map

        // Iterate through each day
        for (Map<String, Float> deviceUsageMap : dailyDeviceUsageMap.values()) {
            for (Map.Entry<String, Float> deviceEntry : deviceUsageMap.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Float usage = deviceEntry.getValue();

                // Accumulate usage per device
                totalUsageMap.put(deviceName, totalUsageMap.getOrDefault(deviceName, 0f) + usage);
            }
        }

        // Calculate average usage for each device and convert to kWh
        for (Map.Entry<String, Float> entry : totalUsageMap.entrySet()) {
            String deviceName = entry.getKey();
            Float totalUsage = entry.getValue();

            // Calculate average usage in kWh
            float averageUsageInKWh = (totalUsage / numberOfDays) / 1000f;

            // Add to the device list
            deviceList.add(new DeviceData(deviceName, averageUsageInKWh));
        }

        // Notify adapter about data changes
        deviceAdapter.notifyDataSetChanged();
        devicesRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showContentWithAnimation() {
        contentLayout.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(500);
        contentLayout.startAnimation(fadeIn);
    }

}

