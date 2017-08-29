package com.asrar.asrartestapp;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.asrar.asrartestapp.Fragments.ImagesFragment;

import butterknife.BindView;

public class MainActivity extends AppCompatActivity {

    private static long back_pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment fragment = new ImagesFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container_body, fragment);
        fragmentTransaction.commit();

    }

    public void onBackPressed() {

        if (back_pressed + 2000 > System.currentTimeMillis()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            if (Build.VERSION.SDK_INT >= 16) {
                finishAffinity();
            } else {
                finish();
            }

        } else {
            Toast.makeText(getBaseContext(), R.string.exit, Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();

    }

}
