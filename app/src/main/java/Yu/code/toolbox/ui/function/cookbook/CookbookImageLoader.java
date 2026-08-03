package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 菜谱封面图加载工具。
 * 支持应用内部文件路径和内容 URI。
 */
public final class CookbookImageLoader {

    private CookbookImageLoader() {
    }

    public static void load(Context context, String imagePath, ImageView target, int placeholderRes) {
        if (target == null) {
            return;
        }
        if (imagePath == null || imagePath.isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }
        try {
            Bitmap bitmap;
            File file = new File(imagePath);
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    bitmap = BitmapFactory.decodeStream(is);
                }
            } else {
                Uri uri = Uri.parse(imagePath);
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is == null) {
                        target.setImageResource(placeholderRes);
                        return;
                    }
                    bitmap = BitmapFactory.decodeStream(is);
                }
            }
            if (bitmap != null) {
                target.setImageBitmap(bitmap);
            } else {
                target.setImageResource(placeholderRes);
            }
        } catch (Exception e) {
            target.setImageResource(placeholderRes);
        }
    }
}
