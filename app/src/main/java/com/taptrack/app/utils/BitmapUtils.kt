package com.taptrack.app.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.taptrack.app.R

fun createTapMarkerBitmap(context: Context, color: Int = Color.parseColor("#0277BD")): BitmapDrawable {
    val size = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.argb(60, 0, 0, 0)
        style = Paint.Style.FILL
    }

    val cx = size / 2f
    val cy = size * 0.42f
    val r = size * 0.32f

    // shadow
    canvas.drawCircle(cx + size * 0.03f, cy + size * 0.03f, r, shadowPaint)
    // circle body
    canvas.drawCircle(cx, cy, r, paint)

    // white inner circle
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r * 0.45f, innerPaint)

    // pointer triangle
    val path = Path().apply {
        moveTo(cx - r * 0.55f, cy + r * 0.6f)
        lineTo(cx + r * 0.55f, cy + r * 0.6f)
        lineTo(cx, size * 0.95f)
        close()
    }
    canvas.drawPath(path, paint)

    return BitmapDrawable(context.resources, bitmap)
}
