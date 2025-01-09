package com.example.mqtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mqtt.ui.theme.MqttTheme
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    val mqttServerUri = "tcp://5e0f1dfcbc8748188a27521c8c41cbcf.s1.eu.hivemq.cloud:8883"
    val username = "testaccount"
    val password = "Niel2011vnt"
    var messageState by mutableStateOf("updating")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host = "5e0f1dfcbc8748188a27521c8c41cbcf.s1.eu.hivemq.cloud"
        val username = "MqttApp"
        val password = "1#9ZiI3o9RWfn*sD0nRA!oBkUr7cZX1di2QuH4E!Y4fV8Z4^Vt"

        val client: Mqtt5Client = Mqtt5Client.builder().identifier(generateRandomString(20)) // use a unique identifier
            .serverHost(host).automaticReconnectWithDefaultConfig()
            .serverPort(8883)
            .sslWithDefaultConfig()
            .build()

        client.toBlocking().connectWith().simpleAuth()
            .username(username)
            .password(password.toByteArray(StandardCharsets.UTF_8)).applySimpleAuth()
            .willPublish()
            .topic("home/will").payload("sensor gone".toByteArray()).applyWillPublish().send()

        runBlocking {
            launch {
                client.toAsync().subscribeWith().topicFilter("kattenvoederbak/#").callback { publish ->
                    if(publish.payloadAsBytes.toString(StandardCharsets.UTF_8)=="sluit"){
                        messageState = "gesloten"
                    }else if (publish.payloadAsBytes.toString(StandardCharsets.UTF_8)=="open"){
                        messageState = "open"
                    }
                }.send()
            }
        }

        enableEdgeToEdge()
        setContent {
            MqttTheme {
                Column {
                    Spacer(modifier = Modifier.height(35.dp))
                    Hok(messageState,client)
                }
            }
        }
    }
}

@Composable
fun Hok(state:String,client: Mqtt5Client) {
    Box(Modifier.height(50.dp)) {
        Row {
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    runBlocking {
                        launch {
                            client.toBlocking().publishWith().topic("kattenvoederbak/klep").payload("sluit".toByteArray()).send()
                        }
                    }
                },
                modifier = Modifier.fillMaxHeight().weight(5.0F)
            ) {
                Text("sluiten")
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(state, modifier = Modifier.fillMaxHeight().width(65.dp).align(Alignment.CenterHorizontally))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    runBlocking {
                        launch {
                            client.toBlocking().publishWith().topic("kattenvoederbak/klep").payload("open".toByteArray()).send()
                        }
                    }
                },
                modifier = Modifier.fillMaxHeight().weight(5.0F)
            ) {
                Text("openen")
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}
fun generateRandomString(len: Int = 15): String{
    val alphanumerics = CharArray(26) { it -> (it + 97).toChar() }.toSet()
        .union(CharArray(9) { it -> (it + 48).toChar() }.toSet())
    return (0..len-1).map {
        alphanumerics.toList().random()
    }.joinToString("")
}