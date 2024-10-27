package com.example.safeguard;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class accident_detection extends AppCompatActivity {

    private static final String TAG = "AccidentDetection";
    private Interpreter tflite;
    private Button btnStartDetection, btnStopDetection;
    private TextView tvDetectionStatus;
    private boolean isDetecting = false;
    private Handler handler = new Handler();
    private Runnable detectionTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accident_detection);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.detect);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.detect:
                    return true;
                case R.id.loc:
                    startActivity(new Intent(getApplicationContext(), UserLocationActivity.class));
                    finish();
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                    return true;
                case R.id.user:
                    startActivity(new Intent(getApplicationContext(), userprofile.class));
                    finish();
                    return true;
            }
            return false;
        });

        btnStartDetection = findViewById(R.id.btnStartDetection);
        btnStopDetection = findViewById(R.id.btnStopDetection);
        tvDetectionStatus = findViewById(R.id.tvDetectionStatus);

        // Initialize TensorFlow Lite Interpreter
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e(TAG, "Error initializing TensorFlow Lite model.", e);
        }

        btnStartDetection.setOnClickListener(v -> startAccidentDetection());
        btnStopDetection.setOnClickListener(v -> stopAccidentDetection());
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        // Load the model from the assets folder
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("accident_detection_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void startAccidentDetection() {
        isDetecting = true;
        tvDetectionStatus.setText("Detection Status: Active");

        detectionTask = new Runnable() {
            @Override
            public void run() {
                if (isDetecting && tflite != null) {  // Check if tflite is initialized
                    float[] inputData = getSensorData();
                    float[] outputData = new float[1];  // Assuming the model has a single output

                    try {
                        // Run the model
                        tflite.run(inputData, outputData);
                        Log.d(TAG, "Model output: " + Arrays.toString(outputData));

                        if (outputData.length > 0 && outputData[0] > 0.5) { // Threshold for accident detection
                            Log.d(TAG, "Accident detected!");
                            sendAlert();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error during model inference.", e);
                    }

                    handler.postDelayed(this, 1000); // Check every second
                } else if (tflite == null) {
                    Log.e(TAG, "TensorFlow Lite interpreter is not initialized.");
                }
            }
        };

        handler.post(detectionTask);
    }

    private void stopAccidentDetection() {
        isDetecting = false;
        handler.removeCallbacks(detectionTask);
        tvDetectionStatus.setText("Detection Status: Inactive");
    }

    private float[] getSensorData() {
        // Example simulated data: Adjust these values based on your model's requirements

        // Simulated normal condition
        float[] normalData = {0.1f, 0.2f, 0.3f}; // Example values

        // Simulated accident condition
        float[] accidentData = {0.6f, 0.8f, 0.9f}; // Example values

        // Flag to simulate accident or normal conditions
        boolean simulateAccident = true; // Change this to test different scenarios

        // Return the appropriate data based on the flag
        return simulateAccident ? accidentData : normalData;
    }

    private void sendAlert() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("alerts");
            String alertId = dbRef.push().getKey();
            if (alertId != null) {
                Alert alert = new Alert(currentUser.getUid(), "Accident detected", System.currentTimeMillis());
                dbRef.child(alertId).setValue(alert);
                sendSMSToTrustedContacts();
                tvDetectionStatus.setText("Accident Detected! Alerts sent.");
            }
        }
    }

    private void sendSMSToTrustedContacts() {
        // Example code to send SMS
        SmsManager smsManager = SmsManager.getDefault();
        String phoneNumber = "1234567890"; // Replace with a test phone number
        String message = "Accident detected. Please respond.";
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Log.d(TAG, "SMS sent to " + phoneNumber);
    }
}

class Alert {
    public String userId;
    public String message;
    public long timestamp;

    public Alert(String userId, String message, long timestamp) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
    }
}
