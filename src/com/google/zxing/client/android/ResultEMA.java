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
			
			
			return points;
			
			
		}

		
		
		//ResultPoint[] points = riordinaPunti(punti);
		
		ResultPoint[] newPunti = new ResultPoint[3];
		
		for(int i=0; i < newPunti.length; i++){
			
			float x = oldPunti[i].getX() + alpha * (points[i].getX() - oldPunti[i].getX() );
			float y = oldPunti[i].getY() + alpha * (points[i].getY() - oldPunti[i].getY() );
			
			newPunti[i] = new ResultPoint(x, y);
			
		}
		
		oldPunti = newPunti;
		
		return newPunti;
		
	}
	
	
	private ResultPoint[] riordinaPunti(ResultPoint[] punti){
		
		ResultPoint[] newPunti = new ResultPoint[punti.length];
		
		for(int i=0; i< punti.length; i++){
			
			double[] distanze = new double[punti.length];
			
			int j=0;
			
			for(int k=0; k< punti.length; k++){
				distanze[k] = Math.pow(oldPunti[i].getX()-punti[k].getX(),2)+Math.pow(oldPunti[i].getY()-punti[k].getY(),2);
				
				if (distanze[k]<distanze[j]) j=k;
			}
			
			newPunti[i]=punti[j];
	
		}
		
		return newPunti;
		
	}
	 

	
	

}
