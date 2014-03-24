package com.google.zxing.client.android;

public class ExponentialMovingAverage {
	
    private float alpha;
    private float oldValue;
    private boolean first;
    
    
    public ExponentialMovingAverage(float alpha) {
    	
        this.alpha = alpha;
        first = true;
    }
    

    public float average(float value) {
        if (first) {
        	
        	first = false;
            oldValue = value;
            return value;
        }
        float newValue = oldValue + alpha * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }
}