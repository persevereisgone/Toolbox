package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CookBookImportExportHelper {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IMPORT_EXECUTOR = Executors.newSingleThreadExecutor();

    public interface UiCallback {
        void showToast(String message);

        void showResultDialog(String title, String message);

        void onImportDataChanged();
    }

    private final Context context;
    private final CookBookManager manager;
    private final UiCallback callback;

    public CookBookImportExportHelper(Context context, CookBookManager manager, UiCallback callback) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext == null ? context : applicationContext;
        this.manager = manager;
        this.callback = new UiCallback() {
            @Override
            public void showToast(String message) {
                MAIN.post(() -> callback.showToast(message));
            }

            @Override
            public void showResultDialog(String title, String message) {
                MAIN.post(() -> callback.showResultDialog(title, message));
            }

            @Override
            public void onImportDataChanged() {
                MAIN.post(callback::onImportDataChanged);
            }
        };
    }

    public void importFromUri(Uri uri) {
        if (uri == null) {
            return;
        }
        callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_import_started));
        IMPORT_EXECUTOR.execute(() -> importFromUriInternal(uri));
    }

    private void importFromUriInternal(Uri uri) {
        File tempFile = null;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_import_read_failed));
                return;
            }
            tempFile = File.createTempFile("cookbook_import_", ".bin", context.getCacheDir());
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                copyStream(inputStream, outputStream);
            }
            if (shouldTreatAsZip(uri, tempFile)) {
                importCookbookFromZip(tempFile);
            } else {
                importCookbookFromCsvOrTxt(tempFile);
            }
        } catch (Exception e) {
            callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_import_failed));
        } finally {
            if (tempFile != null && tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public void exportToUri(Uri uri) {
        try {
            String fileName = uri.getLastPathSegment() == null ? "cookbook_export.zip" : uri.getLastPathSegment();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
                exportTemplateImage(uri);
                return;
            }
            exportCookbookPackageToZip(uri);
        } catch (Exception e) {
            callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_export_failed));
        }
    }

    public void exportTemplateImage(Uri uri) throws IOException {
        int width = 1400;
        int height = 1900;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.rgb(35, 54, 75));
        titlePaint.setTextSize(58);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.rgb(55, 64, 72));
        bodyPaint.setTextSize(34);
        Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintPaint.setColor(Color.rgb(110, 120, 130));
        hintPaint.setTextSize(28);

        canvas.drawText("菜谱导入填写模板", 70, 100, titlePaint);
        canvas.drawText("请按下列格式填写，完成后保存为 CSV 后导入菜谱库。", 70, 165, hintPaint);
        int y = 270;
        String[] lines = {
                "菜谱名称：红烧肉",
                "分类：主菜",
                "食材：五花肉500g，冰糖30g，葱适量",
                "配料：姜5片，八角2颗，桂皮1段，料酒2勺",
                "步骤：",
                "1. 五花肉切块焯水",
                "2. 炒糖色",
                "3. 加入肉块翻炒上色",
                "4. 加入调料加水炖煮1小时",
                "封面图片：导入菜谱库时可在 ZIP 中提供图片"
        };
        for (String line : lines) {
            canvas.drawText(line, 70, y, bodyPaint);
            y += 105;
        }
        canvas.drawText("批量 CSV 列顺序：name, category, ingredients, seasonings, steps, image", 70, y + 80, hintPaint);
        canvas.drawText("image 填写 ZIP 中的封面图片路径，例如：cover_images/cover_1.jpg", 70, y + 135, hintPaint);

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_template_write_failed));
                return;
            }
            outputStream.flush();
        } finally {
            bitmap.recycle();
        }
        callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_template_downloaded));
    }

    private void importCookbookFromZip(File zipFile) {
        Map<String, String> importedImagePaths = new HashMap<>();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile)) {
            List<CookItem> imported = new ArrayList<>();
            int csvCount = 0;
            int imageCount = 0;
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String lowerName = entryName.toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".csv")) {
                    csvCount++;
                    try (InputStream entryStream = zip.getInputStream(entry)) {
                        imported.addAll(parseCookbooksFromCsv(readAll(entryStream)));
                    }
                } else if (lowerName.startsWith("cover_images/")) {
                    imageCount++;
                    try (InputStream entryStream = zip.getInputStream(entry)) {
                        String savedPath = saveZipImageEntry(entryStream, entryName);
                        if (!savedPath.isEmpty()) {
                            importedImagePaths.put(entryName, savedPath);
                            importedImagePaths.put(new File(entryName).getName(), savedPath);
                        }
                    }
                }
            }
            if (csvCount == 0) {
                callback.showResultDialog(
                        context.getString(Yu.code.toolbox.R.string.cookbook_import_fail_title),
                        "导入包中未找到 CSV 数据。\n\nCSV 数量：" + csvCount + "\n图片数量：" + imageCount);
                return;
            }
            if (imported.isEmpty()) {
                callback.showResultDialog(
                        context.getString(Yu.code.toolbox.R.string.cookbook_import_fail_title),
                        "ZIP 中找到了 " + csvCount + " 个 CSV，但没有解析出菜谱记录。\n\n请检查导出包内的 CSV 是否符合字段格式。\n图片数量：" + imageCount);
                return;
            }
            importCookbooksWithDedup(imported, importedImagePaths, csvCount, imageCount);
        } catch (Exception e) {
            callback.showResultDialog(
                    context.getString(Yu.code.toolbox.R.string.cookbook_import_fail_title),
                    "读取 ZIP 时发生错误：" + e.getMessage());
        }
    }

    private void importCookbookFromCsvOrTxt(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            String content = readAll(inputStream);
            List<CookItem> imported = parseCookbooksFromCsv(content);
            importCookbooksWithDedup(imported, new HashMap<>(), 0, 0);
        } catch (Exception e) {
            callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_import_failed));
        }
    }

    private void importCookbooksWithDedup(List<CookItem> imported, Map<String, String> importedImagePaths,
                                          int csvCount, int imageCount) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> importCookbooksWithDedup(imported, importedImagePaths, csvCount, imageCount));
            return;
        }
        if (imported == null || imported.isEmpty()) {
            callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_import_nothing));
            return;
        }
        Set<String> existingNames = new HashSet<>();
        for (CookItem existing : manager.getAllCookbooks()) {
            existingNames.add(normalizeCookbookName(existing.getName()));
        }
        int importedCount = 0;
        int skippedDuplicateCount = 0;
        int restoredImageCount = 0;
        List<CookItem> acceptedCookbooks = new ArrayList<>();
        for (CookItem item : imported) {
            String normalizedName = normalizeCookbookName(item.getName());
            if (normalizedName.isEmpty()) {
                continue;
            }
            if (!existingNames.add(normalizedName)) {
                skippedDuplicateCount++;
                continue;
            }
            item.setCategory(resolveImportedCategory(item.getCategory()));
            item.setId(0);
            String resolvedImage = resolveImportedImage(item.getImageUri(), importedImagePaths);
            if (resolvedImage != null && !resolvedImage.isEmpty() && !resolvedImage.equals(item.getImageUri())) {
                restoredImageCount++;
            }
            item.setImageUri(resolvedImage);
            acceptedCookbooks.add(item);
        }
        importedCount = manager.addCookbooks(acceptedCookbooks);
        if (importedCount == 0) {
            callback.showResultDialog(
                    context.getString(Yu.code.toolbox.R.string.cookbook_import_nothing),
                    "CSV 条数: " + csvCount + "\n图片数: " + imageCount + "\n重名跳过: " + skippedDuplicateCount + "\n\n没有成功导入任何菜谱。");
            return;
        }
        callback.onImportDataChanged();
        callback.showResultDialog(
                context.getString(Yu.code.toolbox.R.string.cookbook_import_done_title),
                "已导入: " + importedCount + " 条\n跳过重名: " + skippedDuplicateCount + " 条\n恢复图片: " + restoredImageCount + " 张\nCSV 条数: " + csvCount + "\n图片数: " + imageCount);
    }

    private String resolveImportedCategory(String categoryName) {
        String normalized = categoryName == null ? "" : categoryName.trim();
        List<String> categories = manager.getCategories();
        if (normalized.isEmpty()) {
            return categories.get(0);
        }
        for (String category : categories) {
            if (category.equalsIgnoreCase(normalized)) {
                return category;
            }
        }
        return manager.addCategory(normalized) ? normalized : manager.getCategories().get(0);
    }

    private List<CookItem> parseCookbooksFromCsv(String content) {
        List<CookItem> result = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return result;
        }
        String delimiterHint = detectDelimiter(content);
        boolean headerSkipped = false;

        if ("csv".equals(delimiterHint)) {
            List<List<String>> records = parseCsvRecords(content);
            for (List<String> parts : records) {
                if (parts.isEmpty()) {
                    continue;
                }
                String joined = joinFields(parts, ",");
                if (!headerSkipped && isHeaderLine(joined)) {
                    headerSkipped = true;
                    continue;
                }
                if (parts.size() < 5) {
                    continue;
                }
                CookItem item = new CookItem();
                item.setName(parts.get(0).trim());
                item.setCategory(parts.size() > 1 ? parts.get(1).trim() : "");
                item.setIngredients(parts.size() > 2 ? parts.get(2).trim() : "");
                item.setSeasonings(parts.size() > 3 ? parts.get(3).trim() : "");
                item.setSteps(parts.size() > 4 ? stripBom(parts.get(4).trim()) : "");
                item.setImageUri(parts.size() > 5 ? parts.get(5).trim() : "");
                if (!item.getName().isEmpty()) {
                    result.add(item);
                }
            }
            return result;
        }

        String[] contentLines = content.split("\\r?\\n");
        for (String rawLine : contentLines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!headerSkipped && isHeaderLine(line)) {
                headerSkipped = true;
                continue;
            }
            List<String> parts = parseDelimitedLine(line, delimiterHint);
            if (parts.size() < 5) {
                continue;
            }
            CookItem item = new CookItem();
            item.setName(parts.get(0).trim());
            item.setCategory(parts.size() > 1 ? parts.get(1).trim() : "");
            item.setIngredients(parts.size() > 2 ? parts.get(2).trim() : "");
            item.setSeasonings(parts.size() > 3 ? parts.get(3).trim() : "");
            item.setSteps(parts.size() > 4 ? stripBom(parts.get(4).trim()) : "");
            item.setImageUri(parts.size() > 5 ? parts.get(5).trim() : "");
            if (!item.getName().isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    private void exportCookbookPackageToZip(Uri uri) throws IOException {
        File exportDir = new File(context.getCacheDir(), "cookbook_export_" + System.currentTimeMillis());
        if (!exportDir.exists()) exportDir.mkdirs();
        File imagesDir = new File(exportDir, "cover_images");
        if (!imagesDir.exists()) imagesDir.mkdirs();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("name,category,ingredients,seasonings,steps,image\n");
        int imageIndex = 0;
        for (CookItem item : manager.getAllCookbooks()) {
            String imageRef = "";
            String imagePath = item.getImageUri();
            if (imagePath != null && !imagePath.isEmpty()) {
                File src = new File(imagePath);
                if (src.exists()) {
                    File dest = new File(imagesDir, "cover_" + (++imageIndex) + ".jpg");
                    try (InputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dest)) {
                        copyStream(in, out);
                    }
                    imageRef = "cover_images/" + dest.getName();
                }
            }
            csvBuilder.append(csvEscape(item.getName())).append(',')
                    .append(csvEscape(item.getCategory())).append(',')
                    .append(csvEscape(item.getIngredients())).append(',')
                    .append(csvEscape(item.getSeasonings())).append(',')
                    .append(csvEscape(item.getSteps())).append(',')
                    .append(csvEscape(imageRef)).append('\n');
        }

        try (OutputStream rawOutput = context.getContentResolver().openOutputStream(uri)) {
            if (rawOutput == null) {
                callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_export_write_failed));
                return;
            }
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(rawOutput)) {
                zipOutputStream.putNextEntry(new ZipEntry("cookbooks.csv"));
                zipOutputStream.write(addUtf8Bom(csvBuilder.toString()).getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();

                File[] coverFiles = imagesDir.listFiles();
                if (coverFiles != null) {
                    for (File file : coverFiles) {
                        zipOutputStream.putNextEntry(new ZipEntry("cover_images/" + file.getName()));
                        try (InputStream in = new FileInputStream(file)) {
                            copyStream(in, zipOutputStream);
                        }
                        zipOutputStream.closeEntry();
                    }
                }
                zipOutputStream.finish();
            }
        }
        callback.showToast(context.getString(Yu.code.toolbox.R.string.cookbook_exported));
    }

    private String readAll(InputStream inputStream) throws IOException {
        byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            copyStream(inputStream, out);
            bytes = out.toByteArray();
        }
        String utf8 = decodeText(bytes, StandardCharsets.UTF_8);
        if (looksLikeText(utf8)) {
            return stripBom(utf8);
        }
        String utf16le = decodeText(bytes, StandardCharsets.UTF_16LE);
        if (looksLikeText(utf16le)) {
            return stripBom(utf16le);
        }
        String utf16be = decodeText(bytes, StandardCharsets.UTF_16BE);
        if (looksLikeText(utf16be)) {
            return stripBom(utf16be);
        }
        String gbk = decodeText(bytes, Charset.forName("GBK"));
        if (looksLikeText(gbk)) {
            return stripBom(gbk);
        }
        return stripBom(utf8);
    }

    private String decodeText(byte[] bytes, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean looksLikeText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int printable = 0;
        int total = Math.min(text.length(), 200);
        for (int i = 0; i < total; i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || c > 127) {
                printable++;
            }
        }
        return printable >= Math.max(1, total / 2);
    }

    private String stripBom(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (text.charAt(0) == '\ufeff') {
            return text.substring(1);
        }
        return text;
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    private String addUtf8Bom(String text) {
        return "\ufeff" + text;
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String resolveImportedImage(String imagePathOrRef, Map<String, String> importedImagePaths) {
        if (imagePathOrRef == null || imagePathOrRef.isEmpty()) {
            return "";
        }
        String mapped = importedImagePaths == null ? null : importedImagePaths.get(imagePathOrRef);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }
        String fileName = new File(imagePathOrRef).getName();
        mapped = importedImagePaths == null ? null : importedImagePaths.get(fileName);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }
        return imagePathOrRef;
    }

    private String saveZipImageEntry(InputStream inputStream, String entryName) throws IOException {
        File dir = new File(context.getFilesDir(), "cookbook_images");
        if (!dir.exists() && !dir.mkdirs()) {
            return "";
        }
        String fileName = new File(entryName).getName();
        File outputFile = new File(dir, System.currentTimeMillis() + "_" + fileName);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            copyStream(inputStream, outputStream);
        }
        return outputFile.getAbsolutePath();
    }

    private boolean shouldTreatAsZip(Uri uri, File file) {
        String name = uri == null ? "" : uri.getLastPathSegment();
        if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return true;
        }
        return isZipFile(file);
    }

    private boolean isZipFile(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] header = new byte[4];
            int read = inputStream.read(header);
            return read >= 4 && header[0] == 'P' && header[1] == 'K';
        } catch (IOException e) {
            return false;
        }
    }

    private List<List<String>> parseCsvRecords(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                currentRecord.add(field.toString());
                field.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                currentRecord.add(field.toString());
                field.setLength(0);
                if (!(currentRecord.size() == 1 && currentRecord.get(0).trim().isEmpty())) {
                    records.add(new ArrayList<>(currentRecord));
                }
                currentRecord.clear();
            } else {
                field.append(c);
            }
        }
        currentRecord.add(field.toString());
        if (!(currentRecord.size() == 1 && currentRecord.get(0).trim().isEmpty())) {
            records.add(currentRecord);
        }
        return records;
    }

    private String joinFields(List<String> fields, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(fields.get(i));
        }
        return builder.toString();
    }

    private List<String> parseDelimitedLine(String line, String delimiterHint) {
        if ("tab".equals(delimiterHint)) {
            return splitDelimitedLine(line, '\t');
        }
        if ("pipe".equals(delimiterHint)) {
            return splitDelimitedLine(line, '|');
        }
        return parseCsvLine(line);
    }

    private String detectDelimiter(String content) {
        if (content == null || content.isEmpty()) {
            return "csv";
        }
        String firstLine = content.split("\\r?\\n", 2)[0];
        if (firstLine.contains("\t")) {
            return "tab";
        }
        if (firstLine.contains("|")) {
            return "pipe";
        }
        return "csv";
    }

    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("name") && (lower.contains("category") || lower.contains("ingredients") || lower.contains("steps"));
    }

    private List<String> splitDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == delimiter && !quoted) {
                values.add(unescapeDelimitedValue(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(unescapeDelimitedValue(current.toString()));
        return values;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == ',') {
                values.add(unescapeDelimitedValue(current.toString()));
                current.setLength(0);
            } else if (c == '"') {
                quoted = true;
            } else {
                current.append(c);
            }
        }
        values.add(unescapeDelimitedValue(current.toString()));
        return values;
    }

    private String unescapeDelimitedValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    private String normalizeCookbookName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
