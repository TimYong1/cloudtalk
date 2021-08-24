package com.zhangwuji.im.ui.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.zhangwuji.im.DB.entity.Department;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.UrlConstant;
import com.zhangwuji.im.config.DBConstant;
import com.zhangwuji.im.config.IntentConstant;
import com.zhangwuji.im.imcore.entity.SearchElement;
import com.zhangwuji.im.imcore.event.LoginEvent;
import com.zhangwuji.im.imcore.event.SocketEvent;
import com.zhangwuji.im.ui.activity.GroupMemberSelectActivity;
import com.zhangwuji.im.ui.activity.MessageActivity;
import com.zhangwuji.im.ui.activity.QRCodeActivity;
import com.zhangwuji.im.ui.activity.UserInfoActivity;
import com.zhangwuji.im.utils.Logger;
import com.zhangwuji.im.utils.pinyin.PinYin.PinYinElement;

import java.text.DecimalFormat;

public class IMUIHelper {

    private static double RATIO = 0.85D;
    public static float density;
    public static int dialogWidth;
    public static int screenWidth;
    public static int screenHeight;
    public static int screenMin;
    public static int screenMax;

    public static int dip2px(float dipValue) {

        return (int) (dipValue * density + 0.5F);
    }

    public static int px2dip(float pxValue) {
        return (int) (pxValue / density + 0.5F);
    }

    public static int getDialogWidth() {
        dialogWidth = (int) ((double) screenMin * RATIO);
        return dialogWidth;
    }

    public static void init(Context context) {
        if (null != context) {
            DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
            screenMin = screenWidth > screenHeight ? screenHeight : screenWidth;
            density = dm.density;
        }
    }

    /**
     * 保留一位小数点
     *
     * @param f
     * @return 作者:fighter <br />
     * 创建时间:2013-6-13<br />
     * 修改时间:<br />
     */
    public static String floatMac1(float f) {
        DecimalFormat decimalFormat = new DecimalFormat("####.#");
        try {
            return decimalFormat.format(f);
        } catch (Exception e) {
            return f + "";
        }
    }

    public static String floatMac(String floatStr) {
        DecimalFormat decimalFormat = new DecimalFormat("####.#");
        try {
            float f = Float.parseFloat(floatStr);
            return decimalFormat.format(f);
        } catch (Exception e) {
            return floatStr;
        }
    }

