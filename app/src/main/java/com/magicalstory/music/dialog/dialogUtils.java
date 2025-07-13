package com.magicalstory.music.dialog;

/**
 * @Classname: dialogUtils
 * @Auther: Created by 奇谈君 on 2024/5/12.
 * @Description: dialog工具类
 */

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.DialogInputBinding;
import com.magicalstory.music.utils.app.ToastUtils;


public class dialogUtils {
    private AlertDialog horizontalProgressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private static dialogUtils instance;
    private ProgressDialog progressDialog;
    private static androidx.appcompat.app.AlertDialog lastAlertDialog;
    private static androidx.appcompat.app.AlertDialog lastInputDialog;
    private static androidx.appcompat.app.AlertDialog lastMessageDialog;

    private dialogUtils() {
        // 私有构造函数，避免外部创建实例
    }


    public static dialogUtils getInstance() {
        if (instance == null) {
            synchronized (dialogUtils.class) {
                if (instance == null) {
                    instance = new dialogUtils();
                }
            }
        }
        return instance;
    }


    public interface onclick {
        void click_confirm();

        void click_cancel();

        void click_three();

    }

    public interface onclick_with_dismiss {
        void click_confirm();

        void click_cancel();

        void click_three();

        void dismiss();
    }

    public interface onclick_input {
        void click_confirm(String text);
    }


    /* @setIcon 设置对话框图标
     * @setTitle 设置对话框标题
     * @setMessage 设置对话框消息提示
     * setXXX方法返回Dialog对象，因此可以链式设置属性
     * */
    public static void showAlertDialog(Context context, String title, String message, String confirm, String cancel, String three, boolean cancelable,
                                       onclick_with_dismiss onclick) {
        // 先尝试关闭之前的对话框，防止窗口泄漏
        dismissLastAlertDialog();

        MaterialAlertDialogBuilder normalDialog = new MaterialAlertDialogBuilder(context);
        normalDialog.setTitle(title);
        normalDialog.setCancelable(cancelable);
        normalDialog.setMessage(message);
        normalDialog.setPositiveButton(confirm.trim(),
                (dialog, which) -> onclick.click_confirm());

        if (cancel != null && !cancel.isEmpty()) {
            normalDialog.setNegativeButton(cancel.trim(),
                    (dialog, which) -> onclick.click_cancel());
        }

        if (three != null && !three.isEmpty()) {
            normalDialog.setNeutralButton(three.trim(),
                    (dialog, which) -> onclick.click_three());
        }

        normalDialog.setOnDismissListener(dialog -> {
            // 对话框消失时，移除lastAlertDialog引用
            if (dialog == lastAlertDialog) {
                lastAlertDialog = null;
            }
            onclick.dismiss();
        });

        // 显示
        lastAlertDialog = normalDialog.show();
    }

