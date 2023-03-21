package com.nicoqueijo.android.currencyconverter.kotlin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nicoqueijo.android.currencyconverter.kotlin.service.AlarmService
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils

class AutoStart : BroadcastReceiver() {
    @JvmField
    var alarm = AlarmReceiver()
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
//            alarm.setAlarm(context)
            startMyAppsService(context)
        }
    }
    private fun startMyAppsService(context:Context) {

        if (!Utils.isServiceRunning(context, AlarmService::class.java.name)) {

            val intent = Intent(context, AlarmService::class.java)
            context.startService(intent)
        }
    }

}