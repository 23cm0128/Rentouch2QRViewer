package jp.ac.jec.cm0128.rentouch2qrviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    val timer: Timer = Timer()
    val deviceName = Build.ID
    var remainBattery = 0

    var isBright = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.qr_view).setOnClickListener {
            val windowAttributes = window.attributes
            if(isBright){
                windowAttributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                windowAttributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            window.attributes = windowAttributes
            isBright = !isBright
        }


        timer.schedule(object : TimerTask() {
            override fun run() {
                try{
                    val bitmapMatrix = MultiFormatWriter().encode(
                        "$deviceName, ${Date().time}, ${remainBattery}",
                        BarcodeFormat.QR_CODE,
                        500,
                        500
                    )
                    val qrBitmap = BarcodeEncoder().createBitmap(bitmapMatrix)

                    runOnUiThread{
                        findViewById<ImageView>(R.id.qr_view).setImageBitmap(qrBitmap)
                    }
                } catch (e: Exception){
                    Snackbar
                        .make(findViewById(R.id.main), "QRコードの生成に失敗しました", Snackbar.LENGTH_SHORT)
                        .setAction("詳細"){
                            AlertDialog.Builder(this@MainActivity)
                                .setMessage(e.stackTraceToString())
                                .show()
                        }
                        .show()
                }
            }
        }, 0, 5000)

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    // バッテリーの状態を受信するBroadcastReceiver
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            remainBattery = (level / scale.toFloat() * 100).toInt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        unregisterReceiver(batteryReceiver)
    }
}