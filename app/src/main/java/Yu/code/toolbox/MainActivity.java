package Yu.code.toolbox;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.GridView;
import android.widget.PopupWindow;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import Yu.code.toolbox.databinding.ActivityMainBinding;
import Yu.code.toolbox.ui.PanelGridAdapter;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // 悬浮面板相关变量
    private PopupWindow mPopupWindow;
    private GridView mGridView;
    private PanelGridAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置ActionBar但隐藏标题
        setSupportActionBar(binding.appBarMain.toolbar);
        // 设置导航栏为白色
        binding.appBarMain.toolbar.setBackgroundColor(getResources().getColor(android.R.color.white));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // 设置导航图标颜色为黑色
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
            // 额外设置导航图标色调
            android.graphics.drawable.Drawable navigationIcon = binding.appBarMain.toolbar.getNavigationIcon();
            if (navigationIcon != null) {
                navigationIcon.setColorFilter(getResources().getColor(android.R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        // 设置状态栏文字颜色为黑色
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11及以上版本
            getWindow().getInsetsController().setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android 6.0到Android 10版本
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 初始化FAB点击事件
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPopupWindow != null && mPopupWindow.isShowing()) {
                    mPopupWindow.dismiss();
                } else {
                    showFloatingPanel(view);
                }
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 显示悬浮面板
    private void showFloatingPanel(View anchorView) {
        // 加载悬浮面板布局
        View panelView = LayoutInflater.from(this).inflate(R.layout.floating_panel, null);

        // 初始化GridView
        mGridView = panelView.findViewById(R.id.panel_grid);
        mAdapter = new PanelGridAdapter(this, getPanelItems());
        mGridView.setAdapter(mAdapter);

        // 创建PopupWindow
        mPopupWindow = new PopupWindow(
                panelView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true
        );

        // 设置动画效果
        mPopupWindow.setAnimationStyle(R.style.PopupAnimation);

        // 获取屏幕宽度和高度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // 20dp转换为像素
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        // 图标大小和间距（从XML布局中获取）
        int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        int horizontalSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        int itemPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()); // panel_item.xml中的padding

        // 计算可用宽度和一行能放的图标数量
        int availableWidth = screenWidth - 2 * margin; // 左右边距各20dp
        int itemTotalWidth = iconSize + 2 * itemPadding; // 图标宽度+两侧padding（文字宽度不超过图标宽度，因为是单行显示）
        int numColumns = availableWidth / (itemTotalWidth + horizontalSpacing); // 向下取整，考虑间距

        // 确保至少有1列
        if (numColumns < 1) {
            numColumns = 1;
        }

        // 额外检查：确保最后一列不会超出可用宽度
        int totalWidthWithColumns = numColumns * itemTotalWidth + (numColumns - 1) * horizontalSpacing;
        if (totalWidthWithColumns > availableWidth) {
            numColumns--;
        }

        // 设置GridView的列数
        mGridView.setNumColumns(numColumns);

        // 强制测量panelView
        panelView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // 设置悬浮面板位于屏幕底部，距离左右和底部各20dp
        int x = margin;
        int y = screenHeight - panelView.getMeasuredHeight() - margin;

        // 设置PopupWindow的宽度
        mPopupWindow.setWidth(screenWidth - 2 * margin);

        mPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);

        // 设置点击外部关闭
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
    }

    // 获取面板数据
    private List<PanelGridAdapter.PanelItem> getPanelItems() {
        List<PanelGridAdapter.PanelItem> items = new ArrayList<>();

        // 添加示例图标和名称
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));

        return items;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}