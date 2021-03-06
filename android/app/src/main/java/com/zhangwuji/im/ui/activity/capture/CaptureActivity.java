package com.zhangwuji.im.ui.activity.capture;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.dtr.zbar.build.ZBarDecoder;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.zhangwuji.im.R;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.ui.activity.NearByPeopleInfoActivity;
import com.zhangwuji.im.ui.activity.NearByPeopleListActivity;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.entity.NearByUser;
import com.zhangwuji.im.ui.helper.ApiAction;
import com.zhangwuji.im.ui.helper.LoginInfoSp;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;


/**
 * Created by Administrator on 2016/8/26 0026.
 */
public class CaptureActivity extends TTBaseActivity implements View.OnClickListener {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private CameraManager mCameraManager;

    private TextView scanResult;
    private FrameLayout scanPreview;
    private Button scanRestart;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;

    private Rect mCropRect = null;
    private boolean barcodeScanned = false;
    private boolean previewing = true;
    private boolean handlermsging = false;

    private ImageView iv_title_back;//??????
    private TextView tv_title_center;//??????

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater.from(this).inflate(R.layout.activity_capture, topContentView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        findViewById();
        initViews();

    }

    private void findViewById() {
        scanPreview = (FrameLayout) findViewById(R.id.capture_preview);
        scanResult = (TextView) findViewById(R.id.capture_scan_result);
        scanRestart = (Button) findViewById(R.id.capture_restart_scan);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);

        topLeftBtn.setOnClickListener(this);
        letTitleTxt.setOnClickListener(this);
        topRightBtn.setOnClickListener(this);

