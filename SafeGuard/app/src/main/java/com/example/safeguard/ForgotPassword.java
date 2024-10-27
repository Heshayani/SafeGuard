package com.example.safeguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {
    private Button reset, backbtn;
    private EditText email;
    private FirebaseAuth auth;
    private String strEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backbtn = findViewById(R.id.backbtn);
        reset = findViewById(R.id.reset);
        email = findViewById(R.id.email);

        auth = FirebaseAuth.getInstance();

        // Set up back button to navigate back to the LoginFragment
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish this activity to go back to the previous fragment
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strEmail = email.getText().toString().trim();
                if (!TextUtils.isEmpty(strEmail)) {
                    ResetPassword();
                } else {
                    email.setError("Email field cannot be empty");
                }
            }
        });
    }

    private void ResetPassword() {
        reset.setVisibility(View.INVISIBLE); // Hide the reset button while processing
        auth.sendPasswordResetEmail(strEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ForgotPassword.this, "Reset password link has been sent to your email", Toast.LENGTH_SHORT).show();
                        // Navigate back to the LoginFragment after successful reset
                        finish(); // Finish this activity to return to the previous fragment
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        reset.setVisibility(View.VISIBLE); // Show the reset button again if there's an error
                        Toast.makeText(ForgotPassword.this, "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
