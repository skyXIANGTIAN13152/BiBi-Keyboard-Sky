package com.brycewg.asrkb.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

/**
 * 基础 Activity 类
 *
 * 统一处理 Android 15 (SDK 35) 边缘到边缘显示的兼容性：
 * - 调用 enableEdgeToEdge() 确保在所有 Android 版本上行为一致
 * - View UI 子类需要自行处理 insets；Compose 页面通过各自 Scaffold/windowInsets 处理
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate() 之前调用以确保窗口属性正确设置
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
}
