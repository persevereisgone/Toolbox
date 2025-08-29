package Yu.code.toolbox.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentHomeBinding;
import Yu.code.toolbox.ui.function.FunctionFragment;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // 静态变量，用于存储当前实例
    private static HomeFragment currentInstance;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // 设置当前实例
        currentInstance = this;

        return root;
    }

    // 显示功能界面
    public void showFunction(int layoutId) {
        binding.homeDefaultLayout.setVisibility(View.GONE);

        // 获取FunctionFragment并显示指定功能
        FunctionFragment functionFragment = (FunctionFragment) getChildFragmentManager()
                .findFragmentById(R.id.function_fragment);
        if (functionFragment != null) {
            functionFragment.showFunction(layoutId);
            // 通过Fragment的View设置可见性
            if (functionFragment.getView() != null) {
                functionFragment.getView().setVisibility(View.VISIBLE);
            }
        }
    }

    // 隐藏功能界面，显示默认界面
    public void hideFunction() {
        binding.homeDefaultLayout.setVisibility(View.VISIBLE);

        // 获取FunctionFragment并隐藏功能
        FunctionFragment functionFragment = (FunctionFragment) getChildFragmentManager()
                .findFragmentById(R.id.function_fragment);
        if (functionFragment != null) {
            functionFragment.hideFunction();
            // 通过Fragment的View设置可见性
            if (functionFragment.getView() != null) {
                functionFragment.getView().setVisibility(View.GONE);
            }
        }
    }

    // 静态方法，用于从外部调用showFunction
    public static void showFunctionStatic(int layoutId) {
        if (currentInstance != null) {
            currentInstance.showFunction(layoutId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // 清除当前实例
        if (currentInstance == this) {
            currentInstance = null;
        }
    }
}