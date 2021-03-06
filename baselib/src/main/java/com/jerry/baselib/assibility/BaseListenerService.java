package com.jerry.baselib.assibility;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.greenrobot.eventbus.EventBus;

import com.jerry.baselib.BaseApp;
import com.jerry.baselib.Key;
import com.jerry.baselib.R;
import com.jerry.baselib.common.util.AppUtils;
import com.jerry.baselib.common.util.CollectionUtils;
import com.jerry.baselib.common.util.FileUtil;
import com.jerry.baselib.common.util.LogUtils;
import com.jerry.baselib.common.util.OnDataChangedListener;
import com.jerry.baselib.common.util.ToastUtil;
import com.jerry.baselib.common.util.WeakHandler;

/**
 * Created by cxk on 2017/2/4. email:471497226@qq.com
 * <p>
 * 获取即时微信聊天记录服务类
 */

public abstract class BaseListenerService extends AccessibilityService {

    protected static BaseListenerService instance;
    protected static int TIME_SSHORT = 800;
    protected static int TIME_SHORT = 2000;
    protected static int TIME_MIDDLE = 3000;
    protected static final int TIME_LONG = 4000;
    protected static final int TIME_LONGLONG = 6000;
    protected static int ERRORCOUNT = 3;
    protected int taskIndex;
    protected int errorCount;
    protected String packageName;
    /**
     * 微信登录
     */
    protected static final int MSG_WX_LOGIN = 801;

    public static int mWidth;
    public static int mHeight;
    protected GlobalActionAutomator mGlobalActionAutomator;

    public static BaseListenerService getInstance() {
        return instance;
    }

    public WeakHandler mWeakHandler;

    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    protected void onServiceConnected() {
        LogUtils.d("service onServiceConnected!");
        ToastUtil.showShortText("服务已开启");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d("service onUnbind!");
        ToastUtil.showShortText("服务已被关闭");
        return super.onUnbind(intent);
    }

    /**
     * 必须重写的方法：系统要中断此service返回的响应时会调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onInterrupt() {
        ToastUtil.showShortText("我快被终结了啊-----");
    }

    /**
     * 是否在首页
     */
    protected abstract boolean isHomePage();

    /**
     * 开启任务
     */
    protected void start(int start) {
        removeAllMessages();
        errorCount = 0;
        mWeakHandler.sendEmptyMessage(start);
    }

    protected void stopScript() {
        if (AppUtils.playing) {
            AppUtils.playing = false;
            errorCount = 0;
            taskIndex = 0;
            pause();
        }
    }

    /**
     * 清理msg
     */
    protected void removeAllMessages() {
        instance.mWeakHandler.removeMessages(MSG_WX_LOGIN);
    }

    /**
     * 微信登录
     */
    public static void wxLogin() {
        if (instance == null) {
            ToastUtil.showLongText("请开启辅助服务哦");
            return;
        }
        instance.mWeakHandler.sendEmptyMessage(MSG_WX_LOGIN);
    }

