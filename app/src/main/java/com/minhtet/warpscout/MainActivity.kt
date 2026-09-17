package com.minhtet.warpscout

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var btnScan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tvOutput)
        btnScan = findViewById(R.id.btnScan)

        btnScan.setOnClickListener {
            btnScan.isEnabled = false
            tvOutput.text = "Extracting binary and starting scan...\n"
            
            // Background thread တွင် အလုပ်လုပ်ရန်
            CoroutineScope(Dispatchers.IO).launch {
                runWarpScout()
            }
        }
    }

    private suspend fun runWarpScout() {
        // Native Library ထဲမှာရှိတဲ့ .so ဖိုင်လမ်းကြောင်းကို ရှာခြင်း
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val executableFile = File(nativeLibDir, "libwarpscout.so")

        try {
            if (!executableFile.exists()) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("\nError: libwarpscout.so not found!")
                    btnScan.isEnabled = true
                }
                return
            }

            // Android OS ကိုယ်တိုင်က Execute permission ပေးထားပြီးသားဖြစ်လို့ 
            // setExecutable ထပ်လုပ်စရာ မလိုတော့ဘဲ တိုက်ရိုက် Run နိုင်ပါပြီ
            val process = ProcessBuilder(
                executableFile.absolutePath, "scan", "-p", "awg", "-P"
            ).redirectErrorStream(true).start()

            val reader = process.inputStream.bufferedReader()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("$line\n")
                }
            }

            process.waitFor()
            
            withContext(Dispatchers.Main) {
                tvOutput.append("\n--- Scan Completed ---")
                btnScan.isEnabled = true
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                tvOutput.append("\nError: ${e.message}")
                btnScan.isEnabled = true
            }
        }
    }
}
