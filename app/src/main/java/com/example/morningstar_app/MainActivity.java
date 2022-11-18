package com.example.morningstar_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.timepicker.MaterialTimePicker.Builder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener{

    private FloatingActionButton btnEditMonday, btnEditTuesday, btnEditWednesday, btnEditThursday, btnEditFriday,
            btnEditSaturday, btnEditSunday, btnEditAll;
    private RelativeLayout parent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initElements();
    }

    @Override
    public void onClick(View view){
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
                }
            });

        }else{
            longPress = false;
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

}