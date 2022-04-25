package com.wyc.label.room

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.Toast
import com.wyc.label.App
import com.wyc.label.R
import com.wyc.label.Utils
import java.util.*

class BluetoothUtils {
    companion object{
        const val REQUEST_BLUETOOTH_PERMISSIONS = 0xabc8
        const val REQUEST_BLUETOOTH_ENABLE = 0X8888

        @JvmStatic
        fun startBlueToothDiscovery(activity: Activity) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) if (bluetoothAdapter.isEnabled) {
                if (!bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.startDiscovery()
                }
            } else {
                Utils.showToast(R.string.not_enbel_bluetooth)
            } else Utils.showToast("设备不支持蓝牙功能！")
        }
        @JvmStatic
        fun hasSupportBluetooth(): Boolean {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val code = bluetoothAdapter != null && bluetoothAdapter.isEnabled
            if (!code) {
                Utils.showToast(R.string.not_enbel_bluetooth)
            }
            return code
        }
        @JvmStatic
        fun stopBlueToothDiscovery() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            }
        }
        @JvmStatic
        fun bondBlueTooth(addr: String?) {
            if (!addr.isNullOrEmpty()) {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null) {
                    try {
                        val device = bluetoothAdapter.getRemoteDevice(addr)
                        if (device.bondState == BluetoothDevice.BOND_NONE) {
                            device.createBond()
                        }
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        Utils.showToast(String.format(Locale.CHINA, "The address of bluetooth:%s,exception:%s", addr, e.message))
                    }
                }
            }
        }
        @JvmStatic
        fun attachReceiver(context: Context, receiver: BroadcastReceiver) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            context.registerReceiver(receiver, intentFilter)
        }
        @JvmStatic
        fun detachReceiver(context: Context, receiver: BroadcastReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}