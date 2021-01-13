package com.jerry.bitcoin;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.alibaba.fastjson.JSON;
import com.jerry.baselib.BaseApp;
import com.jerry.baselib.assibility.BaseListenerService;
import com.jerry.baselib.common.bean.AxiangMeassage;
import com.jerry.baselib.common.flow.FloatItem;
import com.jerry.baselib.common.flow.FloatLogoMenu;
import com.jerry.baselib.common.flow.FloatMenuView;
import com.jerry.baselib.common.util.AppUtils;
import com.jerry.baselib.common.util.DisplayUtil;
import com.jerry.baselib.common.util.JJSON;
import com.jerry.baselib.common.util.LogUtils;
import com.jerry.baselib.common.util.ToastUtil;
import com.jerry.baselib.common.util.WeakHandler;
import com.jerry.bitcoin.beans.CoinBean;
import com.jerry.bitcoin.home.MainActivity;
import com.jerry.bitcoin.interfaces.TaskCallback;
import com.jerry.bitcoin.platform.HuobiTask;

import cn.leancloud.chatkit.event.LCIMIMTypeMessageEvent;
import cn.leancloud.im.v2.AVIMException;
import cn.leancloud.im.v2.AVIMMessageOption;
import cn.leancloud.im.v2.AVIMReservedMessageType;
import cn.leancloud.im.v2.AVIMTypedMessage;
import cn.leancloud.im.v2.callback.AVIMConversationCallback;
import cn.leancloud.im.v2.messages.AVIMTextMessage;

/**
 * Created by cxk on 2017/2/4. email:471497226@qq.com
 * <p>
 * 获取即时微信聊天记录服务类
 */

public class ListenerService extends BaseListenerService {

    /**
     * 擦亮
     */
    private static final int MSG_DO_TASK = 101;
    private static final int UNINSTALL_APP = 1;
    /**
     * 对方的IP
     */
    public static String sDeviceIp;

    private FloatLogoMenu menu;

    private final FloatItem startItem = new FloatItem("开始", 0x99000000, 0x99000000,
        BitmapFactory.decodeResource(BaseApp.getInstance().getResources(), R.drawable.play), "0");
    private final FloatItem stopItem = new FloatItem("暂停", 0x99000000, 0x99000000,
        BitmapFactory.decodeResource(BaseApp.getInstance().getResources(), R.drawable.pause), "0");
    private final List<FloatItem> itemList = new ArrayList<>();
    private TaskCallback mTasksCallback;
    private CoinBean coinBean;

