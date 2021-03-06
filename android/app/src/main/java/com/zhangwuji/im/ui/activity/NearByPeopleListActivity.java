package com.zhangwuji.im.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorEventListener;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.model.LatLng;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.Message;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.manager.IMGroupManager;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.imcore.service.IMServiceConnector;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.utils.json.JsonMananger;
import com.zhangwuji.im.ui.adapter.FriendListAdapter;
import com.zhangwuji.im.ui.adapter.MessageAdapter;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.entity.NearByUser;
import com.zhangwuji.im.ui.entity.UserRelationship;
import com.zhangwuji.im.ui.helper.ApiAction;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.ui.widget.IMGroupAvatar;
import com.zhangwuji.im.ui.widget.SelectableRoundedImageView;
import com.zhangwuji.im.utils.AvatarGenerate;
import com.zhangwuji.im.utils.ScreenUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by AMing on 16/3/8.
 * YuChen
 */
public class NearByPeopleListActivity extends TTBaseActivity implements AMapLocationListener,View.OnClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {

    private PullToRefreshListView lvPTR = null;
    private dataAdapter adapter;
    private TextView mNoData;
    private List<NearByUser> mList=new LinkedList<>();
    private int page=1;
    private ApiAction apiAction=null;
    private ProgressBar progress_bar;
    static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;
    public AMapLocationClientOption mLocationOption = null;
    public AMapLocationClient mlocationClient=null;
    private LatLng myLocation = null;
    double longitude=0,latitude=0;//???????????????
    private LocationSource.OnLocationChangedListener listener;
    LocationListener locationListener;
    public String citycode="";

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        init();
                        //initData();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater.from(this).inflate(R.layout.activity_nearbypeople, topContentView);

        apiAction=new ApiAction(this);


        //TOP_CONTENT_VIEW
        setLeftButton(R.drawable.ac_back_icon);
     //   setRightButton(R.drawable.de_ic_add);

        lvPTR=(PullToRefreshListView)findViewById(R.id.lv_nearbypeople);
        mNoData = (TextView) findViewById(R.id.tv_hints);

        topLeftBtn.setOnClickListener(this);
        letTitleTxt.setOnClickListener(this);
        topRightBtn.setOnClickListener(this);
        setTitle(R.string.nearby);
        progress_bar = (ProgressBar)findViewById(R.id.progress_bar);


        adapter = new dataAdapter(this, mList);
        mNoData.setVisibility(View.GONE);
        lvPTR.getRefreshableView().addHeaderView(LayoutInflater.from(this).inflate(R.layout.tt_messagelist_header,lvPTR.getRefreshableView(), false));
        lvPTR.getRefreshableView().addFooterView(LayoutInflater.from(this).inflate(R.layout.list_more_item,lvPTR.getRefreshableView(), false));
        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
        lvPTR.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        lvPTR.getRefreshableView().setCacheColorHint(Color.WHITE);
        lvPTR.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        lvPTR.setAdapter(adapter);
        lvPTR.setOnRefreshListener(this);
        lvPTR.setMode(PullToRefreshBase.Mode.PULL_FROM_END);//??????????????????
        ILoadingLayout endLabels = lvPTR.getLoadingLayoutProxy(
                false, true);
        endLabels.setPullLabel("????????????...");// ??????????????????????????????
        endLabels.setRefreshingLabel("????????????...");// ?????????
        endLabels.setReleaseLabel("????????????...");// ?????????????????????????????????????????????

