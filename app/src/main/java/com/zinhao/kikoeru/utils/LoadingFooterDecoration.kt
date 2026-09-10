package com.zinhao.kikoeru.utils

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zinhao.kikoeru.R


class LoadingFooterDecoration(
    private val recyclerView: RecyclerView,
    footerHeightDp: Int = 80
) : RecyclerView.ItemDecoration() {

    private val footerHeight: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        footerHeightDp.toFloat(),
        recyclerView.context.resources.displayMetrics
    ).toInt()

    private var bgColor: Int = Color.TRANSPARENT
    private var foregroundColor: Int = Color.WHITE
    init {
        foregroundColor = ContextCompat.getColor(recyclerView.context, R.color.main_color)
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = foregroundColor
        strokeWidth = footerHeightDp*0.2f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    var isLoading: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    rotateAnimator.start()
                    sweepAnimator.start()      // 启动 sweep 动画
                } else {
                    rotateAnimator.cancel()
                    sweepAnimator.cancel()     // 取消 sweep 动画
                }
                recyclerView.post {
                    recyclerView.invalidateItemDecorations()
                }
            }
        }

    private var rotationAngle = 0f
    private val rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            if (isLoading) recyclerView.postInvalidate() // 用 postInvalidate 代替 invalidate
        }
    }

    // ===== 新增：sweepAngle 循环动画 =====
    // 在 60° ~ 300° 之间来回呼吸
    private var sweepAngle = 60f
    private val sweepAnimator = ValueAnimator.ofFloat(60f, 300f).apply {
        duration = 600
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE          // 到了 300 自动反向回到 60
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            sweepAngle = it.animatedValue as Float
        }
    }

    // ======== 给最后一个 item 底部留空间 ========
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (!isLoading) return

        val adapter = parent.adapter ?: return
        val position = parent.getChildAdapterPosition(view)

        // 只在有数据时，给最后一个 item 底部留空间
        if (adapter.itemCount > 0 && position == adapter.itemCount - 1) {
            outRect.bottom = footerHeight
        }
    }

    // ======== 绘制 loading ========
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

        if (!isLoading) {
            rotateAnimator.cancel()
            return
        }

        if (!rotateAnimator.isStarted) {
            rotateAnimator.start()
        }

        val adapter = parent.adapter
        val itemCount = adapter?.itemCount ?: 0
        val radius = (footerHeight * 0.3f).coerceAtMost(100f)

        val centerX: Float
        val centerY: Float

        if (itemCount == 0) {
            // ===== 列表为空：画在 RecyclerView 正中央 =====
            centerX = parent.width / 2f
            centerY = parent.height / 2f
        } else {
            // ===== 列表有数据：画在最后一个 item 下方居中 =====
            val lastView = parent.findViewHolderForAdapterPosition(itemCount - 1)?.itemView
                ?: return // 还没布局好，先不画

            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight
            val bottom = parent.height - parent.paddingBottom
            val top = bottom - footerHeight

            centerX = (left + right) / 2f
            centerY = (bottom + top) / 2f
        }

        val left = parent.getPaddingLeft().toFloat()
        val right = parent.getWidth() - parent.getPaddingRight().toFloat()
        if(isLoading){
            bgPaint.setColor(bgColor)
        }else{
            bgPaint.setColor(Color.TRANSPARENT)
        }

        canvas.drawRect(left,centerY-footerHeight/2,
            right,centerY+footerHeight/2,
            bgPaint)

        // 画旋转圆圈
        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)

        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            0f,
            sweepAngle,
            false,
            paint
        )
        canvas.restore()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {}

    fun destroy() {
        rotateAnimator.cancel()
        sweepAnimator.cancel()
    }
}