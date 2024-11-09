package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class UserActivity extends AppCompatActivity {


    private ImageView profileImageView;
    private TextView nicknameTextView, emailTextView, numberTextView;
    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);


        profileImageView = findViewById(R.id.profileImage);
        nicknameTextView = findViewById(R.id.nicknameText);
        emailTextView = findViewById(R.id.emailText);
        numberTextView = findViewById(R.id.phoneNumber);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadUserData();

        findViewById(R.id.nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Refreshing activity

                startActivity(new Intent(UserActivity.this, MainActivity.class));
            }
        });

        // Setting up a clicker handler for a button "User"
        findViewById(R.id.nav_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move on to activity UserActivity
                finish();
                startActivity(new Intent(UserActivity.this, UserActivity.class));
            }
        });
    }


    private void loadUserData() {
        String userId = currentUser.getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Отримуємо дані користувача
                    String nickname = snapshot.child("nickname").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                    String phoneNumber = snapshot.child("phone number").getValue(String.class);

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
                } else {
                    Toast.makeText(UserActivity.this, "Дані користувача не знайдено в базі", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserActivity", "Помилка зчитування даних користувача", error.toException());
                Toast.makeText(UserActivity.this, "Помилка зчитування даних", Toast.LENGTH_SHORT).show();
            }
        });
    }


}

