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
        val binaryName = "warpscout"
        val executableFile = File(filesDir, binaryName)

        try {
            // ၁။ Assets ထဲမှ Binary ကို Internal Storage သို့ ကူးယူခြင်း
            if (!executableFile.exists()) {
                assets.open(binaryName).use { input ->
                    FileOutputStream(executableFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // ၂။ Execute permission ပေးခြင်း
            executableFile.setExecutable(true)

            // ၃။ AmneziaWG အတွက် Command Run ခြင်း (.\warpscout scan -p awg -P)
            val process = ProcessBuilder(
                executableFile.absolutePath, "scan", "-p", "awg", "-P"
            ).redirectErrorStream(true).start()

            val reader = process.inputStream.bufferedReader()
            var line: String?

            // ၄။ ထွက်လာသော Output များကို ဖတ်ပြီး UI တွင် အချိန်နှင့်တပြေးညီ ဖော်ပြခြင်း
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
