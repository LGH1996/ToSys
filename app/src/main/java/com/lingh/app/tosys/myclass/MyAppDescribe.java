package com.lingh.app.tosys.myclass;

import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class MyAppDescribe {
    public String packageName;
    public String label;
    public String primaryCpuAbi;
    public Drawable appIcon;
    public PackageInfo packageInfo;
    public MyAppConfig myAppConfig;

    public MyAppDescribe(String packageName, String label, String primaryCpuAbi, Drawable appIcon, PackageInfo packageInfo, MyAppConfig myAppConfig) {
        this.packageName = packageName;
        this.label = label;
        this.primaryCpuAbi = primaryCpuAbi;
        this.appIcon = appIcon;
        this.packageInfo = packageInfo;
        this.myAppConfig = myAppConfig;
    }

    @NonNull
    @Override
    public String toString() {
        return "MyAppDescribe{" +
                "packageName='" + packageName + '\'' +
                ", label='" + label + '\'' +
                ", primaryCpuAbi='" + primaryCpuAbi + '\'' +
                ", appIcon=" + appIcon +
                ", packageInfo=" + packageInfo +
                ", myAppConfig=" + myAppConfig +
                '}';
    }
}
