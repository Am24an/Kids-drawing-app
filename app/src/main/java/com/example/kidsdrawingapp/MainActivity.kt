package com.example.kidsdrawingapp

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null


    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->

        uri?.let {
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )


        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)

        ibSave.setOnClickListener {
            showProgressDialog()

            lifecycleScope.launch {
                val drawing = findViewById<FrameLayout>(R.id.fl_drawing_view_container)

                saveBitmapFile(getBitmapFromView(drawing))
            }
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {

            pickImageLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    }

    private fun showBrushSizeChooserDialog() {

        val brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.dialog_brush_size)

        brushDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        brushDialog.findViewById<View>(R.id.ib_small_brush).setOnClickListener {
            drawingView?.setSizeForBrush(10f)
            brushDialog.dismiss()
        }

        brushDialog.findViewById<View>(R.id.ib_medium_brush).setOnClickListener {
            drawingView?.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        brushDialog.findViewById<View>(R.id.ib_large_brush).setOnClickListener {
            drawingView?.setSizeForBrush(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = createBitmap(view.width, view.height)

        val canvas = Canvas(returnedBitmap)
        view.background?.draw(canvas)
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?) {

        withContext(Dispatchers.IO) {
            if (bitmap == null) return@withContext
            try {
                val filename = "KidsDrawing_${System.currentTimeMillis()}.png"
                val resolver = contentResolver
                val values = ContentValues().apply {

                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)

                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")

                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/KidsDrawingApp"
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri: Uri? = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )

                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            throw IOException("Couldn't save the bitmap")
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(it, values, null, null)
                    }

                    runOnUiThread {

                        cancelProgressDialog()
                        Toast.makeText(
                            this@MainActivity, "Image saved successfully", Toast.LENGTH_SHORT
                        ).show()

                        shareImage(it)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    cancelProgressDialog()
                    Toast.makeText(
                        this@MainActivity, "Saving failed", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Method is used to show Custom Progress Dialog.
     */

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /* Set the screen content from a layout resource.
        The resource will be inflated, adding all the top-level views to the screen.*/

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)


        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }

    }

    private fun shareImage(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/png"
        intent.putExtra(
            Intent.EXTRA_STREAM, uri
        )

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        startActivity(Intent.createChooser(intent, "Share Image"))
    }
}




