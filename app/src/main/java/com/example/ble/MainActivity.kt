package com.example.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var mBtnNavigation : Button
    lateinit var mBtnStraight : Button
    lateinit var mBtnTurnRight : Button
    lateinit var mBtnTurnLeft : Button
    lateinit var mBtnUTurnRight : Button
    lateinit var mBtnMix : Button
    lateinit var mtvConnected : TextView
    lateinit var mtvDisconnected : TextView
    lateinit var mEtDelay : EditText

    var bleCount = 0;
    var characters_uuid: String = "0000fff2-0000-1000-8000-00805f9b34fb";
    var state = 0;
    val navigation = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0a.toByte(),0x02.toByte(),0x10.toByte(),
        0x01.toByte(),0x0B.toByte(),0x00.toByte(),0x00.toByte(),0x27.toByte())

    val straight = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0c.toByte(),0x04.toByte(),0x10.toByte(),
        0x02.toByte(),0x0e.toByte(),0x00.toByte(),0x00.toByte(),0x00.toByte(),0x01.toByte(),0x30.toByte())
    val turnRight = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0c.toByte(),0x04.toByte(),0x10.toByte(),
        0x02.toByte(),0x0e.toByte(),0x00.toByte(),0x00.toByte(),0x00.toByte(),0x02.toByte(),0x31.toByte())
    val turnLeft = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0c.toByte(),0x04.toByte(),0x10.toByte(),
        0x02.toByte(),0x0e.toByte(),0x00.toByte(),0x00.toByte(),0x00.toByte(),0x04.toByte(),0x33.toByte())
    val uTurnRight = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0c.toByte(),0x04.toByte(),0x10.toByte(),
        0x02.toByte(),0x0e.toByte(),0x00.toByte(),0x00.toByte(),0x00.toByte(),0x08.toByte(),0x37.toByte())

    var command = byteArrayOf(0xAA.toByte(), 0x55.toByte(), 0x0c.toByte(),0x04.toByte(),0x10.toByte(),
        0x02.toByte(),0x0e.toByte(),0x00.toByte(),0x00.toByte(),0x00.toByte(),0x01.toByte(),0x30.toByte())

    var listaa : ArrayList<BluetoothGattCharacteristic> = ArrayList<BluetoothGattCharacteristic>()

    var myBluetoothGattCharacteristic : BluetoothGattCharacteristic? = null

    var isCommand : Boolean = false

    var isMix : Boolean = false

    var commandDelay : String = "100"

    private fun enableNotification(
        enable: Boolean,
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (gatt == null || characteristic == null) return  //????????????????????? ?????????????????????
        Log.d("TAG", "characters_uuid ---> ${characteristic.uuid.toString()}")
        val characteristicDescriptor =
            characteristic.getDescriptor(UUID.fromString(characteristic.uuid.toString()))
        if (characteristicDescriptor != null) {
            val value =
                if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            characteristicDescriptor.value = value
            gatt.writeDescriptor(characteristicDescriptor)
        }
        gatt.setCharacteristicNotification(characteristic, enable)
    }
    val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) { //??????????????????
            Log.e("BluetoothGatt?????????", "????????????:$newState")
            /**
             * ???????????????
             * * The profile is in disconnected state   *public static final int STATE_DISCONNECTED  = 0;
             * * The profile is in connecting state     *public static final int STATE_CONNECTING    = 1;
             * * The profile is in connected state      *public static final int STATE_CONNECTED    = 2;
             * * The profile is in disconnecting state  *public static final int STATE_DISCONNECTING = 3;
             *
             */
            if (BluetoothGatt.STATE_CONNECTED == newState) {
                Log.e("onConnec?????????", "????????????:")
                gatt.discoverServices() //?????????????????????onServicesDiscovered????????????Services
                Log.e("BLE", "????????????")
                state =1
                runOnUiThread {
                    Runnable {
                        mtvDisconnected.visibility = View.GONE
                        mtvConnected.visibility = View.VISIBLE
                    }.run()
                }
            } else if (BluetoothGatt.STATE_DISCONNECTED == newState) {
                Log.e("?????? ?????????", "????????????:")
                runOnUiThread {
                    Runnable {
                        mtvConnected.visibility = View.GONE
                        mtvDisconnected.visibility = View.VISIBLE
                    }.run()
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) { //????????????????????????????????????????????????
            val list: List<BluetoothGattService> = gatt.getServices()
            for (bluetoothGattService in list) {
                val str = bluetoothGattService.uuid.toString()
                Log.e("onServicesDisc?????????", " ???$str")
                val gattCharacteristics = bluetoothGattService
                    .characteristics

                for (gattCharacteristic in gattCharacteristics) {
                    Log.e("onServicesDisc?????????", " ???" + gattCharacteristic.uuid)

                    if (characters_uuid == gattCharacteristic.uuid.toString()
                    ) {
                    enableNotification(true, gatt, gattCharacteristic)
                    listaa.add(gattCharacteristic)
                    myBluetoothGattCharacteristic = gattCharacteristic
                    Thread {
//                        Thread.sleep(2000)
//                        Log.e("asd", "11111111111Send to ${gattCharacteristic.uuid}")
//                        gattCharacteristic.setValue(navigation)
//                        gatt.writeCharacteristic(gattCharacteristic)
                        while(true) {
                            if (isCommand) {
                                Log.e(
                                    "asd",
                                    "222222222222222Send to ${gattCharacteristic.uuid}"
                                )
                                while(isMix) {
                                    Log.e(
                                        "asd",
                                        "222222222222222Send to ${gattCharacteristic.uuid}"
                                    )
                                    gattCharacteristic.setValue(straight)
                                    gatt.writeCharacteristic(gattCharacteristic)
                                    Thread.sleep(commandDelay.toLong())
                                    gattCharacteristic.setValue(turnRight)
                                    gatt.writeCharacteristic(gattCharacteristic)
                                    Thread.sleep(commandDelay.toLong())
                                    gattCharacteristic.setValue(turnLeft)
                                    gatt.writeCharacteristic(gattCharacteristic)
                                    Thread.sleep(commandDelay.toLong())
                                    gattCharacteristic.setValue(uTurnRight)
                                    gatt.writeCharacteristic(gattCharacteristic)
                                    Thread.sleep(commandDelay.toLong())
                                }
                                gattCharacteristic.setValue(command)
                                gatt.writeCharacteristic(gattCharacteristic)
                                isCommand = false
                            }
                        }
                    }.start()

//                        linkLossService = bluetoothGattService
//                        alertLevel = gattCharacteristic
//                        Log.e("daole", alertLevel.getUuid().toString())
                    }
                }
            }
            // //???????????????????????????????????????
        }

        /*override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) { //????????????????????????????????????????????????
            val list: List<BluetoothGattService> = gatt.getServices()
            for (bluetoothGattService in list) {
                val str = bluetoothGattService.uuid.toString()
                Log.e("onServicesDisc?????????", " ???$str")
                val gattCharacteristics = bluetoothGattService
                    .characteristics

                for (gattCharacteristic in gattCharacteristics) {
                    Log.e("onServicesDisc?????????", " ???" + gattCharacteristic.uuid)
//
//                    if ("0000ffe1-0000-1000-8000-00805f9b34fb" == gattCharacteristic.uuid.toString()
//                    ) {
                        enableNotification(true, gatt, gattCharacteristic)
                        listaa.add(gattCharacteristic)
                        Thread {
                            while(true) {
                                Thread.sleep(2000)
                                Log.e("asd", "Send to ${gattCharacteristic.uuid}")
                                gattCharacteristic.setValue(data)
                                gatt.writeCharacteristic(gattCharacteristic)
                            }
                        }.start()

//                        linkLossService = bluetoothGattService
//                        alertLevel = gattCharacteristic
//                        Log.e("daole", alertLevel.getUuid().toString())
//                    }
                }
            }
            // //???????????????????????????????????????
        }*/

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            Log.e("onCharacteristicRead???", "??????????????????" + characteristic.value)
        }

        /**
         * ????????????????????????
         * @param gatt
         * @param characteristic
         * @param status
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) { //?????????????????????
            Log.e("onCharacteristicWrite???", "??????????????????")

            if (status == BluetoothGatt.GATT_SUCCESS) { //????????????
                Log.e("onCharacteristicWrite???", "????????????")

            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.e("onCharacteristicWrite???", "????????????")
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.e("onCharacteristicWrite???", "?????????")
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?, status: Int
        ) { //descriptor???
            //Log.e("onCDescripticRead???", "??????????????????"+bytesToHexString(characteristic.getValue()));
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) { // Characteristic ??????????????????????????????
            Log.e("CharacteristicChanged???", "??????????????????" + characteristic.value)

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?, status: Int
        ) { //descriptor???
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {}
        override fun onReadRemoteRssi(
            gatt: BluetoothGatt,
            rssi: Int,
            status: Int
        ) { //???Rssi
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mtvConnected = findViewById(R.id.tvConnected)
        mtvDisconnected = findViewById(R.id.tvDisconnect)

        setSupportActionBar(findViewById(R.id.toolbar))
        val REQUEST_ENABLE_BT = 1
        val mBluetoothAdapter: BluetoothAdapter

        var bluetoothDeviceArrayList: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()

        // Initializes Bluetooth adapter.
        val bluetoothManager:BluetoothManager  = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        mBluetoothAdapter = bluetoothManager.adapter;

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            //???????????????
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                //???????????????????????????????????????
                //DialogUtils.shortT(applicationContext, "?????????????????????")
            }
        }

        val callback =
            LeScanCallback { device, rssi, scanRecord ->
                if(!bluetoothDeviceArrayList.contains(device)) {

                    bluetoothDeviceArrayList.add(device)
                    Log.e("BLE", "device : $device name : ${device.name}" )

                    if(device.name == " TEST HUD" && state == 0){
                        Log.e("BLE", "get BLE" )
                        device.connectGatt(this, true, mGattCallback)
                    }
                } else {
//                    bleCount++
//                    if(bleCount > bluetoothDeviceArrayList.size * 2){
//                        mBluetoothAdapter.stopLeScan({
//                                device, rssi, scanRecord ->
//                            Log.e("BLE", "all device : $bluetoothDeviceArrayList")
//                        })
//                    }
                }
                //Log.d("BLE", "run: scanning...")
            }

        mBluetoothAdapter.startLeScan(callback)

        mBtnNavigation = findViewById(R.id.btnNavigation)
        mBtnStraight = findViewById(R.id.btnStraight)
        mBtnTurnRight = findViewById(R.id.btnTurnRight)
        mBtnTurnLeft = findViewById(R.id.btnTurnLeft)
        mBtnUTurnRight = findViewById(R.id.btnUTurnRight)
        mBtnMix = findViewById(R.id.btnMix)

        mBtnNavigation.setOnClickListener(this)
        mBtnStraight.setOnClickListener(this)
        mBtnTurnRight.setOnClickListener(this)
        mBtnTurnLeft.setOnClickListener(this)
        mBtnUTurnRight.setOnClickListener(this)
        mBtnMix.setOnClickListener(this)

        mEtDelay = findViewById(R.id.etDelay)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnNavigation -> {
                isMix = false
                command = navigation
            }
            R.id.btnStraight -> {
                isMix = false
                command = straight
            }
            R.id.btnTurnRight -> {
                isMix = false
                command = turnRight
            }
            R.id.btnTurnLeft -> {
                isMix = false
                command = turnLeft
            }
            R.id.btnUTurnRight -> {
                isMix = false
                command = uTurnRight
            }
            R.id.btnMix -> {
                if ("".equals(mEtDelay.text.toString())) {
                    commandDelay = "100"
                } else {
                    commandDelay = mEtDelay.text.toString()
                }
                isMix = true
            }
        }
        isCommand = true
    }
}