    /**
     * 关闭上一个警告对话框，防止窗口泄漏
     */
    public static void dismissLastAlertDialog() {
        try {
            if (lastAlertDialog != null && lastAlertDialog.isShowing()) {
                lastAlertDialog.dismiss();
                lastAlertDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭所有对话框，应在Activity的onDestroy中调用
     */
    public static void dismissAllDialogs() {
        dismissLastAlertDialog();
        dismissLastInputDialog();
        dismissLastMessageDialog();
        getInstance().dismissProgressDialog();
    }


    public static void showInputDialog(Context context, String title, String button, String text, String hint, String[] titles, String error,
                                       boolean mup, boolean canEmpty, onclick_input onclick_input) {
        // 关闭上一个输入对话框
        dismissLastInputDialog();

        DialogInputBinding dialogInputBinding = DialogInputBinding.inflate(((AppCompatActivity) context).getLayoutInflater());


        if (!text.isEmpty()) {
            dialogInputBinding.textInputEditText.setText(text);
        }

        if (!hint.isEmpty()) {
            dialogInputBinding.textInputEditText.setHint(hint);
        }
        if (mup) {
            dialogInputBinding.textInputEditText.setMaxLines(100);
            dialogInputBinding.textInputEditText.setSingleLine(false);
        }

        //显示输入法
        dialogInputBinding.textInputEditText.requestFocus();
        InputMethodManager inputManager = (InputMethodManager) dialogInputBinding.textInputEditText
                .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(dialogInputBinding.textInputEditText, 0);

        //监听输入法变更
        if (titles != null) {
            dialogInputBinding.textInputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    boolean duplicate = false;//是否重复
                    String input = charSequence.toString().toLowerCase();
                    for (String s : titles) {
                        if (s.isEmpty()) {
                            continue;
                        }
                        if (s.toLowerCase().equals(input)) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (duplicate) {
                        dialogInputBinding.textInputLayout.setError(error);
                    } else {
                        dialogInputBinding.textInputLayout.setError("");

                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.dialog);
        builder.setTitle(title).setView(dialogInputBinding.getRoot()).setNegativeButton(context.getString(R.string.title_cancel), null);
        builder.setPositiveButton(button, (dialog, which) -> {
        });


        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            if (dialogInterface == lastInputDialog) {
                lastInputDialog = null;
            }
        });
        dialog.show();
        lastInputDialog = dialog;


        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text1 = dialogInputBinding.textInputEditText.getText().toString();
            if (text1.isEmpty() && !canEmpty) {
                dialogInputBinding.textInputLayout.setError(context.getResources().getString(R.string.input_cannot_empty));
                return;
            }
            if (titles != null) {

                boolean duplicate = false;//是否重复
                String input = text1.toLowerCase();
                for (String s : titles) {
                    if (s.isEmpty()) {
                        continue;
                    }
                    if (s.toLowerCase().equals(input)) {
                        duplicate = true;
                        break;
                    }
                }

                if (duplicate) {
                    dialogInputBinding.textInputLayout.setError(error);
                    return;
                }
            }

            onclick_input.click_confirm(text1);
            dialog.dismiss();
            if (dialog == lastInputDialog) {
                lastInputDialog = null;
            }
        });
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * 关闭上一个输入对话框
     */
    public static void dismissLastInputDialog() {
        try {
            if (lastInputDialog != null && lastInputDialog.isShowing()) {
                lastInputDialog.dismiss();
                lastInputDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showProgressDialog(Context context, String message) {
        dismissProgressDialog(); // 避免重复显示
        progressDialog = new ProgressDialog(context, R.style.ThemeProgressDialogStyle);
        progressDialog.setMessage(message);
        // 设置进度条颜色
        progressDialog.setCancelable(false); // 设置不可取消
        progressDialog.show();
    }

    public void changeProgressBarText(Context context, String text) {
        if (progressDialog != null) {
            progressDialog.setMessage(text);
        } else {
            showProgressDialog(context, text);
        }
    }

    public void dismissProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void changeProgressDialogText(String text) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(text);
        }
    }

    public void showTipsDialog(final Context context, final String title, final String message) {
        showMessageDialog(context, title, message, context.getString(R.string.dialog_ok), "", "", true, new CustomMessageDialogListener() {
            @Override
            public void onPositiveClick() {

            }

            @Override
            public void onNegativeClick() {

            }

            @Override
            public void onNeutralClick() {

            }
        });
    }

    public void showMessageDialog(final Context context, final String title, final String message, final String positiveText,
                                  final String negativeText, final String neutralText, final boolean cancelable,
                                  final CustomMessageDialogListener listener) {
        // 在主线程中运行
        new Handler(Looper.getMainLooper()).post(() -> {
            // 关闭之前的消息对话框
            dismissLastMessageDialog();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title)
                    .setMessage(message)
                    .setCancelable(cancelable);

            // 设置监听器
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (listener != null) listener.onPositiveClick();
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        if (listener != null) listener.onNeutralClick();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        if (listener != null) listener.onNegativeClick();
                        break;
                }
            };

            // 添加按钮
            if (positiveText != null && !positiveText.isEmpty())
                builder.setPositiveButton(positiveText, dialogClickListener);

            if (negativeText != null && !negativeText.isEmpty())
                builder.setNegativeButton(negativeText, dialogClickListener);

            if (neutralText != null && !neutralText.isEmpty())
                builder.setNeutralButton(neutralText, dialogClickListener);

            // 添加对话框消失监听器
            builder.setOnDismissListener(dialog -> {
                if (dialog == lastMessageDialog) {
                    lastMessageDialog = null;
                }
            });

            // 显示对话框并保存引用
            lastMessageDialog = builder.show();
        });
    }

