package Yu.code.toolbox.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentHomeBinding;
import Yu.code.toolbox.ui.function.FunctionConstants;
import Yu.code.toolbox.ui.function.FunctionFragment;
import Yu.code.toolbox.ui.PanelGridAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private GridView gridView;
    private GridView betaGridView;

    // 静态变量，用于存储当前实例
    private static HomeFragment currentInstance;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        gridView = root.findViewById(R.id.home_panel_grid);
        betaGridView = root.findViewById(R.id.beta_panel_grid);

        // 可使用功能列表
        java.util.List<PanelGridAdapter.PanelItem> stableItems = new java.util.ArrayList<>();
        stableItems.add(new PanelGridAdapter.PanelItem(R.drawable.time, "定时返回"));
        stableItems.add(new PanelGridAdapter.PanelItem(R.drawable.statistics, "统计"));
        PanelGridAdapter stableAdapter = new PanelGridAdapter(requireContext(), stableItems);
        gridView.setAdapter(stableAdapter);

        // 测试版功能列表
        java.util.List<PanelGridAdapter.PanelItem> betaItems = new java.util.ArrayList<>();
        betaItems.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        PanelGridAdapter betaAdapter = new PanelGridAdapter(requireContext(), betaItems);
        betaGridView.setAdapter(betaAdapter);
        // 可使用功能点击事件
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PanelGridAdapter.PanelItem item = (PanelGridAdapter.PanelItem) stableAdapter.getItem(position);
                String name = item.getName();
                if ("定时返回".equals(name) || "定时锁屏".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_TIMER);
                } else if ("统计".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_STATISTICS);
                }
            }
        });

        // 测试版功能点击事件
        betaGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PanelGridAdapter.PanelItem item = (PanelGridAdapter.PanelItem) betaAdapter.getItem(position);
                String name = item.getName();
                if ("录音机".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_RECORDER);
                }
            }
        });

        // 设置当前实例
        currentInstance = this;

        return root;
    }

    // 导航到功能页面
    private void navigateToFunction(int layoutId) {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.slide_out_right)
                .build();

        // 创建Bundle传递功能ID
        android.os.Bundle args = new android.os.Bundle();
        args.putInt("function_id", layoutId);

        navController.navigate(R.id.nav_function, args, navOptions);
    }

    // 显示功能界面（保留用于兼容性）
    public void showFunction(int layoutId) {
        navigateToFunction(layoutId);
    }

    // 隐藏功能界面，显示默认界面（保留用于兼容性）
    public void hideFunction() {
        // 不再需要，因为功能现在在独立页面
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