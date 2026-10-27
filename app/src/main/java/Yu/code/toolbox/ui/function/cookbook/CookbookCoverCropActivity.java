package Yu.code.toolbox.ui.function.cookbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.canhub.cropper.CropImageView;

import java.io.File;

public class CookbookCoverCropActivity extends AppCompatActivity {
    public static final String EXTRA_SOURCE_URI = "cookbook_source_uri";
    public static final String EXTRA_CROPPED_URI = "cookbook_cropped_uri";
    public static final String EXTRA_CROPPED_CACHE_NAME = "cookbook_cropped_cache_name";

    private CropImageView cropImageView;
    private Button applyButton;
    private File outputFile;
    private Uri outputUri;
    private CropImageView.OnSetImageUriCompleteListener imageLoadListener;
    private CropImageView.OnCropImageCompleteListener cropCompleteListener;
    private boolean cropping;
    private boolean completed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Yu.code.toolbox.R.layout.activity_cookbook_cover_crop);

        cropImageView = findViewById(Yu.code.toolbox.R.id.crop_image_view);
        applyButton = findViewById(Yu.code.toolbox.R.id.btn_crop_apply);
        Button cancelButton = findViewById(Yu.code.toolbox.R.id.btn_crop_cancel);

        cropImageView.setAspectRatio(1, 1);
        cropImageView.setFixedAspectRatio(true);
        cropImageView.setCropShape(CropImageView.CropShape.RECTANGLE);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        cropImageView.setMultiTouchEnabled(true);
        cropImageView.setAutoZoomEnabled(true);
        imageLoadListener = (view, uri, error) -> {
            if (error != null) {
                Toast.makeText(this, Yu.code.toolbox.R.string.cookbook_crop_loading_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            applyButton.setEnabled(true);
        };
        cropImageView.setOnSetImageUriCompleteListener(imageLoadListener);
        cropCompleteListener = (view, result) -> {
            cropping = false;
            if (isFinishing()) {
                deleteOutputFile();
                return;
            }
            if (result.getError() != null || outputFile == null || !outputFile.exists() || outputFile.length() == 0) {
                deleteOutputFile();
                applyButton.setEnabled(true);
                Toast.makeText(this, Yu.code.toolbox.R.string.cookbook_crop_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_CROPPED_URI, outputUri.toString());
            resultIntent.putExtra(EXTRA_CROPPED_CACHE_NAME, outputFile.getName());
            completed = true;
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        };
        cropImageView.setOnCropImageCompleteListener(cropCompleteListener);

        cancelButton.setOnClickListener(v -> finish());
        applyButton.setOnClickListener(v -> cropSelection());

        String sourceValue = getIntent().getStringExtra(EXTRA_SOURCE_URI);
        if (sourceValue == null || sourceValue.isEmpty()) {
            finish();
            return;
        }
        cropImageView.setImageUriAsync(Uri.parse(sourceValue));
    }

    private void cropSelection() {
        if (cropping || !applyButton.isEnabled()) {
            return;
        }
        try {
            outputFile = new File(getCacheDir(), "cookbook_crop_" + System.currentTimeMillis() + ".jpg");
            outputUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    outputFile
            );
            cropping = true;
            applyButton.setEnabled(false);
            cropImageView.croppedImageAsync(
                    Bitmap.CompressFormat.JPEG,
                    92,
                    1024,
                    1024,
                    CropImageView.RequestSizeOptions.RESIZE_INSIDE,
                    outputUri
            );
        } catch (Exception e) {
            cropping = false;
            deleteOutputFile();
            applyButton.setEnabled(true);
            Toast.makeText(this, Yu.code.toolbox.R.string.cookbook_crop_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteOutputFile() {
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
        outputFile = null;
        outputUri = null;
    }

    @Override
    protected void onDestroy() {
        if (!completed) {
            deleteOutputFile();
        }
        super.onDestroy();
    }
}
