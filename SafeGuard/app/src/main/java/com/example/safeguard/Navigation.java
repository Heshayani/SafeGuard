package com.example.safeguard;


import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Navigation extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        BottomNavigationView bottomNavigationView=findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.home:
                    return true;
                case R.id.loc:
                    startActivity(new Intent(getApplicationContext(), UserLocationActivity.class));
                    finish();
                    return true;
                case R.id.detect:
                    startActivity(new Intent(getApplicationContext(),accident_detection.class));
                    finish();
                    return true;
                case R.id.user:
                    startActivity(new Intent(getApplicationContext(),userprofile.class));
                    finish();
                    return true;
            }
            return false;
        });
    }
}
