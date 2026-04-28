package Yu.code.toolbox.ui.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentSettingsBinding;
import Yu.code.toolbox.Utils.DialogUtils;

public class SettingsFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final int REQUEST_CODE_TAKE_PHOTO = 1002;

    private FragmentSettingsBinding binding;
    private UserProfileManager profileManager;
    private File avatarFile;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        profileManager = new UserProfileManager(requireContext());

        initUserInfo();
        initVersion();
        setupListeners();

        return root;
    }

    private void initUserInfo() {
        binding.textUsernameValue.setText(profileManager.getUserName());
        binding.textBioValue.setText(profileManager.getUserBio());
    }

    private void initVersion() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            binding.textVersion.setText("v" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.textVersion.setText("v1.0.0");
        }
    }

    private void setupListeners() {
        binding.itemUsername.setOnClickListener(v -> {
            showEditDialog("用户名", profileManager.getUserName(), (value) -> {
                profileManager.setUserName(value);
                binding.textUsernameValue.setText(value);
            });
        });

        binding.itemBio.setOnClickListener(v -> {
            showEditDialog("简介", profileManager.getUserBio(), (value) -> {
                profileManager.setUserBio(value);
                binding.textBioValue.setText(value);
            });
        });

        binding.itemAvatar.setOnClickListener(v -> {
            showAvatarDialog();
        });

        binding.itemBackup.setOnClickListener(v -> {
            // TODO: 实现数据备份功能
        });

        binding.itemRestore.setOnClickListener(v -> {
            // TODO: 实现数据恢复功能
        });

        binding.itemClearCache.setOnClickListener(v -> {
            // TODO: 实现清除缓存功能
        });
    }

    private void showEditDialog(String title, String currentValue, OnValueChangedListener listener) {
        DialogUtils.showInputDialog(requireContext(), title, "", currentValue, newValue -> {
            if (!newValue.isEmpty()) {
                listener.onChanged(newValue);
            }
        });
    }

    private void showAvatarDialog() {
        String[] options = {"从相册选择", "拍照", "使用默认头像"};
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("选择头像")
                .setItems(options, (dialogInterface, which) -> {
                    switch (which) {
                        case 0:
                            pickImageFromGallery();
                            break;
                        case 1:
                            takePhoto();
                            break;
                        case 2:
                            setDefaultAvatar();
                            break;
                    }
                })
                .create();

        dialog.show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            // 创建临时文件保存照片
            try {
                avatarFile = createImageFile();
                if (avatarFile != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(avatarFile));
                    startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "avatar_" + System.currentTimeMillis();
        File storageDir = requireContext().getFilesDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void setDefaultAvatar() {
        profileManager.setUserAvatar("");
    }

    private void saveAvatar(Bitmap bitmap) {
        try {
            File avatarDir = new File(requireContext().getFilesDir(), "avatars");
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }
            File avatarFile = new File(avatarDir, "avatar.jpg");
            FileOutputStream fos = new FileOutputStream(avatarFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            profileManager.setUserAvatar(avatarFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == requireActivity().RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_PICK_IMAGE:
                    if (data != null && data.getData() != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(), data.getData());
                            saveAvatar(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case REQUEST_CODE_TAKE_PHOTO:
                    if (avatarFile != null && avatarFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                        saveAvatar(bitmap);
                    }
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface OnValueChangedListener {
        void onChanged(String value);
    }
}