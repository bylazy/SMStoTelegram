package com.bylazy.smstotelegram

import android.content.Context
import android.os.Parcelable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.parcelize.Parcelize

const val DATASTORE = "datastore"

const val FROM = "from"
const val TEXT = "text"

const val HOME = "home"
const val CONV = "conv"
const val HELP = "help"
const val INFO = "info"

val showInitialWarningKey = booleanPreferencesKey("shown")

val Context.datastore by preferencesDataStore(name = DATASTORE)

@Parcelize
data class HomeState(val active: Boolean,
                     val tested: Boolean,
                     val phone: String,
                     val sendall: Boolean,
                     val filter: String,
                     val channel: String,
                     val bot: String,
                     val prefix: String) : Parcelable {
    companion object Keys {
        val ACTIVE = booleanPreferencesKey("active")
        val TESTED = booleanPreferencesKey("tested")
        val PHONE = stringPreferencesKey("phone")
        val SENDALL = booleanPreferencesKey("all")
        val FILTER = stringPreferencesKey("filter")
        val TOKEN = stringPreferencesKey("token")
        val BOT = stringPreferencesKey("bot")
        val PREFIX = stringPreferencesKey("prefix")
    }
}

val initialState = HomeState(active = false,
    tested = false,
    phone = "Phone(s)",
    sendall = false,
    filter = "Filter",
    channel = "Not set",
    bot = "Not set",
    prefix = "FW: "
)

data class Conversation(val from: String, val snippet: String)
