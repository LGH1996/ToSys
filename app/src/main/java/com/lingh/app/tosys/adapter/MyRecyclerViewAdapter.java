package com.lingh.app.tosys.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.lingh.app.tosys.BuildConfig;
import com.lingh.app.tosys.activity.MainActivity;
import com.lingh.app.tosys.databinding.ViewItemBinding;
import com.lingh.app.tosys.databinding.ViewSettingBinding;
import com.lingh.app.tosys.dialog.NormalDialog;
import com.lingh.app.tosys.dialog.WaitingDialog;
import com.lingh.app.tosys.fragment.ConfigFragment;
import com.lingh.app.tosys.myclass.MyAppDescribe;
import com.lingh.app.tosys.myclass.MyUpdateMessage;
import com.lingh.app.tosys.myclass.MyViewModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final MainActivity mainActivity;
    private final MyViewModel myViewModel;

    public MyRecyclerViewAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.myViewModel = new ViewModelProvider(mainActivity, new ViewModelProvider.AndroidViewModelFactory(mainActivity.getApplication())).get(MyViewModel.class);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            ViewSettingBinding binding = ViewSettingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.version.setText("版本：" + BuildConfig.VERSION_NAME);
            binding.expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (binding.setting.getVisibility() == View.VISIBLE) {
                        binding.expand.setRotation(180);
                        binding.divider.setVisibility(View.GONE);
                        binding.setting.setVisibility(View.GONE);
                    } else {
                        binding.expand.setRotation(0);
                        binding.divider.setVisibility(View.VISIBLE);
                        binding.setting.setVisibility(View.VISIBLE);
                    }
                }
            });
            binding.update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Observable<MyUpdateMessage> observable = myViewModel.myHttpRequest.getUpdateMessage();
                    observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<MyUpdateMessage>() {
                        WaitingDialog waitDialog;

                        @Override
                        public void onSubscribe(Disposable d) {
                            waitDialog = new WaitingDialog();
                            waitDialog.setCancelable(false);
                            waitDialog.show(mainActivity.getSupportFragmentManager(), null);
                        }

                        @Override
                        public void onNext(MyUpdateMessage updateMessage) {
                            try {
                                String appName = updateMessage.assets.get(0).name;
                                Matcher matcher = Pattern.compile("\\d+").matcher(appName);
                                if (matcher.find()) {
                                    int newVersion = Integer.parseInt(matcher.group());
                                    if (newVersion > BuildConfig.VERSION_CODE) {
                                        NormalDialog dialog = new NormalDialog();
                                        dialog.setShowMsg(updateMessage.body);
                                        dialog.setOnNavigationButtonClickListener("取消", null);
                                        dialog.setOnPositiveButtonClickListener("确定", new Runnable() {
                                            @Override
                                            public void run() {
                                                mainActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateMessage.assets.get(0).browser_download_url)));
                                            }
                                        });
                                        dialog.show(mainActivity.getSupportFragmentManager(), null);
                                    } else {
                                        Toast.makeText(mainActivity, "当前已是最新版本", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Throwable e) {
                                Toast.makeText(mainActivity, "解析版本号时出现错误", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                            Toast.makeText(mainActivity, "查询新版本时出现错误", Toast.LENGTH_SHORT).show();
                            waitDialog.dismiss();
                            e.printStackTrace();
                        }

                        @Override
                        public void onComplete() {
                            waitDialog.dismiss();
                        }
                    });
                }
            });
            binding.contact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openChat = new Intent(Intent.ACTION_VIEW, Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3Dw3oVSTyApiatRQNpBpZbdxWYVdK5f-08"));
                    if (openChat.resolveActivity(mainActivity.getPackageManager()) != null) {
                        mainActivity.startActivity(openChat);
                    }
                }
            });
            binding.expand.post(new Runnable() {
                @Override
                public void run() {
                    binding.expand.callOnClick();
                }
            });
            return new HeaderViewHolder(binding.getRoot(), binding);
        } else {
            ViewItemBinding binding = ViewItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyAppDescribe myAppDescribe = (MyAppDescribe) v.getTag();
                    if (myAppDescribe.packageInfo != null) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + myAppDescribe.packageName));
                        mainActivity.startActivity(intent);
                    } else {
                        Toast.makeText(mainActivity, "该应用未安装", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            binding.item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!myViewModel.isModuleValid()) {
                        Toast.makeText(mainActivity, "模块未激活", Toast.LENGTH_SHORT).show();
                    } else if (!myViewModel.isModuleEnabled) {
                        Toast.makeText(mainActivity, "模块被禁用", Toast.LENGTH_SHORT).show();
                    } else {
                        myViewModel.curPosition = (int) v.getTag();
                        Fragment fragment = ConfigFragment.getInstance();
                        mainActivity.gotoSecondFragment(fragment);
                    }
                }
            });
            return new NormalViewHolder(binding.getRoot(), binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NormalViewHolder) {
            NormalViewHolder normalViewHolder = (NormalViewHolder) holder;
            MyAppDescribe myAppDescribe = myViewModel.myAppDescribeListFilter.get(position - 1);
            normalViewHolder.binding.icon.setImageDrawable(myAppDescribe.appIcon);
            normalViewHolder.binding.label.setText(myAppDescribe.label + (myAppDescribe.primaryCpuAbi == null ? "" : " (" + myAppDescribe.primaryCpuAbi + ")"));
            normalViewHolder.binding.pkgName.setText(myAppDescribe.packageName);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) normalViewHolder.binding.item.getLayoutParams();
            lp.bottomMargin = position - 1 == myViewModel.myAppDescribeListFilter.size() - 1 ? normalViewHolder.bottomMargin * 2 : normalViewHolder.bottomMargin;
            normalViewHolder.binding.item.setLayoutParams(lp);
            int enhanceFlag = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_PERSISTENT;
            if (myAppDescribe.packageInfo != null) {
                boolean hasSysSharedUserId = TextUtils.equals(myAppDescribe.packageInfo.sharedUserId, "android.uid.system");
                if (myAppDescribe.myAppConfig.enable) {
                    StringBuilder stringBuilder = new StringBuilder();
                    if (hasSysSharedUserId) {
                        stringBuilder.append("status: on");
                    } else {
                        stringBuilder.append("status: on (重新安装后生效)");
                    }
                    if (myAppDescribe.myAppConfig.flag == enhanceFlag) {
                        stringBuilder.append("\n");
                        stringBuilder.append("flag: system persist");
                    }
                    normalViewHolder.binding.msg.setVisibility(View.VISIBLE);
                    normalViewHolder.binding.msg.setText(stringBuilder.toString());
                } else {
                    if (hasSysSharedUserId) {
                        normalViewHolder.binding.msg.setVisibility(View.VISIBLE);
                        normalViewHolder.binding.msg.setText("status: off (重新安装后生效)");
                    } else {
                        normalViewHolder.binding.msg.setVisibility(View.GONE);
                    }
                }
            } else {
                normalViewHolder.binding.msg.setVisibility(View.VISIBLE);
                normalViewHolder.binding.msg.setText("status: on (该应用未安装)");
            }
            normalViewHolder.binding.setting.setTag(myAppDescribe);
            normalViewHolder.binding.item.setTag(position - 1);
        }
    }

    @Override
    public int getItemCount() {
        return myViewModel.myAppDescribeListFilter.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class NormalViewHolder extends RecyclerView.ViewHolder {
        public ViewItemBinding binding;
        public int topMargin;
        public int bottomMargin;

        public NormalViewHolder(View itemView, ViewItemBinding binding) {
            super(itemView);
            this.binding = binding;
            topMargin = ((LinearLayout.LayoutParams) binding.item.getLayoutParams()).topMargin;
            bottomMargin = ((LinearLayout.LayoutParams) binding.item.getLayoutParams()).bottomMargin;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public ViewSettingBinding binding;

        public HeaderViewHolder(View itemView, ViewSettingBinding binding) {
            super(itemView);
            this.binding = binding;
        }
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
