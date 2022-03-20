package com.lingh.app.tosys.myclass;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MyViewModel extends AndroidViewModel {
    private final String ACTION_UPDATE_CONFIG = "action.com.lingh.app.tosys.update.config";
    private final Application mApplication;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String configPath;
    public String temPkgSavePath;
    public ArrayList<MyAppDescribe> myAppDescribeList;
    public ArrayList<MyAppDescribe> myAppDescribeListFilter;
    public HashMap<String, MyAppConfig> myAppConfigHashMap;
    public int cpuAbi;
    public int curPosition;
    public boolean isModuleEnabled = true;
    public MyHttpRequest myHttpRequest;

    public MyViewModel(@NonNull Application application) {
        super(application);
        mApplication = application;
        initData();
    }

    private void initData() {
        configPath = mApplication.getDataDir().getAbsolutePath() + File.separator + "config.ini";
        temPkgSavePath = mApplication.getCacheDir() + File.separator + "package.apk";
        cpuAbi = Arrays.toString(Build.SUPPORTED_ABIS).contains("64") ? 64 : 32;
        myHttpRequest = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build().create(MyHttpRequest.class);
        try {
            Class<?> propClass = Class.forName("android.os.SystemProperties");
            Method method = propClass.getDeclaredMethod("getBoolean", String.class, boolean.class);
            method.setAccessible(true);
            Object result = method.invoke(propClass, "persist.lingh.tosys.enable", true);
            isModuleEnabled = result == null || (boolean) result;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        File file = new File(configPath);
        try (Scanner scanner = new Scanner(file)) {
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
            myAppConfigHashMap = gson.fromJson(builder.toString(), new TypeToken<HashMap<String, MyAppConfig>>() {
            }.getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (myAppConfigHashMap == null) {
            myAppConfigHashMap = new HashMap<>();
        }
        myAppDescribeList = new ArrayList<>();
        myAppDescribeListFilter = new ArrayList<>();
    }

    public MyAppConfig putMyAppConfig(MyAppConfig value) {
        MyAppConfig result = myAppConfigHashMap.put(value.packageName, value);
        updateConfigs();
        mApplication.sendBroadcast(new Intent(ACTION_UPDATE_CONFIG).putExtra("extra_status", "put").putExtra("extra_value", gson.toJson(value)).setPackage("android"));
        return result;
    }

    public MyAppConfig removeMyAppConfig(MyAppConfig value) {
        MyAppConfig result = myAppConfigHashMap.remove(value.packageName);
        updateConfigs();
        mApplication.sendBroadcast(new Intent(ACTION_UPDATE_CONFIG).putExtra("extra_status", "remove").putExtra("extra_value", gson.toJson(value)).setPackage("android"));
        return result;
    }

    public void updateConfigs() {
        try (FileOutputStream outPutStream = new FileOutputStream(configPath)) {
            String str = gson.toJson(myAppConfigHashMap, new TypeToken<HashMap<String, MyAppConfig>>() {
            }.getType());
            outPutStream.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isModuleValid() {
        return false;
    }
}
