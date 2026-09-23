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
    private lateinit var btnScan2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tvOutput)
        btnScan = findViewById(R.id.btnScan)
        btnScan2 = findViewById(R.id.btnScan2)

        // ပထမ Scan ခလုတ် (AWG)
        btnScan.setOnClickListener {
            disableButtons()
            tvOutput.text = "Starting Scan 1 (AWG)...\n"
            
            CoroutineScope(Dispatchers.IO).launch {
                copyAccountFileIfNeeded()
                executeCommand("scan", "-p", "awg", "-P")
            }
        }

        // ဒုတိယ Scan ခလုတ် (QUIC)
        btnScan2.setOnClickListener {
            disableButtons()
            tvOutput.text = "Starting Scan 2 (QUIC)...\n"
            
            CoroutineScope(Dispatchers.IO).launch {
                copyAccountFileIfNeeded()
                executeCommand("scan", "-p", "awg", "-P", "-gen-i1", "quic")
            }
        }
    }

    private fun disableButtons() {
        btnScan.isEnabled = false
        btnScan2.isEnabled = false
    }

    private fun enableButtons() {
        btnScan.isEnabled = true
        btnScan2.isEnabled = true
    }

    private suspend fun copyAccountFileIfNeeded() {
        val accountFile = File(filesDir, "warpscout-account.json")
        if (!accountFile.exists()) {
            try {
                assets.open("warpscout-account.json").use { input ->
                    FileOutputStream(accountFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    tvOutput.append("Account file loaded.\n")
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
                    enableButtons()
                }
                return
            }

            val commandList = mutableListOf(executableFile.absolutePath)
            commandList.addAll(args)

            val processBuilder = ProcessBuilder(commandList)
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
