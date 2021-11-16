package com.vimedia.layabridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.qq.e.base.IStart;
import com.vimedia.core.common.utils.LogUtil;
import com.vimedia.game.AdLiveData;
import com.vimedia.game.GameEvent;
import com.vimedia.game.GameManager;
import com.vimedia.game.LifecycleManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import layaair.game.browser.ConchJNI;

/**
 * laya游戏能力页面
 *
 * @author ggd
 */
public class LayaWbActivity extends Activity implements LifecycleOwner , IStart {

    GameManager gameManager;

    LifecycleManager gameLifecycle = new LifecycleManager(this);


    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        EventBus.getDefault().register(this);
        initViewModel();
        gameLifecycle.onCreate(bundle, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        gameLifecycle.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameManager.setKeyEnable(gameManager.isKey(), 600);
        gameLifecycle.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameManager.setKeyEnable(false, 0);
        gameLifecycle.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        gameLifecycle.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        gameLifecycle.onDestroy();
        if (!isTaskRoot()) {
            return;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!gameManager.getKeyEnable()) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!gameManager.getKeyEnable()) {
            return false;
        }
        return super.onKeyUp(keyCode, event);
    }


    @Override
    public void onBackPressed() {
        gameLifecycle.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean b) {
        super.onWindowFocusChanged(b);
        gameLifecycle.onWindowFocusChanged(b);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        gameLifecycle.onNewIntent(this, intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        gameLifecycle.onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * 初始化
     */
    void initViewModel() {
        gameManager = GameManager.getInstance();
        gameManager.initContext(this);
        getLifecycle().addObserver(gameManager);
    }


    /**
     * 视频播放回调
     *
     * @param result
     * @param data
     */
    public void AdResultCall(boolean result, AdLiveData data) {
        final String params = String.format("%s#%b#%d#%s#%s#%s#%s",
                data.getAdName(),
                result,
                data.getEcpm(),
                data.getSid(),
                data.getPalatformName(),
                data.getOpenType(),
                data.getTradeId());
        StringBuilder cmd = new StringBuilder("window.VideoCallBack(");
        cmd.append(params);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }

    /**
     * 广告点击监听
     *
     * @param adName
     */
    public void AdClickedCall(final String adName) {
        StringBuilder cmd = new StringBuilder("window.AdClicked(");
        cmd.append(adName);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return gameLifecycle.getLifecycle();
    }

    /**
     * 监听GameEvent事件
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(GameEvent event) {
        if (event != null) {
            Object[] objects = event.getObjs();
            switch (event.getEventType()) {
                case GameEvent.GAME_EVENT_LOGIN:
                    loginResultCallCreator((boolean) objects[0]);
                    break;
                case GameEvent.GAME_EVENT_LOGIN_INFO:
                case GameEvent.GAME_EVENT_USERINFO:
                    getUserInfoResultCallCreator((boolean) objects[0], (String) objects[1]);
                    break;
                case GameEvent.GAME_EVENT_GAME_PARAM_INFO:
                    requestGameParamCallCreator((String) objects[0], (int) objects[1]);
                    break;
                case GameEvent.GAME_EVENT_CASH_INFO:
                    requestCashInfoCallCreator((int) objects[0], (int) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_INTEGRAL_DATA:
                    requestIntegralDataCallCreator((String) objects[0], (String) objects[1]);
                    break;
                case GameEvent.GAME_EVENT_NET_CASH_INFO:
                    requestNetCashInfoCallCreator((int) objects[0], (String) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_INVITE_INFO:
                    requestInviteInfoCallCreator((int) objects[0], (String) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_HB_GROUP:
                    requestHbGroupInfoCallCreator((int) objects[0], (String) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_PVP:
                    requestPvpInfoCallCreator((int) objects[0], (String) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_CDKEY:
                    requestCDKeyCallCreator((String) objects[0], (String) objects[1], (String) objects[2]);
                    break;
                case GameEvent.GAME_EVENT_RESULT_PAY:
                    PayResultCallUnity((String) objects[0], (int) objects[1], (String) objects[2]);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 监听AdLiveData事件
     *
     * @param adLiveData
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void adevent(AdLiveData adLiveData) {
        if (adLiveData != null) {
            String adName = adLiveData.getAdName();
            String adType = adLiveData.getAdType();
            int adResult = adLiveData.getAdResult();
            LogUtil.e("LayaWbActivity", "广告回调 code:" + adLiveData.getCode() + ",adName:" + adName);

            if (adLiveData.getCode() == AdLiveData.AD_CLICK_CODE) {
                AdClickedCall(adName);
            } else if (adLiveData.getCode() == AdLiveData.AD_OPENRESULT_CODE) {
                if (!adType.equals("banner"))//banner有自动刷新，不通知结果给unity
                {
                    LogUtil.e("LayaWbActivity", "广告回调 adResult:" + adResult);
                    if (adResult == AdLiveData.ADRESULT_SUCCESS) {
                        AdResultCall(true, adLiveData);
                    } else {
                        AdResultCall(false, adLiveData);
                    }
                }
            }
        }
    }

    /**
     * 监听字符串事件
     *
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(final String data) {
        if (!TextUtils.isEmpty(data) && data.contains("outOrderId#")) {
            final String outOrderId = data.split("#")[1];
//            Cocos2dxHelper.runOnGLThread(new Runnable() {
//                @Override
//                public void run() {
//                    Cocos2dxJavascriptJavaBridge.evalString("cc.CoreManager.IntegralOutOrderIdCallBack(\"" + outOrderId + "\")");
//                }
//            });
        }
    }

    /**
     * 上报下发道具成功
     *
     * @param json
     */
    public void reportIntegral(String json) {
        GameManager.getInstance().reportIntegral(json);
    }

    /**
     * 打开移动积分兑换h5页面
     */
    public void openIntegralActivity() {
        GameManager.getInstance().openIntegralActivity();
    }

    /**
     * 主动查询积分兑换
     *
     * @return
     */
    public void getIntegralData() {
        GameManager.getInstance().getIntegralData();
    }


    public void requestGameParamCallCreator(final String info, final int result) {
        //result 0成功非0失败
        StringBuilder cmd = new StringBuilder("window.GameConfigCallBack(");
        cmd.append(result);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }

    /**
     * 移动积分兑换 Creator
     *
     * @param callType
     * @param info
     */
    public void requestIntegralDataCallCreator(final String callType, final String info) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                Cocos2dxJavascriptJavaBridge.evalString("cc.CoreManager.GetIntegralDataCallBack(\"" + callType + "#" + info + "\")");
//            }
//        });
    }

    /**
     * 真实领红包 回调creator
     *
     * @param result
     * @param action
     * @param desc
     */
    public void requestNetCashInfoCallCreator(final int result, final String action, final String desc) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                String en = Base64.encode(desc.getBytes());
//                //直接传json数据creator解析报错，加密后传输；
//                String js = "cc.NetCashManager.GetNetCashCallBack(";
//                String params = "'" + result + "','" + action + "','" + en + "'";
//                js = js + params + ");";
//                Cocos2dxJavascriptJavaBridge.evalString(js);
//            }
//        });
    }

    /**
     * 好友邀请 回调creator
     *
     * @param result
     * @param action
     * @param desc
     */
    public void requestInviteInfoCallCreator(final int result, final String action, final String desc) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                String en = Base64.encode(desc.getBytes());
