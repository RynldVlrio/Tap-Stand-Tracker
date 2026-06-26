package com.taptrack.app.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.taptrack.app.R

fun createLocationDotBitmap(context: Context): Bitmap {
    val dp = context.resources.displayMetrics.density
    val size = (22 * dp).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // Subtle drop shadow
    canvas.drawCircle(cx + dp, cy + dp, cx - dp, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 0, 0)
        style = Paint.Style.FILL
    })
    // White border ring
    canvas.drawCircle(cx, cy, cx - dp * 0.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    })
    // Blue fill
    canvas.drawCircle(cx, cy, cx - dp * 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A73E8")
        style = Paint.Style.FILL
    })
    return bitmap
}

/** Orange star-pin used for user-defined landmarks on the map. */
fun createLandmarkMarkerBitmap(context: Context, color: Int = Color.parseColor("#FF9800")): BitmapDrawable {
    val dp = context.resources.displayMetrics.density
    val size = (48 * dp).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = size / 2f
    val badgeR = size * 0.30f
    val badgeCy = badgeR + size * 0.04f
    val neckY = badgeCy + badgeR * 0.60f
    val tipY = size * 0.97f

    val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(55, 0, 0, 0); style = Paint.Style.FILL }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE; style = Paint.Style.FILL }

    // drop shadow
    canvas.drawCircle(cx + dp, badgeCy + dp, badgeR, shadowPaint)
    canvas.drawPath(Path().apply {
        moveTo(cx - badgeR * 0.42f + dp, neckY + dp)
        lineTo(cx + badgeR * 0.42f + dp, neckY + dp)
        lineTo(cx + dp, tipY + dp)
        close()
    }, shadowPaint)

    // badge + pointer
    canvas.drawCircle(cx, badgeCy, badgeR, colorPaint)
    canvas.drawPath(Path().apply {
        moveTo(cx - badgeR * 0.42f, neckY)
        lineTo(cx + badgeR * 0.42f, neckY)
        lineTo(cx, tipY)
        close()
    }, colorPaint)

    // white border ring
    canvas.drawCircle(cx, badgeCy, badgeR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp * 1.6f
    })

    // 5-pointed star inside badge
    val starOuter = badgeR * 0.52f
    val starInner = starOuter * 0.42f
    val starPath = Path()
    for (i in 0 until 5) {
        val outerAngle = (-Math.PI / 2 + 2 * Math.PI * i / 5).toFloat()
        val innerAngle = (outerAngle + Math.PI / 5).toFloat()
        val ox = cx + starOuter * kotlin.math.cos(outerAngle)
        val oy = badgeCy + starOuter * kotlin.math.sin(outerAngle)
        val ix = cx + starInner * kotlin.math.cos(innerAngle)
        val iy = badgeCy + starInner * kotlin.math.sin(innerAngle)
        if (i == 0) starPath.moveTo(ox, oy) else starPath.lineTo(ox, oy)
        starPath.lineTo(ix, iy)
    }
    starPath.close()
    canvas.drawPath(starPath, whitePaint)

    return BitmapDrawable(context.resources, bmp)
}

/** Small text-label bitmap for showing a boundary layer name on the map. */
fun createBoundaryLabelBitmap(context: Context, text: String, borderColor: Int): BitmapDrawable {
    val dp = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * dp
        color = borderColor or 0xFF000000.toInt()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(2f * dp, 0f, 1f * dp, Color.argb(100, 0, 0, 0))
    }
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val bounds = android.graphics.Rect()
    textPaint.getTextBounds(text, 0, text.length, bounds)
    val pad = 4f * dp
    val w = bounds.width() + pad * 2
    val h = bounds.height() + pad * 2
    val bmp = Bitmap.createBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawRoundRect(android.graphics.RectF(0f, 0f, w, h), pad, pad, bgPaint)
    canvas.drawText(text, pad, h - pad, textPaint)
    return BitmapDrawable(context.resources, bmp)
}

