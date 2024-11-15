package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.LinkedHashMap;


public class MainViewModel extends ViewModel {
    private final EnergyUsageRepository energyUsageRepository;
    private final MutableLiveData<LinkedHashMap<String, Float>> dailyTotalsMap = new MutableLiveData<>();

    public MainViewModel() {
        energyUsageRepository = EnergyUsageRepository.getInstance();
    }

    public LiveData<LinkedHashMap<String, Float>> getDailyTotalsMap() {
        return dailyTotalsMap;
    }

    public void loadLastMonthData() {
        energyUsageRepository.loadLastMonth(() -> dailyTotalsMap.postValue(new LinkedHashMap<>(energyUsageRepository.getDailyTotalsMap())));
    }

}