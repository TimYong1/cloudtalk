package com.zhangwuji.im.ui.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.IntentConstant;
import com.zhangwuji.im.imcore.entity.SystemMessage;
import com.zhangwuji.im.imcore.event.PriorityEvent;
import com.zhangwuji.im.imcore.event.UserInfoEvent;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.imcore.service.IMServiceConnector;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.network.IMAction;
import com.zhangwuji.im.ui.adapter.FriendListAdapter;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.base.TTBaseFragmentActivity;
import com.zhangwuji.im.ui.helper.ApiAction;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.helper.LoginInfoSp;
import com.zhangwuji.im.ui.widget.BottomMenuDialog;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.ui.widget.photo.PhotoUtils;
import com.zhangwuji.im.utils.AvatarGenerate;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MyUserActivity extends TTBaseActivity implements View.OnClickListener {


    private IMService imService;
    private User currentUser;
    private int currentUserId;
    private RelativeLayout user_avatar,rl_nickanme,rl_sex,rl_id,rl_qrcode,rl_sign_info;
    private TextView tv_nickanme,tv_sex,tv_id,tv_sign_info;
    private IMBaseImageView user_portrait;
    int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    private PhotoUtils photoUtils;
    private BottomMenuDialog dialog;
    private String imageUrl;
    private Uri selectUri;
    private QMUITipDialog tipDialog;

    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onIMServiceConnected() {
            logger.d("detail#onIMServiceConnected");

            imService = imServiceConnector.getIMService();
            if (imService == null) {
                logger.e("detail#imService is null");
                return;
            }

            currentUserId = getIntent().getIntExtra(IntentConstant.KEY_PEERID,0);
            if(currentUserId == 0){
                logger.e("detail#intent params error!!");
                return;
            }
            currentUser = imService.getContactManager().findContact(currentUserId);
            init_userinfo();
        }
        @Override
        public void onServiceDisconnected() {}
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater.from(this).inflate(R.layout.my_info_activity, topContentView);
        imServiceConnector.connect(this);
        //TOP_CONTENT_VIEW
        setLeftButton(R.drawable.ac_back_icon);
        setLeftText("??????");
        topLeftBtn.setOnClickListener(this);
        letTitleTxt.setOnClickListener(this);
        setTitle("????????????");
    }

    public void init_userinfo()
    {
        user_avatar=findViewById(R.id.user_avatar);
        rl_nickanme=findViewById(R.id.rl_nickanme);
        rl_sex=findViewById(R.id.rl_sex);
        rl_id=findViewById(R.id.rl_id);
        rl_qrcode=findViewById(R.id.rl_qrcode);
        rl_sign_info=findViewById(R.id.rl_sign_info);


        tv_nickanme=findViewById(R.id.tv_nickanme);
        tv_sex=findViewById(R.id.tv_sex);
        tv_id=findViewById(R.id.tv_id);
        tv_sign_info=findViewById(R.id.tv_sign_info);

        tv_nickanme.setText(currentUser.getMainName());
        tv_id.setText(currentUser.getPeerId()+"");

        if(currentUser.getGender()==1)
        {
            tv_sex.setText("???");
        }
        else
        {
            tv_sex.setText("???");
        }

        setPortraitChangeListener();
        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showPhotoDialog();

            }
        });

        rl_sign_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(MyUserActivity.this);
                builder.setTitle("??????????????????")
                        .setPlaceholder("???????????????????????????")
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("??????", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("??????", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                String text = builder.getEditText().getText().toString();
                                if (!text.equals("")) {
                                    ApiAction apiAction=new ApiAction(MyUserActivity.this);
                                    apiAction.edit_userinfo(0, text, "", text, new BaseAction.ResultCallback<String>() {
                                        @Override
                                        public void onSuccess(String s) {

                                            Toast.makeText(MyUserActivity.this,"????????????!",Toast.LENGTH_SHORT).show();

                                            tv_sign_info.setText(text);
                                            imService.getLoginManager().setLoginInfo(currentUser);

                                        }

                                        @Override
                                        public void onError(String errString) {
                                            dialog.dismiss();
                                        }
                                    });
                                }
                                dialog.dismiss();
                            }
                        })
                        .create(mCurrentDialogStyle).show();
            }
        });

        rl_nickanme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(MyUserActivity.this);
                builder.setTitle("????????????")
                        .setPlaceholder("?????????????????????")
                        .setDefaultText(currentUser.getMainName())
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .addAction("??????", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("??????", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                String text = builder.getEditText().getText().toString();
                                if (!text.equals("")) {
                                    ApiAction apiAction=new ApiAction(MyUserActivity.this);
                                    apiAction.edit_userinfo(0, text, "", "", new BaseAction.ResultCallback<String>() {
                                        @Override
                                        public void onSuccess(String s) {

                                            Toast.makeText(MyUserActivity.this,"????????????!",Toast.LENGTH_SHORT).show();

                                            tv_nickanme.setText(text);
                                            currentUser.setMainName(text);
                                            imService.getLoginManager().setLoginInfo(currentUser);
                                            EventBus.getDefault().postSticky(UserInfoEvent.USER_INFO_OK);
                                        }

                                        @Override
                                        public void onError(String errString) {
                                            dialog.dismiss();
                                        }
                                    });
                                }
                                dialog.dismiss();
                            }
                        })
                        .create(mCurrentDialogStyle).show();
            }
        });

        rl_sex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] items = new String[]{"???", "???"};
                new QMUIDialog.MenuDialogBuilder(MyUserActivity.this)
                        .addItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                                int sex=which+1;
                                ApiAction apiAction=new ApiAction(MyUserActivity.this);
                                apiAction.edit_userinfo(sex, "", "", "", new BaseAction.ResultCallback<String>() {
                                    @Override
                                    public void onSuccess(String s) {

                                        Toast.makeText(MyUserActivity.this,"????????????!",Toast.LENGTH_SHORT).show();
                                        tv_sex.setText(sex==1?"???":"???");
                                        currentUser.setGender(sex);
                                        imService.getLoginManager().setLoginInfo(currentUser);
                                    }

                                    @Override
                                    public void onError(String errString) {
                                        dialog.dismiss();
                                    }
                                });

                            }
                        })
                        .setTitle("???????????????")
                        .create(mCurrentDialogStyle).show();
            }
        });


        user_portrait=findViewById(R.id.user_portrait);
        user_portrait.setCorner(8);
        user_portrait.setImageUrl(AvatarGenerate.generateAvatar(currentUser.getAvatar(),currentUser.getMainName(),currentUser.getPeerId()+""));
        user_portrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyUserActivity.this, DetailPortraitActivity.class);
                intent.putExtra(IntentConstant.KEY_AVATAR_URL, currentUser.getAvatar());
                intent.putExtra(IntentConstant.KEY_IS_IMAGE_CONTACT_AVATAR, true);

                startActivity(intent);
            }
        });

        rl_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMUIHelper.openQRCodeActivity(MyUserActivity.this, LoginInfoSp.instance().getLoginInfoIdentity().getLoginName());
            }
        });

    }

    private void setPortraitChangeListener() {
        photoUtils = new PhotoUtils(new PhotoUtils.OnPhotoResultListener() {
            @Override
            public void onPhotoResult(Uri uri) {
                if (uri != null && !TextUtils.isEmpty(uri.getPath())) {
                    selectUri = uri;
                   // LoadDialog.show(mContext);
                    uploadImage("", "", selectUri);
                }
            }

            @Override
            public void onPhotoCancel() {

            }
        });
    }

    static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;

    /**
     * ???????????????
     */
    @TargetApi(23)
    private void showPhotoDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new BottomMenuDialog(MyUserActivity.this);
        dialog.setConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    int checkPermission = checkSelfPermission(Manifest.permission.CAMERA);
                    if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS);
                        } else {
                            new AlertDialog.Builder(MyUserActivity.this)
                                    .setMessage("??????????????????????????????????????????")
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS);
                                        }
                                    })
                                    .setNegativeButton("??????", null)
                                    .create().show();
                        }
                        return;
                    }
                }
                photoUtils.takePicture(MyUserActivity.this);
            }
        });
        dialog.setMiddleListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                photoUtils.selectPicture(MyUserActivity.this);
            }
        });
        dialog.show();
    }

    /**
     * ??????????????????
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, baos);// ???????????????????????????100????????????????????????????????????????????????baos???
        int options = 90;

        while (baos.toByteArray().length / 1024 > 100) { // ?????????????????????????????????????????????100kb,??????????????????
            baos.reset(); // ??????baos?????????baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// ????????????options%?????????????????????????????????baos???
            options -= 10;// ???????????????10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// ?????????????????????baos?????????ByteArrayInputStream???
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// ???ByteArrayInputStream??????????????????
        return bitmap;
    }
    /**
     * ?????????????????????????????????
     *
     * @param srcPath ???????????????????????????????????????
     * @return
     */
    public static Bitmap getimage(String srcPath) {

        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // ??????????????????????????????options.inJustDecodeBounds ??????true???
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// ????????????bm??????

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // ??????????????????????????????800*480??????????????????????????????????????????
        float hh = 800f;// ?????????????????????800f
        float ww = 480f;// ?????????????????????480f
        // ????????????????????????????????????????????????????????????????????????????????????????????????
        int be = 1;// be=1???????????????
        if (w > h && w > ww) {// ???????????????????????????????????????????????????
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// ???????????????????????????????????????????????????
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// ??????????????????
        // ??????????????????????????????????????????options.inJustDecodeBounds ??????false???
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);// ?????????????????????????????????????????????
    }

    /**
     * ?????????????????????????????????
     *
     * @param image ?????????Bitmap???????????????
     * @return
     */
    public static Bitmap compressScale(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, baos);

        // ????????????????????????1M,????????????????????????????????????BitmapFactory.decodeStream????????????
        if (baos.toByteArray().length / 1024 > 1024) {
            baos.reset();// ??????baos?????????baos
            image.compress(Bitmap.CompressFormat.JPEG, 90, baos);// ????????????50%?????????????????????????????????baos???
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // ??????????????????????????????options.inJustDecodeBounds ??????true???
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // ??????????????????????????????800*480??????????????????????????????????????????
        // float hh = 800f;// ?????????????????????800f
        // float ww = 480f;// ?????????????????????480f
        float hh = 500f;
        float ww = 500f;
        // ????????????????????????????????????????????????????????????????????????????????????????????????
        int be = 1;// be=1???????????????
        if (w > h && w > ww) {// ???????????????????????????????????????????????????
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) { // ???????????????????????????????????????????????????
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be; // ??????????????????
        // newOpts.inPreferredConfig = Config.RGB_565;//???????????????ARGB888???RGB565

        // ??????????????????????????????????????????options.inJustDecodeBounds ??????false???
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);

        // return compressImage(bitmap);// ?????????????????????????????????????????????

        return bitmap;
    }
    private BitmapFactory.Options getBitmapOption(int inSampleSize){
        System.gc();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = inSampleSize;
        return options;
    }

    public void saveBitmapFile(Bitmap bitmap, String filepath) {
        File file = new File(filepath);//???????????????????????????
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @param path
     *            ????????????????????????
     *            ???????????????URL
     * @throws Exception
     */
    public void uploadImage(final String domain, String imageToken, Uri path){

        File file = new File(path.getPath());
        if (file.exists() && file.length() > 0) {

            try
            {
                Bitmap bitmap=BitmapFactory.decodeFile(path.getPath(),getBitmapOption(2));

                Bitmap bitmap1=compressScale(bitmap);
                saveBitmapFile(bitmap,path.getPath());
                file = new File(path.getPath());
                tipDialog = new QMUITipDialog.Builder(this)
                        .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                        .setTipWord("?????????..")
                        .create();
                tipDialog.show();

                IMAction imAction=new IMAction(MyUserActivity.this);
                imAction.postFile(file, 1, file.getName(), new BaseAction.ResultCallback<String>() {
                    @Override
                    public void onSuccess(String s) {
                        JSONObject jsonObject=JSONObject.parseObject(s);
                        if(jsonObject.getIntValue("code")==0)
                        {
                            JSONObject data=jsonObject.getJSONObject("data");
                            String picurl=data.getString("src");

                            ApiAction apiAction=new ApiAction(MyUserActivity.this);
                            apiAction.edit_userinfo(0, "", picurl, "", new BaseAction.ResultCallback<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    tipDialog.cancel();
                                    currentUser.setAvatar(picurl);
                                    imService.getLoginManager().setLoginInfo(currentUser);
                                    user_portrait.setImageUrl(AvatarGenerate.generateAvatar(currentUser.getAvatar(),currentUser.getMainName(),currentUser.getPeerId()+""));
                                    Toast.makeText(MyUserActivity.this,"??????????????????!",Toast.LENGTH_SHORT).show();

                                    EventBus.getDefault().postSticky(UserInfoEvent.USER_INFO_OK);

                                }

                                @Override
                                public void onError(String errString) {
                                    tipDialog.cancel();
                                }
                            });
                        }
                        else
                        {
                            tipDialog.cancel();
                            Toast.makeText(MyUserActivity.this,"??????????????????!",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errString) {
                        tipDialog.cancel();
                        Toast.makeText(MyUserActivity.this,"??????????????????!",Toast.LENGTH_SHORT).show();

                    }
                });


            }catch (Exception e){return;}


        } else {
        }
    }


    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.left_btn:
            case R.id.left_txt:
                finish();
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PhotoUtils.INTENT_CROP:
            case PhotoUtils.INTENT_TAKE:
            case PhotoUtils.INTENT_SELECT:
                photoUtils.onActivityResult(MyUserActivity.this, requestCode, resultCode, data);
                break;

        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        imServiceConnector.disconnect(this);
        super.onDestroy();
    }

}