        lvPTR.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                NearByUser userinfo=mList.get(i-2);
                Intent intent=new Intent(NearByPeopleListActivity.this,NearByPeopleInfoActivity.class);
                intent.putExtra("userinfo",userinfo);
                NearByPeopleListActivity.this.startActivity(intent);
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("??????????????????????????????????????????")
                            .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .setNegativeButton("??????", null)
                            .create().show();
                }
                return;
            }
        }
        init();
        initData();
    }

    private void init()
    {

        mlocationClient = new AMapLocationClient(this);
        //?????????????????????
        mLocationOption = new AMapLocationClientOption();
        //??????????????????
        mlocationClient.setLocationListener(this);
        //???????????????????????????????????????Battery_Saving?????????????????????Device_Sensors??????????????????
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //??????????????????,????????????,?????????2000ms
        mLocationOption.setInterval(2000);
        //??????????????????
        mlocationClient.setLocationOption(mLocationOption);
        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // ??????????????????????????????????????????????????????????????????2000ms?????????????????????????????????stopLocation()???????????????????????????
        // ???????????????????????????????????????????????????onDestroy()??????
        // ?????????????????????????????????????????????????????????????????????stopLocation()???????????????????????????sdk???????????????
        //????????????
        mlocationClient.startLocation();
    }

    private void initData() {
        if(longitude<=0) {
            try {
                longitude = Double.parseDouble(IMUIHelper.getConfigNameAsString(this, "longitude"));
                latitude = Double.parseDouble(IMUIHelper.getConfigNameAsString(this, "latitude"));
                citycode = IMUIHelper.getConfigNameAsString(this, "citycode");
            }catch (Exception e){}
        }
        if(longitude<=0)return;

        apiAction.getNearByUser(citycode,longitude+"", latitude+"", page, new BaseAction.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                progress_bar.setVisibility(View.GONE);
                JSONObject objec=JSON.parseObject(s);
                if(objec.getIntValue("code")==200)
                {
                    List<NearByUser> mList2=new LinkedList<>();
                    mList2=JsonMananger.jsonToList(objec.getJSONArray("data").toJSONString(),NearByUser.class);
                    if (mList2 != null && mList2.size() > 0)
                    {
                        if(page==1)mList.clear();

                        mList.addAll(mList2);
                        adapter.updateListView(mList);
                        mNoData.setVisibility(View.GONE);
                        if(mList2.size()<20)
                        {
                            ((TextView)lvPTR.getRefreshableView().findViewById(R.id.list_more_txt)).setText("????????????????????????.");
                            lvPTR.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    lvPTR.setMode(PullToRefreshBase.Mode.DISABLED);//????????????
                                }
                            }, 2000);
                        }
                        else
                        {
                            ((TextView)lvPTR.getRefreshableView().findViewById(R.id.list_more_txt)).setText("??????????????????.");
                            lvPTR.setMode(PullToRefreshBase.Mode.PULL_FROM_END);
                        }
                        lvPTR.onRefreshComplete();
                    }
                    else
                    {
                        if(page==1) {
                            lvPTR.setVisibility(View.GONE);
                            mNoData.setVisibility(View.VISIBLE);
                        }
                        lvPTR.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                lvPTR.setMode(PullToRefreshBase.Mode.DISABLED);//????????????
                            }
                        }, 2000);
                        lvPTR.onRefreshComplete();
                    }
                }
                else
                {
                    lvPTR.onRefreshComplete();
                    if(page==1) {
                        lvPTR.setVisibility(View.GONE);
                        mNoData.setVisibility(View.VISIBLE);
                    }
                    lvPTR.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            lvPTR.setMode(PullToRefreshBase.Mode.DISABLED);//????????????
                        }
                    }, 2000);
                    ((TextView)lvPTR.getRefreshableView().findViewById(R.id.list_more_txt)).setText("????????????????????????.");

                }
            }
            @Override
            public void onError(String errString) {
                lvPTR.onRefreshComplete();
                progress_bar.setVisibility(View.GONE);
            }
        });
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
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        page=1;
        progress_bar.setVisibility(View.VISIBLE);
        refreshView.postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();
            }
        }, 200);

    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
        page++;
        progress_bar.setVisibility(View.VISIBLE);
        initData();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
            if (listener != null) {
                listener.onLocationChanged(aMapLocation);// ?????????????????????
            }
            myLocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());//???????????????????????????
            longitude = myLocation.longitude;
            latitude = myLocation.latitude;
            mlocationClient.stopLocation();
            citycode=aMapLocation.getCityCode();

            IMUIHelper.setConfigName(this,"longitude",longitude+"");
            IMUIHelper.setConfigName(this,"latitude",latitude+"");
            IMUIHelper.setConfigName(this,"citycode",citycode);

            page=1;
            initData();
        }
        else
        {
            try {
                longitude = Double.parseDouble(IMUIHelper.getConfigNameAsString(this, "longitude"));
                latitude = Double.parseDouble(IMUIHelper.getConfigNameAsString(this, "latitude"));
                citycode = IMUIHelper.getConfigNameAsString(this, "citycode");
            }catch (Exception e){}
            page=1;
            initData();
        }
    }


    class dataAdapter extends BaseAdapter {

        private Context context;

        private List<NearByUser> list;

        public dataAdapter(Context context, List<NearByUser> list) {
            this.context = context;
            this.list = list;
        }

        /**
         * ?????????????????? ??????UI?????????
         */
        public void updateListView(List<NearByUser> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (list != null) return list.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (list == null)
                return null;

            if (position >= list.size())
                return null;

            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            final NearByUser mContent = list.get(position);
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.nearbypeople_item, null);
                viewHolder.tv_type = (TextView) convertView.findViewById(R.id.tv_type);
                viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
                viewHolder.iv_faceurl = (IMBaseImageView) convertView.findViewById(R.id.new_header);
                viewHolder.iv_sex = (ImageView) convertView.findViewById(R.id.iv_sex);
                viewHolder.tv_distance = (TextView) convertView.findViewById(R.id.tv_distance);
                viewHolder.tv_signature = (TextView) convertView.findViewById(R.id.tv_signature);
                viewHolder.tv_online = (TextView) convertView.findViewById(R.id.tv_online);
                viewHolder.tv_isfriend = (TextView) convertView.findViewById(R.id.tv_isfriend);

            viewHolder.tv_name.setText(mContent.getNickname());

            int sex=mContent.getSex();
            if(sex==1){
                viewHolder.iv_sex.setImageResource(R.drawable.userinfo_male);
            }else if(sex==2){
                viewHolder.iv_sex.setImageResource(R.drawable.userinfo_female);
            }
            else
            {
                viewHolder.iv_sex.setVisibility(View.GONE);
            }
            String distance=mContent.getDists();
            try {
                if(Float.parseFloat(distance)>1000)
                {
                    distance = (IMUIHelper.floatMac1(Float.parseFloat(distance) / 1000)) + "????????????.";
                }
                else
                {
                    distance = (IMUIHelper.floatMac(distance)) + "?????????.";
                }
            } catch (Exception e) {
            }

            viewHolder.tv_distance.setText(distance);
            if(!mContent.getSign_info().equals(""))
            {
                viewHolder.tv_signature.setText(mContent.getSign_info());
            }else{
                viewHolder.tv_signature.setText("??????????????????.");
            }

            viewHolder.iv_faceurl.setCorner(8);
            viewHolder.iv_faceurl.setImageUrl(AvatarGenerate.generateAvatar(mContent.getAvatar(),mContent.getMainName(),mContent.getId()+""));

            return convertView;
        }


        class ViewHolder {
            TextView tv_type;//????????????
            TextView tv_name;//??????
            IMBaseImageView iv_faceurl;//??????
            ImageView iv_sex;//??????
            TextView tv_distance;//??????
            TextView tv_signature;//????????????
            TextView tv_online;//????????????
            TextView tv_isfriend;//???????????????
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
