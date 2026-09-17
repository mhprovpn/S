package com.minhtet.warpscout

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var btnScan: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tvOutput)
        btnScan = findViewById(R.id.btnScan)
        btnRegister = findViewById(R.id.btnRegister)

        // Register ခလုတ် နှိပ်သောအခါ
        btnRegister.setOnClickListener {
            disableButtons()
            tvOutput.text = "Registering Account...\n"
            CoroutineScope(Dispatchers.IO).launch {
                executeCommand("register")
            }
        }

        // Scan ခလုတ် နှိပ်သောအခါ
        btnScan.setOnClickListener {
            disableButtons()
            tvOutput.text = "Starting scan...\n"
            CoroutineScope(Dispatchers.IO).launch {
                executeCommand("scan", "-p", "awg", "-P")
            }
        }
    }

    private fun disableButtons() {
        btnScan.isEnabled = false
        btnRegister.isEnabled = false
    }

    private fun enableButtons() {
        btnScan.isEnabled = true
        btnRegister.isEnabled = true
    }

    private suspend fun executeCommand(vararg args: String) {
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val executableFile = File(nativeLibDir, "libwarpscout.so")

        try {
            if (!executableFile.exists()) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("\nError: libwarpscout.so not found in Native Library Directory!")
                    enableButtons()
                }
                return
            }

            // Command စာရင်း တည်ဆောက်ခြင်း
            val commandList = mutableListOf(executableFile.absolutePath)
            commandList.addAll(args)

            // ProcessBuilder ဖြင့် Command Run ခြင်း
            val processBuilder = ProcessBuilder(commandList)
            processBuilder.directory(filesDir) // အရေးကြီး: JSON ဖိုင်ကို App ၏ Storage တွင် သိမ်းရန်
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()

            val reader = process.inputStream.bufferedReader()
            var line: String?

            // Output ကို ဖတ်ပြီး UI တွင် ပြသခြင်း
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    tvOutput.append("$line\n")
                }
            }

            process.waitFor()
            
            withContext(Dispatchers.Main) {
                tvOutput.append("\n--- Completed ---")
                enableButtons()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                tvOutput.append("\nError: ${e.message}")
                enableButtons()
            }
        }
    }
}
