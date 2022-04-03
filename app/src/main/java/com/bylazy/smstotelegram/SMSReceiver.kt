package com.bylazy.smstotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val sms = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val from = sms.first().originatingAddress?:"missing"
        var text = ""

        for (message in sms) text += message.displayMessageBody

        val workerInputData = Data.Builder()
        workerInputData.putString(FROM, from)
        workerInputData.putString(TEXT, text)

        val worker = OneTimeWorkRequestBuilder<ResendWorker>()
            .setInputData(workerInputData.build())
            .build()

        WorkManager
            .getInstance(context)
            .enqueue(worker)
    }
}