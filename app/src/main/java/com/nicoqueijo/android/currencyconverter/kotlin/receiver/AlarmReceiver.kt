package com.nicoqueijo.android.currencyconverter.kotlin.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.nicoqueijo.android.currencyconverter.kotlin.data.Repository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

//@InstallIn
//@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
//
//    @Inject
//    lateinit var defaultRepository: Repository

    private val scope = CoroutineScope(Dispatchers.IO)


    override fun onReceive(context: Context, intent: Intent) {
        // Is triggered when alarm goes off, i.e. receiving a system broadcast
//        RepositoryModule.provideRepository(context,get())
//        Toast.makeText(context, "tttt........", Toast.LENGTH_SHORT).show();

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl: PowerManager.WakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myapp:mywakelocktag")
//        wl.acquire(30 * 60 * 1000L /*30 minutes*/)
        wl.acquire(1 * 60 * 1000L /*1 minutes*/)

        Log.d("alram","Alarm onReceive start")

/*
        CoroutineScope(Dispatchers.IO).launch {
            defaultRepository.fetchCurrencies()
        }
*/
        Log.d("alram","Alarm onReceive end")

        Toast.makeText(context, "Alarm<<........", Toast.LENGTH_SHORT).show();
    }

    fun setAlarm(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_MUTABLE
            )
        } else {
//            ("VERSION.SDK_INT < S")
            PendingIntent.getBroadcast(
                context, 0, intent, 0
            )

        }

        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
//            (1000 * 60 * 30).toLong(),
            (1000 * 60 * 1).toLong(),
//            (1000).toLong(),
            pendingIntent
        )
        Log.d("alarm","\"Alarm set in \" + \"1 minute\"")

        Toast.makeText(context, "Alarm set in " + "1 minute", Toast.LENGTH_LONG).show();
    }

}
