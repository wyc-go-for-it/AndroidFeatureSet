package com.wyc.kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wyc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class KotlinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kotlinactivity_main)

        var name by CustomProperty<String>()


        name = "wyc"

        testFlow()
    }

    private fun testFlow(){

        CoroutineScope(Dispatchers.IO).launch {

            flow {
                //上游
                emit("hello world ${Thread.currentThread()}")
                emit("hello world ${Thread.currentThread()}")
            }.transform {
                emit("$it 1")
            }.transform {
                emit("$it 2")
            }.transform {
                emit("$it 3")
            }.collect {
                Logger.d("$it")
            }

        }
    }

    companion object{
        @JvmStatic
        fun start(c:Context){
            c.startActivity(Intent(c,KotlinActivity::class.java))
        }
    }
}