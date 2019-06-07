package com.alipay.sdk.pay.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;


import java.io.IOException;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 此demo用来展示如何结合定位SDK实现定位，并使用MyLocationOverlay绘制定位位置 同时展示如何使用自定义图标绘制并点击时弹出泡泡
 */
public class LocationDemo extends Activity implements SensorEventListener {
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;

    /**
     * 获取权限使用的 RequestCode
     */
    private static final int PERMISSIONS_REQUEST_CODE = 1002;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    MapView mMapView;
    BaiduMap mBaiduMap;

    // UI相关
    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        requestPermission();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        mCurrentMode = LocationMode.FOLLOWING;


        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                mCurrentMode, true, mCurrentMarker));

        // UI初始化
        Button refreshBtn = (Button) findViewById(R.id.refresh);

        OnClickListener refreshListener = new OnClickListener() {
            public void onClick(View v) {
                mBaiduMap.setMyLocationData(locData);
            }
        };

        refreshBtn.setOnClickListener(refreshListener);

        Button userBtn = (Button) findViewById(R.id.user);

        OnClickListener userListener = new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), UserActivity.class));
            }
        };

        userBtn.setOnClickListener(userListener);

        Button shareBtn = (Button) findViewById(R.id.share);

        OnClickListener shareListener = new OnClickListener() {
            public void onClick(View v) {
                payV2();
            }
        };

        shareBtn.setOnClickListener(shareListener);
    }

    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (checkPermission(Manifest.permission.READ_PHONE_STATE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.ACCESS_WIFI_STATE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.INTERNET, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, PERMISSIONS_REQUEST_CODE);

        } else {
            showToast(this, getString(R.string.permission_already_granted));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStop() {
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }

    private static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public void payV2() {
        Observable.create(new Observable.OnSubscribe<Response>() {
            OkHttpClient client = new OkHttpClient();

            @Override
            public void call(Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder().url("http://192.168.18.71:8080/order_info").build();
                try {
                    Response response = client.newCall(request).execute();
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                    if (!response.isSuccessful()) subscriber.onError(new Exception("error"));
                } catch (IOException e) {
                    subscriber.onError(e);
                }


            }
        }).map(new Func1<Response, Map<String, String>>() {
            @Override
            public Map<String, String> call(Response response) {
                final String orderInfo;
                try {
                    orderInfo = response.body().string();
                    PayTask alipay = new PayTask(LocationDemo.this);
                    Map<String, String> result = alipay.payV2(orderInfo, true);
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;

            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Map<String, String>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Map<String, String> response) {
                @SuppressWarnings("unchecked")
                PayResult payResult = new PayResult(response);
                /**
                 * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                 */
                String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                String resultStatus = payResult.getResultStatus();
                // 判断resultStatus 为9000则代表支付成功
                if (TextUtils.equals(resultStatus, "9000")) {
                    // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                    showAlert(LocationDemo.this, getString(R.string.pay_success) + payResult);
                } else {
                    // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                    showAlert(LocationDemo.this, getString(R.string.pay_failed) + payResult);
                }

            }
        });
    }

    private static void showAlert(Context ctx, String info) {
        showAlert(ctx, info, null);
    }

    private static void showAlert(Context ctx, String info, DialogInterface.OnDismissListener onDismiss) {
        new AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton(R.string.confirm, null)
                .setOnDismissListener(onDismiss)
                .show();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /**
                     * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        showAlert(LocationDemo.this, getString(R.string.pay_success) + payResult);
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        showAlert(LocationDemo.this, getString(R.string.pay_failed) + payResult);
                    }
                    break;
                }
                case SDK_AUTH_FLAG: {
                    @SuppressWarnings("unchecked")
                    AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
                    String resultStatus = authResult.getResultStatus();

                    // 判断resultStatus 为“9000”且result_code
                    // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                    if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
                        // 获取alipay_open_id，调支付时作为参数extern_token 的value
                        // 传入，则支付账户为该授权账户
                        showAlert(LocationDemo.this, getString(R.string.auth_success) + authResult);
                    } else {
                        // 其他状态值则为授权失败
                        showAlert(LocationDemo.this, getString(R.string.auth_failed) + authResult);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        ;
    };

}
