package Yu.code.toolbox.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class UserProfileManager {

    private static final String PREF_NAME = "user_profile";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_BIO = "user_bio";

    private final SharedPreferences preferences;

    public UserProfileManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "我的工具箱");
    }

    public void setUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserAvatar() {
        return preferences.getString(KEY_USER_AVATAR, "");
    }

    public void setUserAvatar(String avatar) {
        preferences.edit().putString(KEY_USER_AVATAR, avatar).apply();
    }

    public String getUserBio() {
        return preferences.getString(KEY_USER_BIO, "让常用操作更简单");
    }

    public void setUserBio(String bio) {
        preferences.edit().putString(KEY_USER_BIO, bio).apply();
    }
}