        setTitle("?????????");
        setLeftButton(R.drawable.ac_back_icon);

    }

    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 123);
    }

    private void initViews() {
        try {
            autoFocusHandler = new Handler();
            mCameraManager = new CameraManager(this);
            try {
                mCameraManager.openDriver();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera = mCameraManager.getCamera();
            mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
            scanPreview.addView(mPreview);

        } catch (Exception e) {
            goToAppSetting();
        }

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.85f);
        animation.setDuration(3000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.REVERSE);
        scanLine.startAnimation(animation);
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!handlermsging) {

                Size size = camera.getParameters().getPreviewSize();

                // ????????????????????????data?????????????????????????????????????????????????????????
                byte[] rotatedData = new byte[data.length];
                for (int y = 0; y < size.height; y++) {
                    for (int x = 0; x < size.width; x++)
                        rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
                }

                // ??????????????????
                int tmp = size.width;
                size.width = size.height;
                size.height = tmp;

                initCrop();
                ZBarDecoder zBarDecoder = new ZBarDecoder();
                String result = zBarDecoder.decodeCrop(rotatedData, size.width, size.height, mCropRect.left, mCropRect.top, mCropRect.width(), mCropRect.height());

                if (!TextUtils.isEmpty(result)) {
                    previewing = false;
                    handlermsging = true;
                    barcodeScanned = true;
                    Log.e("tag", "onPreviewFrame: " + result.trim().toString());
                    if (result.trim().contains("groupcode") && result.trim().contains("groupId")) {
                        try {

                            String p = result.split("\\?")[1];
                            String[] allp = p.split("&");

                            //                        Intent intent = new Intent(CaptureActivity.this, GroupAddActivity.class);
                            //                        intent.putExtra("TargetId", allp[0].split("=")[1]);
                            //                        intent.putExtra("inviteUID", allp[1].split("=")[1]);
                            //                        intent.putExtra("data", allp[2].split("=")[1]);
                            //
                            //                       // scanResult.setText("barcode result " +  allp[0]);
                            //                        startActivity(intent);
                            //                        finish();
                        } catch (Exception ee) {
                        }
                    }else if(result.startsWith("http://qrlogin.b56.cn/"))
                    {
                       final String qrcode=result.replace("http://qrlogin.b56.cn/?","").replace("from=web&","").replace("cloudtalk_qr_code=","");
                        QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(CaptureActivity.this);
                        builder.setMessage("??????????????????Web????")
                                .setTitle("??????")
                                .addAction("??????", new QMUIDialogAction.ActionListener() {
                                    @Override
                                    public void onClick(QMUIDialog dialog, int index) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                })
                                .addAction(0,"??????",QMUIDialogAction.ACTION_PROP_NEGATIVE,new QMUIDialogAction.ActionListener() {
                                    @Override
                                    public void onClick(QMUIDialog qmuiDialog, int i) {

                                        ApiAction apiAction=new ApiAction(CaptureActivity.this);
                                        apiAction.agreeQRLogin(qrcode, new BaseAction.ResultCallback<String>() {
                                            @Override
                                            public void onSuccess(String s) {
                                                Toast.makeText(CaptureActivity.this,"????????????!",Toast.LENGTH_SHORT).show();
                                                qmuiDialog.dismiss();
                                                finish();
                                            }

                                            @Override
                                            public void onError(String errString) {
                                                Toast.makeText(CaptureActivity.this,"????????????!",Toast.LENGTH_SHORT).show();
                                                qmuiDialog.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                })
                                .setCancelable(true)
                                .create();
                        builder.show();


                    }
                    else if (result.startsWith("http")) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(result);
                        intent.setData(content_url);
                        startActivity(intent);
                        finish();
                    } else if (result.startsWith("QRCode:")) {
                        handlerQRCode(result);
                    } else if (result.trim().length() == 11) {
                        Intent intent = new Intent(CaptureActivity.this, NearByPeopleListActivity.class);
                        intent.putExtra("myphone", result);
                        intent.putExtra("type", "scan");
                        startActivity(intent);
                        finish();
                    } else {

                    }
                }
            }
        }

    };

    /**
     * ????????????????????????
     *
     * @param result
     */
    private void handlerQRCode(String result) {
        String[] split = result.split("QRCode:");

        if (!LoginInfoSp.instance().getLoginInfoIdentity().getLoginName().equals(split[1])) {

            Log.e("tag", "handlerQRCode: " + "----" + split[1]);
            ApiAction apiAction = new ApiAction(getApplicationContext());
            apiAction.getUserInfoByPhone(split[1], new BaseAction.ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    org.json.JSONObject jsonObject = null;
                    int code = 0;
                    try {
                        jsonObject = new org.json.JSONObject(s);
                        code = jsonObject.getInt("code");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (code == 200) {
                        try {
                            NearByUser nearByUser = JSON.parseObject(jsonObject.getJSONObject("data").getJSONArray("userinfo").get(0).toString(), NearByUser.class);
                            Intent intent = new Intent(getApplicationContext(), NearByPeopleInfoActivity.class);
                            intent.putExtra("userinfo", nearByUser);
                            startActivity(intent);
                            finish();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        handlermsging = false;

                        Toast.makeText(getApplicationContext(), "?????????????????????!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errString) {
                    handlermsging = false;
                    Toast.makeText(getApplicationContext(), "?????????????????????!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            handlermsging = false;
            Toast.makeText(this, "??????????????????????????????", Toast.LENGTH_SHORT).show();
        }

    }

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    /**
     * ??????????????????????????????
     */
    private void initCrop() {
        int cameraWidth = mCameraManager.getCameraResolution().y;
        int cameraHeight = mCameraManager.getCameraResolution().x;

        /** ??????????????????????????????????????? */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** ??????????????????????????? */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** ?????????????????????????????????????????????x?????? */
        int x = cropLeft * cameraWidth / containerWidth;
        /** ?????????????????????????????????????????????y?????? */
        int y = cropTop * cameraHeight / containerHeight;

        /** ???????????????????????????????????? */
        int width = cropWidth * cameraWidth / containerWidth;
        /** ???????????????????????????????????? */
        int height = cropHeight * cameraHeight / containerHeight;

        /** ?????????????????????????????? */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera = null;
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
}