    public static String getConfigNameAsString(Context ctx, String key) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("cloudtalk_im_data", ctx.MODE_PRIVATE);
        return sharedPreferences.getString(key, "");
    }

    public static int getConfigNameAsInt(Context ctx, String key) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("cloudtalk_im_data", ctx.MODE_PRIVATE);
        return sharedPreferences.getInt(key, 0);
    }

    public static void setConfigName(Context ctx, String key, int vaule) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("cloudtalk_im_data", ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, vaule);
        editor.commit();
        editor.apply();
    }

    public static void setConfigName(Context ctx, String key, String vaule) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("cloudtalk_im_data", ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, vaule);
        editor.commit();
        editor.apply();
    }

    // 在视图中，长按用户信息条目弹出的对话框
    public static void handleContactItemLongClick(final User contact, final Context ctx) {
        if (contact == null || ctx == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Light_Dialog));
        builder.setTitle(contact.getMainName());
        String[] items = new String[]{ctx.getString(R.string.check_profile),
                ctx.getString(R.string.start_session)};

        final int userId = contact.getPeerId();
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        IMUIHelper.openUserProfileActivity(ctx, userId);
                        break;
                    case 1:
                        IMUIHelper.openChatActivity(ctx, contact.getSessionKey());
                        break;
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }


    // 根据event 展示提醒文案
    public static int getLoginErrorTip(LoginEvent event) {
        switch (event) {
            case LOGIN_AUTH_FAILED:
                return R.string.login_error_general_failed;
            case LOGIN_INNER_FAILED:
                return R.string.login_error_unexpected;
            default:
                return R.string.login_error_unexpected;
        }
    }

    public static int getSocketErrorTip(SocketEvent event) {
        switch (event) {
            case CONNECT_MSG_SERVER_FAILED:
                return R.string.connect_msg_server_failed;
            case REQ_MSG_SERVER_ADDRS_FAILED:
                return R.string.req_msg_server_addrs_failed;
            default:
                return R.string.login_error_unexpected;
        }
    }

    // 跳转到聊天页面
    public static void openChatActivity(Context ctx, String sessionKey) {
        Intent intent = new Intent(ctx, MessageActivity.class);
        intent.putExtra(IntentConstant.KEY_SESSION_KEY, sessionKey);
        ctx.startActivity(intent);
    }


    //跳转到用户信息页面
    public static void openUserProfileActivity(Context ctx, int contactId) {
        Intent intent = new Intent(ctx, UserInfoActivity.class);
        intent.putExtra(IntentConstant.KEY_PEERID, contactId);
        ctx.startActivity(intent);
    }

    public static void openGroupMemberSelectActivity(Context ctx, String sessionKey) {
        Intent intent = new Intent(ctx, GroupMemberSelectActivity.class);
        intent.putExtra(IntentConstant.KEY_SESSION_KEY, sessionKey);
        ctx.startActivity(intent);
    }

    public static void openQRCodeActivity(Context ctx, String key) {
        Intent intent = new Intent(ctx, QRCodeActivity.class);
        intent.putExtra(IntentConstant.KEY_QRCODE, key);
        ctx.startActivity(intent);
    }


    // 对话框回调函数
    public interface dialogCallback {
        public void callback();
    }

    public static void showCustomDialog(Context context, int visibale, String title, final dialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialog_view = inflater.inflate(R.layout.tt_custom_dialog, null);
        final EditText editText = (EditText) dialog_view.findViewById(R.id.dialog_edit_content);
        editText.setVisibility(visibale);
        TextView textText = (TextView) dialog_view.findViewById(R.id.dialog_title);
        textText.setText(title);
        builder.setView(dialog_view);

        builder.setPositiveButton(context.getString(R.string.tt_ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.callback();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(context.getString(R.string.tt_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    public static void callPhone(Context ctx, String phoneNumber) {
        if (ctx == null) {
            return;
        }
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                + phoneNumber));

        ctx.startActivity(intent);
    }


    // 文字高亮显示
    public static void setTextHilighted(TextView textView, String text, SearchElement searchElement) {
        textView.setText(text);
        if (textView == null
                || TextUtils.isEmpty(text)
                || searchElement == null) {
            return;
        }

        int startIndex = searchElement.startIndex;
        int endIndex = searchElement.endIndex;
        if (startIndex < 0 || endIndex > text.length()) {
            return;
        }
        // 开始高亮处理
        int color = Color.rgb(69, 192, 26);
        textView.setText(text, BufferType.SPANNABLE);
        Spannable span = (Spannable) textView.getText();
        span.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    /**
     * 如果图片路径是以  http开头,直接返回
     * 如果不是， 需要集合自己的图像路径生成规律
     *
     * @param avatarUrl
     * @return
     */
    public static String getRealAvatarUrl(String avatarUrl) {
        if (avatarUrl.toLowerCase().contains("http")) {
            return avatarUrl;
        } else if (avatarUrl.trim().isEmpty()) {
            return "";
        } else {
            return UrlConstant.AVATAR_URL_PREFIX + avatarUrl;
        }
    }


    // search helper start
    public static boolean handleDepartmentSearch(String key, Department department) {
        if (TextUtils.isEmpty(key) || department == null) {
            return false;
        }
        department.getSearchElement().reset();

        return handleTokenFirstCharsSearch(key, department.getPinyinElement(), department.getSearchElement())
                || handleTokenPinyinFullSearch(key, department.getPinyinElement(), department.getSearchElement())
                || handleNameSearch(department.getDepartName(), key, department.getSearchElement());
    }


    public static boolean handleGroupSearch(String key, Group group) {
        if (TextUtils.isEmpty(key) || group == null) {
            return false;
        }
        group.getSearchElement().reset();

        return handleTokenFirstCharsSearch(key, group.getPinyinElement(), group.getSearchElement())
                || handleTokenPinyinFullSearch(key, group.getPinyinElement(), group.getSearchElement())
                || handleNameSearch(group.getMainName(), key, group.getSearchElement());
    }

    public static boolean handleContactSearch(String key, User contact) {
        if (TextUtils.isEmpty(key) || contact == null) {
            return false;
        }

        contact.getSearchElement().reset();

        return handleTokenFirstCharsSearch(key, contact.getPinyinElement(), contact.getSearchElement())
                || handleTokenPinyinFullSearch(key, contact.getPinyinElement(), contact.getSearchElement())
                || handleNameSearch(contact.getMainName(), key, contact.getSearchElement());
        // 原先是 contact.name 代表花名的意思嘛??
    }

    public static boolean handleNameSearch(String name, String key,
                                           SearchElement searchElement) {
        int index = name.indexOf(key);
        if (index == -1) {
            return false;
        }

        searchElement.startIndex = index;
        searchElement.endIndex = index + key.length();

        return true;
    }

    public static boolean handleTokenFirstCharsSearch(String key, PinYinElement pinYinElement, SearchElement searchElement) {
        return handleNameSearch(pinYinElement.tokenFirstChars, key.toUpperCase(), searchElement);
    }

    public static boolean handleTokenPinyinFullSearch(String key, PinYinElement pinYinElement, SearchElement searchElement) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }

        String searchKey = key.toUpperCase();

        //onLoginOut the old search result
        searchElement.reset();

        int tokenCnt = pinYinElement.tokenPinyinList.size();
        int startIndex = -1;
        int endIndex = -1;

        for (int i = 0; i < tokenCnt; ++i) {
            String tokenPinyin = pinYinElement.tokenPinyinList.get(i);

            int tokenPinyinSize = tokenPinyin.length();
            int searchKeySize = searchKey.length();

            int keyCnt = Math.min(searchKeySize, tokenPinyinSize);
            String keyPart = searchKey.substring(0, keyCnt);

            if (tokenPinyin.startsWith(keyPart)) {

                if (startIndex == -1) {
                    startIndex = i;
                }

                endIndex = i + 1;
            } else {
                continue;
            }

            if (searchKeySize <= tokenPinyinSize) {
                searchKey = "";
                break;
            }

            searchKey = searchKey.substring(keyCnt, searchKeySize);
        }

        if (!searchKey.isEmpty()) {
            return false;
        }

        if (startIndex >= 0 && endIndex > 0) {
            searchElement.startIndex = startIndex;
            searchElement.endIndex = endIndex;

            return true;
        }

        return false;
    }

    // search helper end


    public static void setViewTouchHightlighted(final View view) {
        if (view == null) {
            return;
        }

        view.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundColor(Color.rgb(1, 175, 244));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.setBackgroundColor(Color.rgb(255, 255, 255));
                }
                return false;
            }
        });
    }


    // 这个还是蛮有用的,方便以后的替换
    public static int getDefaultAvatarResId(int sessionType) {
        if (sessionType == DBConstant.SESSION_TYPE_SINGLE) {
            return R.drawable.tt_default_user_portrait_corner;
        } else if (sessionType == DBConstant.SESSION_TYPE_GROUP) {
            return R.drawable.group_default;
        } else if (sessionType == DBConstant.SESSION_TYPE_GROUP) {
            return R.drawable.discussion_group_default;
        }

        return R.drawable.tt_default_user_portrait_corner;
    }


    public static void setEntityImageViewAvatarNoDefaultPortrait(ImageView imageView,
                                                                 String avatarUrl, int sessionType, int roundPixel) {
        setEntityImageViewAvatarImpl(imageView, avatarUrl, sessionType, false, roundPixel);
    }

    public static void setEntityImageViewAvatarImpl(ImageView imageView,
                                                    String avatarUrl, int sessionType, boolean showDefaultPortrait, int roundPixel) {
        if (avatarUrl == null) {
            avatarUrl = "";
        }

        String fullAvatar = getRealAvatarUrl(avatarUrl);
        int defaultResId = -1;

        if (showDefaultPortrait) {
            defaultResId = getDefaultAvatarResId(sessionType);
        }

        displayImage(imageView, fullAvatar, defaultResId, roundPixel);
    }

    public static void displayImage(ImageView imageView,
                                    String resourceUri, int defaultResId, int roundPixel) {

        Logger logger = Logger.getLogger(IMUIHelper.class);

        logger.d("displayimage#displayImage resourceUri:%s, defeaultResourceId:%d", resourceUri, defaultResId);

        if (resourceUri == null) {
            resourceUri = "";
        }

        boolean showDefaultImage = !(defaultResId <= 0);

        if (TextUtils.isEmpty(resourceUri) && !showDefaultImage) {
            logger.e("displayimage#, unable to display image");
            return;
        }


        DisplayImageOptions options;
        if (showDefaultImage) {
            options = new DisplayImageOptions.Builder().
                    showImageOnLoading(defaultResId).
                    showImageForEmptyUri(defaultResId).
                    showImageOnFail(defaultResId).
                    cacheInMemory(true).
                    cacheOnDisk(true).
                    considerExifParams(true).
                    displayer(new RoundedBitmapDisplayer(roundPixel)).
                    imageScaleType(ImageScaleType.EXACTLY).// 改善OOM
                    bitmapConfig(Bitmap.Config.RGB_565).// 改善OOM
                    build();
        } else {
            options = new DisplayImageOptions.Builder().
                    cacheInMemory(true).
                    cacheOnDisk(true).
                    //			considerExifParams(true).
                    //			displayer(new RoundedBitmapDisplayer(roundPixel)).
                    //			imageScaleType(ImageScaleType.EXACTLY).// 改善OOM
                    //			bitmapConfig(Bitmap.Config.RGB_565).// 改善OOM
                            build();
        }

        ImageLoader.getInstance().displayImage(resourceUri, imageView, options, null);
    }


    public static void displayImageNoOptions(ImageView imageView,
                                             String resourceUri, int defaultResId, int roundPixel) {

        Logger logger = Logger.getLogger(IMUIHelper.class);

        logger.d("displayimage#displayImage resourceUri:%s, defeaultResourceId:%d", resourceUri, defaultResId);

        if (resourceUri == null) {
            resourceUri = "";
        }

        boolean showDefaultImage = !(defaultResId <= 0);

        if (TextUtils.isEmpty(resourceUri) && !showDefaultImage) {
            logger.e("displayimage#, unable to display image");
            return;
        }

        DisplayImageOptions options;
        if (showDefaultImage) {
            options = new DisplayImageOptions.Builder().
                    showImageOnLoading(defaultResId).
                    showImageForEmptyUri(defaultResId).
                    showImageOnFail(defaultResId).
                    cacheInMemory(true).
                    cacheOnDisk(true).
                    considerExifParams(true).
                    displayer(new RoundedBitmapDisplayer(roundPixel)).
                    imageScaleType(ImageScaleType.EXACTLY).// 改善OOM
                    bitmapConfig(Bitmap.Config.RGB_565).// 改善OOM
                    build();
        } else {
            options = new DisplayImageOptions.Builder().
                    //                    cacheInMemory(true).
                    //                    cacheOnDisk(true).
                            imageScaleType(ImageScaleType.EXACTLY).// 改善OOM
                    bitmapConfig(Bitmap.Config.RGB_565).// 改善OOM
                    build();
        }
        ImageLoader.getInstance().displayImage(resourceUri, imageView, options, null);
    }


    public static void setListViewHeightBasedOnChildren(ListView listView) {
        //获取ListView对应的Adapter
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0, len = listAdapter.getCount(); i < len; i++) { //listAdapter.getCount()返回数据项的数目
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0); //计算子项View 的宽高
            totalHeight += listItem.getMeasuredHeight(); //统计所有子项的总高度
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        //listView.getDividerHeight()获取子项间分隔符占用的高度
        //params.height最后得到整个ListView完整显示需要的高度
        listView.setLayoutParams(params);
    }

    @SuppressWarnings("unused")
    private static void setExpandableListViewHeight(ExpandableListView listView) {
        //获取ListView对应的Adapter
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0, len = listAdapter.getCount(); i < len; i++) {   //listAdapter.getCount()返回数据项的数目
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);  //计算子项View 的宽高
            totalHeight += listItem.getMeasuredHeight();  //统计所有子项的总高度
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        //listView.getDividerHeight()获取子项间分隔符占用的高度
        //params.height最后得到整个ListView完整显示需要的高度
        listView.setLayoutParams(params);
    }

}
