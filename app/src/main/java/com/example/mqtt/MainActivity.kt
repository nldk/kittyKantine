package com.example.mqtt

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mqtt.ui.theme.MqttTheme
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            var firstStart = true
            try {
                load(applicationContext, "firstStart")
                firstStart = false
            } catch (e: Exception) {

            }
            super.onCreate(savedInstanceState)
            val host = ""
            var username = ""
            var password = ""
            var messageState by mutableStateOf("not updated")
            lateinit var client: Mqtt5Client

            if (!firstStart) {
                username = load(applicationContext,"username")
                password = load(applicationContext,"pass")
                client = Mqtt5Client.builder().identifier(load(applicationContext,"id"))
                    .serverHost(host).automaticReconnectWithDefaultConfig()
                    .serverPort(8883)
                    .sslWithDefaultConfig()
                    .build()

                client.toBlocking().connectWith().simpleAuth()
                    .username(username)
                    .password(password.toByteArray(StandardCharsets.UTF_8)).applySimpleAuth()
                    .willPublish()
                    .topic("home/will").payload("sensor gone".toByteArray()).applyWillPublish()
                    .send()

                runBlocking {
                    launch {
                        client.toAsync().subscribeWith().topicFilter("kattenvoederbak/#")
                            .callback { publish ->
                                if (publish.payloadAsBytes.toString(StandardCharsets.UTF_8) == "sluit") {
                                    messageState = "gesloten"
                                } else if (publish.payloadAsBytes.toString(StandardCharsets.UTF_8) == "open") {
                                    messageState = "open"
                                }
                            }.send()
                    }
                }
            }
            enableEdgeToEdge()
            setContent {
                MqttTheme {
                    if (!firstStart) {
                        Column {
                            Spacer(modifier = Modifier.height(35.dp))
                            Row {
                                Hok(messageState, client)
                            }
                        }
                    } else {
                        var name by remember { mutableStateOf("") }
                        var Password by remember { mutableStateOf("") }
                        Column {
                            Spacer(modifier = Modifier.height(35.dp))
                            Row {
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(text = "username")
                                    OutlinedTextField(value = name, onValueChange = { text ->
                                        name = text
                                    })
                                    Text(text = "password")
                                    OutlinedTextField(value = Password, onValueChange = { text ->
                                        Password = text
                                    })
                                    Button(
                                        onClick = {
                                            save(applicationContext, "firstStart", "false")
                                            save(applicationContext,"username",name)
                                            save(applicationContext,"pass",Password)
                                            save(applicationContext,"id",generateRandomString(20))
                                            recreate()
                                        },
                                    ) {
                                        Text("OK")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            enableEdgeToEdge()
            setContent {
                Column {
                    Spacer(modifier = Modifier.height(35.dp))
                    Text(e.toString())
                }
            }
        }
    }
}

@Composable
fun Hok(state: String, client: Mqtt5Client) {
    Box(Modifier.height(50.dp)) {
        Row {
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    runBlocking {
                        launch {
                            client.toBlocking().publishWith().topic("kattenvoederbak/klep")
                                .payload("sluit".toByteArray()).send()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(5.0F)
            ) {
                Text("sluiten")
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    state, modifier = Modifier
                        .fillMaxHeight()
                        .width(65.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    runBlocking {
                        launch {
                            client.toBlocking().publishWith().topic("kattenvoederbak/klep")
                                .payload("open".toByteArray()).send()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(5.0F)
            ) {
                Text("openen")
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

fun generateRandomString(len: Int = 15): String {
    val alphanumerics = CharArray(26) { it -> (it + 97).toChar() }.toSet()
        .union(CharArray(9) { it -> (it + 48).toChar() }.toSet())
    return (0..len - 1).map {
        alphanumerics.toList().random()
    }.joinToString("")
}

fun save(context: Context, filename: String, content: String) {
    val fileContents = content
    context.openFileOutput(filename, MODE_PRIVATE).use {
        it.write(fileContents.toByteArray())
    }
}

fun load(context: Context, filename: String): String {
    val content = context.openFileInput(filename).bufferedReader().use {
        it.readText()
    }
    return content
}