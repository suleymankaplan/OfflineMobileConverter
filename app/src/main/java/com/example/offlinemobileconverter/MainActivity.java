package com.example.offlinemobileconverter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    final Fragment homeFragment = new HomeFragment();
    final Fragment filesFragment = new FilesFragment();
    final Fragment aboutFragment = new AboutFragment();
    final FragmentManager fm = getSupportFragmentManager();
    Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);

        fm.beginTransaction().add(R.id.fragment_container, aboutFragment, "3").hide(aboutFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, filesFragment, "2").hide(filesFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fm.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
                return true;
            } else if (itemId == R.id.nav_files) {
                fm.beginTransaction().hide(activeFragment).show(filesFragment).commit();
                activeFragment = filesFragment;
                return true;
            } else if (itemId == R.id.nav_about) {
                fm.beginTransaction().hide(activeFragment).show(aboutFragment).commit();
                activeFragment = aboutFragment;
                return true;
            }
            return false;
        });
    }
}