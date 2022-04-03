package com.bylazy.smstotelegram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import java.net.UnknownHostException

class ResendWorker(private val ctx: Context, workerParameters: WorkerParameters):
    CoroutineWorker(ctx, workerParameters){
    override suspend fun doWork(): Result {

        //Get data from Broadcast Receiver
        val from = inputData.getString(FROM)
        val text = inputData.getString(TEXT) ?: ""

        var data = initialState

        //Get saved data
        ctx.applicationContext.datastore.data.take(1).collect { preferences ->
            data = HomeState(active = preferences[HomeState.ACTIVE] ?: initialState.active,
                tested = preferences[HomeState.TESTED] ?: initialState.tested,
                phone = preferences[HomeState.PHONE] ?: initialState.phone,
                sendall = preferences[HomeState.SENDALL] ?: initialState.sendall,
                filter = preferences[HomeState.FILTER] ?: initialState.filter,
                channel = preferences[HomeState.TOKEN] ?: initialState.channel,
                bot = preferences[HomeState.BOT] ?: initialState.bot,
                prefix = preferences[HomeState.PREFIX] ?: initialState.prefix)
        }

        //Get list of phones
        val phones = data.phone.split(" ").filter { it.isNotBlank() }

        //Check basic bot settings
        if (!data.active || data.channel.isBlank() || data.bot.isBlank()) return Result.success()

        //Check if sender is in list (or if forward all messages)
        if (!data.sendall)
            if (!phones.contains(from) ) return Result.success()

        //Checking compliance with the text filter
        if (data.filter.isNotBlank() && !text.contains(data.filter, true)) return Result.success()

        //Add a spacer if a prefix is specified
        val prefix = if (data.prefix.isBlank()) "" else data.prefix+" "

        //Build request URL
        val requestUrl = "https://api.telegram.org/bot" +
                data.bot +
                "/sendMessage?chat_id=" +
                data.channel +
                "&text=" +
                prefix +
                text//.replace("//s", "_")

        //Get Ktor client
        val client = HttpClient {
            HttpResponseValidator {
                handleResponseException { exception ->
                    when (exception) {
                        is ClientRequestException ->
                            throw Exception("Check Telegram Bot settings")
                        is UnknownHostException ->
                            throw Exception("Check Internet connection")
                        else ->
                            throw Exception("Unknown forwarding error: "
                                +exception.message)
                    }
                }
            }
        }

        //Perform request
        return try {
            client.get<HttpResponse>(requestUrl)
            //todo - save
            Result.success()
        } catch (e: Exception) {

            //Notify when failed
            notify(e.message?:"Message forwarding error: Unknown")
            Result.failure()
        }
    }

    //Show notification when Failure
    private fun notify(message: String) {

        //Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.channel_name)
            val descriptionText = applicationContext.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ResendingResult", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        //Intent for notification tapped event
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent
            .getActivity(
                applicationContext,
                0,
                intent,
                0
            )

        //Build Notification
        val builder = NotificationCompat.Builder(applicationContext, "ResendingResult")
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Message forwarding error")
            .setContentText(message)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        //Show notification
        //todo - check permission
        with(NotificationManagerCompat.from(applicationContext)) {
            notify((0..1000).random(), builder.build())
        }
    }
}
