package com.sureshkumar.PrintDemo.services;

/**
 * Created by Sureshkumar on 12-06-2015.
 */
public interface PrintCompleteService {
    public void onMessage(int status);
    public void respondAfterWifiSwitch();
}