    /**
     * 关闭上一个消息对话框
     */
    public static void dismissLastMessageDialog() {
        try {
            if (lastMessageDialog != null && lastMessageDialog.isShowing()) {
                lastMessageDialog.dismiss();
                lastMessageDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface CustomMessageDialogListener {
        void onPositiveClick();

        void onNegativeClick();

        void onNeutralClick();
    }


    public interface InputDialogListener {
        void onInputProvided(String input);

        default void onCancel() {
            // 默认空实现，子类可以选择性地重写
        }
    }

    public void showInputDialog(final Context context, final String title, final String hint, final String text, final boolean numericInput,
                                final String tips, final InputDialogListener listener) {
        new Handler(Looper.getMainLooper()).post(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title);
            builder.setCancelable(true); // 确保对话框可以被取消

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(64, 32, 64, 32);

            // 创建输入框
            final EditText input = new EditText(context);
            input.setHint(hint);
            input.setText(text);
            if (numericInput) {
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            layout.addView(input);

            // 如果tips不为空，添加TextView到编辑框下方
            if (tips != null && !tips.isEmpty()) {
                TextView tipTextView = new TextView(context);
                tipTextView.setText(tips);

                // 设置TextView的布局参数
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.topMargin = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics()
                );
                tipTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tipTextView.setTextColor(context.getResources().getColor(R.color.text_secondary));
                tipTextView.setLayoutParams(layoutParams);

                // 添加TextView到布局
                layout.addView(tipTextView);
            }

            builder.setView(layout);

            builder.setPositiveButton(context.getString(R.string.dialog_confirm), (dialog, which) -> {
                String userInput = input.getText().toString();
                listener.onInputProvided(userInput);
            });

            if (!text.isEmpty()) {
                input.setSelection(text.length());
            }

            builder.setNegativeButton(context.getString(R.string.dialog_cancel), (dialog, which) -> {
                if (listener != null) {
                    listener.onCancel();
                }
            });

            final androidx.appcompat.app.AlertDialog dialog = builder.create();

            // 设置对话框取消监听器
            dialog.setOnCancelListener(dialogInterface -> {
                if (listener != null) {
                    listener.onCancel();
                }
            });

            dialog.setOnShowListener(dialogInterface -> {
                input.requestFocus();
                input.postDelayed(() -> {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200); // 延时以确保对话框已经完全显示
            });

            dialog.show();
        });
    }

    public void showMenuDialog(Context context, String title, String[] menus, DialogInterface.OnClickListener listener) {
        new Handler(Looper.getMainLooper()).post(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title);
            builder.setItems(menus, listener);
            androidx.appcompat.app.AlertDialog dialog = builder.create();
            dialog.show();
        });
    }


