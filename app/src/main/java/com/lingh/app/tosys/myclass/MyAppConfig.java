package com.lingh.app.tosys.myclass;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Objects;

public class MyAppConfig {
    public boolean enable;
    public int flag;
    public String packageName;
    public String label;

    public MyAppConfig(String packageName, String label, int flag, boolean enable) {
        this.packageName = packageName;
        this.label = label;
        this.flag = flag;
        this.enable = enable;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) return false;
        if (this == obj) return true;
        if (!(obj instanceof MyAppConfig)) return false;
        MyAppConfig myAppConfig = (MyAppConfig) obj;
        return TextUtils.equals(this.packageName, myAppConfig.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName);
    }

    @NonNull
    @Override
    public String toString() {
        return "MyAppConfig{" +
                "enable=" + enable +
                ", flag=" + flag +
                ", packageName='" + packageName + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
