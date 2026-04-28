package Yu.code.toolbox.function.lottery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import java.util.List;

/**
 * 转盘抽奖视图
 */
public class LotteryWheelView extends View {

    private Paint sectorPaint;
    private Paint textPaint;
    private Paint borderPaint;
    private List<LotteryItem> items;
    private float currentRotation = 0f;
    private float targetRotation = 0f;
    private boolean isSpinning = false;
    private OnSpinCompleteListener spinCompleteListener;
    private SoundManager soundManager;
    private Interpolator interpolator;
    private long animationStartTime;
    private long animationDuration = 3000; // 3秒动画

    // 颜色数组，用于轮盘扇形区域
    private String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    };

    public interface OnSpinCompleteListener {
        void onSpinComplete(LotteryItem selectedItem);
    }

    public LotteryWheelView(Context context) {
        super(context);
        init();
    }

    public LotteryWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LotteryWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectorPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(2, 0, 1, Color.BLACK);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        soundManager = SoundManager.getInstance(getContext());
        interpolator = new AccelerateDecelerateInterpolator();
    }

    public void setItems(List<LotteryItem> items) {
        this.items = items;
        invalidate();
    }

    public void setOnSpinCompleteListener(OnSpinCompleteListener listener) {
        this.spinCompleteListener = listener;
    }

    public void startSpin() {
        if (items == null || items.isEmpty()) {
            return;
        }

        isSpinning = true;
        animationStartTime = System.currentTimeMillis();

        // 随机旋转角度，至少转3圈
        float randomAngle = (float) (Math.random() * 360);
        targetRotation = currentRotation + 1080 + randomAngle; // 3圈 + 随机角度

        // 播放开始音效
        soundManager.playSpinSound();

        // 开始动画
        post(rotationRunnable);
    }

    private Runnable rotationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSpinning) {
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - animationStartTime;

                if (elapsed < animationDuration) {
                    // 使用插值器计算动画进度
                    float progress = (float) elapsed / animationDuration;
                    float interpolatedProgress = interpolator.getInterpolation(progress);

                    // 计算当前旋转角度
                    float startRotation = currentRotation;
                    float totalRotation = targetRotation - startRotation;
                    currentRotation = startRotation + (totalRotation * interpolatedProgress);

                    invalidate();
                    postDelayed(this, 16); // 60fps
                } else {
                    // 动画结束
                    currentRotation = targetRotation;
                    isSpinning = false;
                    invalidate();

                    // 播放结果音效
                    soundManager.playResultSound();

                    // 计算选中的项目
                    if (spinCompleteListener != null) {
                        LotteryItem selectedItem = getSelectedItem();
                        spinCompleteListener.onSpinComplete(selectedItem);
                    }
                }
            }
        }
    };

    private LotteryItem getSelectedItem() {
        if (items == null || items.isEmpty()) {
            return null;
        }

        // 计算当前指针指向的角度（0度在顶部）
        float normalizedAngle = (360 - (currentRotation % 360)) % 360;
        if (normalizedAngle < 0) {
            normalizedAngle += 360;
        }

        float sectorAngle = 360f / items.size();
        int selectedIndex = (int) (normalizedAngle / sectorAngle);

        if (selectedIndex >= items.size()) {
            selectedIndex = items.size() - 1;
        }

        return items.get(selectedIndex);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (items == null || items.isEmpty()) {
            drawEmptyWheel(canvas);
            return;
        }

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY) - 20;

        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // 绘制扇形区域
        float sectorAngle = 360f / items.size();
        float startAngle = currentRotation;

        for (int i = 0; i < items.size(); i++) {
            // 设置扇形颜色
            String colorStr = items.get(i).getColor();
            if (colorStr == null || colorStr.isEmpty()) {
                colorStr = colors[i % colors.length];
            }
            sectorPaint.setColor(Color.parseColor(colorStr));

            // 绘制扇形
            canvas.drawArc(rectF, startAngle, sectorAngle, true, sectorPaint);

            // 绘制扇形边框
            canvas.drawArc(rectF, startAngle, sectorAngle, true, borderPaint);

            // 绘制文字
            drawTextInSector(canvas, centerX, centerY, radius, startAngle, sectorAngle, items.get(i).getName());

            startAngle += sectorAngle;
        }
    }

    private void drawTextInSector(Canvas canvas, int centerX, int centerY, int radius,
                                  float startAngle, float sectorAngle, String text) {
        float textAngle = startAngle + sectorAngle / 2;
        float textRadius = radius * 0.7f;

        float textX = centerX + (float) (textRadius * Math.cos(Math.toRadians(textAngle)));
        float textY = centerY + (float) (textRadius * Math.sin(Math.toRadians(textAngle)));

        // 根据扇形大小调整文字大小
        float textSize = Math.min(radius / 8, 24f);
        textPaint.setTextSize(textSize);

        // 如果文字太长，进行换行处理
        if (text.length() > 6) {
            String firstLine = text.substring(0, Math.min(6, text.length()));
            String secondLine = text.length() > 6 ? text.substring(6) : "";

            canvas.drawText(firstLine, textX, textY - textSize / 3, textPaint);
            if (!secondLine.isEmpty()) {
                canvas.drawText(secondLine, textX, textY + textSize / 3, textPaint);
            }
        } else {
            canvas.drawText(text, textX, textY, textPaint);
        }
    }

    private void drawEmptyWheel(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY) - 20;

        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // 绘制灰色圆形
        sectorPaint.setColor(Color.parseColor("#E0E0E0"));
        canvas.drawArc(rectF, 0, 360, true, sectorPaint);

        // 绘制边框
        canvas.drawArc(rectF, 0, 360, true, borderPaint);

        // 绘制提示文字
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.parseColor("#666666"));
        canvas.drawText("请添加抽奖项目", centerX, centerY, textPaint);
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    public void reset() {
        currentRotation = 0f;
        targetRotation = 0f;
        isSpinning = false;
        invalidate();
    }
}
