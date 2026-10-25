package sa.kono.app.ui.customer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/** A round, coloured map pin with a white glyph (store / home / courier) for osmdroid markers. */
fun mapPin(ctx: Context, colorInt: Int, @DrawableRes glyph: Int, sizeDp: Int = 40): Drawable {
    val d = ctx.resources.displayMetrics.density
    val px = (sizeDp * d).toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
    c.drawCircle(px / 2f, px / 2f + 2 * d, px / 2f - 3 * d, shadow)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt }
    c.drawCircle(px / 2f, px / 2f, px / 2f - 3 * d, fill)
    ContextCompat.getDrawable(ctx, glyph)?.let {
        val pad = (px * 0.28f).toInt()
        it.setBounds(pad, pad, px - pad, px - pad)
        it.setTint(0xFFFFFFFF.toInt())
        it.draw(c)
    }
    return BitmapDrawable(ctx.resources, bmp)
}
