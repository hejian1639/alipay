package com.alipay.sdk.pay.demo;

import java.io.IOException;
import java.util.Map;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.EnvUtils;
import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.pay.demo.util.OrderInfoUtil2_0;
import com.baidu.mapapi.SDKInitializer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 重要说明：
 * <p>
 * 本 Demo 只是为了方便直接向商户展示支付宝的整个支付流程，所以将加签过程直接放在客户端完成
 * （包括 OrderInfoUtil2_0_HK 和 OrderInfoUtil2_0）。
 * <p>
 * 在真实 App 中，私钥（如 RSA_PRIVATE 等）数据严禁放在客户端，同时加签过程务必要放在服务端完成，
 * 否则可能造成商户私密数据泄露或被盗用，造成不必要的资金损失，面临各种安全风险。
 * <p>
 * Warning:
 * <p>
 * For demonstration purpose, the assembling and signing of the request parameters are done on
 * the client side in this demo application.
 * <p>
 * However, in practice, both assembling and signing must be carried out on the server side.
 */
public class PayDemoActivity extends AppCompatActivity {

    /**
     * 用于支付宝支付业务的入参 app_id。
     */
    public static final String APPID = "2016080300155645";

    /**
     * 用于支付宝账户登录授权业务的入参 pid。
     */
    public static final String PID = "2016080300155645";

    /**
     * 用于支付宝账户登录授权业务的入参 target_id。
     */
    public static final String TARGET_ID = "2016080300155645";

    /**
     * pkcs8 格式的商户私钥。
     * <p>
     * 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个，如果两个都设置了，本 Demo 将优先
     * 使用 RSA2_PRIVATE。RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议商户使用
     * RSA2_PRIVATE。
     * <p>
     * 建议使用支付宝提供的公私钥生成工具生成和获取 RSA2_PRIVATE。
     * 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1
     */
    public static final String RSA2_PRIVATE = "";


