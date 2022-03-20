package com.lingh.app.tosys.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;

import com.lingh.app.tosys.R;
import com.lingh.app.tosys.databinding.ActivityMainBinding;
import com.lingh.app.tosys.dialog.NormalDialog;
import com.lingh.app.tosys.fragment.MainFragment;
import com.lingh.app.tosys.myclass.MyViewModel;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MyViewModel myViewModel;
    private FragmentManager fragmentManager;
    private Fragment mainFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ActivityMainBinding mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        myViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MyViewModel.class);
        fragmentManager = getSupportFragmentManager();
        mainFragment = MainFragment.getInstance();
        gotoMainFragment();
        if (!myViewModel.isModuleValid()) {
            showModuleInvalidDialog();
        } else if (!myViewModel.isModuleEnabled) {
            showModuleDisabledDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            FileUtils.forceDelete(new File(myViewModel.temPkgSavePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gotoMainFragment() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.remove(currentFragment);
        }
        if (mainFragment.isAdded()) {
            transaction.show(mainFragment);
        } else {
            transaction.add(R.id.container, mainFragment);
        }
        transaction.commit();
        currentFragment = mainFragment;
    }

    public void gotoSecondFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        transaction.add(R.id.container, fragment);
        transaction.commit();
        currentFragment = fragment;
    }

    private void showModuleInvalidDialog() {
        NormalDialog dialog = new NormalDialog();
        dialog.setCancelable(false);
        dialog.setShowMsg(getString(R.string.invalid_tip));
        dialog.setOnPositiveButtonClickListener("确定", null);
        dialog.show(getSupportFragmentManager(), null);
    }

    private void showModuleDisabledDialog() {
        NormalDialog dialog = new NormalDialog();
        dialog.setCancelable(false);
        dialog.setShowMsg(getString(R.string.disabled_tip));
        dialog.setOnPositiveButtonClickListener("确定", null);
        dialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onBackPressed() {
        if (currentFragment != mainFragment) {
            gotoMainFragment();
        } else {
            super.onBackPressed();
        }
    }
}