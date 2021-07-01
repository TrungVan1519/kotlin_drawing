package com.example.drawing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    companion object {
        const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
    }

    @SuppressLint("StaticFieldLeak")
    inner class BitMapAsyncTask(private val bitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var processDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result: String
            try {
                val bytes = ByteArrayOutputStream()
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .toString() + File.separator + "Camera"

                if (!File(imagesDir).exists()) {
                    File(imagesDir).mkdir()
                }

                val f = File(imagesDir, "DrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
                val fos = FileOutputStream(f)
                fos.write(bytes.toByteArray())
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()

                result = f.absolutePath
            } catch (ex: java.lang.Exception) {
                result = ""
                ex.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            cancelProgressDialog()

            if (result!!.isNotEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully :$result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) { path, uri ->
                val sharedIntent = Intent(Intent.ACTION_MEDIA_SHARED)
                sharedIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharedIntent.type = "image/png"
                startActivity(Intent.createChooser(sharedIntent, "Share"))
            }
        }

        private fun showProgressDialog() {
            processDialog = Dialog(this@MainActivity)
            processDialog.setContentView(R.layout.dialog_custom_progress)
            processDialog.show()
        }

        private fun cancelProgressDialog() {
            processDialog.dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: handle e
        // change size of brush
        vDrawing.setBrushSize(20f)
        btnBrush.setOnClickListener {
            showBrushSizeDialog()
        }

        // change color
        var currentBtn = vColors[1] as ImageButton
        currentBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        val e = View.OnClickListener { v ->
            if (v.id != currentBtn.id) {
                v as ImageButton

                // update color
                val colorTag = v.tag.toString()
                vDrawing.setColor(colorTag)

                // update ui
                v.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
                currentBtn.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.pallet_normal
                    )
                )
                currentBtn = v
            }
        }

        btnSkin.setOnClickListener(e)
        btnBlack.setOnClickListener(e)
        btnRed.setOnClickListener(e)
        btnGreen.setOnClickListener(e)
        btnBlue.setOnClickListener(e)
        btnYellow.setOnClickListener(e)
        btnLollipop.setOnClickListener(e)
        btnViolet.setOnClickListener(e)

        // open external storage
        btnGallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST)
            } else {
                requestStoragePermission()
            }
        }

        // undo
        btnUndo.setOnClickListener {
            vDrawing.onClickUndo()
        }

        // save
        btnSave.setOnClickListener {
            BitMapAsyncTask(getBitmapFromView(vContainer)).execute()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted now you can read the storage files",
                    Toast.LENGTH_LONG
                ).show()

                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST)
            } else {
                Toast.makeText(
                    this,
                    "Oops you denied the permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
                try {
                    if (data!!.data != null) {
                        imgBackground.visibility = View.VISIBLE
                        imgBackground.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this,
                            "Error in parsing the image or its corrupted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        brushDialog.show()

        val e = View.OnClickListener { v ->
            when (v.id) {
                brushDialog.btnSmallBrush.id -> vDrawing.setBrushSize(10f)
                brushDialog.btnMediumBrush.id -> vDrawing.setBrushSize(20f)
                brushDialog.btnLargeBrush.id -> vDrawing.setBrushSize(30f)
            }

            brushDialog.dismiss()
        }

        brushDialog.btnSmallBrush.setOnClickListener(e)
        brushDialog.btnMediumBrush.setOnClickListener(e)
        brushDialog.btnLargeBrush.setOnClickListener(e)
    }

    private fun getBitmapFromView(v: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = v.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        v.draw(canvas)
        return returnedBitmap
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).toString()
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to add a background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
        )
    }
}