//                //直接传json数据creator解析报错，加密后传输；
//                String js = "cc.InviteManager.GetInviteCallBack(";
//                String params = "'" + result + "','" + action + "','" + en + "'";
//                js = js + params + ");";
//                Cocos2dxJavascriptJavaBridge.evalString(js);
//            }
//        });
    }

    /**
     * 登录回调
     *
     * @param result
     */
    public void loginResultCallCreator(final boolean result) {
        int params;
        if (result) {
            params = 0;
        } else {
            params = 1;
        }
        StringBuilder cmd = new StringBuilder("window.SocialCallBack(");
        cmd.append(params);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }

    /**
     * 用户信息回调
     *
     * @param result
     * @param info
     */
    public void getUserInfoResultCallCreator(final boolean result, final String info) {
        int params;
        if (result) {
            params = 0;
        } else {
            params = 1;
        }
        StringBuilder cmd = new StringBuilder("window.UserInfoCallBack(");
        cmd.append(params);
        cmd.append(',');
        cmd.append(info);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }

    /**
     * 红包群 回调creator
     *
     * @param result
     * @param action
     * @param info
     */
    public void requestHbGroupInfoCallCreator(final int result, final String action, final String info) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                String en = Base64.encode(info.getBytes());
//                //直接传json数据creator解析报错，加密后传输；
//                String js = "cc.HbGroupManager.GetHbGroupCallBack(";
//                String params = "'" + result + "','" + action + "','" + en + "'";
//                js = js + params + ");";
//                Cocos2dxJavascriptJavaBridge.evalString(js);
//            }
//        });
    }


    /**
     * 获取当前电量
     */
    public String getWifiSSID() {
        return GameManager.getInstance().getWifiSSID();
    }


    /**
     * 积分模块是否打开
     *
     * @return
     */
    public boolean redeemEnable() {
        return GameManager.getInstance().redeemEnable();
    }

    /**
     * 红包回调
     */
    public void requestCashInfoCallCreator(final int tag, final int status, final String info) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                String en = Base64.encode(info.getBytes());
