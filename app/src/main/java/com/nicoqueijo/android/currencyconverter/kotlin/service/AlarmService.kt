package com.nicoqueijo.android.currencyconverter.kotlin.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nicoqueijo.android.currencyconverter.kotlin.receiver.AlarmReceiver

class AlarmService : Service() {
    var alarm = AlarmReceiver()
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        alarm.setAlarm(this)
        return START_STICKY
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent, startId: Int) {
        alarm.setAlarm(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}