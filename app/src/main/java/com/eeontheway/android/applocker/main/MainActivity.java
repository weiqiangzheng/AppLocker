package com.eeontheway.android.applocker.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.applock.AppLockService;
import com.eeontheway.android.applocker.applock.AppLockSettingsManager;
import com.eeontheway.android.applocker.update.ApkUpdater;
import com.igexin.sdk.PushManager;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobInstallation;

/**
 * 主Activity
 * 用于显示程序主界面
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class MainActivity extends AppCompatActivity {
    private Toolbar tb_main;
    private DrawerLayout dl_main;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private long lastBackKeyPressTime = 0;
    private AppLockSettingsManager settingsManager;

    /**
     * Activity的onCreate函数
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsManager = AppLockSettingsManager.getInstance(this);

        initSDK();
        initViews();
        setTitle(R.string.app_locker);

        // 检查密码是否设置，只有当设置后，才能启动
        checkPassword();
    }

    /**
     * Activity的onDestroy函数
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化Bmob SDK
     */
    private void initSDK () {
        final String APPID = "46120064d8e98adb870a67247a102485";

        // 初始化基础SDK
        Bmob.initialize(this, APPID);

        // 保存设备信息
        BmobInstallation.getCurrentInstallation(this).save();

        // 初始化个推的推送服务
        PushManager.getInstance().initialize(getApplicationContext());
    }

    /**
     * 初始化ActionBar
     */
    private void initViews() {
        // 配置标题栏
        tb_main = (Toolbar)findViewById(R.id.tl_main);
        tb_main.setTitle(getString(R.string.app_locker));
        tb_main.setTitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(tb_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // 配置DrawLayout
        dl_main = (DrawerLayout)findViewById(R.id.dl_main);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, dl_main, tb_main,
                R.string.opene, R.string.close);
        actionBarDrawerToggle.syncState();
        dl_main.setDrawerListener(actionBarDrawerToggle);

        dl_main.openDrawer(Gravity.LEFT);
    }

    /**
     * 检查密码
     */
    private void checkPassword() {
        String password = settingsManager.getPassword();
        if((password == null) || (password.isEmpty())) {
            PasswordSetActivity.statActivity(this);
        } else {
            startCheckUpdate();
            AppLockService.startBlockService(this);
        }
    }

    /**
     * 等待初始密码的设置结果
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // 密码设置正确，保存密码
            settingsManager.savePassword(data.getStringExtra(PasswordSetActivity.RETURN_PARAM_PASS));

            // 正常启动
            startCheckUpdate();
            AppLockService.startBlockService(this);
        } else {
            // 取消设置，结束应用
            finish();
        }
    }

    /**
     * Activiy的onCreateOptionMenu回调
     * @param menu 创建的菜单
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_app_locker_setting, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 处理返回按钮按下的响应
     * @param item 被按下的项
     * @return 是否需要被继续处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_applocker_setting:
                SettingsActivity.start(MainActivity.this);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * 连续两次按返回键才认为是退出
     */
    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastBackKeyPressTime) < 2000) {
            finish();
        } else {
            Toast.makeText(MainActivity.this, R.string.exit_confirm_alert, Toast.LENGTH_SHORT).show();
            lastBackKeyPressTime = currentTime;
        }
    }

    /**
     * 检查更新
     */
    private void startCheckUpdate() {
        // 读取存储的更新使能设置
        SharedPreferences ref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String key = getResources().getString(R.string.autoupdateKey);
        boolean checkUpdateEnable = ref.getBoolean(key, true);

        // 如果要更新，则调用更新管理器
        if (checkUpdateEnable) {
            ApkUpdater apkupdater = new ApkUpdater(MainActivity.this);
            apkupdater.start(getResources().getString(R.string.updateInfoUrl));
        }
    }
}