    /**
     * 微信采集微信号
     */
    private void handleWxLogin() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            if ("com.tencent.mm".contentEquals(rootNode.getPackageName())) {
                List<AccessibilityNodeInfo> bqs = rootNode.findAccessibilityNodeInfosByText("我");
                if (!CollectionUtils.isEmpty(bqs)) {
                    AccessibilityNodeInfo bq = bqs.get(bqs.size() - 1);
                    Rect rect = new Rect();
                    bq.getBoundsInScreen(rect);
                    // 在底部
                    if (rect.top > mHeight * 0.8) {
                        if (exeClickText("我")) {
                            errorCount = 0;
                            List<AccessibilityNodeInfo> dcfs = rootNode.findAccessibilityNodeInfosByText("微信号");
                            if (!CollectionUtils.isEmpty(dcfs)) {
                                AccessibilityNodeInfo dcf = dcfs.get(dcfs.size() - 1);
                                CharSequence textc = dcf.getText();
                                if (textc != null) {
                                    String text = textc.toString();
                                    if (!TextUtils.isEmpty(text)) {
                                        ToastUtil.showLongText(text);
                                        text = text.replace("微信号：", "");
                                        Bundle map = new Bundle();
                                        map.putString(Key.WXCODE, text);
                                        EventBus.getDefault().post(map);
                                        back();
                                        return;
                                    }
                                }
                            }
                        }
                    } else {
                        back();
                        errorCount++;
                    }
                } else {
                    back();
                    errorCount++;
                }
            } else if (!"android".contentEquals(rootNode.getPackageName())) {
                errorCount++;
            }
        } else {
            errorCount++;
        }
        if (errorCount > 5) {
            ToastUtil.showLongText("微信授权失败");
            errorCount = 0;
        } else {
            this.mWeakHandler.postDelayed(this::handleWxLogin, TIME_MIDDLE);
        }
    }

    /**
     * 暂停任务
     */
    public void pause() {
        removeAllMessages();
    }

    protected void backToHome(EndCallback endCallback) {
        if (isHomePage()) {
            endCallback.onEnd(true);
        } else {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    /**
     * 判断是否含有文案
     */
    protected boolean hasText(String... texts) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }
        for (String text : texts) {
            List<AccessibilityNodeInfo> indicators = rootNode.findAccessibilityNodeInfosByText(text);
            if (CollectionUtils.isEmpty(indicators)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 震动发声提示
     */
    protected void giveNotice() {
        Vibrator vibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[]{500, 1000, 500, 1000}, -1);
        }
        SoundPool mSoundPool;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSoundPool = new SoundPool.Builder().setMaxStreams(10).build();
        } else {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
        }
        int mWinMusic = mSoundPool.load(BaseApp.getInstance(), R.raw.fadein, 1);
        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundPool.play(mWinMusic, 0.6F, 0.6F, 0, 0, 1.0F));
    }

    public void swipToClickText(String text, EndCallback endCallback) {
        swipToClickText(text, 2, endCallback);
    }

    public void swipToClickText(String text, int rate, EndCallback endCallback) {
        if (errorCount > 3) {
            errorCount = 0;
            endCallback.onEnd(false);
            return;
        }
        if ((exeClickText(text, 0, (int) (mHeight * 0.1), mWidth, (int) (mHeight * 0.9)))) {
            errorCount = 0;
            this.mWeakHandler.postDelayed(() -> endCallback.onEnd(true), TIME_SHORT);
            return;
        }
        errorCount++;
        exeSwip(mWidth >> rate, (int) (mHeight * 0.75), mWidth >> rate, mHeight >> 2);
        this.mWeakHandler.postDelayed(() -> swipToClickText(text, rate, endCallback), TIME_SHORT);
    }

    public void swipToLongClickText(List<String> texts, EndCallback endCallback) {
        if (errorCount > 3) {
            errorCount = 0;
            endCallback.onEnd(false);
            return;
        }
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            for (String text : texts) {
                List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByText(text);
                if (nodes.size() > 0) {
                    if (exeLongClick(nodes.get(0))) {
                        errorCount = 0;
                        this.mWeakHandler.postDelayed(() -> endCallback.onEnd(true), TIME_SHORT);
                    }
                    return;
                }
            }
        }
        errorCount++;
        exeSwip(mWidth >> 2, (int) (mHeight * 0.75), mWidth >> 2, (int) (mHeight * 0.25));
        this.mWeakHandler.postDelayed(() -> swipToLongClickText(texts, endCallback), TIME_SHORT);
    }

    public String getNodeText(AccessibilityNodeInfo root) {
        CharSequence txt = root.getText();
        if (txt == null) {
            txt = Key.NIL;
        }
        return txt.toString();
    }

    public String getNodeText(AccessibilityNodeInfo root, String id) {
        List<AccessibilityNodeInfo> inputs = root.findAccessibilityNodeInfosByViewId(packageName + id);
        if (!CollectionUtils.isEmpty(inputs)) {
            AccessibilityNodeInfo node = inputs.get(inputs.size() - 1);
            CharSequence txt = node.getText();
            if (txt == null) {
                txt = Key.NIL;
            }
            return txt.toString();
        }
        return Key.NIL;
    }

    public boolean input(String id, String text) {
        return input(id, text, true);
    }

    public boolean input(String id, String text, boolean last) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            List<AccessibilityNodeInfo> inputs = root.findAccessibilityNodeInfosByViewId(packageName + id);
            if (!CollectionUtils.isEmpty(inputs)) {
                AccessibilityNodeInfo node = inputs.get(last ? inputs.size() - 1 : 0);
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                return true;
            }
        }
        return false;
    }

    public void input(AccessibilityNodeInfo node, String text) {
        //粘贴板
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    public void back() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    @SuppressLint("DefaultLocale")
    public void exeSwip(int startX, int startY, int endX, int endY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mGlobalActionAutomator == null) {
                mGlobalActionAutomator = new GlobalActionAutomator(this);
            }
            try {
                if (!mGlobalActionAutomator.swipe(startX, startY, endX, endY, 800)) {
                    ToastUtil.showShortText("辅助停止喽 重启试试");
                }
            } catch (Throwable e) {
                LogUtils.e(e.getLocalizedMessage());
            }
        } else {
            String swip = "input swipe %d %d %d %d %d";
            execShellCmd(String.format(swip, startX, startY, endX, endY, 800));
        }
    }

    @SuppressLint("DefaultLocale")
    public void exeClicks(List<Point> points, int index, OnDataChangedListener<Integer> callBack) {
        if (CollectionUtils.isItemInCollection(index, points)) {
            Point point = points.get(index);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (mGlobalActionAutomator == null) {
                    mGlobalActionAutomator = new GlobalActionAutomator(this);
                }
                try {
                    if (!mGlobalActionAutomator.click(point.x, point.y)) {
                        ToastUtil.showShortText("辅助停止喽 重启试试");
                    }
                } catch (Throwable e) {
                    LogUtils.e(e.getLocalizedMessage());
                }
            } else {
                String click = "input tap %d %d";
                execShellCmd(String.format(click, point.x, point.y));
            }
            mWeakHandler.postDelayed(() -> exeClicks(points, index + 1, callBack), TIME_SSHORT);
        } else {
            if (callBack != null) {
                callBack.onDataChanged(index);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public boolean exeLongClick(AccessibilityNodeInfo target) {
        Rect rect = new Rect();
        target.getBoundsInScreen(rect);
        int x = (rect.left + rect.right) >> 1;
        int y = (rect.top + rect.bottom) >> 1;
        if (rect.left >= 0 && rect.right <= mWidth && rect.top >= 0 && rect.bottom <= mHeight) {
            return exeLongClick(x, y);
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean exeLongClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mGlobalActionAutomator == null) {
                mGlobalActionAutomator = new GlobalActionAutomator(this);
            }
            try {
                if (!mGlobalActionAutomator.longClick(x, y)) {
                    ToastUtil.showShortText("辅助停止喽 重启试试");
                    return false;
                }
            } catch (Throwable e) {
                LogUtils.e(e.getLocalizedMessage());
                return false;
            }
        } else {
            String click = "input swipe %d %d %d %d %d";
            execShellCmd(String.format(click, x, y, x, y, 900));
        }
        return true;
    }

    @SuppressLint("DefaultLocale")
    public void exeClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mGlobalActionAutomator == null) {
                mGlobalActionAutomator = new GlobalActionAutomator(this);
            }
            try {
                if (!mGlobalActionAutomator.click(x, y)) {
                    ToastUtil.showShortText("辅助停止喽 重启试试");
                }
            } catch (Throwable e) {
                LogUtils.e(e.getLocalizedMessage());
            }
        } else {
            String click = "input tap %d %d";
            execShellCmd(String.format(click, x, y));
        }
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClick(AccessibilityNodeInfo target) {
        Rect rect = new Rect();
        target.getBoundsInScreen(rect);
        int x = (rect.left + rect.right) >> 1;
        int y = (rect.top + rect.bottom) >> 1;
        if (rect.left >= 0 && rect.right <= mWidth && rect.top >= 0 && rect.bottom <= mHeight) {
            exeClick(x, y);
            return true;
        }
        return exeClick(target, rect.left, rect.top, rect.right, rect.bottom);
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClick(AccessibilityNodeInfo target, int left, int top, int right, int bottom) {
        Rect rect = new Rect();
        target.getBoundsInScreen(rect);
        int x = (rect.left + rect.right) >> 1;
        int y = (rect.top + rect.bottom) >> 1;
        if (rect.left >= left && rect.right <= right && rect.top >= top && rect.bottom <= bottom) {
            exeClick(x, y);
            return true;
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public void clickFirst(String id) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByViewId(packageName + id);
            if (!CollectionUtils.isEmpty(nodes)) {
                exeClick(nodes.get(0));
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public boolean clickLast(String id) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByViewId(packageName + id);
            if (!CollectionUtils.isEmpty(nodes)) {
                return exeClick(nodes.get(nodes.size() - 1));
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClickId(String id, int parentIn) {
        return exeClickId(getRootInActiveWindow(), id, parentIn);
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClickId(AccessibilityNodeInfo parent, String id, int parentIn) {
        if (parent != null) {
            List<AccessibilityNodeInfo> nodes = parent.findAccessibilityNodeInfosByViewId(packageName + id);
            if (!CollectionUtils.isEmpty(nodes)) {
                AccessibilityNodeInfo target = nodes.get(nodes.size() - 1);
                for (int i = 0; i < parentIn; i++) {
                    target = target.getParent();
                }
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean quickClickText(String text) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByText(text);
            if (!CollectionUtils.isEmpty(nodes)) {
                AccessibilityNodeInfo target = nodes.get(nodes.size() - 1);
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClickText(String text) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByText(text);
            if (!CollectionUtils.isEmpty(nodes)) {
                return exeClick(nodes.get(nodes.size() - 1));
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClickText(String text, int left, int top, int right, int bottom) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByText(text);
            if (!CollectionUtils.isEmpty(nodes)) {
                return exeClick(nodes.get(nodes.size() - 1), left, top, right, bottom);
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public boolean exeClickText(String text, int index) {
        AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
        if (newRootNode != null) {
            List<AccessibilityNodeInfo> nodes = newRootNode.findAccessibilityNodeInfosByText(text);
            if (CollectionUtils.isItemInCollection(index, nodes)) {
                exeClick(nodes.get(index));
                return true;
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public void exeClickTexts(List<String> strs, EndCallback endCallback) {
        if (CollectionUtils.isEmpty(strs)) {
            endCallback.onEnd(false);
        } else {
            exeClickTexts(strs, 0, endCallback);
        }
    }

    @SuppressLint("DefaultLocale")
    private void exeClickTexts(List<String> strs, int index, EndCallback endCallback) {
        if (errorCount > 3) {
            errorCount = 0;
            endCallback.onEnd(false);
            return;
        }
        if (CollectionUtils.isItemInCollection(index, strs)) {
            String str = strs.get(index);
            if (exeClickText(str)) {
                mWeakHandler.postDelayed(() -> exeClickTexts(strs, index + 1, endCallback), TIME_SSHORT);
            } else {
                errorCount++;
                mWeakHandler.postDelayed(() -> exeClickTexts(strs, index, endCallback), TIME_SSHORT);
            }
            return;
        }
        errorCount = 0;
        mWeakHandler.postDelayed(() -> endCallback.onEnd(true), TIME_SSHORT);
    }

    private void execShellCmd(String cmd) {
        LogUtils.d(cmd);
        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            outputStream = process.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeBytes(cmd + "\n");
            dataOutputStream.flush();
        } catch (IOException e) {
            ToastUtil.showShortText("请查看是否获取root权限");
            e.printStackTrace();
        } finally {
            FileUtil.close(dataOutputStream, outputStream);
        }
    }

    protected interface EndCallback {

        void onEnd(boolean result);
    }
}