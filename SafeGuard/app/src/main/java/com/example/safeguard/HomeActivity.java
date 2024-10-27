package com.example.safeguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RippleBackground rippleBackground;
    private ImageView imageView;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private long lastClickTime = 0;
    private MediaPlayer mediaPlayer;
    private static final int SMS_PERMISSION_CODE = 1;
    private static final int CALL_PERMISSION_CODE = 2;
    private DatabaseReference userRef;
    private List<String> trustedContacts = new ArrayList<>();
    private Handler handler;
    private Runnable callRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    return true;
                case R.id.loc:
                    startActivity(new Intent(getApplicationContext(), UserLocationActivity.class));
                    finish();
                    return true;
                case R.id.detect:
                    startActivity(new Intent(getApplicationContext(), accident_detection.class));
                    finish();
                    return true;
                case R.id.user:
                    startActivity(new Intent(getApplicationContext(), userprofile.class));
                    finish();
                    return true;
            }
            return false;
        });

        rippleBackground = findViewById(R.id.content);
        imageView = findViewById(R.id.emergency);

        // Load alert sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);

        // Initialize Firebase reference
        userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        handler = new Handler();
        callRunnable = new Runnable() {
            @Override
            public void run() {
                if (checkCallPermission()) {
                    callTrustedContacts();
                } else {
                    requestCallPermission();
                }
            }
        };

        imageView.setOnClickListener(v -> {
            long currentClickTime = System.currentTimeMillis();
            if (currentClickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                // Double click detected
                rippleBackground.startRippleAnimation();
                playAlertSound();
                if (checkSmsPermission()) {
                    fetchTrustedContactsAndSendAlerts();
                } else {
                    requestSmsPermission();
                }

                // Schedule the automatic call if alert is not stopped
                handler.postDelayed(callRunnable, 5 * 60 * 1000); // 5 minutes
            }
            lastClickTime = currentClickTime;
        });

        rippleBackground.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                stopAlertSound();
                handler.removeCallbacks(callRunnable); // Cancel automatic call if stopped
            }
            return false;
        });

        rippleBackground.setOnClickListener(v -> rippleBackground.stopRippleAnimation());
    }

    private void playAlertSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    private void stopAlertSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.prepareAsync(); // Prepare for the next playback
        }
    }

    private void fetchTrustedContactsAndSendAlerts() {
        // Fetch contacts from the "contact" node
        userRef.child("contact").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                trustedContacts.clear();
                Log.d("SafeguardApp", "Contacts data snapshot: " + dataSnapshot.toString());
                if (dataSnapshot.exists()) {
                    for (DataSnapshot contactSnapshot : dataSnapshot.getChildren()) {
                        String contact = contactSnapshot.getValue(String.class);
                        if (contact != null) {
                            // Ensure the contact has the +94 prefix
                            if (!contact.startsWith("+94")) {
                                contact = "+94" + contact.substring(1);
                            }
                            trustedContacts.add(contact);
                            Log.d("SafeguardApp", "Contact added: " + contact);
                        }
                    }
                    if (trustedContacts.isEmpty()) {
                        Log.d("SafeguardApp", "No contacts found after processing");
                        Toast.makeText(HomeActivity.this, "No trusted contacts found.", Toast.LENGTH_SHORT).show();
                    } else {
                        sendAlertToTrustedContacts();
                    }
                } else {
                    Log.d("SafeguardApp", "Contacts node does not exist");
                    Toast.makeText(HomeActivity.this, "No trusted contacts found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("SafeguardApp", "Failed to retrieve contacts: " + databaseError.getMessage());
                Toast.makeText(HomeActivity.this, "Failed to retrieve contacts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAlertToTrustedContacts() {
        for (String contact : trustedContacts) {
            sendSms(contact, "Alert! Emergency detected. Please check immediately.");
        }
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Alert sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send alert to " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void callTrustedContacts() {
        for (String contact : trustedContacts) {
            makeCall(contact);
        }
    }

    private void makeCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(android.net.Uri.parse("tel:" + phoneNumber));
        if (checkCallPermission()) {
            startActivity(callIntent);
        } else {
            requestCallPermission();
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    private boolean checkCallPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCallPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchTrustedContactsAndSendAlerts();
            } else {
                Toast.makeText(this, "SMS permission is required to send alerts.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CALL_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for calls
            } else {
                Toast.makeText(this, "Call permission is required to make calls.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(callRunnable); // Remove callback when activity is destroyed
    }
}
