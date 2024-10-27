package com.example.safeguard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class userprofile extends AppCompatActivity {

    private TextView helloUser;
    private EditText address, medical, name, contact;
    private Button saveChanges;
    private ImageView imagesign, profileIcon;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userRef;
    private ViewPager2 viewPager2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userprofile);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.user);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.user:
                    return true;
                case R.id.loc:
                    startActivity(new Intent(getApplicationContext(), UserLocationActivity.class));
                    finish();
                    return true;
                case R.id.detect:
                    startActivity(new Intent(getApplicationContext(), accident_detection.class));
                    finish();
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                    return true;
            }
            return false;
        });

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());

        // Initialize Views
        helloUser = findViewById(R.id.hellouser);
        address = findViewById(R.id.address);
        medical = findViewById(R.id.medical);
        name = findViewById(R.id.name);
        contact = findViewById(R.id.contact);
        saveChanges = findViewById(R.id.savechanges);
        imagesign = findViewById(R.id.imagesign);

        // Set Username TextView
        if (user != null) {
            String email = user.getEmail();
            String username = email != null ? email.split("@")[0] : "User";
            helloUser.setText(username);
        }

        // Load user data
        loadUserData();

        // Save changes to Firebase Database
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();
                Intent intent = new Intent(userprofile.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Sign out when the sign-out icon is clicked
        imagesign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String strAddress = snapshot.child("address").getValue(String.class);
                    String strMedical = snapshot.child("medical").getValue(String.class);
                    String strName = snapshot.child("name").getValue(String.class);
                    String strContact = snapshot.child("contact").getValue(String.class);

                    address.setText(strAddress != null ? strAddress : "");
                    medical.setText(strMedical != null ? strMedical : "");
                    name.setText(strName != null ? strName : "");
                    contact.setText(strContact != null ? strContact : "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(userprofile.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        String strAddress = address.getText().toString().trim();
        String strMedical = medical.getText().toString().trim();
        String strName = name.getText().toString().trim();
        String strContact = contact.getText().toString().trim();

        if (strAddress.isEmpty() || strMedical.isEmpty() || strName.isEmpty() || strContact.isEmpty()) {
            Toast.makeText(userprofile.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("address", strAddress);
        userInfo.put("medical", strMedical);
        userInfo.put("name", strName);
        userInfo.put("contact", strContact);

        userRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(userprofile.this, "Information saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(userprofile.this, "Failed to save information", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void signOut() {
        auth.signOut();
        Intent intent = new Intent(userprofile.this, MainActivity.class);
        intent.putExtra("redirectToLogin", true); // Add this extra to trigger navigation to the login fragment
        startActivity(intent);
        finish();
    }
}
