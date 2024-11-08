package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class UserActivity extends AppCompatActivity {


    private Button addDeviceButton;
    private LinearLayout devicesListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        addDeviceButton = findViewById(R.id.addDeviceButton);
        devicesListContainer = findViewById(R.id.devicesListContainer);

        // Натискання на кнопку "Додати"
        addDeviceButton.setOnClickListener(v -> {
            addNewDevice("Device Name", "Additional Info");  // Додаємо новий пристрій із зразковими даними
            addDeviceButton.setVisibility(View.GONE);  // Приховуємо кнопку "Додати" після додавання пристрою
        });
    }

    // Метод для додавання нового пристрою
    private void addNewDevice(String deviceName, String additionalInfo) {
        // Створення вигляду для нового пристрою на основі макета device_item.xml
        View deviceView = getLayoutInflater().inflate(R.layout.device_item, devicesListContainer, false);

        TextView deviceNameText = deviceView.findViewById(R.id.deviceName);
        ImageButton deleteButton = deviceView.findViewById(R.id.deleteDeviceButton);

        // Встановлення тексту назви пристрою та додаткової інформації
        deviceNameText.setText(deviceName);

        // Обробник натискання кнопки "Видалити"
        deleteButton.setOnClickListener(v -> {
            devicesListContainer.removeView(deviceView);
            // Показати кнопку "Додати" знову, якщо немає більше пристроїв
            if (devicesListContainer.getChildCount() == 0) {
                addDeviceButton.setVisibility(View.VISIBLE);
            }
        });

        // Додаємо вигляд пристрою до контейнера та відображаємо його
        devicesListContainer.setVisibility(View.VISIBLE);
        devicesListContainer.addView(deviceView);
    }
}

