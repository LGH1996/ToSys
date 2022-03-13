package com.lingh.app.tosys.fragment;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lingh.app.tosys.BuildConfig;
import com.lingh.app.tosys.R;
import com.lingh.app.tosys.databinding.FragmentConfigBinding;
import com.lingh.app.tosys.dialog.NormalDialog;
import com.lingh.app.tosys.dialog.WaitingDialog;
import com.lingh.app.tosys.myclass.MyAppDescribe;
import com.lingh.app.tosys.myclass.MyViewModel;

import org.apache.commons.io.FileUtils;

import java.io.File;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConfigFragment extends Fragment {
    private FragmentConfigBinding binding;
    private MyViewModel myViewModel;
    private MyAppDescribe myAppDescribe;
    private WaitingDialog copyWaitingDialog;
    private int enhanceFlag;
    private boolean isInstalled;
    private boolean hasSysSharedUserId;
    private int copyState;
    private int appAbi;

    public static ConfigFragment getInstance() {
        return new ConfigFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(MyViewModel.class);
        myAppDescribe = myViewModel.myAppDescribeListFilter.get(myViewModel.curPosition);
        enhanceFlag = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_PERSISTENT;
        copyWaitingDialog = new WaitingDialog();
        appAbi = myAppDescribe.primaryCpuAbi == null ? myViewModel.cpuAbi : myAppDescribe.primaryCpuAbi.contains("64") ? 64 : 32;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        initStates();
        initViews();
        initEvents();
    }

    private void initStates() {
        try {
            myAppDescribe.packageInfo = requireContext().getPackageManager().getPackageInfo(myAppDescribe.packageName, PackageManager.GET_META_DATA);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            isInstalled = false;
            e.printStackTrace();
        }
        if (myAppDescribe.packageInfo != null) {
            hasSysSharedUserId = TextUtils.equals(myAppDescribe.packageInfo.sharedUserId, "android.uid.system");
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter emitter) throws Throwable {
                FileUtils.copyFile(new File(myAppDescribe.packageInfo.applicationInfo.sourceDir), new File(myViewModel.temPkgSavePath));
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onNext(@NonNull Integer integer) {

            }

            @Override
            public void onError(@NonNull Throwable e) {
                copyState = -1;
            }

            @Override
            public void onComplete() {
                copyState = 1;
                if (copyWaitingDialog.isResumed()) {
                    copyWaitingDialog.dismiss();
                    unInstallCurrentPackage();
                }
            }
        });
    }

    private void initViews() {
        binding.icon.setImageDrawable(myAppDescribe.appIcon);
        binding.pkgName.setText(myAppDescribe.packageName);
        binding.enhance.setChecked(myAppDescribe.myAppConfig.flag == enhanceFlag);
        binding.onOff.setChecked(myAppDescribe.myAppConfig.enable);
        if (isInstalled) {
            if ((myAppDescribe.myAppConfig.enable && !hasSysSharedUserId) || (!myAppDescribe.myAppConfig.enable && hasSysSharedUserId)) {
                binding.reinstall.setText("点击卸载");
                binding.reinstall.setVisibility(View.VISIBLE);
                binding.reinstall.setOnClickListener(v -> unInstallCurrentPackage());
            } else {
                binding.reinstall.setVisibility(View.INVISIBLE);
            }
        } else {
            PackageInfo saveInfo = requireContext().getPackageManager().getPackageArchiveInfo(myViewModel.temPkgSavePath, PackageManager.GET_META_DATA);
            if (saveInfo != null && TextUtils.equals(saveInfo.packageName, myAppDescribe.packageName)) {
                binding.reinstall.setText("点击安装");
                binding.reinstall.setVisibility(View.VISIBLE);
                binding.reinstall.setOnClickListener(v -> installCurrentPackage());
            } else {
                binding.reinstall.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void initEvents() {
        binding.enhance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (myAppDescribe.myAppConfig.enable) {
                    myAppDescribe.myAppConfig.flag = isChecked ? enhanceFlag : 0;
                    myViewModel.putMyAppConfig(myAppDescribe.myAppConfig);
                }
            }
        });
        binding.onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    myAppDescribe.myAppConfig.enable = true;
                    myAppDescribe.myAppConfig.flag = binding.enhance.isChecked() ? enhanceFlag : 0;
                    myViewModel.putMyAppConfig(myAppDescribe.myAppConfig);
                } else {
                    myAppDescribe.myAppConfig.enable = false;
                    myAppDescribe.myAppConfig.flag = 0;
                    myViewModel.removeMyAppConfig(myAppDescribe.myAppConfig);
                }
                if (isInstalled && ((myAppDescribe.myAppConfig.enable && !hasSysSharedUserId) || (!myAppDescribe.myAppConfig.enable && hasSysSharedUserId))) {
                    showReinstallDialog();
                }
                initViews();
            }
        });
    }

    private void showReinstallDialog() {
        NormalDialog dialog = new NormalDialog();
        if (myViewModel.cpuAbi != appAbi && myAppDescribe.myAppConfig.enable) {
            dialog.setShowMsg(getString(R.string.abi_un_match_tip, appAbi, myViewModel.cpuAbi));
        } else {
            dialog.setShowMsg(getString(R.string.reinstall_tip));
        }
        dialog.setOnPositiveButtonClickListener("确定", this::unInstallCurrentPackage);
        dialog.setOnNavigationButtonClickListener("取消", null);
        dialog.show(getChildFragmentManager(), null);
    }

    private void installCurrentPackage() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", new File(myViewModel.temPkgSavePath));
        intent.setDataAndType(uri, requireContext().getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void unInstallCurrentPackage() {
        if (copyState == 0) {
            copyWaitingDialog.show(getChildFragmentManager(), null);
        } else if (copyState == 1) {
            Uri unInstallUri = Uri.parse("package:" + myAppDescribe.packageName);
            Intent intent = new Intent(Intent.ACTION_DELETE, unInstallUri);
            startActivity(intent);
        } else if (copyState == -1) {
            Toast.makeText(requireContext(), R.string.apk_analysis_err, Toast.LENGTH_SHORT).show();
        }
    }
}