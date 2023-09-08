package com.example.wycmqtt

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

class MqttActivity : AppCompatActivity() {

    private var mqttPublishClient:MqttAsyncClient? = null
    private var mqttSubscribeClient:MqttClient? = null

    private var publishMsg:EditText? = null
    private var subscribeMsg:TextView? = null

    private var hasNotExit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt)

        initBtn()
        initMsg()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClient()
    }

    private fun initMsg(){
        publishMsg = findViewById(R.id.publish_msg)
        subscribeMsg = findViewById(R.id.subscribe_msg)
    }

    private fun initBtn(){
        findViewById<Button>(R.id.publish_btn)?.let {
            it.setOnClickListener {
                try {
                    mqttPublishClient?.publish(topic, MqttMessage((publishMsg!!.text.toString()+ Date().time).toByteArray()))
                }catch (e:MqttException){
                    e.printStackTrace()
                }
            }
        }

        findViewById<Button>(R.id.subscribe_btn)?.let {
            it.setOnClickListener {
                try {
                    mqttSubscribeClient?.subscribe(topic)
                }catch (e:MqttException){
                    e.printStackTrace()
                }
            }
        }

        findViewById<Button>(R.id.start_btn)?.let {
            it.setOnClickListener {
                startClient()
            }
        }
    }

    private fun initPublishClient(){
        mqttPublishClient  = MqttAsyncClient(server, publishClientId,MemoryPersistence())
        val options = MqttConnectOptions()
        options.userName = "wyc"
        options.password = "aa168168".toCharArray()

        mqttPublishClient!!.setCallback(object :MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                publishMsg!!.setText(String.format("connectionLost:%s",cause.toString()))
                if (hasNotExit){
                    mqttPublishClient?.reconnect()
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.e("publish messageArrived", "topic:$topic,message:$message")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.e("publish deliveryComplete", "token:$token")
            }

        })

        mqttPublishClient!!.connect(options)
    }

    private fun initSubscribeClient(){
        mqttSubscribeClient = MqttClient(server, subscribeClientId,MemoryPersistence())
        val options = MqttConnectOptions()
        options.userName = "wyc"
        options.password = "aa168168".toCharArray()

        mqttSubscribeClient!!.setCallback(object :MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                subscribeMsg!!.text = String.format("connectionLost:%s",cause.toString())
                if (hasNotExit){
                    mqttSubscribeClient?.reconnect()
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                subscribeMsg!!.text = String.format("topic:%s,message:%s",topic,message)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.e("subscribe deliveryComplete", "token:$token")
            }

        })
        mqttSubscribeClient!!.connect(options)
    }

    private fun stopClient(){
        try {
            hasNotExit = false

            mqttPublishClient?.disconnect()
            mqttSubscribeClient?.disconnect()

            mqttPublishClient?.close(true)
            mqttSubscribeClient?.close(true)
        }catch (e:MqttException){
            e.printStackTrace()
        }
    }

    private fun startClient(){
        stopClient()

        hasNotExit = true

        try {
            initPublishClient()
            initSubscribeClient()
        }catch (e:MqttException){
            e.printStackTrace()
        }
    }

    companion object{
        const val server = "tcp://192.168.0.254:1883"
        const val wServer = "tcp://mqtt.eclipseprojects.io:1883"
        const val publishClientId = "wyc/mqtt"
        const val subscribeClientId = "wyc/subscribe/mqtt"
        const val topic = "wyc/mqtt/test"
        @JvmStatic
        fun start(context: Context){
            context.startActivity(Intent(context,MqttActivity::class.java))
        }
    }
}