package com.example.vsuriyal.nmapnetwork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.vsuriyal.nmapnetwork.Utils.Companion.search
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.InetAddress
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.content.ContextCompat.getSystemService
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    val dest by lazy { filesDir.parent + "/bin/" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        BackGroundTask(edit,this,progress,dest).execute()

    }

    override fun onResume() {
        super.onResume()

        btn.setOnClickListener{
            btn.isEnabled = false
            SearchTask(btn,text,edit,progress,dest).execute()
        }

    }


    class SearchTask(val btn: Button, val text:TextView, val edit: EditText, val progress:ProgressBar, val dest:String) :AsyncTask<Unit,Unit,String>(){
        override fun doInBackground(vararg params: Unit?): String {
            val nmapbin = dest+"nmap-7.31/bin/nmap"
            val cmdline = "$nmapbin -sn -n ${edit.text}"
            return search(cmdline)
        }

        override fun onPreExecute() {
            progress.visibility = View.VISIBLE
        }

        override fun onPostExecute(result: String?) {
            text.text = result
            progress.visibility = View.GONE
            btn.isEnabled = true
        }
    }

    class BackGroundTask(val edit: EditText,val context: Context,val progress:ProgressBar,val dest:String): AsyncTask<Unit,Unit,String>() {
        var isWifiPresent:Boolean by Delegates.notNull()
        override fun onPreExecute() {
            progress.visibility = View.VISIBLE
            isWifiPresent = isWifiEnabled()
        }
        override fun doInBackground(vararg params: Unit?):String {
            val arch = System.getProperty("os.arch")
            val value:Int = if(arch.contains("86"))R.raw.nmapbintarx86 else R.raw.nmapbintarx64

            val raw = context.resources.openRawResource(value)
            unzip(dest, raw)
            installPlugin(dest)
            return getIP()

        }

        override fun onPostExecute(result: String?) {
            val add = result!!.split(".")
            edit.text = if (isWifiPresent) SpannableStringBuilder("${add[0]}.${add[1]}.${add[2]}.0/24") else SpannableStringBuilder("Wifi is Disabled")
            progress.visibility = View.GONE
        }

        fun getIP():String{
            if(isWifiPresent) {
                val string = search("ip addr")
                val sub = string.split(" wlan0")[1].trim().split(" inet ")[1].split(" brd ")[0].trim().split(" ")
                return sub[sub.size - 1]
            }
            return "Wifi is Disabled"
            //return InetAddress.getLocalHost().getHostAddress()
        }

        fun isWifiEnabled():Boolean{
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            if (activeNetwork != null) {
                // connected to the internet
                if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                    return true
                }
            }
            return false
        }

        fun unzip(dest: String, raw: InputStream) {
            try {
                val zin = ZipInputStream(raw)
                var ze: ZipEntry= zin.nextEntry
                while (ze != null) {
                    Log.v("NetworkMapper", "Unzipping " + ze.name)

                    if (ze.isDirectory) {
                        makedir(dest + ze.name)
                    } else {

                        val buffer = ByteArray(2048)

                        val outStream = FileOutputStream(dest + ze.name)
                        val bufferOut = BufferedOutputStream(outStream, buffer.size)
                        var size: Int = zin.read(buffer, 0, buffer.size)
                        while (size  != -1) {
                            bufferOut.write(buffer, 0, size)
                            size = zin.read(buffer, 0, buffer.size)
                        }

                        bufferOut.flush()
                        bufferOut.close()
                    }
                    ze= zin.nextEntry
                }
                zin.close()
            } catch (e: Exception) {
            }

        }

        fun makedir(dir: String) {
            val myDir = File(dir)

            if (!myDir.isDirectory) {
                myDir.mkdirs()
            }
        }

        fun installPlugin(str:String) {
            val bindir = str+"nmap-7.31/bin/"
            val commands = arrayOf("ncat", "ndiff", "nmap", "nping")
            try {
                for (singlecommand in commands) {
                    Runtime.getRuntime().exec("/system/bin/chmod 755 $bindir$singlecommand")
                }
            } catch (e: IOException) {
            }

        }

    }

}
