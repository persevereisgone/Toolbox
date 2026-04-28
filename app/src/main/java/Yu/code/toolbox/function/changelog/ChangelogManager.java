package Yu.code.toolbox.function.changelog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import Yu.code.toolbox.R;

public class ChangelogManager {
    private final Context context;
    private final View rootView;
    private final LinearLayout changelogContainer;

    public ChangelogManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.changelogContainer = rootView.findViewById(R.id.ll_changelog_container);
        renderChangelogEntries();
    }

    private void renderChangelogEntries() {
        changelogContainer.removeAllViews();
        try {
            JSONArray entries = loadEntriesFromRaw();
            if (entries.length() == 0) {
                addFallbackEntry("暂无更新日志", "当前没有可展示的版本记录。");
                return;
            }

            // 仅展示当前版本（默认取第一条）
            changelogContainer.addView(createEntryView(entries.getJSONObject(0)));

            // 历史版本折叠区域
            if (entries.length() > 1) {
                addHistorySection(entries);
            }
        } catch (Exception e) {
            addFallbackEntry("更新日志加载失败", "请检查 changelog_entries.json 格式是否正确。");
        }
    }

    private void addHistorySection(JSONArray entries) throws Exception {
        LinearLayout historyCard = new LinearLayout(context);
        historyCard.setOrientation(LinearLayout.VERTICAL);
        historyCard.setBackgroundResource(R.drawable.app_card_bg);
        historyCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(12);
        historyCard.setLayoutParams(cardParams);

        TextView title = new TextView(context);
        title.setText("历史版本");
        title.setTextSize(18);
        title.setTextColor(context.getResources().getColor(R.color.app_text_primary));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView toggle = new TextView(context);
        toggle.setText("展开历史版本");
        toggle.setTextSize(14);
        toggle.setTextColor(context.getResources().getColor(R.color.app_primary_dark));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        toggleParams.topMargin = dp(8);
        toggle.setLayoutParams(toggleParams);

        LinearLayout historyContainer = new LinearLayout(context);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyContainer.setVisibility(View.GONE);
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        historyParams.topMargin = dp(10);
        historyContainer.setLayoutParams(historyParams);

        for (int i = 1; i < entries.length(); i++) {
            historyContainer.addView(createEntryView(entries.getJSONObject(i)));
        }

        toggle.setOnClickListener(v -> {
            boolean expanded = historyContainer.getVisibility() == View.VISIBLE;
            historyContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
            toggle.setText(expanded ? "展开历史版本" : "收起历史版本");
        });

        historyCard.addView(title);
        historyCard.addView(toggle);
        historyCard.addView(historyContainer);
        changelogContainer.addView(historyCard);
    }

    private View createEntryView(JSONObject entry) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_changelog_entry, changelogContainer, false);
        TextView tvVersion = itemView.findViewById(R.id.tv_changelog_version);
        TextView tvContent = itemView.findViewById(R.id.tv_changelog_content);
        tvVersion.setText(entry.optString("version", ""));
        tvContent.setText(entry.optString("content", ""));
        return itemView;
    }

    private void addFallbackEntry(String title, String content) {
        try {
            JSONObject fallback = new JSONObject();
            fallback.put("version", title);
            fallback.put("content", content);
            changelogContainer.addView(createEntryView(fallback));
        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private JSONArray loadEntriesFromRaw() throws Exception {
        InputStream inputStream = context.getResources().openRawResource(R.raw.changelog_entries);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        inputStream.close();
        return new JSONArray(builder.toString());
    }
}
