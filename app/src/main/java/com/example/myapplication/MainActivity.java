package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    DatabaseReference databaseRef;
    private FirebaseAuth mAuth;

    private BarChart barChart;
    private Map<String, Float> dailyTotalsMap;
    private View loadingView, contentLayout;
    private int daysLoaded = 0;
    private static final int TWO_WEEK_DAYS = 14 ;


//TODO 1.Змінити налаштування загрузки щоб при переході на іншу сторінку з MainActivity сторінка зберігалась щоб не
// завантажувати її знову.
// 2. Доналаштувати завантаження графіка на головній сторінці замість цілої сторінки
// 3. змінити структура бази данних щоб кожен користувач містив інформацію про споживання СВОЇХ девайсів
// 4. налаштувати сторінки для кнопок Monitoring, Statistics, Analysis
// 5. По можливості приєднати ChatGPT для сторінки Analysis
// 6. витягувати дані через юзер айді а не просто дані без юзера

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Користувач не авторизований, перенаправляємо на екран авторизації
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Закриваємо MainActivity, щоб не було повернення сюди
            return;
        } else {
            saveUserToDatabaseIfNotExists(currentUser);
        }

        setContentView(R.layout.activity_main);



        FullDayEnergyUsageSimulator simulator = new FullDayEnergyUsageSimulator();
        loadingView = findViewById(R.id.loading_view);
        contentLayout = findViewById(R.id.contentLayout);
        barChart = findViewById(R.id.barChart);


        databaseRef = FirebaseDatabase.getInstance().getReference("houseEnergyUsage");

        dailyTotalsMap = new LinkedHashMap<>(); // Use of LinkedHashMap for saving addiction order

        loadingView.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        // Loading data for last two weeks
        loadLastNDaysData(TWO_WEEK_DAYS);

        //middle panel buttons
        findViewById(R.id.monitoring_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MonitoringActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.statics_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.nav_home).setOnClickListener(v -> recreate());
        findViewById(R.id.nav_user).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

    }

    private void saveUserToDatabaseIfNotExists(FirebaseUser user) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users");
        String userId = user.getUid();

        databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Користувача ще немає, створюємо новий запис
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("nickname", user.getDisplayName());
                    userData.put("email", user.getEmail());
                    userData.put("phone number", user.getPhoneNumber());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

                    databaseRef.child(userId).setValue(userData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("MainActivity", "Дані користувача збережені у базі.");
                                } else {
                                    Log.e("MainActivity", "Помилка збереження даних користувача", task.getException());
                                }
                            });
                } else {
                    Log.d("MainActivity", "Користувач вже існує в базі даних.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Помилка зчитування даних користувача з бази даних", error.toException());
            }
        });
    }


    private void loadLastNDaysData(int numDays) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialize a variable to control the number of days with data
        final int[] daysToLoad = {0}; // An array to provide access in anonymous classes

        // A loop to load data from the last numDays
        for (int day = 0; day < numDays; day++) {
            String date = dateFormat.format(calendar.getTime());
            dailyTotalsMap.put(date, 0.0f);

            // Завантаження даних для кожного дня
            databaseRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    float dailyTotal = 0.0f;

                    // Якщо дані для цього дня є
                    if (snapshot.exists()) {
                        for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                            for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                                Double usage = deviceSnapshot.getValue(Double.class);
                                if (usage != null) {
                                    dailyTotal += usage;
                                }
                            }
                        }

                        // Зберігаємо дані в Map
                        dailyTotalsMap.put(date, dailyTotal);
                    }

                    daysLoaded++;

                    // Якщо всі дні завантажено, відображаємо графік
                    if (daysLoaded == numDays) {
                        displayBarChart();
                        loadingView.setVisibility(View.GONE);
                        contentLayout.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Помилка завантаження даних", error.toException());
                }
            });

            // Переходимо до попереднього дня
            calendar.add(Calendar.DATE, -1);
        }
    }

    private void displayBarChart() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();
        int index = 0;

        // Фільтруємо тільки дні, для яких є дані
        ArrayList<String> dates = new ArrayList<>(dailyTotalsMap.keySet());
        for (String date : dates) {
            float dailyTotal = dailyTotalsMap.get(date);
            if (dailyTotal > 0.0f) {
                barEntries.add(new BarEntry(index, dailyTotal));
                dateLabels.add(date);  // Додаємо дату для відображення на осі X
                index++;
            }
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "Total energy consumption(Вт)");

        // Встановлюємо кастомний колір для стовпців
        barDataSet.setColor(0xFF2C7CD3); // #2C7CD3
        barDataSet.setValueTextSize(10f); // Розмір тексту на стовпцях

        BarData barData = new BarData(barDataSet);

        // Встановлюємо дані на графік
        barChart.setData(barData);
        barChart.invalidate(); // Оновлюємо графік

        // Налаштування діаграми
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true); // Діаграма заповнює простір

        // Налаштування осі X для відображення дат
        XAxis xAxis = barChart.getXAxis();
        YAxis yAxis = barChart.getAxisLeft();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dateLabels.get((int) value);  // Повертаємо дату для кожного стовпця
            }
        });

        yAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f); // Забезпечуємо, щоб кожен стовпець мав свою дату
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    }

    private void calculateTotalDailyUsage(String date) {
        databaseRef = FirebaseDatabase.getInstance().getReference("houseEnergyUsage").child(date);

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> totalUsage = new HashMap<>();

                for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot deviceSnapshot : timeSnapshot.getChildren()) {
                        String deviceName = deviceSnapshot.getKey();
                        Double deviceUsage = deviceSnapshot.getValue(Double.class);

                        if (deviceUsage != null) {
                            // Додаємо споживання пристрою до загальної суми
                            double currentTotal = totalUsage.getOrDefault(deviceName, 0.0);
                            totalUsage.put(deviceName, currentTotal + deviceUsage);
                        }
                    }
                }

                // Логування загального споживання за кожним пристроєм
                double grandTotal = 0.0;
                for (Map.Entry<String, Double> entry : totalUsage.entrySet()) {
                    String device = entry.getKey();
                    Double usage = entry.getValue();
                    Log.d("FireBaseListener", "Пристрій: " + device + ", Загальне споживання: " + usage + " Вт-год");

                    grandTotal += usage;
                }
                Log.d("FireBaseListener", "Загальне споживання електроенергії за день: " + grandTotal + " Вт-год");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FireBaseListener", "Помилка зчитування даних: ", error.toException());
           }
        });
    }
}

