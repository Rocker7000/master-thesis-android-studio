package com.example.myapplication;

public class DeviceData {
    private String name;
    private float usagePercentage;

    public DeviceData(String name, float usagePercentage) {
        this.name = name;
        this.usagePercentage = usagePercentage;
    }

    public String getName() {
        return name;
    }

    public void setUsagePercentage(float usagePercentage) {
        this.usagePercentage = usagePercentage;
    }
    public float getUsagePercentage() {
        return usagePercentage;
    }
}