package com.bylazy.smstotelegram

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.UnknownHostException


class MainViewModel(private val app: Application): AndroidViewModel(app) {

    val conversations = MutableStateFlow<List<Conversation>>(listOf())
    val conversationsLoading = MutableStateFlow(true)

    val messageToShow = mutableStateOf("")

    val shouldShowWarning = app.datastore.data.map {
        it[showInitialWarningKey]?:true
    }

    fun warningShown() {
        viewModelScope.launch {
            app.datastore.edit {
                it[showInitialWarningKey] = false
            }
        }
    }

    var selectedFrom = mutableStateOf(Conversation("", ""))

    fun selectFromConvList() {
        if (selectedFrom.value.from.isNotBlank()) {
            var old = currentState.value.phone
            if (old.isNotBlank() && old.last() != ' ') {old += " "}
            currentState.value = currentState.value
                .copy(phone = "$old${selectedFrom.value.from} ")
            viewModelScope.launch {
                app.datastore.edit {
                    it[HomeState.PHONE] = currentState.value.phone
                }
            }
        }
    }

    fun selectConvItem(conversation: Conversation) {
        selectedFrom.value = conversation
    }

    fun getConversations(){
        selectedFrom.value = Conversation("", "")
        conversationsLoading.tryEmit(true)
        val data : MutableList<Conversation> = mutableListOf()
        val pr = arrayOf(
            Telephony.Sms.Conversations.SNIPPET,
            Telephony.Sms.Conversations.ADDRESS + " as " + Telephony.Sms.Conversations.ADDRESS)
        val cursor = app.contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            pr,
            null,
            null,
            Telephony.Sms.Conversations.DEFAULT_SORT_ORDER
        )
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) data.add(Conversation(
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ADDRESS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET))
                ))
            }
        } catch (e: IllegalArgumentException) {
            messageToShow.value = "Can\'t get list of recent conversations!"
        } finally {
            cursor?.close()
        }
        conversations.tryEmit(data)
        conversationsLoading.tryEmit(false)
    }

    val currentState = mutableStateOf(initialState)

    private val homeState = app.datastore.data
        .map { preferences ->
        HomeState(active = preferences[HomeState.ACTIVE] ?: initialState.active,
            tested = preferences[HomeState.TESTED] ?: initialState.tested,
            phone = preferences[HomeState.PHONE] ?: initialState.phone,
            sendall = preferences[HomeState.SENDALL] ?: initialState.sendall,
            filter = preferences[HomeState.FILTER] ?: initialState.filter,
            channel = preferences[HomeState.TOKEN] ?: initialState.channel,
            bot = preferences[HomeState.BOT] ?: initialState.bot,
            prefix = preferences[HomeState.PREFIX] ?: initialState.prefix)
    }

    fun updateCurrentState(homeState: HomeState) {
        viewModelScope.launch {
            val testValid = if (currentState.value.bot != homeState.bot
                || currentState.value.channel != homeState.channel) false
            else currentState.value.tested
            currentState.value = homeState
            app.datastore.edit { preferences ->
                preferences[HomeState.ACTIVE] = homeState.active
                preferences[HomeState.TESTED] = testValid
                preferences[HomeState.PHONE] = homeState.phone
                preferences[HomeState.SENDALL] = homeState.sendall
                preferences[HomeState.FILTER] = homeState.filter
                preferences[HomeState.TOKEN] = homeState.channel
                preferences[HomeState.BOT] = homeState.bot
                preferences[HomeState.PREFIX] = homeState.prefix
            }
        }
    }

    fun startStopForwarding(start: Boolean) {
        val state = when (start) {
            true -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            false -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            val receiver = ComponentName(app.applicationContext, SMSReceiver::class.java)
            app.packageManager.setComponentEnabledSetting(receiver,
                state, PackageManager.DONT_KILL_APP)
            viewModelScope.launch {
                app.datastore.edit { preferences ->
                    preferences[HomeState.ACTIVE] = start
                }
            }
        }
        catch (e: Exception) {
            //e.message?.let { Log.d("State change error", it) }
            messageToShow.value = "Can\'t start or stop SMS receiver"
        }
    }

    fun testBot() {
        viewModelScope.launch {
            val client = HttpClient(CIO) {
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
            val url = "https://api.telegram.org/bot" +
                    currentState.value.bot +
                    "/sendMessage?chat_id=" +
                    currentState.value.channel +
                    "&text=test"
            try {
                client.get<HttpResponse>(url)
                currentState.value = currentState.value.copy(tested = true)
                app.datastore.edit {
                    it[HomeState.TESTED] = true
                }
                messageToShow.value = "Test message successfully sent!"
            } catch (e: Exception) {

                currentState.value = currentState.value.copy(tested = false)
                app.datastore.edit {
                    it[HomeState.TESTED] = false
                }

                messageToShow.value = "Test message failed: ${e.message}"
            }
        }
    }

    init {
        viewModelScope.launch {
            homeState.collect {
                currentState.value = it
            }
            checkStatus()
        }

    }

    private fun checkStatus() {
        viewModelScope.launch {
            val receiver = ComponentName(app.applicationContext, SMSReceiver::class.java)
            val status = when (app.packageManager.getComponentEnabledSetting(receiver)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                else -> false
            }
            delay(1000)
            currentState.value = currentState.value.copy(active = status)
            app.datastore.edit {
                it[HomeState.ACTIVE] = currentState.value.active
            }
        }
    }

}




