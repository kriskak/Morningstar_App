package com.example.morningstar_app;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.timepicker.MaterialTimePicker.Builder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener{

    public final static String TAG = "MainActivity";

    //-------------------------- UI vars ----------------------
    private FloatingActionButton btnEditMonday, btnEditTuesday, btnEditWednesday, btnEditThursday, btnEditFriday,
            btnEditSaturday, btnEditSunday, btnEditAll;
    private Button btnConnectSenderDevice;
    private RelativeLayout parent;
    private TextView txtUntilWakeup, txtConnectSenderDeviceInfo;
    private String settingsHeadline="dummy text";
    private static String minute;
    private static String hour;
    private static Day Monday = new Day("Monday");
    private static Day Tuesday = new Day("Tuesday");
    private static Day Wednesday = new Day("Wednesday");
    private static Day Thursday = new Day("Thursday");
    private static Day Friday = new Day("Friday");
    private static Day Saturday = new Day("Saturday");
    private static Day Sunday = new Day("Sunday");
    private static Day ALL = new Day("ALL");
    private static ArrayList<Day> days = new ArrayList<>();
    private ArrayList<FloatingActionButton> btns = new ArrayList<>();
    private boolean longPress;
    //-------------------------- UI vars ----------------------

    //-------------------------- bluetooth vars ----------------------

    private boolean scanning, deviceFound;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner leScanner;
    private int handlerDelay = 6000;
    private BluetoothDevice senderDevice;
    private String senderDeviceName = "myCAT";//"ESP32Eva";//
    private String senderDeviceAddress = "null";
    private BLEservice bleService;

    //-------------------------- bluetooth vars ----------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initElements();
    }

    @Override
    public void onClick(View view){
        if(view.getId()==R.id.btnConnectSenderDevice){
            querySenderDeviceAddress(); //searches for sender Device and binds to the BLE service

    //----------------------- UI Implementation --------------------------
        }else{
            if(!longPress){
                setSettingsHeadline(view);
                MaterialTimePicker picker = initPicker(view);
                View btnSelected = view;
                picker.show(getSupportFragmentManager(),"SHOWING TP");
                picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(picker.getMinute()<10){
                            minute = "0"+picker.getMinute();
                        }else{
                            minute = String.valueOf(picker.getMinute());
                        }
                        if(picker.getHour()<10){
                            hour = "0"+picker.getHour();
                        }else{
                            hour = String.valueOf(picker.getHour());
                        }
                        Day d = getCorrespondingDay(btnSelected);
                        if(d.getName().equals("ALL")){
                            OverwriteAllDialogFragment fr = new OverwriteAllDialogFragment();
                            try{
                                fr.show(getSupportFragmentManager().beginTransaction(), "SHOWING DIALOG");
                            }catch(Exception e){
                                //Log.d("EXCEPTION: ",""+e);
                            }

                        }else{
                            d.setWakeHour(hour);
                            d.setWakeMinute(minute);
                            Log.d("DETECTED: ","\n Wake up time: "+d.getName()+" at "+hour+":"+minute);
                        }
                        updateTextview();
                    }
                });

            }else{
                longPress = false;
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onLongClick(View view) {
        longPress = true;
        Day d = getCorrespondingDay(view);
        if(d.getName().equals("ALL")){
            if(d.isSet()) {
                for (Day x : days) {
                    x.setSet(false);
                }
                for (FloatingActionButton b : btns) {
                    if (b.getId() != R.id.btnEditAll) {
                        b.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.btn_off)));
                    }else{}
                }
            }else{
                for (Day x : days) {
                    x.setSet(true);
                }
                for (FloatingActionButton b : btns) {
                    if (b.getId() != R.id.btnEditAll) {
                        b.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.sunshine)));
                    }else{}
                }
            }
        }else {
            if (d.isSet()) {
                view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.btn_off)));
                d.setSet(false);
            } else {
                view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.sunshine)));
                d.setSet(true);
            }
        }
        updateTextview();
        return false;
    }

    public String getSettingsHeadline() {
        return settingsHeadline;
    }

    public void setSettingsHeadline(String settingsHeadline) {
        this.settingsHeadline = settingsHeadline;

    }
    public void setSettingsHeadline(View view){
        Day d = getCorrespondingDay(view);
        if(d.getName().equals("ALL")){
            setSettingsHeadline("Edit all days at once");
        }else{
            setSettingsHeadline(d.getName());
        }
    }
    public MaterialTimePicker initPicker(View view){
        Builder builder = new MaterialTimePicker.Builder();
        builder.setTimeFormat(TimeFormat.CLOCK_24H);
        builder.setTitleText(getSettingsHeadline());
        Day d = getCorrespondingDay(view);
        builder.setMinute(Integer.parseInt(d.getWakeMinute()));
        builder.setHour(Integer.parseInt(d.getWakeHour()));
        try {
           builder.setTheme(R.style.Morningstar_App_MaterialTP);
        }catch(Exception e){
        }
        MaterialTimePicker picker = builder.build();
        return picker;
    }

    void initElements(){
        btnEditMonday = findViewById(R.id.btnEditMonday);
        btnEditTuesday = findViewById(R.id.btnEditTuesday);
        btnEditWednesday = findViewById(R.id.btnEditWednesday);
        btnEditThursday = findViewById(R.id.btnEditThursday);
        btnEditFriday = findViewById(R.id.btnEditFriday);
        btnEditSaturday = findViewById(R.id.btnEditSaturday);
        btnEditSunday = findViewById(R.id.btnEditSunday);
        btnEditAll = findViewById(R.id.btnEditAll);
        parent = findViewById(R.id.parent);
        txtUntilWakeup = findViewById(R.id.txtUntilWakeup);
        btnConnectSenderDevice = findViewById(R.id.btnConnectSenderDevice);
        txtConnectSenderDeviceInfo = findViewById(R.id.txtConnectSenderDeviceInfo);

        days.add(Monday);
        days.add(Tuesday);
        days.add(Wednesday);
        days.add(Thursday);
        days.add(Friday);
        days.add(Saturday);
        days.add(Sunday);
        days.add(ALL);

        btns.add(btnEditAll);
        btns.add(btnEditMonday);
        btns.add(btnEditTuesday);
        btns.add(btnEditWednesday);
        btns.add(btnEditThursday);
        btns.add(btnEditFriday);
        btns.add(btnEditSaturday);
        btns.add(btnEditSunday);

        for(FloatingActionButton b: btns){
            b.setOnClickListener(MainActivity.this);
            b.setOnLongClickListener(MainActivity.this);
        }
        btnConnectSenderDevice.setOnClickListener(MainActivity.this);
        performPermissionCheck();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTextview();
                startRepeatTimer();
            }
        }, 500);

    }

    private Day getCorrespondingDay(View view) {
        switch (view.getId()) {
            case R.id.btnEditMonday:
                return Monday;
            case R.id.btnEditTuesday:
                return Tuesday;
            case R.id.btnEditWednesday:
                return Wednesday;
            case R.id.btnEditThursday:
                return Thursday;
            case R.id.btnEditFriday:
                return Friday;
            case R.id.btnEditSaturday:
                return Saturday;
            case R.id.btnEditSunday:
                return Sunday;
            case R.id.btnEditAll:
                return ALL;
            default:return new Day("null");
        }
    }

    //currently not needed
    /*private View getCorrespondingView(Day d) {
        switch (d.getName()) {
            case "Monday":
                return btnEditMonday;
            case "Tuesday":
                return btnEditTuesday;
            case "Wednesday":
                return btnEditWednesday;
            case "Thursday":
                return btnEditThursday;
            case "Friday":
                return btnEditFriday;
            case "Saturday":
                return btnEditSaturday;
            case "Sunday":
                return btnEditSunday;
            case "ALL":
                return btnEditAll;
            default: return null;
        }
    }

*/
    private static void overwriteAll(){
        for(Day x: days){
            x.setWakeHour(hour);
            x.setWakeMinute(minute);
        }
        Log.d("DETECTED: ","\n Wake up time: every Day at "+hour+":"+minute);
    }

    @SuppressLint("NewApi")
    private void updateTextview(){
        int d = LocalDateTime.now().getDayOfWeek().getValue();
        boolean textSet = false;
        for(Day day: days){
            if(days.indexOf(day)+1 > d && days.indexOf(day)!=7){
                if(day.isSet()){
                    int ddiff = days.indexOf(day)+1-d;
                    //Log.d("DEBUG ","day "+d+" ddiff" +ddiff+" loop-day index "+days.indexOf(day));
                    int diffs[] = calculateTimeDifference(day,ddiff);
                    textSet = true;
                    txtUntilWakeup.setText("Time until wakeup: " + diffs[0] + " days " + diffs[1] + " h " + diffs[2] + " min");
                    break;
                }
            }
        }
        if(!textSet){
            for(Day day: days){
                if(days.indexOf(day)+1 <= d){
                    if(day.isSet()) {
                        int ddiff = (7 - d) + days.indexOf(day) + 1;
                        int diffs[] = calculateTimeDifference(day,ddiff);
                        textSet = true;
                        txtUntilWakeup.setText("Time until wakeup: " + diffs[0] + " days " + diffs[1] + " h " + diffs[2] + " min");
                        break;
                    }
                }
            }
        }
        if(!textSet){
            txtUntilWakeup.setText("no light-alarms set");
        }
    }

    @SuppressLint("NewApi")
    private int[] calculateTimeDifference(Day day, int ddiff){
        int h = LocalDateTime.now().getHour();
        int m = LocalDateTime.now().getMinute();
        int hdiff = 333;
        int mdiff = 333;
        int wakeHour = Integer.parseInt(day.getWakeHour());
        int wakeMinute = Integer.parseInt(day.getWakeMinute());
        if (wakeHour > h) {
            hdiff = wakeHour - h;
            if (wakeMinute >= m) {
                mdiff = wakeMinute - m;
            } else {
                hdiff = hdiff - 1;
                mdiff = 60 - m + wakeMinute;
            }
        }
        if (wakeHour < h) {
            ddiff = ddiff - 1;
            hdiff = 24 - h + wakeHour;
            if (wakeMinute >= m) {
                mdiff = wakeMinute - m;
            } else {
                hdiff = hdiff - 1;
                mdiff = 60 - m + wakeMinute;
            }
        }
        if (wakeHour == h) {
            hdiff = 0;
            if (wakeMinute >= m) {
                mdiff = wakeMinute - m;
            } else {
                ddiff = ddiff - 1;
                hdiff = 23;
                mdiff = 60 - m + wakeMinute;
            }
        }
        int[] diffs = {ddiff,hdiff,mdiff};
        return diffs;
    }

    public void startRepeatTimer() {
        CountDownTimer countDownTimer = new CountDownTimer(60000, 60000) {
            public void onTick(long millisUntilFinished) {
                updateTextview();
            }
            public void onFinish() {
                start();
            }
        }.start();
    }

    public static class OverwriteAllDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Overwirte all Clocks?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            overwriteAll();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }

