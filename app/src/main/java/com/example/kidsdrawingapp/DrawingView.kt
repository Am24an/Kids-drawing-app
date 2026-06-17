package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap

/**
 * This class contains the attributes for the main layout of our application.
 * */
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    //A variable of customPath inner class to use it further.
    private var mDrawPath: CustomPath? = null

    //An instance of the Bitmap
    private var mCanvasBitmap: Bitmap? = null

    //The paint class holds the style and color information about how to draw geometries, text and bitmaps.
    private var mDrawPaint: Paint? = null

    // Instance of canvas paint view.
    private var mCanvasPaint: Paint? = null

    // A variable for stroke/brush size to draw on the canvas.
    private var mBrushSize: Float = 0.toFloat()

    // A variable to hold a color of the stroke.
    private var color = Color.BLACK

    /**
     * A variable for canvas which wil be initialized later and used.
     * The canvas class holds the "draw" calls. to draw something, you need 4 basic component
     * the draw calls (writing into the bitmap), a drawing primitive(e.g. rect,
     * Path, text, Bitmap), and a paint(to describe the colors and styles for the drawing)
     * */
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()  //ArrayList for Paths
    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    fun onClickUndo() {
        if (mPaths.isNotEmpty()) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    /**
     * This method initializes the attributes of the ViewForDrawing class.
     * */
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE //this is to draw a stroke style
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND // this is for stroke join
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND // this is for stroke cap
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        // mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = createBitmap(w, h)
        canvas = Canvas(mCanvasBitmap!!)
    }


    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     * */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        for (path in mPaths) {
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        //we create path here
        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)

            }

            else -> return false
        }
        invalidate()
        return true
    }


    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    // An inner class for custom path with two parameter as color and stroke size.
    internal inner class CustomPath
        (var color: Int, var brushThickness: Float) : Path()

}