package com.sureshkumar.PrintDemo.observers;

/**
 * Created by Anil on 8/13/2014.
 */
public interface Observable {
    public void notifyObserver(boolean bool);
    public void attach(Observer observer);
    public void detach(Observer observer);
    public void notify(Object param);
    public void updateProgress(int percentage);
}