//                //直接传json数据creator解析报错，加密后传输；
//                String js = "cc.CashManager.GetCashCallBack(";
//                String args = "'" + tag + "','" + status + "','" + en + "'";
//                js = js + args + ");";
//                Cocos2dxJavascriptJavaBridge.evalString(js);
//            }
//        });
    }

    /**
     * 获取媒体音量
     *
     * @return
     */
    public int getMusicVolume() {
        return GameManager.getInstance().getMusicVolume();
    }

    /**
     * 积分v2获取订单列表
     *
     * @param outOrderId
     */
    public void getOrderData(String outOrderId) {
        GameManager.getInstance().getOrderData(outOrderId);
    }

    /**
     * 积分v2获取商品列表
     */
    public void getProdouctData() {
        GameManager.getInstance().getProdouctData();
    }

    /**
     * 积分v2获取未发货订单列表
     */
    public void getLostOrderData() {
        GameManager.getInstance().getLostOrderData();
    }

    /**
     * 积分v2更新订单列表状态
     *
     * @param orderList
     */
    public void updateOrderState(String orderList) {
        GameManager.getInstance().updateOrderState(orderList);
    }


    /**
     * 积分v2打开h5页
     *
     * @param actJson
     */
    public void openNewIntegralActivity(String actJson) {
        GameManager.getInstance().openNewIntegralActivity(actJson);
    }

    /**
     * 对战 回调unity
     *
     * @param result
     * @param action
     * @param info
     */
    public void requestPvpInfoCallCreator(final int result, final String action, final String info) {
//        Cocos2dxHelper.runOnGLThread(new Runnable() {
//            @Override
//            public void run() {
//                String en = Base64.encode(info.getBytes());
//                //直接传json数据creator解析报错，加密后传输；
//                String js = "cc.PvpManager.GetPvpCallBack(";
//                String params = "'" + result + "','" + action + "','" + en + "'";
//                js = js + params + ");";
//                Cocos2dxJavascriptJavaBridge.evalString(js);
//            }
//        });
    }
    
    /**
     * 兑换码 回调Creator
     *
     * @param price
     * @param status
     * @param msg
     */
    public void requestCDKeyCallCreator(final String price, final String status, final String msg) {
        StringBuilder cmd = new StringBuilder("window.CDKeyCallBack(");
        cmd.append(status);
        cmd.append(',');
        cmd.append(price);
        cmd.append(',');
        cmd.append(msg);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }


    /**
     * 支付掉单逻辑
     */
    public ArrayList<Integer> payList = new ArrayList<Integer>();

    public int[] GetPayList() {
//        Log.d("PayLog", "补单查询");
        int[] d = new int[payList.size()];
        for (int i = 0; i < payList.size(); i++) {
            d[i] = payList.get(i);
//            Log.d("PayLog", "补单: " + d[i]);
        }
        return d;
    }

    public void ClearPayList(int id) {
        for (int i = 0; i < payList.size(); i++) {
            if (payList.get(i) == id) {
                payList.remove(i);
                break;
            }
        }
    }

    /**
     * 支付结果回调到unity
     *
     * @param result
     * @param id
     */
    public void PayResultCallUnity(String result, int id, String userData) {
        String params;
        params = "defult";
        if (result.equals("1")) {
            payList.add(id);
            params = "Paysuccess";
        } else if (result.equals("2")) {
            params = "Payfail";
        } else if (result.equals("3")) {
            params = "Paycancel";
        }
        LogUtil.e("PayCheckCallBack", "PayCheckCallBack params = " + params + ",id = " + id + ", userData = "+ userData);
        StringBuilder cmd = new StringBuilder("window.PayCheckCallBack(");
        cmd.append(params);
        cmd.append(',');
        cmd.append(id);
        cmd.append(',');
        cmd.append(userData);
        cmd.append(')');
        ConchJNI.RunJS(cmd.toString());
    }
}
