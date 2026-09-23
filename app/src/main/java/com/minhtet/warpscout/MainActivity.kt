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
            tvOutput.text = "Preparing account and starting scan...\n"
            
            CoroutineScope(Dispatchers.IO).launch {
                // ၁။ Scan မစခင် JSON ဖိုင်ကို အရင် ကူးယူပါမယ်
                copyAccountFileIfNeeded()
                
                // ၂။ Scan Command ကို တန်းပြီး Run ပါမယ်
                executeCommand("scan", "-p", "awg", "-P")
            }
        }
    }

    // Assets ထဲက JSON ဖိုင်ကို filesDir သို့ ကူးယူမည့် Function
    private suspend fun copyAccountFileIfNeeded() {
        val accountFile = File(filesDir, "warpscout-account.json")
        
        // ဖိုင် မရှိသေးမှသာ ကူးယူပါမည်
        if (!accountFile.exists()) {
            try {
                assets.open("warpscout-account.json").use { input ->
                    FileOutputStream(accountFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    tvOutput.append("Account file loaded successfully.\n")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("Error copying account file: ${e.message}\n")
                }
            }
        }
    }

    private suspend fun executeCommand(vararg args: String) {
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

            val commandList = mutableListOf(executableFile.absolutePath)
            commandList.addAll(args)

            val processBuilder = ProcessBuilder(commandList)
            // JSON ဖိုင်ရှိရာ filesDir ကို Working Directory အဖြစ် သတ်မှတ်ခြင်း
            processBuilder.directory(filesDir)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val reader = process.inputStream.bufferedReader()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("$line\n")
                }
            }

            process.waitFor()
            
            withContext(Dispatchers.Main) {
                tvOutput.append("\n--- Completed ---")
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
