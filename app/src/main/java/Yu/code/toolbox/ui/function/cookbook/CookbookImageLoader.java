package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 菜谱封面图加载工具。
 * 后台线程采样解码，避免主线程卡顿与过大位图。
 */
public final class CookbookImageLoader {

    private static final int DEFAULT_MAX_SIZE = 512;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CookbookImageLoader() {
    }

    public static void load(Context context, String imagePath, ImageView target, int placeholderRes) {
        load(context, imagePath, target, placeholderRes, DEFAULT_MAX_SIZE);
    }

    public static void load(Context context, String imagePath, ImageView target, int placeholderRes, int maxSize) {
        if (target == null) {
            return;
        }
        final Object requestToken = imagePath == null ? "" : imagePath;
        target.setTag(Yu.code.toolbox.R.id.tag_cookbook_image_request, requestToken);
        if (imagePath == null || imagePath.isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }

        final Context appContext = context.getApplicationContext();
        final int safeMax = maxSize <= 0 ? DEFAULT_MAX_SIZE : maxSize;
        target.setImageResource(placeholderRes);

        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeSampled(appContext, imagePath, safeMax);
            MAIN.post(() -> {
                Object current = target.getTag(Yu.code.toolbox.R.id.tag_cookbook_image_request);
                if (current == null || !current.equals(requestToken)) {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    return;
                }
                if (bitmap != null) {
                    target.setImageBitmap(bitmap);
                } else {
                    target.setImageResource(placeholderRes);
                }
            });
        });
    }

    private static Bitmap decodeSampled(Context context, String imagePath, int maxSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            decodeOnce(context, imagePath, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize, maxSize);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return decodeOnce(context, imagePath, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decodeOnce(Context context, String imagePath, BitmapFactory.Options options) throws Exception {
        File file = new File(imagePath);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                return BitmapFactory.decodeStream(is, null, options);
            }
        }
        Uri uri = Uri.parse(imagePath);
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                return null;
            }
            return BitmapFactory.decodeStream(is, null, options);
        }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