    @Override
    public void onCreate() {
        super.onCreate();
        setTasksCallback(new HuobiTask());
        mWeakHandler = new WeakHandler(msg -> {
            switch (msg.what) {
                case MSG_DO_TASK:
                    mWeakHandler.postDelayed(this::doTask, TIME_LONGLONG);
                    return true;
                case UNINSTALL_APP:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (getApplicationInfo() != null) {
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                            Intent it = new Intent(Intent.ACTION_DELETE, uri);
                            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            MyApplication.getInstance().startActivity(it);
                            mWeakHandler.postDelayed(() -> exeClickText(msg.obj.toString()), TIME_MIDDLE);
                        }
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // TODO 初始化Items
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        ListenerService.mWidth = point.x;
        ListenerService.mHeight = point.y - DisplayUtil.getNavigationBarHeightIfRoom(MainActivity.getInstance());
        ToastUtil.showShortText("服务已开启\n屏幕宽：" + ListenerService.mWidth + "\n屏幕高：" + ListenerService.mHeight);
        itemList.clear();
        itemList.add(startItem);

        if (menu == null) {
            menu = new FloatLogoMenu.Builder()
                .withContext(
                    getApplication())//这个在7.0（包括7.0）以上以及大部分7.0以下的国产手机上需要用户授权，需要搭配<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
                .logo(BitmapFactory.decodeResource(BaseApp.getInstance().getResources(), R.drawable.menu))
                .drawCicleMenuBg(true)
                .backMenuColor(0xffe4e3e1)
                .setBgDrawable(ContextCompat.getDrawable(BaseApp.getInstance(), R.drawable.yw_game_float_menu_bg))
                //这个背景色需要和logo的背景色一致
                .addFloatItem(itemList)
                .defaultLocation(FloatLogoMenu.LEFT)
                .drawRedPointNum(false)
                .showWithListener(new FloatMenuView.SimpleMenuClickListener() {
                    @Override
                    public void onItemClick(int position, String title) {
                        if (AppUtils.playing) {
                            stopScript();
                            itemList.clear();
                            itemList.add(startItem);
                            menu.updateFloatItemList(itemList);
                            menu.hide();
                            return;
                        }
                        if (AppUtils.isAccessibilitySettingsOff(ListenerService.this)) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            ListenerService.this.startActivity(intent);
                            ToastUtil.showLongText("请先开启辅助哦");
                            return;
                        }
                        AppUtils.playing = true;
                        if (position != 0) {
                            return;
                        }
                        start(MSG_DO_TASK);
                        itemList.clear();
                        itemList.add(stopItem);
                        menu.updateFloatItemList(itemList);
                        menu.hide();
                    }
                });
            menu.show();
        }
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    protected boolean isHomePage() {
        return true;
    }

    /**
     * 清理msg
     */
    @Override
    protected void removeAllMessages() {
        super.removeAllMessages();
        mWeakHandler.removeMessages(MSG_DO_TASK);
    }

    private void doTask() {
        if (!AppUtils.playing) {
            return;
        }
        switch (taskIndex) {
            case 0:
                coinBean = mTasksCallback.getBuyInfo(this);
                LogUtils.d(coinBean.toString());
                sendMessage(JSON.toJSONString(coinBean));
                break;
            default:
                break;
        }
        mWeakHandler.sendEmptyMessage(MSG_DO_TASK);
    }

    public void setTasksCallback(final TaskCallback tasksCallback) {
        mTasksCallback = tasksCallback;
        packageName = mTasksCallback.getPackageName();
    }

    /**
     * 发送消息给控制端
     */
    protected void sendMessage(String content) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
        AVIMTextMessage message = new AVIMTextMessage();
        message.setText(content);
        AVIMMessageOption option = new AVIMMessageOption();
        if (content.startsWith("tr:")) {
            option.setTransient(true);
        } else {
            option.setReceipt(true);
        }
        mTasksCallback.getAvimConversation(imConversation -> {
            if (imConversation == null) {
                LogUtils.e("imConversation is null");
                return;
            }
            imConversation.sendMessage(message, option, new AVIMConversationCallback() {
                @Override
                public void done(AVIMException e) {
                    if (null != e) {
                        ToastUtil.showShortText(e.getMessage());
                        return;
                    }
                    LogUtils.d("imConversation send success");
                }
            });
        });
    }


    /**
     * 处理推送过来的消息 同理，避免无效消息，此处加了 conversation id 判断
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LCIMIMTypeMessageEvent messageEvent) {
        mTasksCallback.getAvimConversation(imConversation -> {
            if (null != imConversation && null != messageEvent &&
                imConversation.getConversationId().equals(messageEvent.conversation.getConversationId())) {
                AVIMTypedMessage typedMessage = messageEvent.message;
                if (typedMessage.getMessageType() == AVIMReservedMessageType.TextMessageType.getType()) {
                    String text = ((AVIMTextMessage) typedMessage).getText();
                    AxiangMeassage meassage = JJSON.parseObject(text, AxiangMeassage.class);
                    if (meassage != null) {
                        if (!AppUtils.getDeviceId().equals(meassage.getDeviceId())) {
                            return;
                        }
                        String from = meassage.getNickname();
                        if (from == null) {
                            ToastUtil.showShortText("收到来自：" + typedMessage.getFrom() + "的消息，内容为：" + meassage.getContent());
                        }
                    }
                }
            }
        });

    }
}