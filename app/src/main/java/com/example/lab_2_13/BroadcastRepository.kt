package com.example.lab_2_13

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BroadcastRepository(private val context: Context) {

    fun getBroadcastEvents(): Flow<BroadcastEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { safeIntent ->
                    val action = safeIntent.action ?: "UNKNOWN_ACTION"
                    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    val currentTime = timeFormat.format(Date())

                    val extrasMap = safeIntent.extras?.let { bundle ->
                        bundle.keySet().joinToString("\n") { key ->
                            val value = bundle.get(key)
                            "• $key: $value"
                        }
                    } ?: "немає додаткових параметрів (Extras)"

                    val readableName = when (action) {
                        Intent.ACTION_POWER_CONNECTED -> "зарядний пристрій підключено"
                        Intent.ACTION_POWER_DISCONNECTED -> "зарядний пристрій відключено"
                        Intent.ACTION_AIRPLANE_MODE_CHANGED -> "зміна режиму 'У літаку'"
                        Intent.ACTION_BATTERY_LOW -> "Критично низький рівень батареї"
                        Intent.ACTION_BATTERY_OKAY -> "рівень батареї відновлено"
                        Intent.ACTION_HEADSET_PLUG -> "Зміна стану навушників"
                        else -> action
                    }

                    val details = buildString {
                        append("Системна дія: ").append(action).append("\n\n")
                        append("Параметри Intent:\n").append(extrasMap)
                    }

                    val event = BroadcastEvent(
                        id = UUID.randomUUID().toString(),
                        type = readableName,
                        timestamp = currentTime,
                        details = details
                    )

                    trySend(event)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }

        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            0
        }

        ContextCompat.registerReceiver(context, receiver, filter, receiverFlags)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}