package com.example.vsuriyal.nmapnetwork

import android.content.Context
import android.os.AsyncTask
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

class Utils{
    companion object {

    fun search(cmdline:String): String {

        val wholeoutput = StringBuilder("")
        val commands = arrayOf(cmdline)

        val outputStream: DataOutputStream
        val inputStream: BufferedReader

        try {
            val processBuilder = ProcessBuilder("sh")
            processBuilder.redirectErrorStream(true)
            val scanProcess = processBuilder.start()

            outputStream = DataOutputStream(scanProcess.outputStream)
            inputStream = BufferedReader(InputStreamReader(scanProcess.inputStream))

            for (single in commands) {
                outputStream.writeBytes(single + "\n")
                outputStream.flush()
            }
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            var pstdout: String? = inputStream.readLine()
            while (pstdout != null) {
                if (pstdout != null) {
                    pstdout += "\n"
                    wholeoutput.append(pstdout)
                }
                pstdout = inputStream.readLine()
            }

        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return wholeoutput.toString()
    }
    }
}