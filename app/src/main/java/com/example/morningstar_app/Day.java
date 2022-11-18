package com.example.morningstar_app;

public class Day {
    private String name;
    private String wakeHour = "06";
    private String wakeMinute = "00";
    private boolean set = true;

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }



    public Day(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWakeHour() {
        return wakeHour;
    }

    public void setWakeHour(String wakeHour) {
        this.wakeHour = wakeHour;
    }

    public String getWakeMinute() {
        return wakeMinute;
    }

    public void setWakeMinute(String wakeMinute) {
        this.wakeMinute = wakeMinute;
    }

}