    public static final String RSA_PRIVATE =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC4WmUFv1m8FX79\n" +
                    "Hwdfblt3F8iJ39FvlkCfEWdwUA4vq8SgZrn3BnaDJxsZE5JK25NWgJmMoWQo0/Dy\n" +
                    "Y2PXyssan1Vex5OhA23VJ2oNQO2NaE+2/KGcsVnMgY5JRbMsvLdqNqcjlqoL16GW\n" +
                    "55FEs/e+gXWlr4BriMAyTHLteKCCzqtQ3IzwkW+dl5hvZtqS0ryJm90U5G0OY1d1\n" +
                    "B/o67ohZjmsfBPVsszf2S4uISQV+jOHW9QLX1MSGHOtYY7NMM8v14r2JQzk1bFKM\n" +
                    "saeNgoI3PFRLR5GGH67RGPd6tZeWlF3PQ8x+PGMAz8xMI2v8V4go7UaEcpDxInAQ\n" +
                    "iBgaHghpAgMBAAECggEAOkpKuwB/7RqW5o4TFLeeMA/TFtUwMao+kI0+OaL2eVhE\n" +
                    "ebtBfbwLaPjKdqlbjlPTSHytEGxPcSFihCGtBiRXB/ntUmSkZzrgVY961un6ssWS\n" +
                    "vvcuWo8xJ5qnVL+3IaQiEnFpc7VF2yy5KS/bHPASm6fvfQ36+wVoT/8gL7n+dp08\n" +
                    "433WBrukeNqOWgd4ph+MfnoBQVrU1A75qc29eF9Zu3PbmpWGuJ1Ss2scUoWKzGoz\n" +
                    "XFyDxVfT4ttBA/fcTMsJcP9i7jMEqrPakxSzY0qWmxfRrnU7WoEY2YOCuzbwsYjF\n" +
                    "m2lnA5vak0PfIKZediMgK3oT80nF3rWk+nuOK7zLAQKBgQDc6Q18QgIWddbZ3jr5\n" +
                    "Eu5NPmPegP7Wkrkv1Ln1HtWEJWhYqRx5cZn+Xe9Y7s+M7bEO2PWYvv9XyDrtesWO\n" +
                    "XckGFJ9pQRT9uT1Xjh2bvvByCTKtrhPY5OUhJDftDUqAyDqiCYUhPD5fgBPruatZ\n" +
                    "SPkNn5Vs3IRLoj+AmHxsXPYEkQKBgQDVos19Y+V3MbKSYcJFM5dpgwcsty3ITuVo\n" +
                    "eaTgsE+lBLbfdS16TIki2scWfH5fHfqJ/2hZDNrxtkI003OxSiQ6hrKjzCvsfzdX\n" +
                    "N8+i2OYYtUJx7uAi4KxkLtbCWFiux4NT5cdWE6EVn+9D3ghOCwnM59kt3mpXF4vk\n" +
                    "kjRuWSBSWQKBgQCG+q+OuRZleuMohC0se3C4KeMD1XXgzg3xN93X9FsqP2Eeq1YG\n" +
                    "rm8ViXfxsxmGVsXU+KQ4DJwgmuvGpzmG368w0/EDcwzMax8cHG3i+CwawZwBm1ft\n" +
                    "Mw79zQ+O/IwFXFLXJ1H9GL5TxAJoCi60g24J8PRwa906CY0bmhCMddCswQKBgHR2\n" +
                    "Rw2XOs2qww13y59MC7vSWUPwLD4pfHhi8BYpJ5HvKv5Qc9VbCdt9ZtEEVyPlROKb\n" +
                    "4LfDAUyJrjf8GfLJV0ysh5AxyrQnefTLER9WnMFVdv6DWZ7J2lDXE+5omRfW3eoL\n" +
                    "topfMw6F3LqjvOVGsTYKImQV/uSximq1pW2OmscpAoGAe4+Rhxw6wHrsUNbhp8PU\n" +
                    "8ScLODtEO17LPGPDRPfmmATck3763JyWTU6C5zZeJiP9UQy3lFU7FP8GCbGUma4C\n" +
                    "eVfEQXndD/TI/2nkjqdHEykrbFn2s48w1m/2R2n2NqHUKcnQ3x9e3MQ+xsexOatl\n" +
                    "gsRfsOGAhnk3iolbAYQax2E=";

    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;


    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    public class SDKReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }

            if (action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
                // 开放鉴权错误信息描述
                showToast(context, "key 验证出错! 错误码 :"
                        + intent.getIntExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_CODE, 0)
                        + " ; 错误信息 ："
                        + intent.getStringExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_MESSAGE));
            } else if (action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)) {
                showToast(context, "key 验证成功! 功能可以正常使用");
            } else if (action.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                showToast(context, "网络出错");
            }
        }
    }

    private SDKReceiver mReceiver;

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
                        showAlert(PayDemoActivity.this, getString(R.string.pay_success) + payResult);
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        showAlert(PayDemoActivity.this, getString(R.string.pay_failed) + payResult);
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
                        showAlert(PayDemoActivity.this, getString(R.string.auth_success) + authResult);
                    } else {
                        // 其他状态值则为授权失败
                        showAlert(PayDemoActivity.this, getString(R.string.auth_failed) + authResult);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_main);
        requestPermission();
        EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);

        // 注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消监听 SDK 广播
        unregisterReceiver(mReceiver);
    }

    /**
     * 获取权限使用的 RequestCode
     */
    private static final int PERMISSIONS_REQUEST_CODE = 1002;

    /**
     * 检查支付宝 SDK 所需的权限，并在必要的时候动态获取。
     * 在 targetSDK = 23 以上，READ_PHONE_STATE 和 WRITE_EXTERNAL_STORAGE 权限需要应用在运行时获取。
     * 如果接入支付宝 SDK 的应用 targetSdk 在 23 以下，可以省略这个步骤。
     */
    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (checkPermission(Manifest.permission.READ_PHONE_STATE, Process.myPid(), Process.myUid())
                != PackageManager.PERMISSION_GRANTED
                || checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid())
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, PERMISSIONS_REQUEST_CODE);

        } else {
            showToast(this, getString(R.string.permission_already_granted));
        }
    }

    /**
     * 权限获取回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {

                // 用户取消了权限弹窗
                if (grantResults.length == 0) {
                    showToast(this, getString(R.string.permission_rejected));
                    return;
                }

                // 用户拒绝了某些权限
                for (int x : grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        showToast(this, getString(R.string.permission_rejected));
                        return;
                    }
                }

                // 所需的权限均正常获取
                showToast(this, getString(R.string.permission_granted));
            }
        }
    }


    /**
     * 支付宝支付业务示例
     */
    public void payV2(View v) throws IOException {
        if (TextUtils.isEmpty(APPID) || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))) {
            showAlert(this, getString(R.string.error_missing_appid_rsa_private));
            return;
        }


        final Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url("http://192.168.18.75:8080/order_info").build();
                try {
                    Response response = client.newCall(request).execute();
                    final String orderInfo = response.body().string();
                    PayTask alipay = new PayTask(PayDemoActivity.this);
                    Map<String, String> result = alipay.payV2(orderInfo, true);
                    Log.i("msp", result.toString());

                    Message msg = new Message();
                    msg.what = SDK_PAY_FLAG;
                    msg.obj = result;
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * 支付宝账户授权业务示例
     */
    public void authV2(View v) {
        if (TextUtils.isEmpty(PID) || TextUtils.isEmpty(APPID)
                || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))
                || TextUtils.isEmpty(TARGET_ID)) {
            showAlert(this, getString(R.string.error_auth_missing_partner_appid_rsa_private_target_id));
            return;
        }

        /*
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * authInfo 的获取必须来自服务端；
         */
        boolean rsa2 = (RSA2_PRIVATE.length() > 0);
        Map<String, String> authInfoMap = OrderInfoUtil2_0.buildAuthInfoMap(PID, APPID, TARGET_ID, rsa2);
        String info = OrderInfoUtil2_0.buildOrderParam(authInfoMap);

        String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(authInfoMap, privateKey, rsa2);
        final String authInfo = info + "&" + sign;
        Runnable authRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造AuthTask 对象
                AuthTask authTask = new AuthTask(PayDemoActivity.this);
                // 调用授权接口，获取授权结果
                Map<String, String> result = authTask.authV2(authInfo, true);

                Message msg = new Message();
                msg.what = SDK_AUTH_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }

    /**
     * 获取支付宝 SDK 版本号。
     */
    public void showSdkVersion(View v) {
        PayTask payTask = new PayTask(this);
        String version = payTask.getVersion();
        showAlert(this, getString(R.string.alipay_sdk_version_is) + version);
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

    private static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }
}
