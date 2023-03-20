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
import android.widget.Toast
import androidx.hilt.lifecycle.ViewModelInject
import androidx.room.Insert
import com.nicoqueijo.android.currencyconverter.kotlin.data.DefaultRepository
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.SplashViewModel
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

//@InstallIn
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

//    @Inject
    lateinit var defaultRepository: DefaultRepository
//    lateinit var defaultRepository: SplashViewModel
    private val scope = CoroutineScope(Dispatchers.IO)


    override fun onReceive(context: Context, intent: Intent) {
        // Is triggered when alarm goes off, i.e. receiving a system broadcast
//        RepositoryModule.provideRepository(context,get())
        if (intent.action == "FOO_ACTION") {
            val fooString = intent.getStringExtra("KEY_FOO_STRING")
            Toast.makeText(context, fooString, Toast.LENGTH_LONG).show()

            // do api call here after 30 minutes

        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl: PowerManager.WakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myapp:mywakelocktag")
//        wl.acquire(30 * 60 * 1000L /*30 minutes*/)
        wl.acquire(1*60*1000L /*1 minutes*/)

        CoroutineScope(Dispatchers.IO).launch {
            defaultRepository.fetchCurrencies()
        }

        Toast.makeText(context, "Alarm................................", Toast.LENGTH_LONG).show();
    }

    fun setAlarm(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context, 0, intent, FLAG_MUTABLE
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
            pendingIntent
        )
        Toast.makeText(context, "Alarm set in " + "30 minute", Toast.LENGTH_LONG).show();

    }

}
