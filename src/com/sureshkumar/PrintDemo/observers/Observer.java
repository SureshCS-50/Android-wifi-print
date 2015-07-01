package com.sureshkumar.PrintDemo.observers;

/**
 * Created by Anil on 8/13/2014.
 */
public interface Observer {
    public void update();
    void updateObserver(boolean bool);
    void updateObserverProgress(int percentage);
}
