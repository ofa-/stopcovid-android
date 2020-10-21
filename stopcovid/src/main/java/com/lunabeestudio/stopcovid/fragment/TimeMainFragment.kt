package com.lunabeestudio.stopcovid.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

abstract class TimeMainFragment : MainFragment() {

    private val timeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            timeRefresh()
        }
    }

    abstract fun timeRefresh()

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(timeUpdateReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(timeUpdateReceiver)
    }
}