    public void showMessageDialog2(final Context context, final String title, final String message, final String positiveText,
                                   final String negativeText, final String neutralText, final boolean cancelable,
                                   final CustomMessageDialogListener listener) {
        new Handler(Looper.getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title)
                    .setMessage(message)
                    .setCancelable(cancelable);

            // 设置监听器
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (listener != null) listener.onPositiveClick();
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        if (listener != null) listener.onNeutralClick();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        if (listener != null) listener.onNegativeClick();
                        break;
                }
            };

            // 设置按钮
            if (positiveText != null && !positiveText.isEmpty()) {
                builder.setPositiveButton(positiveText, dialogClickListener);
            }
            if (neutralText != null && !neutralText.isEmpty()) {
                builder.setNeutralButton(neutralText, dialogClickListener);
            }
            if (negativeText != null && !negativeText.isEmpty()) {
                builder.setNegativeButton(negativeText, dialogClickListener);
            }

            AlertDialog dialog = builder.create();


            dialog.show();


            if (positiveText.equals(context.getString(R.string.dialog_delete)) || positiveText.equals(context.getString(R.string.dialog_read)) || positiveText.equals(context.getString(R.string.dialog_dismiss)) || positiveText.equals(context.getString(R.string.dialog_logout)) || positiveText.equals(context.getString(R.string.dialog_exit)) || positiveText.equals(context.getString(R.string.dialog_recall)) || positiveText.equals(context.getString(R.string.dialog_unfollow)) || positiveText.equals(context.getString(R.string.dialog_clear))) {
                // 获取 AlertDialog 的按钮
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setTextColor(context.getResources().getColor(R.color.md_theme_error));
            }

            if (negativeText.equals(context.getString(R.string.dialog_delete)) || negativeText.equals(context.getString(R.string.dialog_read)) || negativeText.equals(context.getString(R.string.dialog_dismiss)) || negativeText.equals(context.getString(R.string.dialog_logout)) || negativeText.equals(context.getString(R.string.dialog_exit)) || negativeText.equals(context.getString(R.string.dialog_recall)) || negativeText.equals(context.getString(R.string.dialog_unfollow)) || negativeText.equals(context.getString(R.string.dialog_clear))) {
                // 获取 AlertDialog 的按钮
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                positiveButton.setTextColor(context.getResources().getColor(R.color.md_theme_error));
            }

        });


    }

    public void showSingleChoiceDialog(Context context, String title, String[] items, String currentItem, DialogInterface.OnClickListener listener) {
        new Handler(Looper.getMainLooper()).post(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title);

            // 找到当前选中项的索引
            int currentIndex = 0;
            if (currentItem != null) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i].equals(currentItem)) {
                        currentIndex = i;
                        break;
                    }
                }
            }

            // 设置点击选项时的监听器，在回调后自动关闭对话框
            builder.setSingleChoiceItems(items, currentIndex, (dialog, which) -> {
                if (listener != null) {
                    listener.onClick(dialog, which);
                }
                dialog.dismiss();
            });

            // 添加取消按钮
            builder.setNegativeButton(context.getString(R.string.dialog_cancel), (dialog, which) -> dialog.dismiss());

            // 应用 Material You 主题
            androidx.appcompat.app.AlertDialog dialog = builder.create();


            dialog.show();
        });
    }

    // 新增的显示横向进度条的dialog
    public void showHorizontalProgressDialog(Context context, String title, int progress, int maxProgress) {
        if (horizontalProgressDialog != null && horizontalProgressDialog.isShowing()) {
            updateHorizontalProgress(progress, maxProgress);
            return;
        }

        dismissHorizontalProgressDialog(); // 避免重复显示

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CommProgressDialog);
        builder.setTitle(title);

        LinearLayout layout = new LinearLayout(context);
        layout.setBackgroundColor(context.getResources().getColor(R.color.dialog_background));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(maxProgress);
        progressBar.setProgress(progress);

        progressText = new TextView(context);

        //if (maxProgress > 100) {
        //    progressText.setText(FileSizeUtil.FormetFileSize(progress) + "/" + FileSizeUtil.FormetFileSize(maxProgress));
        //} else {
        //
        //}

        progressText.setText(progress + "/" + maxProgress);
        progressText.setPadding(0, 16, 0, 0);

        layout.addView(progressText);
        layout.addView(progressBar);


        builder.setView(layout);

        horizontalProgressDialog = builder.create();

        horizontalProgressDialog.setCancelable(false);
        horizontalProgressDialog.show();
    }

    public void updateHorizontalProgress(Context context, String title, int progress, int maxProgress) {
        if (horizontalProgressDialog == null || !horizontalProgressDialog.isShowing()) {
            showHorizontalProgressDialog(context, title, progress, maxProgress);
        } else {
            updateHorizontalProgress(progress, maxProgress);
        }
    }

    public void updateHorizontalProgress(int progress, int maxProgress) {
        if (horizontalProgressDialog != null && horizontalProgressDialog.isShowing()) {
            progressBar.setProgress(progress);
            progressBar.setMax(maxProgress);

            //if (maxProgress > 100) {
            //    progressText.setText(FileSizeUtil.FormetFileSize(progress) + "/" + FileSizeUtil.FormetFileSize(maxProgress));
            //} else {
            //    progressText.setText(progress + "/" + maxProgress);
            //}
            progressText.setText(progress + "/" + maxProgress);

        }
    }

    public void dismissHorizontalProgressDialog() {
        if (horizontalProgressDialog != null && horizontalProgressDialog.isShowing()) {
            horizontalProgressDialog.dismiss();
            horizontalProgressDialog = null;
        }
    }

    /**
     * 显示带有自定义View的对话框
     */
    public static void showCustomViewDialog(Context context, String title, android.view.View customView,
                                            String confirm, String cancel, String neutral, boolean cancelable,
                                            onclick_with_dismiss onclick) {
        // 先尝试关闭之前的对话框，防止窗口泄漏
        dismissLastAlertDialog();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title)
                .setCancelable(cancelable)
                .setView(customView);

        // 设置确认按钮
        if (confirm != null && !confirm.isEmpty()) {
            builder.setPositiveButton(confirm.trim(), (dialog, which) -> onclick.click_confirm());
        }

        // 设置取消按钮
        if (cancel != null && !cancel.isEmpty()) {
            builder.setNegativeButton(cancel.trim(), (dialog, which) -> onclick.click_cancel());
        }

        // 设置中性按钮
        if (neutral != null && !neutral.isEmpty()) {
            builder.setNeutralButton(neutral.trim(), (dialog, which) -> onclick.click_three());
        }

        // 设置对话框消失监听器
        builder.setOnDismissListener(dialog -> {
            if (dialog == lastAlertDialog) {
                lastAlertDialog = null;
            }
            onclick.dismiss();
        });

        // 显示对话框并保存引用
        lastAlertDialog = builder.show();
    }

    public interface OnNumberInputListener {
        void onConfirm(int number);
    }

    public void showNumberInputDialog(Context context, String title, String hint, int defaultValue, OnNumberInputListener listener) {
        new Handler(Looper.getMainLooper()).post(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title);
            builder.setCancelable(true);

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(64, 32, 64, 32);

            final EditText input = new EditText(context);
            input.setHint(hint);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText(String.valueOf(defaultValue));
            input.setSelection(String.valueOf(defaultValue).length());
            layout.addView(input);

            builder.setView(layout);

            builder.setPositiveButton(context.getString(R.string.dialog_confirm), (dialog, which) -> {
                String userInput = input.getText().toString();
                if (!userInput.isEmpty()) {
                    try {
                        int number = Integer.parseInt(userInput);
                        listener.onConfirm(number);
                    } catch (NumberFormatException e) {
                        ToastUtils.showToast(context, context.getString(R.string.error_invalid_number));
                    }
                }
            });

            builder.setNegativeButton(context.getString(R.string.dialog_cancel), null);

            final androidx.appcompat.app.AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> {
                input.requestFocus();
                input.postDelayed(() -> {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200);
            });

            dialog.show();
        });
    }
}