//------------------- Bluetooth Implementation -------------------------
@SuppressLint("NewApi")
private boolean performPermissionCheck() {
    String[] permissions = new String[]{"android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH"
    };
    boolean permissionCheck = (ActivityCompat.checkSelfPermission(MainActivity.this,
            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
            && (ActivityCompat.checkSelfPermission(MainActivity.this,
            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
    if (!permissionCheck) {
        requestPermissions(permissions, 200);
    }
    return permissionCheck;
}
    @SuppressLint({"NewApi", "MissingPermission"})
    public void querySenderDeviceAddress(){

        scanning = false;
        try {
            BluetoothManager btManager = getSystemService(BluetoothManager.class);
            btAdapter = btManager.getAdapter();
            leScanner = btAdapter.getBluetoothLeScanner();

            if (performPermissionCheck()) {
                if (!scanning) {
                    scanning = true;
                    leScanner.startScan(scanCallback);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void run() {
                            if (scanning) {
                                leScanner.stopScan(scanCallback);
                                scanning = false;
                            }
                        }
                    }, handlerDelay);
                }
            }
        } catch (java.lang.NullPointerException npE) {
            Toast.makeText(MainActivity.this,"you need to turn on bluetooth",Toast.LENGTH_LONG).show();
        }
    }
    @SuppressLint("NewApi")
    ScanCallback scanCallback = new ScanCallback() { //callback muss für start und stopp gleich sein sonst stoppt scan nie
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            try {
                senderDevice = result.getDevice();
                if (senderDevice.getName().equals(senderDeviceName)) {
                    leScanner.stopScan(scanCallback);
                    Log.i(TAG,"Found Device");
                    txtConnectSenderDeviceInfo.setText("connected!");
                    deviceFound = true;
                    senderDeviceAddress = senderDevice.getAddress();
                    scanning = false;
                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bindToService();
                        }
                    },500);

                }
            } catch (java.lang.NullPointerException e){
                //Log.d("EXCEPTION: ",""+e); //wird jedes mal getriggert wenn das bt device null ist = oft
            }
        }
    };
    private void bindToService(){
        if(performPermissionCheck()){
            Intent gattServiceIntent = new Intent(MainActivity.this, BLEservice.class);
            Log.d("gattServiceIntent: "," "+gattServiceIntent);
            boolean binded = bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d("binded: ","status "+binded);
        }
    }
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEservice.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG,"Gatt connected");

            } else if (BLEservice.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG,"Gatt disconnected");
                deviceFound = false;
            } else if (BLEservice.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG,"Some Services were discovered ");
            }else if (BLEservice.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG,"Data from Service available ");
                String bpm = intent.getStringExtra("BPM");
                Log.i(TAG,"bpm is: "+bpm);

            }
            else if (BLEservice.ACTION_GATT_CLOSE.equals(action)){
                deviceFound = false;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(senderDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() { //-------> important else Broadcasts are not received
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEservice.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEservice.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEservice.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEservice.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEservice.ACTION_GATT_CLOSE);
        return intentFilter;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("onServiceConnected: ","invoked");
            bleService = ((BLEservice.LocalBinder) service).getService();
            if (bleService != null) {
                if (!bleService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                bleService.connect(senderDeviceAddress);
            }
        }

        @Override
        public void onNullBinding(ComponentName name) {
            ServiceConnection.super.onNullBinding(name);
            Log.d("onNullBinding: ","invoked");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
        }

    };
//------------------- Bluetooth Implementation -------------------------



}