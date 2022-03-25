package com.lingh.app.tosys.xposed;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lingh.app.tosys.myclass.MyAppConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MyXposedHook implements IXposedHookLoadPackage {
    private static final Class<?> classSystemProperties = XposedHelpers.findClass("android.os.SystemProperties", MyXposedHook.class.getClassLoader());
    private static final boolean isHookEnable = (boolean) XposedHelpers.callStaticMethod(classSystemProperties, "getBoolean", "persist.lingh.tosys.enable", true);
    private static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = XposedHelpers.getStaticIntField(PackageManager.class, "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE");
    private static final String dataDir = "//data//user//0//com.lingh.app.tosys//config.ini";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final HashMap<String, MyHookAppConfig> hookConfigMap = new HashMap<>();
    private static final String ACTION_UPDATE_CONFIG = "action.com.lingh.app.tosys.update.config";
    private static BroadcastReceiver broadcastReceiver;

    static {
        try {
            if (isHookEnable) {
                readConfigFromFile();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void readConfigFromFile() {
        try (Scanner scanner = new Scanner(new File(dataDir))) {
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
            HashMap<String, MyAppConfig> myAppConfigMap = gson.fromJson(builder.toString(), new TypeToken<HashMap<String, MyAppConfig>>() {
            }.getType());
            if (myAppConfigMap != null) {
                for (Map.Entry<String, MyAppConfig> e : myAppConfigMap.entrySet()) {
                    hookConfigMap.put(e.getKey(), new MyHookAppConfig(e.getValue(), null));
                }
            }
            XposedBridge.log("LinGH->config->" + hookConfigMap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (TextUtils.equals(lpparam.packageName, "com.lingh.app.tosys")) {
            Class<?> classMyViewModel = XposedHelpers.findClassIfExists("com.lingh.app.tosys.myclass.MyViewModel", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(classMyViewModel, "isModuleValid", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }

        if (!isHookEnable) {
            return;
        }

        if (TextUtils.equals(lpparam.packageName, "android") && TextUtils.equals(lpparam.processName, "android")) {

            Class<?> classPackageParser = XposedHelpers.findClassIfExists("android.content.pm.PackageParser", lpparam.classLoader);
            Class<?> classPackageManagerService = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService", lpparam.classLoader);
            Class<?> classPackageManagerServiceUtils = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader);
            Class<?> classPackageParserPackage = XposedHelpers.findClassIfExists("android.content.pm.PackageParser.Package", lpparam.classLoader);
            Class<?> classPackageSetting = XposedHelpers.findClassIfExists("com.android.server.pm.PackageSetting", lpparam.classLoader);
            Class<?> classPackageManagerException = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerException", lpparam.classLoader);
            Class<?> classActivityManagerService = XposedHelpers.findClassIfExists("com.android.server.am.ActivityManagerService", lpparam.classLoader);
            Class<?> classUriGrantsManagerService = XposedHelpers.findClassIfExists("com.android.server.uri.UriGrantsManagerService", lpparam.classLoader);
            Class<?> classAmGrantUri = XposedHelpers.findClassIfExists("com.android.server.am.ActivityManagerService.GrantUri", lpparam.classLoader);
            Class<?> classUriGrantUri = XposedHelpers.findClassIfExists("com.android.server.uri.GrantUri", lpparam.classLoader);
            Class<?> classParsingPackageUtils = XposedHelpers.findClassIfExists("android.content.pm.parsing.ParsingPackageUtils", lpparam.classLoader);
            Class<?> classParseTypeImpl = XposedHelpers.findClassIfExists("android.content.pm.parsing.result.ParseTypeImpl", lpparam.classLoader);
            Class<?> classParsingPackageImpl = XposedHelpers.findClassIfExists("android.content.pm.parsing.ParsingPackageImpl", lpparam.classLoader);
            Class<?> classParsedProvider = XposedHelpers.findClassIfExists("android.content.pm.parsing.component.ParsedProvider", lpparam.classLoader);
            Class<?> classPackageParserProvider = XposedHelpers.findClassIfExists("android.content.pm.PackageParser.Provider", lpparam.classLoader);

            XposedBridge.hookAllMethods(classActivityManagerService, "finishBooting", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedBridge.log("LinGH->finishBooting");
                        if (broadcastReceiver != null) {
                            return;
                        }
                        broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    if (TextUtils.equals(intent.getStringExtra("extra_status"), "put")) {
                                        MyAppConfig myAppConfig = gson.fromJson(intent.getStringExtra("extra_value"), MyAppConfig.class);
                                        hookConfigMap.put(myAppConfig.packageName, new MyHookAppConfig(myAppConfig, null));
                                    }
                                    if (TextUtils.equals(intent.getStringExtra("extra_status"), "remove")) {
                                        MyAppConfig myAppConfig = gson.fromJson(intent.getStringExtra("extra_value"), MyAppConfig.class);
                                        hookConfigMap.remove(myAppConfig.packageName);
                                    }
                                    XposedBridge.log("LinGH->register->broadcast->config->" + hookConfigMap);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        AndroidAppHelper.currentApplication().registerReceiver(broadcastReceiver, new IntentFilter(ACTION_UPDATE_CONFIG));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            XposedBridge.hookAllMethods(classPackageParser, "parseBaseApkCommon", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedBridge.log("LinGH->parseBaseApkCommon->args->" + Arrays.toString(param.args));
                        Object result = param.getResult();
                        XposedBridge.log("LinGH->parseBaseApkCommon->result->" + result);
                        if (!classPackageParserPackage.isAssignableFrom(result.getClass())) {
                            return;
                        }
                        String packageName = (String) XposedHelpers.getObjectField(result, "packageName");
                        MyHookAppConfig myHookAppConfig = hookConfigMap.get(packageName);
                        if (myHookAppConfig == null) {
                            return;
                        }
                        XposedHelpers.setObjectField(result, "mSharedUserId", "android.uid.system");
                        XposedBridge.log("LinGH->parseBaseApkCommon->hook");
                        //android.content.pm.PackageParser.Provider
                        List<?> providers = (List<?>) XposedHelpers.getObjectField(result, "providers");
                        if (providers == null || providers.isEmpty()) {
                            return;
                        }
                        if (!classPackageParserProvider.isAssignableFrom(providers.get(0).getClass())) {
                            return;
                        }
                        myHookAppConfig.providerAuthorityList = new ArrayList<>();
                        for (Object e : providers) {
                            ProviderInfo info = (ProviderInfo) XposedHelpers.getObjectField(e, "info");
                            myHookAppConfig.providerAuthorityList.add(info.authority);
                        }
                        XposedBridge.log("LinGH->parseBaseApkCommon->hasProvider->" + myHookAppConfig.providerAuthorityList);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedBridge.hookAllMethods(classParsingPackageUtils, "parsePackage", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->parsePackage->args->" + Arrays.toString(param.args));
                            Object result = param.getResult();
                            XposedBridge.log("LinGH->parsePackage->result->" + result);
                            if (!classParseTypeImpl.isAssignableFrom(result.getClass())) {
                                return;
                            }
                            Object parsingPackageImpl = XposedHelpers.getObjectField(result, "mResult");
                            XposedBridge.log("LinGH->parsePackage->parsingPackageImpl->" + parsingPackageImpl);
                            if (!classParsingPackageImpl.isAssignableFrom(parsingPackageImpl.getClass())) {
                                return;
                            }
                            String packageName = (String) XposedHelpers.getObjectField(parsingPackageImpl, "packageName");
                            MyHookAppConfig myHookAppConfig = hookConfigMap.get(packageName);
                            if (myHookAppConfig == null) {
                                return;
                            }
                            XposedHelpers.setObjectField(parsingPackageImpl, "sharedUserId", "android.uid.system");
                            XposedBridge.log("LinGH->parsePackage->hook");
                            //android.content.pm.parsing.component.ParsedProvider
                            List<?> providers = (List<?>) XposedHelpers.getObjectField(parsingPackageImpl, "providers");
                            if (providers == null || providers.isEmpty()) {
                                return;
                            }
                            if (!classParsedProvider.isAssignableFrom(providers.get(0).getClass())) {
                                return;
                            }
                            myHookAppConfig.providerAuthorityList = new ArrayList<>();
                            for (Object e : providers) {
                                String authority = (String) XposedHelpers.getObjectField(e, "authority");
                                if (!TextUtils.isEmpty(authority)) {
                                    myHookAppConfig.providerAuthorityList.add(authority);
                                }
                            }
                            XposedBridge.log("LinGH->parsePackage->hasProvider->" + myHookAppConfig.providerAuthorityList);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                XposedBridge.hookAllMethods(classPackageManagerService, "verifySignaturesLP", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->verifySignaturesLP->args->" + Arrays.toString(param.args));
                            if (param.args.length < 2) {
                                return;
                            }
                            if (!classPackageSetting.isAssignableFrom(param.args[0].getClass())
                                    || !classPackageParserPackage.isAssignableFrom(param.args[1].getClass())) {
                                return;
                            }
                            Object pkgSetting = param.args[0];
                            Object pkg = param.args[1];
                            String pkgSettingName = (String) XposedHelpers.getObjectField(pkgSetting, "name");
                            String pkgPackageName = (String) XposedHelpers.getObjectField(pkg, "packageName");
                            MyHookAppConfig setConfig = hookConfigMap.get(pkgSettingName);
                            MyHookAppConfig pacConfig = hookConfigMap.get(pkgPackageName);
                            if (setConfig == null && pacConfig == null) {
                                return;
                            }
                            try {
                                param.getResultOrThrowable();
                            } catch (Throwable throwable) {
                                if (classPackageManagerException.isAssignableFrom(throwable.getClass())) {
                                    int error = XposedHelpers.getIntField(throwable, "error");
                                    if (error == INSTALL_FAILED_SHARED_USER_INCOMPATIBLE) {
                                        param.setResult(null);
                                        XposedBridge.log("LinGH->verifySignaturesLP->hook");
                                    }
                                }
                                throwable.printStackTrace();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                XposedBridge.hookAllMethods(classPackageManagerServiceUtils, "verifySignatures", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->verifySignatures->args->" + Arrays.toString(param.args));
                            if (param.args.length < 1) {
                                return;
                            }
                            if (!classPackageSetting.isAssignableFrom(param.args[0].getClass())) {
                                return;
                            }
                            Object pkgSetting = param.args[0];
                            String pkgSettingName = (String) XposedHelpers.getObjectField(pkgSetting, "name");
                            MyHookAppConfig myHookAppConfig = hookConfigMap.get(pkgSettingName);
                            if (myHookAppConfig == null) {
                                return;
                            }
                            try {
                                param.getResultOrThrowable();
                            } catch (Throwable throwable) {
                                if (classPackageManagerException.isAssignableFrom(throwable.getClass())) {
                                    int error = XposedHelpers.getIntField(throwable, "error");
                                    if (error == INSTALL_FAILED_SHARED_USER_INCOMPATIBLE) {
                                        param.setResult(false);
                                        XposedBridge.log("LinGH->verifySignatures->hook");
                                    }
                                }
                                throwable.printStackTrace();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                XposedBridge.hookAllMethods(classActivityManagerService, "checkGrantUriPermissionLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->checkGrantUriPermissionLocked->args->" + Arrays.toString(param.args));
                            if (param.args.length < 5) {
                                return;
                            }
                            if (!classAmGrantUri.isAssignableFrom(param.args[2].getClass())) {
                                return;
                            }
                            Object grantUri = param.args[2];
                            Uri uri = (Uri) XposedHelpers.getObjectField(grantUri, "uri");
                            String authority = uri.getAuthority();
                            if (authority == null) {
                                return;
                            }
                            for (MyHookAppConfig e : hookConfigMap.values()) {
                                if (e.providerAuthorityList != null && e.providerAuthorityList.contains(authority)) {
                                    int lastTargetUid = (int) param.args[4];
                                    if (lastTargetUid < 0 && param.args[1] != null) {
                                        PackageManager packageManager = AndroidAppHelper.currentApplication().getPackageManager();
                                        lastTargetUid = packageManager.getPackageUid((String) param.args[1], PackageManager.GET_META_DATA);
                                    }
                                    if (lastTargetUid >= 0) {
                                        param.setResult(lastTargetUid);
                                        XposedBridge.log("LinGH->checkGrantUriPermissionLocked->hook->lastTargetUid:" + lastTargetUid);
                                    }
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                XposedBridge.hookAllMethods(classUriGrantsManagerService, "checkGrantUriPermission", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->checkGrantUriPermission->args" + Arrays.toString(param.args));
                            if (param.args.length < 5) {
                                return;
                            }
                            if (!classUriGrantUri.isAssignableFrom(param.args[2].getClass())) {
                                return;
                            }
                            Object grantUri = param.args[2];
                            Uri uri = (Uri) XposedHelpers.getObjectField(grantUri, "uri");
                            String authority = uri.getAuthority();
                            if (authority == null) {
                                return;
                            }
                            for (MyHookAppConfig e : hookConfigMap.values()) {
                                if (e.providerAuthorityList != null && e.providerAuthorityList.contains(authority)) {
                                    int lastTargetUid = (int) param.args[4];
                                    if (lastTargetUid < 0 && param.args[1] != null) {
                                        PackageManager packageManager = AndroidAppHelper.currentApplication().getPackageManager();
                                        lastTargetUid = packageManager.getPackageUid((String) param.args[1], PackageManager.GET_META_DATA);
                                    }
                                    if (lastTargetUid >= 0) {
                                        param.setResult(lastTargetUid);
                                        XposedBridge.log("LinGH->checkGrantUriPermission->hook->lastTargetUid:" + lastTargetUid);
                                    }
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedBridge.hookAllMethods(classUriGrantsManagerService, "checkGrantUriPermissionUnlocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedBridge.log("LinGH->checkGrantUriPermissionUnlocked->args" + Arrays.toString(param.args));
                            if (param.args.length < 5) {
                                return;
                            }
                            if (!classUriGrantUri.isAssignableFrom(param.args[2].getClass())) {
                                return;
                            }
                            Object grantUri = param.args[2];
                            Uri uri = (Uri) XposedHelpers.getObjectField(grantUri, "uri");
                            String authority = uri.getAuthority();
                            if (authority == null) {
                                return;
                            }
                            for (MyHookAppConfig e : hookConfigMap.values()) {
                                if (e.providerAuthorityList != null && e.providerAuthorityList.contains(authority)) {
                                    int lastTargetUid = (int) param.args[4];
                                    if (lastTargetUid < 0 && param.args[1] != null) {
                                        PackageManager packageManager = AndroidAppHelper.currentApplication().getPackageManager();
                                        lastTargetUid = packageManager.getPackageUid((String) param.args[1], PackageManager.GET_META_DATA);
                                    }
                                    if (lastTargetUid >= 0) {
                                        param.setResult(lastTargetUid);
                                        XposedBridge.log("LinGH->checkGrantUriPermissionUnlocked->hook->lastTargetUid:" + lastTargetUid);
                                    }
                                    break;
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            Method[] methodsPms = classPackageManagerService.getDeclaredMethods();
            for (Method method : methodsPms) {
                if (PackageInfo.class.isAssignableFrom(method.getReturnType())) {
                    Class<?>[] params = method.getParameterTypes();
                    Object[] objects = new Object[params.length + 1];
                    System.arraycopy(params, 0, objects, 0, params.length);
                    objects[params.length] = new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                PackageInfo result = (PackageInfo) param.getResult();
                                if (result == null
                                        || result.applicationInfo == null
                                        || result.applicationInfo.uid != Process.SYSTEM_UID) {
                                    return;
                                }
                                MyHookAppConfig myHookAppConfig = hookConfigMap.get(result.packageName);
                                if (myHookAppConfig == null) {
                                    return;
                                }
                                result.applicationInfo.flags |= myHookAppConfig.myAppConfig.flag;
                                XposedBridge.log("LinGH->PackageInfo->package:" + myHookAppConfig.myAppConfig.packageName + "->method:" + method.getName());
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    XposedHelpers.findAndHookMethod(classPackageManagerService, method.getName(), objects);
                }
                if (ApplicationInfo.class.isAssignableFrom(method.getReturnType())) {
                    Class<?>[] params = method.getParameterTypes();
                    Object[] objects = new Object[params.length + 1];
                    System.arraycopy(params, 0, objects, 0, params.length);
                    objects[params.length] = new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                ApplicationInfo result = (ApplicationInfo) param.getResult();
                                if (result == null
                                        || result.uid != Process.SYSTEM_UID) {
                                    return;
                                }
                                MyHookAppConfig myHookAppConfig = hookConfigMap.get(result.packageName);
                                if (myHookAppConfig == null) {
                                    return;
                                }
                                result.flags |= myHookAppConfig.myAppConfig.flag;
                                XposedBridge.log("LinGH->ApplicationInfo->package->" + myHookAppConfig.myAppConfig.packageName + "->method:" + method.getName());
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    XposedHelpers.findAndHookMethod(classPackageManagerService, method.getName(), objects);
                }
            }
        }
    }

    public static class MyHookAppConfig {
        public MyAppConfig myAppConfig;
        public List<String> providerAuthorityList;

        public MyHookAppConfig(MyAppConfig myAppConfig, List<String> providerAuthorityList) {
            this.myAppConfig = myAppConfig;
            this.providerAuthorityList = providerAuthorityList;
        }

        @NonNull
        @Override
        public String toString() {
            return "MyHookAppConfig{" +
                    "myAppConfig=" + myAppConfig +
                    ", providerAuthorityList=" + providerAuthorityList +
                    '}';
        }
    }
}
