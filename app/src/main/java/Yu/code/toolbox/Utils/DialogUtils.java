package Yu.code.toolbox.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;

import Yu.code.toolbox.R;

/**
 * 对话框工具类
 * 统一管理对话框的创建和按钮颜色设置
 */
public class DialogUtils {

    /**
     * 创建确认对话框
     * @param context 上下文
     * @param title 标题
     * @param message 消息
     * @param positiveText 确认按钮文字
     * @param negativeText 取消按钮文字
     * @param positiveListener 确认按钮监听器
     * @param negativeListener 取消按钮监听器
     * @return AlertDialog
     */
    public static AlertDialog createConfirmDialog(Context context, String title, String message,
                                                  String positiveText, String negativeText,
                                                  OnDialogButtonClickListener positiveListener,
                                                  OnDialogButtonClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);

        if (positiveText != null) {
            builder.setPositiveButton(positiveText, (dialog, which) -> {
                if (positiveListener != null) {
                    positiveListener.onClick(dialog, which);
                }
            });
        }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (dialog, which) -> {
                if (negativeListener != null) {
                    negativeListener.onClick(dialog, which);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮文字颜色
        if (positiveText != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_blue_dark));
        }
        if (negativeText != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    context.getResources().getColor(android.R.color.black));
        }

        return dialog;
    }

    /**
     * 创建危险操作确认对话框（删除、清空等）
     * @param context 上下文
     * @param title 标题
     * @param message 消息
     * @param positiveText 确认按钮文字
     * @param negativeText 取消按钮文字
     * @param positiveListener 确认按钮监听器
     * @param negativeListener 取消按钮监听器
     * @return AlertDialog
     */
    public static AlertDialog createDangerConfirmDialog(Context context, String title, String message,
                                                        String positiveText, String negativeText,
                                                        OnDialogButtonClickListener positiveListener,
                                                        OnDialogButtonClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);

        if (positiveText != null) {
            builder.setPositiveButton(positiveText, (dialog, which) -> {
                if (positiveListener != null) {
                    positiveListener.onClick(dialog, which);
                }
            });
        }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (dialog, which) -> {
                if (negativeListener != null) {
                    negativeListener.onClick(dialog, which);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮文字颜色（危险操作用红色）
        if (positiveText != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_red_dark));
        }
        if (negativeText != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    context.getResources().getColor(android.R.color.black));
        }

        return dialog;
    }

    /**
     * 创建带自定义视图的对话框
     * @param context 上下文
     * @param title 标题
     * @param view 自定义视图
     * @param positiveText 确认按钮文字
     * @param negativeText 取消按钮文字
     * @param positiveListener 确认按钮监听器
     * @param negativeListener 取消按钮监听器
     * @return AlertDialog
     */
    public static AlertDialog createCustomViewDialog(Context context, String title, View view,
                                                     String positiveText, String negativeText,
                                                     OnDialogButtonClickListener positiveListener,
                                                     OnDialogButtonClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view);

        if (positiveText != null) {
            builder.setPositiveButton(positiveText, (dialog, which) -> {
                if (positiveListener != null) {
                    positiveListener.onClick(dialog, which);
                }
            });
        }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (dialog, which) -> {
                if (negativeListener != null) {
                    negativeListener.onClick(dialog, which);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮文字颜色
        if (positiveText != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_blue_dark));
        }
        if (negativeText != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    context.getResources().getColor(android.R.color.black));
        }

        return dialog;
    }

    /**
     * 创建信息提示对话框
     * @param context 上下文
     * @param title 标题
     * @param message 消息
     * @param buttonText 按钮文字
     * @param listener 按钮监听器
     * @return AlertDialog
     */
    public static AlertDialog createInfoDialog(Context context, String title, String message,
                                               String buttonText, OnDialogButtonClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    if (listener != null) {
                        listener.onClick(dialog, which);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮文字颜色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                context.getResources().getColor(android.R.color.black));

        return dialog;
    }

    /**
     * 对话框按钮点击监听器接口
     */
    public interface OnDialogButtonClickListener {
        void onClick(DialogInterface dialog, int which);
    }

    /**
     * 对话框确认监听器接口（用于输入对话框）
     */
    public interface OnDialogConfirmListener {
        void onConfirm(String input);
    }

    /**
     * 对话框取消监听器接口（用于输入对话框）
     */
    public interface OnDialogCancelListener {
        void onCancel();
    }

    /**
     * 显示通用输入对话框
     * @param context 上下文
     * @param title 标题
     * @param hint 提示文字
     * @param defaultValue 默认值
     * @param confirmListener 确认监听器
     * @return AlertDialog
     */
    public static AlertDialog showInputDialog(Context context, String title, String hint,
                                              String defaultValue, OnDialogConfirmListener confirmListener) {
        return showInputDialog(context, title, hint, defaultValue, confirmListener, null);
    }

    /**
     * 显示通用输入对话框
     * @param context 上下文
     * @param title 标题
     * @param hint 提示文字
     * @param defaultValue 默认值
     * @param confirmListener 确认监听器
     * @param cancelListener 取消监听器
     * @return AlertDialog
     */
    public static AlertDialog showInputDialog(Context context, String title, String hint,
                                              String defaultValue, OnDialogConfirmListener confirmListener,
                                              OnDialogCancelListener cancelListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_input_common, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etInput = dialogView.findViewById(R.id.et_dialog_input);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        tvTitle.setText(title);
        etInput.setHint(hint);
        if (defaultValue != null) {
            etInput.setText(defaultValue);
            etInput.setSelection(defaultValue.length());
        }

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (confirmListener != null) {
                confirmListener.onConfirm(input);
            }
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }

    /**
     * 显示通用输入对话框（使用资源ID）
     * @param context 上下文
     * @param titleResId 标题资源ID
     * @param hintResId 提示文字资源ID
     * @param defaultValue 默认值
     * @param confirmListener 确认监听器
     * @return AlertDialog
     */
    public static AlertDialog showInputDialog(Context context, @StringRes int titleResId,
                                              @StringRes int hintResId, String defaultValue,
                                              OnDialogConfirmListener confirmListener) {
        return showInputDialog(context,
                context.getString(titleResId),
                context.getString(hintResId),
                defaultValue,
                confirmListener,
                null);
    }

    /**
     * 显示数字输入对话框
     * @param context 上下文
     * @param title 标题
     * @param hint 提示文字
     * @param defaultValue 默认值
     * @param confirmListener 确认监听器
     * @return AlertDialog
     */
    public static AlertDialog showNumberInputDialog(Context context, String title, String hint,
                                                   String defaultValue, OnDialogConfirmListener confirmListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_input_common, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etInput = dialogView.findViewById(R.id.et_dialog_input);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        tvTitle.setText(title);
        etInput.setHint(hint);
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (defaultValue != null) {
            etInput.setText(defaultValue);
            etInput.setSelection(defaultValue.length());
        }

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (confirmListener != null) {
                confirmListener.onConfirm(input);
            }
            dialog.dismiss();
        });

        dialog.show();
        return dialog;
    }
}
