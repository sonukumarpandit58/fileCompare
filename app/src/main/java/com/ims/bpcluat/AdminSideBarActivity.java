package com.ims.bpcluat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.ims.bpcluat.databinding.ActivityAdminSideBarBinding;
import com.ims.bpcluat.databinding.ActivitySideBarBinding;
import com.ims.bpcluat.fragment.AppInfoFragment;
import com.ims.bpcluat.fragment.OperatorViewAddFragment;
import com.ims.bpcluat.fragment.PumpFragment;
import com.ims.bpcluat.fragment.ReprintFragment;
import com.ims.bpcluat.fragment.TxnHistorySummaryFragment;

public class AdminSideBarActivity extends AppCompatActivity {

    ActivityAdminSideBarBinding xml;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        xml = ActivityAdminSideBarBinding.inflate(getLayoutInflater());
        setContentView(xml.getRoot());

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.OpenDrawer,R.string.CloseDrawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        OperatorViewAddFragment fragment = new OperatorViewAddFragment();
        if(fragment != null){
            loadFragement(fragment);
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id == R.id.adminAppInfo){
                    loadFragement(new AppInfoFragment());
                }else if(id == R.id.adminSignout){
                    Intent intent = new Intent(AdminSideBarActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    public void loadFragement(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container,fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}