fun createTapMarkerBitmap(context: Context, color: Int = Color.parseColor("#0277BD")): BitmapDrawable {
    val dp = context.resources.displayMetrics.density
    val size = (56 * dp).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = size / 2f
    val badgeR = size * 0.295f
    val badgeCy = badgeR + size * 0.035f
    val neckY = badgeCy + badgeR * 0.58f
    val tipY = size * 0.97f

    val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(55, 0, 0, 0); style = Paint.Style.FILL }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE; style = Paint.Style.FILL }

    // drop shadow
    val sdx = dp * 1.5f; val sdy = dp * 1.5f
    canvas.drawCircle(cx + sdx, badgeCy + sdy, badgeR, shadowPaint)
    canvas.drawPath(Path().apply {
        moveTo(cx - badgeR * 0.42f + sdx, neckY + sdy)
        lineTo(cx + badgeR * 0.42f + sdx, neckY + sdy)
        lineTo(cx + sdx, tipY + sdy)
        close()
    }, shadowPaint)

    // badge circle + pointer
    canvas.drawCircle(cx, badgeCy, badgeR, colorPaint)
    canvas.drawPath(Path().apply {
        moveTo(cx - badgeR * 0.42f, neckY)
        lineTo(cx + badgeR * 0.42f, neckY)
        lineTo(cx, tipY)
        close()
    }, colorPaint)

    // white border ring
    canvas.drawCircle(cx, badgeCy, badgeR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp * 1.8f
    })

    // ── faucet icon (white) centred in badge ──
    val lw = badgeR * 0.195f          // pipe thickness
    val rr = lw / 2f                  // rounded-rect corner radius
    val iTop   = badgeCy - badgeR * 0.55f
    val iBot   = badgeCy + badgeR * 0.35f
    val iLeft  = cx - badgeR * 0.55f
    val iRight = cx + badgeR * 0.55f
    val iW     = iRight - iLeft

    // ① horizontal inlet pipe (top, 70 % of icon width)
    canvas.drawRoundRect(iLeft, iTop, iLeft + iW * 0.70f, iTop + lw, rr, rr, whitePaint)
    // ② left end cap
    canvas.drawRoundRect(iLeft, iTop, iLeft + lw * 1.2f, iTop + lw * 2.2f, rr, rr, whitePaint)
    // ③ vertical body (drops from 30 % of icon width)
    val bodyX = iLeft + iW * 0.30f
    canvas.drawRoundRect(bodyX - lw / 2f, iTop + lw * 0.5f, bodyX + lw / 2f, iBot - lw, rr, rr, whitePaint)
    // ④ horizontal elbow going right
    val elbowY = iBot - lw * 1.3f
    canvas.drawRoundRect(bodyX - lw / 2f, elbowY - lw / 2f, iLeft + iW * 0.85f, elbowY + lw / 2f, rr, rr, whitePaint)
    // ⑤ vertical nozzle at right end of elbow
    val nozzleL = iLeft + iW * 0.85f - lw
    canvas.drawRoundRect(nozzleL, elbowY + lw / 2f, nozzleL + lw, iBot + lw * 0.6f, rr, rr, whitePaint)
    // ⑥ water drop below nozzle (teardrop: circle + upward-pointing triangle)
    val drCx   = nozzleL + lw / 2f
    val drTopY = iBot + lw * 0.6f + dp * 0.5f
    val drR    = lw * 0.80f
    canvas.drawCircle(drCx, drTopY + drR, drR, whitePaint)
    canvas.drawPath(Path().apply {
        moveTo(drCx, drTopY)
        lineTo(drCx - drR * 0.65f, drTopY + drR)
        lineTo(drCx + drR * 0.65f, drTopY + drR)
        close()
    }, whitePaint)

    return BitmapDrawable(context.resources, bmp)
}
