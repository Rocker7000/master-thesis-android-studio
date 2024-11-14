package com.example.myapplication;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class FullDayEnergyUsageSimulator {

    private DatabaseReference databaseRef;
    private Random random;
    private FirebaseUser user;

    public FullDayEnergyUsageSimulator() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        random = new Random();
    }

    public void simulateDailyUsageFor(int day) {
        if (user == null) {
            Log.e("FirebaseEnergyUsage", "Користувач не авторизований");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)); // Поточний рік
        calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH)); // Поточний місяць
        calendar.set(Calendar.DAY_OF_MONTH, day);

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);

        // Налаштування базового споживання для кожного пристрою
        double refrigeratorUsage = 0.86 * 1000 / 144; // Середнє споживання холодильника за 10 хвилин
        double computerBaseUsage = 240 / 6.0;         // Базове споживання комп'ютера на 10 хвилин
        double microwaveBaseUsage = 1000 / 6.0;       // Базове споживання мікрохвильовки на 10 хвилин
        double lightingBaseUsage = 5 * 10 / 6.0;      // Базове споживання 5 лампочок

        int microwaveCount = 0;
        int washingMachineHour = random.nextInt(16) + 6;  // Пральна машинка включається раз на добу в ранковий або денний час
        int computerStartMorning = 6;                     // Комп'ютер працює зранку
        int computerStartEvening = 18 + random.nextInt(2); // Вечірнє включення комп'ютера
        int computerEndEvening = 23;

        // Дані пристроїв
        HashMap<String, String> devices = new HashMap<>();
        devices.put("refrigerator", "refrigerator");
        devices.put("computer", "computer");
        devices.put("microwave", "microwave");
        devices.put("washing_machine", "washing machine");
        devices.put("lighting", "lighting");

        // Записуємо дані пристроїв у Firebase
        databaseRef.child("users").child(user.getUid()).child("Devices").setValue(devices)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseEnergyUsage", "Дані пристроїв успішно збережено");
                    } else {
                        Log.e("FirebaseEnergyUsage", "Помилка запису пристроїв", task.getException());
                    }
                });

        for (int i = 0; i < 144; i++) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Дані споживання
            HashMap<String, Double> usageData = new HashMap<>();

            // Холодильник: постійне споживання з невеликим випадковим відхиленням
            usageData.put("refrigerator", refrigeratorUsage * (0.9 + 0.2 * random.nextDouble()));

            // Комп'ютер: варіації споживання згідно з вашим графіком
            if ((hour == computerStartMorning && minute < 40) ||
                    (hour >= computerStartEvening && hour < computerEndEvening)) {
                usageData.put("computer", computerBaseUsage * (0.8 + 0.4 * random.nextDouble()));
            } else {
                usageData.put("computer", 0.0);
            }

            // Мікрохвильова піч: вмикається тричі на день у випадкові моменти
            if (microwaveCount < 3 && hour >= 7 && hour <= 22 && random.nextInt(144) < 3) {
                usageData.put("microwave", microwaveBaseUsage * (0.7 + 0.6 * random.nextDouble()));
                microwaveCount++;
            } else {
                usageData.put("microwave", 0.0);
            }

            // Пральна машинка: працює протягом години один раз на кілька днів
            if (hour == washingMachineHour && minute < 60) {
                usageData.put("washing_machine", (800.0 / 6) * (0.8 + 0.4 * random.nextDouble()));
            } else {
                usageData.put("washing_machine", 0.0);
            }

            // Освітлення: різні лампочки працюють випадковий час, з варіаціями потужності
            double lightingUsage = 0.0;
            if (hour >= 6 && hour < 8) { // Ранок
                lightingUsage = lightingBaseUsage * random.nextDouble();
            } else if (hour >= 8 && hour < 18) { // День
                lightingUsage = lightingBaseUsage * (random.nextDouble() < 0.1 ? random.nextDouble() : 0.0);
            } else if (hour >= 18 && hour < 23) { // Вечір
                lightingUsage = lightingBaseUsage * (0.7 + 0.3 * random.nextDouble());
            } else { // Ніч
                lightingUsage = lightingBaseUsage * (random.nextDouble() < 0.05 ? random.nextDouble() : 0.0);
            }
            usageData.put("lighting", lightingUsage);

            // Зберігаємо дані в Firebase
            databaseRef.child("users").child(user.getUid()).child("houseEnergyUsage").child(date).child(time).setValue(usageData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FirebaseEnergyUsage", "Дані успішно збережено: " + date + " " + time);
                        } else {
                            Log.e("FirebaseEnergyUsage", "Помилка запису", task.getException());
                        }
                    });

            // Збільшуємо час на 10 хвилин
            calendar.add(Calendar.MINUTE, 10);
        }
    }
    public void simulateDailyUsage() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);

        // Налаштування базового споживання для кожного пристрою
        double refrigeratorUsage = 0.86 * 1000 / 144; // Середнє споживання холодильника за 10 хвилин
        double computerBaseUsage = 240 / 6.0;         // Базове споживання комп'ютера на 10 хвилин
        double microwaveBaseUsage = 1000 / 6.0;       // Базове споживання мікрохвильовки на 10 хвилин
        double lightingBaseUsage = 5 * 10 / 6.0;      // Базове споживання 5 лампочок

        int microwaveCount = 0;
        int washingMachineHour = random.nextInt(16) + 6;  // Пральна машинка включається раз на добу в ранковий або денний час
        int computerStartMorning = 6;                     // Комп'ютер працює зранку
        int computerStartEvening = 18 + random.nextInt(2); // Вечірнє включення комп'ютера
        int computerEndEvening = 23;

        for (int i = 0; i < 144; i++) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Дані споживання
            HashMap<String, Double> usageData = new HashMap<>();

            // Холодильник: постійне споживання з невеликим випадковим відхиленням
            usageData.put("refrigerator", refrigeratorUsage * (0.9 + 0.2 * random.nextDouble()));

            // Комп'ютер: варіації споживання згідно з вашим графіком
            if ((hour == computerStartMorning && minute < 40) ||
                    (hour >= computerStartEvening && hour < computerEndEvening)) {
                usageData.put("computer", computerBaseUsage * (0.8 + 0.4 * random.nextDouble()));
            } else {
                usageData.put("computer", 0.0);
            }

            // Мікрохвильова піч: вмикається тричі на день у випадкові моменти
            if (microwaveCount < 3 && hour >= 7 && hour <= 22 && random.nextInt(144) < 3) {
                usageData.put("microwave", microwaveBaseUsage * (0.7 + 0.6 * random.nextDouble()));
                microwaveCount++;
            } else {
                usageData.put("microwave", 0.0);
            }

            // Пральна машинка: працює протягом години один раз на кілька днів
            if (hour == washingMachineHour && minute < 60) {
                usageData.put("washing_machine", (800.0 / 6) * (0.8 + 0.4 * random.nextDouble()));
            } else {
                usageData.put("washing_machine", 0.0);
            }

            // Освітлення: різні лампочки працюють випадковий час, з варіаціями потужності
            double lightingUsage = 0.0;
            if (hour >= 6 && hour < 8) { // Ранок
                lightingUsage = lightingBaseUsage * random.nextDouble();
            } else if (hour >= 8 && hour < 18) { // День
                lightingUsage = lightingBaseUsage * (random.nextDouble() < 0.1 ? random.nextDouble() : 0.0);
            } else if (hour >= 18 && hour < 23) { // Вечір
                lightingUsage = lightingBaseUsage * (0.7 + 0.3 * random.nextDouble());
            } else { // Ніч
                lightingUsage = lightingBaseUsage * (random.nextDouble() < 0.05 ? random.nextDouble() : 0.0);
            }
            usageData.put("lighting", lightingUsage);

            // Зберігаємо дані в Firebase
            databaseRef.child(date).child(time).setValue(usageData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FirebaseEnergyUsage", "Дані успішно збережено: " + date + " " + time);
                        } else {
                            Log.e("FirebaseEnergyUsage", "Помилка запису", task.getException());
                        }
                    });

            // Збільшуємо час на 10 хвилин
            calendar.add(Calendar.MINUTE, 10);
        }
    }
}