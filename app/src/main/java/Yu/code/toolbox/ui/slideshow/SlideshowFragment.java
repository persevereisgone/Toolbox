package Yu.code.toolbox.ui.slideshow;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.io.File;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentSlideshowBinding;
import Yu.code.toolbox.ui.settings.UserProfileManager;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initUserInfo();
        initVersion();
        setupClickListeners();

        return root;
    }

    private void initUserInfo() {
        UserProfileManager profileManager = new UserProfileManager(requireContext());
        binding.userName.setText(profileManager.getUserName());
        binding.userBio.setText(profileManager.getUserBio());
        loadAvatar(profileManager);
    }

    private void loadAvatar(UserProfileManager profileManager) {
        String avatarPath = profileManager.getUserAvatar();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File file = new File(avatarPath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarPath);
                binding.userAvatar.setImageBitmap(bitmap);
                return;
            }
        }
        binding.userAvatar.setImageResource(R.drawable.ic_baseline_person_24);
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

    private void setupClickListeners() {
        binding.itemSettings.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_settings);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}