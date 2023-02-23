package com.example.morningstar_app;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;

import static no.nordicsemi.android.ble.error.GattError.GATT_INTERNAL_ERROR;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;


public class BLEservice extends Service {

    private Binder binder = new LocalBinder();

    public static final String TAG = "BLEservice: ";

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt;

    public BluetoothDevice device;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.ble_with_nordicble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.ble_with_nordicble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.ble_with_nordicble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.ble_with_nordicble.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_CLOSE =
            "com.example.ble_with_nordicble.ACTION_GATT_CLOSE";
    public final static String SENDER_SERVICE_UUID_STRING =
            "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    public final static String SENDER_CHARACTERISTIC_UUID_STRING =
            "beb5483e-36e1-4688-b7f5-ea07361b26a8";


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    private int connectionState;
    private Handler hrHandler;

    private Queue<Runnable> commandQueue = new ArrayBlockingQueue<>(100); //---> muss instanziiert werden sonst: NULL-Pointer Exception
    private boolean commandQueueBusy;
    private Handler bleHandler;
    private int nrTries = 0;
    private final String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";


    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bleHandler = new Handler(getMainLooper()); //---> Handler läuft auf dem Main Thread (siehe Blog post 3)
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }
    @SuppressLint({"MissingPermission", "NewApi"})
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        try {
            device = bluetoothAdapter.getRemoteDevice(address);
            Log.w(TAG,"found Device "+device.getName());
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback,TRANSPORT_LE); //TODO:-------> Testing
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }
    @SuppressLint("MissingPermission")
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
        broadcastUpdate(ACTION_GATT_CLOSE);
    }

    class LocalBinder extends Binder {
        public BLEservice getService() {
            return BLEservice.this;
        }
    }
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG,"onConnectionStateChange triggered, status: "+status+" newState: "+newState);

            if(status == GATT_SUCCESS){
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectionState = STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    int bondstate = gatt.getDevice().getBondState();
                    if(bondstate == BOND_NONE || bondstate == BOND_BONDED) {
                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                boolean result = bluetoothGatt.discoverServices();
                                if(!result) {
                                    Log.d(TAG,"discoverServices failed");
                                }
                            }
                        },30);
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    gatt.close();
                }
            }else{
                gatt.close();
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                Log.i(TAG, "discovered services");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                final List<BluetoothGattService> services = gatt.getServices();
                Log.i(TAG, "discovered"+services.size()+" services for "+device.getName());
                for(BluetoothGattService n: services){
                    List<BluetoothGattCharacteristic> characteristics = n.getCharacteristics();
                    for(BluetoothGattCharacteristic m: characteristics){
                        setNotify(m,true);
                        if(m.getProperties() == PROPERTY_READ) {
                            Log.i(TAG,"service: "+services.indexOf(n)+" characteristic: "+characteristics.indexOf(m)+" m.getProperties() == PROPERTY_READ: "+(m.getProperties() == PROPERTY_READ));
                            readCharacteristic(m);
                        }else{
                            Log.e(TAG, "service: "+services.indexOf(n)+" characteristic: "+characteristics.indexOf(m)+" ERROR: Characteristic cannot be read: property: "+m.getProperties()+" readproperty: "+PROPERTY_READ);
                        }

                    }
                }
            } else if (status == GATT_INTERNAL_ERROR){
                Log.e(TAG, "onServicesDiscovered received status: " + status);
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for(BluetoothGattDescriptor d: descriptors){
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(d);
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            // Do some checks first
            if(status!= GATT_SUCCESS) {
                Log.e(TAG, "ERROR: Write descriptor failed");
                return;
            }
            completedCommand();
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG,"characteristic read - deprecated method");
            byte[] b = characteristic.getValue();
            int value = b[0];
            Log.i(TAG,"value from deprecated method: "+value); //----------> Reading only works with deprecated method
            if (status == GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
            }
            Log.i(TAG,"Status "+status);
            completedCommand();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public boolean setNotify(BluetoothGattCharacteristic characteristic, final boolean enable) {
        // Check if characteristic is valid
        if(characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring setNotify request");
            return false;
        }

        // Get the CCC Descriptor for the characteristic
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID));
        if(descriptor == null) {
            Log.e(TAG, "ERROR: Could not get CCC descriptor for characteristic "+characteristic.getUuid());
            return false;
        }

        // Check if characteristic has NOTIFY or INDICATE properties and set the correct byte value to be written
        byte[] value;
        int properties = characteristic.getProperties();
        if ((properties & PROPERTY_NOTIFY) > 0) {
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((properties & PROPERTY_INDICATE) > 0) {
            value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        } else {
            Log.e(TAG, "ERROR: Characteristic "+characteristic.getUuid()+" does not have notify or indicate property");
            return false;
        }
        final byte[] finalValue = enable ? value : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

        // Queue Runnable to turn on/off the notification now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // First set notification for Gatt object
                if(!bluetoothGatt.setCharacteristicNotification(descriptor.getCharacteristic(), enable)) {
                    Log.e(TAG, "ERROR: setCharacteristicNotification failed for descriptor: "+ descriptor.getUuid());
                }
                // Then write to descriptor
                descriptor.setValue(finalValue);
                boolean result;
                result = bluetoothGatt.writeDescriptor(descriptor);
                if(!result) {
                    Log.e(TAG, "ERROR: writeDescriptor failed for descriptor: "+ descriptor.getUuid());
                    completedCommand();
                } else {
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue write command");
        }

        return result;
    }

    public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, "characteristic is: "+characteristic);
        if(bluetoothGatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request");
            return false;
        }

        // Check if characteristic is valid
        if(characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request");
            return false;
        }

        // Enqueue the read command now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if(!bluetoothGatt.readCharacteristic(characteristic)) {
                    Log.e(TAG, "ERROR: readCharacteristic failed for characteristic: " + characteristic.getUuid());
                    completedCommand();
                } else {
                    Log.d(TAG, "reading characteristic "+characteristic.getUuid());
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
        }
        return result;
    }

    private void completedCommand() {
        commandQueueBusy = false;
        commandQueue.poll();
        nextCommand();
    }
    private void nextCommand() {
        // If there is still a command being executed then bail out
        if(commandQueueBusy) {
            return;
        }

        // Check if we still have a valid gatt object
        if (bluetoothGatt == null) {
            Log.e(TAG, "ERROR: GATT is 'null' for peripheral "+device.getAddress()+", clearing command queue");
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }

        // Execute the next command in the queue
        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;
            bleHandler.post(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, "ERROR: Command exception for device " + device.getName());
                    }
                }
            });
        }
    }
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        byte[] b = characteristic.getValue();
        int heartRate = b[0];
        String hr = Integer.toString(heartRate);
        intent.putExtra("BPM", String.valueOf(heartRate));
        sendBroadcast(intent);
    }
}
