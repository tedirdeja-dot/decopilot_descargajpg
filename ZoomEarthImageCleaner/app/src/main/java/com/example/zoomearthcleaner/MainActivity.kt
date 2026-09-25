package com.example.zoomearthcleaner

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var loadButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEditText = findViewById(R.id.urlEditText)
        loadButton = findViewById(R.id.loadButton)
        saveButton = findViewById(R.id.saveButton)
        webView = findViewById(R.id.webView)

        setupWebView()

        // Cargar URL inicial
        val defaultUrl = getString(R.string.default_url)
        webView.loadUrl(defaultUrl)

        loadButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            if (url.isNotEmpty()) {
                webView.loadUrl(url)
            } else {
                Toast.makeText(this, "Introduce una URL válida", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            captureAndSaveWebView()
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Evitar abrir navegador externo
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Aquí ya tenemos la previsualización lista en el WebView
            }
        }
    }

    private fun captureAndSaveWebView() {
        // Crear bitmap del contenido del WebView (imagen limpia sin HTML como archivo)
        val bitmap = createBitmapFromWebView(webView)
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo capturar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = saveBitmapToGallery(bitmap)
            if (uri != null) {
                Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Excepción al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBitmapFromWebView(webView: WebView): Bitmap? {
        // Medir y dibujar el WebView en un bitmap
        val width = webView.width
        val height = webView.height
        if (width == 0 || height == 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        webView.draw(canvas)
        return bitmap
    }

    @Throws(IOException::class)
    private fun saveBitmapToGallery(bitmap: Bitmap): android.net.Uri? {
        val filename = "zoom_earth_clean_${System.currentTimeMillis()}.png"

        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(itemUri).use { outStream ->
            if (outStream == null) return null
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)
        }

        return itemUri
    }
}