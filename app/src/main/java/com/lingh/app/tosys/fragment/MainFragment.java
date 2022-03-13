package com.lingh.app.tosys.fragment;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.lingh.app.tosys.R;
import com.lingh.app.tosys.activity.MainActivity;
import com.lingh.app.tosys.adapter.MyRecyclerViewAdapter;
import com.lingh.app.tosys.databinding.FragmentMainBinding;
import com.lingh.app.tosys.myclass.MyAppConfig;
import com.lingh.app.tosys.myclass.MyAppDescribe;
import com.lingh.app.tosys.myclass.MyViewModel;

import java.lang.reflect.Field;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private MyViewModel myViewModel;
    private MyRecyclerViewAdapter myRecyclerViewAdapter;

    public static MainFragment getInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(MyViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initViews();
        initEvents();
    }

    private void initViews() {
        myRecyclerViewAdapter = new MyRecyclerViewAdapter((MainActivity) requireActivity());
        binding.rvApps.setAdapter(myRecyclerViewAdapter);
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<Boolean> emitter) throws Throwable {
                PackageManager packageManager = requireContext().getPackageManager();
                PackageInfo sysInfo = packageManager.getPackageInfo("android", PackageManager.GET_SIGNATURES);
                List<MyAppConfig> unInstalledConfigs = new ArrayList<>(myViewModel.myAppConfigHashMap.values());
                List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_META_DATA | PackageManager.GET_SIGNATURES);
                for (PackageInfo e : packageInfoList) {
                    if (!e.applicationInfo.sourceDir.startsWith("/data/app/") || Arrays.equals(sysInfo.signatures, e.signatures)) {
                        continue;
                    }
                    String label = e.applicationInfo.loadLabel(packageManager).toString();
                    Drawable icon = e.applicationInfo.loadIcon(packageManager);
                    Field field = ApplicationInfo.class.getDeclaredField("primaryCpuAbi");
                    field.setAccessible(true);
                    String primaryCpuAbi = (String) field.get(e.applicationInfo);
                    MyAppConfig myAppConfig = myViewModel.myAppConfigHashMap.get(e.packageName);
                    myAppConfig = myAppConfig != null ? myAppConfig : new MyAppConfig(e.packageName, label, 0, false);
                    unInstalledConfigs.remove(myAppConfig);
                    myViewModel.myAppDescribeList.add(new MyAppDescribe(e.packageName, label, primaryCpuAbi, icon, e, myAppConfig));
                }
                for (MyAppConfig e : unInstalledConfigs) {
                    myViewModel.myAppDescribeList.add(new MyAppDescribe(e.packageName, e.label, null, ResourcesCompat.getDrawable(getResources(), R.drawable.svg_uninstalled, null), null, e));
                }
                myViewModel.myAppDescribeList.sort(new Comparator<MyAppDescribe>() {
                    @Override
                    public int compare(MyAppDescribe o1, MyAppDescribe o2) {
                        return Collator.getInstance(Locale.CHINESE).compare(o1.label, o2.label);
                    }
                });
                myViewModel.myAppDescribeListFilter.addAll(myViewModel.myAppDescribeList);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                binding.rvApps.setVisibility(View.INVISIBLE);
                binding.pbLoading.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull Boolean aBoolean) {

            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                myRecyclerViewAdapter.notifyDataSetChanged();
                binding.rvApps.setVisibility(View.VISIBLE);
                binding.pbLoading.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void initEvents() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint.equals("@on")) {
                    myViewModel.myAppDescribeListFilter.clear();
                    for (MyAppDescribe e : myViewModel.myAppDescribeList) {
                        if (e.myAppConfig.enable) {
                            myViewModel.myAppDescribeListFilter.add(e);
                        }
                    }
                    return new FilterResults();
                }
                if (constraint.equals("@off")) {
                    myViewModel.myAppDescribeListFilter.clear();
                    for (MyAppDescribe e : myViewModel.myAppDescribeList) {
                        if (!e.myAppConfig.enable) {
                            myViewModel.myAppDescribeListFilter.add(e);
                        }
                    }
                    return new FilterResults();
                }
                if (constraint.equals("@32")) {
                    myViewModel.myAppDescribeListFilter.clear();
                    for (MyAppDescribe e : myViewModel.myAppDescribeList) {
                        if (e.primaryCpuAbi == null || !e.primaryCpuAbi.contains("64")) {
                            myViewModel.myAppDescribeListFilter.add(e);
                        }
                    }
                    return new FilterResults();
                }
                if (constraint.equals("@64")) {
                    myViewModel.myAppDescribeListFilter.clear();
                    for (MyAppDescribe e : myViewModel.myAppDescribeList) {
                        if (e.primaryCpuAbi == null || e.primaryCpuAbi.contains("64")) {
                            myViewModel.myAppDescribeListFilter.add(e);
                        }
                    }
                    return new FilterResults();
                }
                if (constraint.toString().startsWith("@")) {
                    myViewModel.myAppDescribeListFilter.clear();
                    myViewModel.myAppDescribeListFilter.addAll(myViewModel.myAppDescribeList);
                    return new FilterResults();
                } else {
                    myViewModel.myAppDescribeListFilter.clear();
                    for (MyAppDescribe e : myViewModel.myAppDescribeList) {
                        if (e.label.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            myViewModel.myAppDescribeListFilter.add(e);
                        }
                    }
                    return new FilterResults();
                }
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                myRecyclerViewAdapter.notifyDataSetChanged();
            }
        };
        binding.searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filter.filter(s.toString().trim());
            }
        });
        binding.rvApps.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (binding.searchBox.hasFocus()) {
                    binding.getRoot().requestFocus();
                }
                return false;
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            myRecyclerViewAdapter.notifyItemChanged(myViewModel.curPosition + 1);
        }
    }
}