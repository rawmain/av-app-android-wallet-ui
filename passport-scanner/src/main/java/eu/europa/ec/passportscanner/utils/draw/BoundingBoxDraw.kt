package eu.europa.ec.passportscanner.utils.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt

class BoundingBoxDraw(context: Context, var rect: Rect): View(context) {

    private lateinit var boundaryPaint: Paint

    @ColorInt
    val boundingBoxColor = 0x6FFF0000

    init {
        init()
    }

    private fun init() {
        boundaryPaint = Paint()
        boundaryPaint.color = boundingBoxColor
        boundaryPaint.strokeWidth = 5f
        boundaryPaint.style = Paint.Style.STROKE
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(),  rect.right.toFloat(), rect.bottom.toFloat(), boundaryPaint)
    }
}
