package com.lingh.app.tosys.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.lingh.app.tosys.databinding.ViewDialogBinding;

public class NormalDialog extends DialogFragment {
    private Runnable onPositiveButtonClickListener;
    private Runnable onNavigationButtonClickListener;
    private String showMsg;
    private String positiveText;
    private String navigationText;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ViewDialogBinding binding = ViewDialogBinding.inflate(LayoutInflater.from(requireContext()));
        binding.msg.setText(showMsg);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(binding.getRoot());
        if (positiveText != null) {
            builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (onPositiveButtonClickListener != null) {
                        onPositiveButtonClickListener.run();
                    }
                }
            });
        }
        if (navigationText != null) {
            builder.setNegativeButton(navigationText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (onNavigationButtonClickListener != null) {
                        onNavigationButtonClickListener.run();
                    }
                }
            });
        }
        return builder.create();
    }

    public void setShowMsg(String msg) {
        showMsg = msg;
    }

    public void setOnPositiveButtonClickListener(String str, Runnable onPositiveButtonClickListener) {
        this.positiveText = str;
        this.onPositiveButtonClickListener = onPositiveButtonClickListener;
    }

    public void setOnNavigationButtonClickListener(String str, Runnable onNavigationButtonClickListener) {
        this.navigationText = str;
        this.onNavigationButtonClickListener = onNavigationButtonClickListener;
    }
}
