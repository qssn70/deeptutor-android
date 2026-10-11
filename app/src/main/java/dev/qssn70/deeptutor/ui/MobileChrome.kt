package dev.qssn70.deeptutor.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import dev.qssn70.deeptutor.config.MobilePreferences

enum class ChromeAction {
    BACK, FORWARD, HOME, RELOAD, TEXT_SIZE, READING_FIXES, COPY_LINK, SERVER
}

/**
 * The webpage owns DeepTutor navigation. This overlay only exposes client
 * actions, leaving almost the entire display to the existing mobile web UI.
 * Long-press the unobtrusive menu button to move it to the other corner.
 */
class MobileChrome(
    context: Context,
    private val preferences: MobilePreferences,
    private val onAction: (ChromeAction) -> Unit
) : FrameLayout(context) {
    private var browser: WebView? = null
    private val isNight = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val foreground = if (isNight) Color.WHITE else Color.rgb(32, 35, 42)
    private val background = if (isNight) Color.rgb(28, 30, 36) else Color.WHITE

    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progressTintList = ColorStateList.valueOf(Color.rgb(65, 116, 225))
        progressBackgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        visibility = View.GONE
    }

    private val errorDescription = TextView(context).apply {
        textSize = 14f
        setTextColor(foreground)
        gravity = Gravity.CENTER
    }

    private val errorOverlay = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(24), dp(24), dp(24))
        setBackgroundColor(background)
        isClickable = true
        visibility = View.GONE

        addView(TextView(context).apply {
            text = "连接 DeepTutor 失败"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(foreground)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addView(errorDescription, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        addView(Button(context).apply {
            text = "重试"
            setOnClickListener { onAction(ChromeAction.RELOAD) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(18)
        })

        addView(Button(context).apply {
            text = "修改服务器地址"
            setOnClickListener { onAction(ChromeAction.SERVER) }
        })
    }

    private val menuButton = TextView(context).apply {
        text = "⋮"
        textSize = 25f
        setTextColor(foreground)
        gravity = Gravity.CENTER
        contentDescription = "客户端操作菜单；长按可切换左右位置"
        isFocusable = true
        isClickable = true
        elevation = dp(5).toFloat()
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(if (isNight) Color.rgb(42, 44, 52) else Color.rgb(248, 249, 252))
            setStroke(dp(1), if (isNight) Color.rgb(74, 75, 82) else Color.rgb(220, 223, 230))
        }
        setOnClickListener { showActions() }
        setOnLongClickListener {
            preferences.menuOnLeft = !preferences.menuOnLeft
            updateMenuPosition()
            Toast.makeText(context, "已调整操作按钮位置", Toast.LENGTH_SHORT).show()
            true
        }
    }

    init {
        setBackgroundColor(background)
    }

    fun attach(view: WebView) {
        browser = view
        removeAllViews()
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(progress, LayoutParams(LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP))
        addView(errorOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(menuButton, menuParams())
    }

    fun loading(progressPercent: Int) {
        progress.progress = progressPercent.coerceIn(0, 100)
        progress.visibility = if (progressPercent in 1..99) View.VISIBLE else View.GONE
    }

    fun showError(details: String) {
        errorDescription.text = details
        errorOverlay.visibility = View.VISIBLE
        loading(100)
    }

    fun hideError() {
        errorOverlay.visibility = View.GONE
    }

    private fun showActions() {
        val view = browser ?: return
        val popup = PopupMenu(context, menuButton, Gravity.END)
        fun add(action: ChromeAction, title: String, enabled: Boolean = true) {
            popup.menu.add(0, action.ordinal, action.ordinal, title).isEnabled = enabled
        }
        add(ChromeAction.BACK, "返回上一页", view.canGoBack())
        add(ChromeAction.FORWARD, "前进", view.canGoForward())
        add(ChromeAction.HOME, "DeepTutor 首页")
        add(ChromeAction.RELOAD, "刷新网页")
        add(ChromeAction.TEXT_SIZE, "文字大小：" + preferences.textZoomPercent + "%")
        add(ChromeAction.READING_FIXES, if (preferences.enableReadingFixes) "关闭公式溢出优化" else "开启公式溢出优化")
        add(ChromeAction.COPY_LINK, "复制当前页面链接")
        add(ChromeAction.SERVER, "切换服务器")
        popup.setOnMenuItemClickListener { item ->
            onAction(ChromeAction.values()[item.itemId])
            true
        }
        popup.show()
    }

    private fun menuParams() = LayoutParams(dp(44), dp(44),
        Gravity.TOP or if (preferences.menuOnLeft) Gravity.START else Gravity.END
    ).apply {
        topMargin = dp(6)
        leftMargin = dp(7)
        rightMargin = dp(7)
    }

    private fun updateMenuPosition() {
        menuButton.layoutParams = menuParams()
    }

    private fun dp(value: Int): Int =
        (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(),
            resources.displayMetrics) + 0.5f).toInt()
}
