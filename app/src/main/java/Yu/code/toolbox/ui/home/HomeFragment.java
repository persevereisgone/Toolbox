package Yu.code.toolbox.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentHomeBinding;
import Yu.code.toolbox.ui.PanelGridAdapter;
import Yu.code.toolbox.ui.function.FunctionConstants;

public class HomeFragment extends Fragment {

    private static final String PREFS_HOME_DEBUG = "home_debug_prefs";
    private static final String KEY_BETA_UNLOCKED = "beta_unlocked";
    private static final int DEV_TAP_REQUIRED = 5;
    private static final long DEV_TAP_TIMEOUT_MS = 1200L;

    private FragmentHomeBinding binding;
    private GridView gridView;
    private GridView betaGridView;
    private PanelGridAdapter betaAdapter;
    private SharedPreferences debugPreferences;
    private boolean betaUnlocked;
    private int devTapCount;
    private long lastDevTapAt;

    private static HomeFragment currentInstance;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        debugPreferences = requireContext().getSharedPreferences(PREFS_HOME_DEBUG, Context.MODE_PRIVATE);
        betaUnlocked = debugPreferences.getBoolean(KEY_BETA_UNLOCKED, false);

        gridView = root.findViewById(R.id.home_panel_grid);
        betaGridView = root.findViewById(R.id.beta_panel_grid);

        List<PanelGridAdapter.PanelItem> stableItems = new ArrayList<>();
        stableItems.add(new PanelGridAdapter.PanelItem(R.drawable.time, "定时返回"));
        stableItems.add(new PanelGridAdapter.PanelItem(R.drawable.statistics, "统计"));
        PanelGridAdapter stableAdapter = new PanelGridAdapter(requireContext(), stableItems);
        gridView.setAdapter(stableAdapter);

        List<PanelGridAdapter.PanelItem> betaItems = new ArrayList<>();
        betaItems.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        betaItems.add(new PanelGridAdapter.PanelItem(R.drawable.extract, "转盘抽奖"));
        betaItems.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "定时录屏"));
        betaAdapter = new PanelGridAdapter(requireContext(), betaItems);
        betaGridView.setAdapter(betaAdapter);

        updateBetaSectionVisibility();
        setupDeveloperEntry();

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

        betaGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PanelGridAdapter.PanelItem item = (PanelGridAdapter.PanelItem) betaAdapter.getItem(position);
                String name = item.getName();
                if ("录音机".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_RECORDER);
                } else if ("转盘抽奖".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_LOTTERY);
                } else if ("定时录屏".equals(name)) {
                    navigateToFunction(FunctionConstants.FUNCTION_SCREEN_RECORDER);
                }
            }
        });

        currentInstance = this;
        return root;
    }

    private void setupDeveloperEntry() {
        binding.homeDevEntry.setOnClickListener(v -> {
            long now = SystemClock.elapsedRealtime();
            if (now - lastDevTapAt > DEV_TAP_TIMEOUT_MS) {
                devTapCount = 0;
            }
            lastDevTapAt = now;
            devTapCount++;

            if (devTapCount >= DEV_TAP_REQUIRED) {
                betaUnlocked = !betaUnlocked;
                debugPreferences.edit().putBoolean(KEY_BETA_UNLOCKED, betaUnlocked).apply();
                updateBetaSectionVisibility();
                Toast.makeText(requireContext(), betaUnlocked ? "测试区已开启" : "测试区已隐藏", Toast.LENGTH_SHORT).show();
                devTapCount = 0;
            }
        });
    }

    private void updateBetaSectionVisibility() {
        if (binding == null) {
            return;
        }
        int visible = betaUnlocked ? View.VISIBLE : View.GONE;
        binding.betaSectionCard.setVisibility(visible);
        binding.betaSectionTitle.setVisibility(visible);
        binding.betaPanelGrid.setVisibility(visible);
        binding.betaSectionSubtitle.setVisibility(visible);
    }

    private void navigateToFunction(int layoutId) {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.slide_out_right)
                .build();

        Bundle args = new Bundle();
        args.putInt("function_id", layoutId);

        navController.navigate(R.id.nav_function, args, navOptions);
    }

    public void showFunction(int layoutId) {
        navigateToFunction(layoutId);
    }

    public void hideFunction() {
    }

    public static void showFunctionStatic(int layoutId) {
        if (currentInstance != null) {
            currentInstance.showFunction(layoutId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (currentInstance == this) {
            currentInstance = null;
        }
    }
}
