package com.example.safeguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupFragment extends Fragment {

    private EditText username, email, password, confirmpw;
    private Button signupbtn;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        username = view.findViewById(R.id.username);
        email = view.findViewById(R.id.email);
        password = view.findViewById(R.id.password);
        confirmpw = view.findViewById(R.id.confirmpw);
        signupbtn = view.findViewById(R.id.signupbtn);

        auth = FirebaseAuth.getInstance();

        signupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from EditText fields
                String name = username.getText().toString().trim();
                String em = email.getText().toString().trim();
                String pw = password.getText().toString().trim();
                String cnfirmpw = confirmpw.getText().toString().trim();

                // Validate fields
                if (TextUtils.isEmpty(name)) {
                    username.setError("UserName is required");
                    username.requestFocus();
                } else if (TextUtils.isEmpty(em)) {
                    email.setError("Email is required");
                    email.requestFocus();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
                    email.setError("Valid Email is required");
                    email.requestFocus();
                } else if (TextUtils.isEmpty(pw)) {
                    password.setError("Please enter your password");
                    password.requestFocus();
                } else if (pw.length() < 6) {
                    password.setError("Password is too weak");
                    password.requestFocus();
                } else if (TextUtils.isEmpty(cnfirmpw)) {
                    confirmpw.setError("Please confirm your password");
                    confirmpw.requestFocus();
                } else if (!pw.equals(cnfirmpw)) {
                    confirmpw.setError("Passwords do not match");
                    confirmpw.requestFocus();
                    // Clear the entered passwords
                    password.setText("");
                    confirmpw.setText("");
                } else {
                    registerUser(name, em, pw);
                }
            }
        });
    }

    private void registerUser(String name, String em, String pw) {
        auth.createUserWithEmailAndPassword(em, pw).addOnCompleteListener(getActivity(),
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "User Signup Successfully", Toast.LENGTH_SHORT).show();

                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Create a HelperClass instance to store in Firebase
                                HelperClass helperClass = new HelperClass(name, em); // Ensure HelperClass constructor matches
                                DatabaseReference Rusers = FirebaseDatabase.getInstance().getReference("Registered user");
                                Rusers.child(firebaseUser.getUid()).setValue(helperClass).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Verification email sent.", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(getActivity(),userprofile.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                        getActivity().finish();
                                                    } else {
                                                        Toast.makeText(getContext(), "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        } else {
                                            Toast.makeText(getContext(), "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(getContext(), "User Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}

