package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;

public class ResultEMA  {

	ResultPoint[] punti;
	ResultPoint[] oldPunti;
	private float alpha;
	//private int n;
	
	public ResultEMA(float alpha) {
		
		 this.alpha = alpha;
		// n=0;
	       
	}
	
	public ResultPoint[] media(ResultPoint[] points){
		
		if(oldPunti == null){
			
			oldPunti = points;
		//	n++;
			return points;
			
		}
		
		//n++;
		
		//alpha = 2/(n+1);
		
		ResultPoint[] newPunti = new ResultPoint[3];
		
		for(int i=0; i < 3; i++){
			
			float x = oldPunti[i].getX() + alpha * (points[i].getX() - oldPunti[i].getX() );
			float y = oldPunti[i].getY() + alpha * (points[i].getY() - oldPunti[i].getY() );
			
			newPunti[i] = new ResultPoint(x, y);
			
		}
		
		oldPunti = newPunti;
		
		return newPunti;
		
	}
	

	
	

}
