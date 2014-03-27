/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 3;//20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int frameColor;
  //private final int laserColor;
  private int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;
  
  
  float scaleX;
  float scaleY;
  
  
  long start = 0;
  long tempo = 0;
  boolean prova;

  private ResultPoint[] risultato;
  
  ResultEMA ema;
  
  int angle;
  
  int beta;
  double r;
  
  double density;
  
  boolean spostati=false;
  
  List<Point> puntiDinamici = new ArrayList<Point>(2);
  int [] puntiDinamiciColor = new int[3];
  

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
   
    resultColor = resources.getColor(R.color.result_view);
  
    
    frameColor = resources.getColor(R.color.viewfinder_frame);
    
    
    risultato = null;
    
    ema = new ResultEMA(0.5f);
    
    puntiDinamici.add(new Point());
    
    puntiDinamici.add(new Point());
    
    puntiDinamici.add(new Point());
    
    puntiDinamiciColor[0] = resources.getColor(R.color.viewfinder_laser);
    puntiDinamiciColor[1] = resources.getColor(R.color.viewfinder_laser);
    
    puntiDinamiciColor[2] = resources.getColor(R.color.possible_result_points);
    
    
    beta=0;
    r=7;
    
    //laserColor = resources.getColor(R.color.viewfinder_laser);
    
   // laserColor = resources.getColor(android.R.color.black);
    
  //  resultPointColor = resources.getColor(R.color.possible_result_points);
    
    resultPointColor = resources.getColor(R.color.viewfinder_laser);
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<ResultPoint>(5);
    lastPossibleResultPoints = null;
    
  
    
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @Override
  public void onDraw(Canvas canvas) {
    Rect frame = cameraManager.getFramingRect();
    if (frame == null) {
      return;
    }
    
    
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    
    
    
    
    
    
    
    //questo vecchio metodo disegna il barcode dopo la decodifica

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
      
      
    } else {
    	
    	

      // Disegna il bordo della cornice
    	
      paint.setColor(frameColor);
      canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
      canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
      canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
      canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

      
      
      
      Rect previewFrame = cameraManager.getFramingRectInPreview();
      
      scaleX = frame.width() / (float) previewFrame.width();
      scaleY = frame.height() / (float) previewFrame.height();

      
     
      int frameLeft = frame.left;
      int frameTop = frame.top;
      
      
      
      
      
      if(risultato != null){
    	  
    	  
      	paint.setColor(resultPointColor);
      	
      	synchronized (risultato) {
              for (ResultPoint point : risultato) {
            	  canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                          frameTop + (int) (point.getY() * scaleY),
                          POINT_SIZE, paint);
              	}
      		}
      	
      	
      	//disegno il testo e due linee che uniscono i punti
      	
      //ipotizzando che il QrCode sia di 2,5 cm ho la densità px/cm
      	density = distance(risultato[1], risultato[2], scaleX, scaleY, 2.3);
      	
      	
      	paint.setTextSize(25);
      	
      	canvas.drawText("Angolo: " +angle+ "° - Densità: " +(int) density+ " px/cm - Beta: "+beta , 100, 100, paint);
      	
      ///canvas.drawText("Angolo: " +angle+ "° - X1: "+ (int)risultato[1].getX()+" , "+ (int)risultato[1].getY()+" Y1: "+ (int)risultato[2].getX()+" , "+ (int)risultato[2].getY()+"cos: "+ Math.cos(angle*Math.PI / 180)+" sin: "+ Math.sin(angle*Math.PI / 180) , 100, 100, paint);
      	
      	
      	canvas.drawLine(frameLeft, frameTop +100, (float) (frameLeft +density), frameTop + 100, paint);
      	
      	paint.setStrokeWidth(10.0f);
      	
      	canvas.drawLine(frameLeft + (int) (risultato[0].getX() * scaleX), frameTop + (int) (risultato[0].getY() * scaleY), frameLeft + (int) (risultato[1].getX() * scaleX),frameTop + (int) (risultato[1].getY() * scaleY), paint);
      	canvas.drawLine(frameLeft + (int) (risultato[1].getX() * scaleX), frameTop + (int) (risultato[1].getY() * scaleY),  frameLeft + (int) (risultato[2].getX() * scaleX), frameTop + (int) (risultato[2].getY() * scaleY), paint);
      	
      	
      	
      	puntiDinamici.get(0).x = (int) (frameLeft + (risultato[2].getX() * scaleX) + (3*density*Math.cos(angle*Math.PI / 180)));
      	
      	puntiDinamici.get(0).y =  (int) (frameTop + (risultato[2].getY() * scaleY) + (3*density*Math.sin(angle*Math.PI / 180)));
      	
      	paint.setColor(puntiDinamiciColor[0]);
      	canvas.drawCircle(  puntiDinamici.get(0).x , puntiDinamici.get(0).y , (float) density/4 ,  paint);
      	
      	
      	puntiDinamici.get(1).x = (int) (frameLeft + (risultato[2].getX() * scaleX) + (4*density*Math.cos((angle+45)*Math.PI / 180)));
      	
      	puntiDinamici.get(1).y = (int) (frameTop + (risultato[2].getY() * scaleY) + (4*density*Math.sin((angle+45)*Math.PI / 180)));
      	
      	
      	paint.setColor(puntiDinamiciColor[1]);
      	canvas.drawCircle(  puntiDinamici.get(1).x , puntiDinamici.get(1).y , (float) density/4 ,  paint);
      	
      	
      	puntiDinamici.get(2).x = (int) (frameLeft + (risultato[2].getX() * scaleX) + (r*density*Math.cos((angle+beta)*Math.PI / 180)));

      	puntiDinamici.get(2).y = (int) (frameTop + (risultato[2].getY() * scaleY) + (r*density*Math.sin((angle+beta)*Math.PI / 180)));
      	
      	
      	paint.setColor(puntiDinamiciColor[2]);
      	canvas.drawCircle(  puntiDinamici.get(2).x , puntiDinamici.get(2).y , (float) density/4 ,  paint);
      	
      	//canvas.drawCircle(  (float) (frameLeft + (risultato[2].getX() * scaleX) + (5*density*Math.cos((angle+45)*Math.PI / 180))) , (float)   (frameTop + (risultato[2].getY() * scaleY) - (5*density*Math.sin((angle+45)*Math.PI / 180)))  ,POINT_SIZE ,  paint);
      	
      	
     
      }
      
      
      // incrementa lo start: se dopo x postInvalidate (circa 500 ms) che non vengono ricevuti nuovi punti 
      // imposta il risultato a null e i punti non vengono più disegnati a schermo
      start++;
      
      if(start>6) {
    	  risultato = null;
    	  
    	  ema = new ResultEMA(0.5f);
    	  
    	  beta=0;
    	  r=7;
      	}

      } 
      
    

      // dopo aver finito setta un tempo di ANIMATION_DELAY e richiama l'onDraw all'interno della cornice
    
     postInvalidateDelayed(80L,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
    

  

public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }
  
  
  
  

  //i possibili result point vengono inviati !!
  //il punto 0 è in basso a sinistra,
  //il punto 1 è in alto a sinistra
  //il punto 2 è in alto a destra
  public void addPossibleResultPoint(ResultPoint point[]) {
	  
	  //risultato = point;
	  
	   risultato = ema.media(point);
	   
	    angle = (int) Math.toDegrees(Math.atan2(risultato[2].getY() - risultato[1].getY(), risultato[2].getX() - risultato[1].getX()));


	  
   
   
   //reimposto il contatore dei refresh senza punti a 0
   start=0;
   
   
   
   // utile per verificare la velocità di detection
   if(tempo != 0){
	   
	   Log.d(VIEW_LOG_TAG, "Punti trovati in " + (System.currentTimeMillis() - tempo) + " ms");
	   tempo = 0;
   }
   
   tempo =  System.currentTimeMillis();
   
   
   
  
   
  }
  
  public  double distance(ResultPoint pattern1, ResultPoint pattern2, float scaleX , float scaleY , double lenght) {
	    float xDiff = (pattern1.getX() - pattern2.getX())*scaleX;
	    float yDiff = (pattern1.getY() - pattern2.getY())*scaleY;
	    
	    return  ( Math.sqrt((double) (xDiff * xDiff + yDiff * yDiff))/lenght);
	  }
  
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
	  
	  if(risultato != null ){
		  
		  int eventX = (int) event.getX();
		  int eventY = (int) event.getY();  
		  
		  
		  if(event.getAction()==MotionEvent.ACTION_UP){
			  
			  if(spostati){
				  
				 float deltaX = event.getX() - puntiDinamici.get(2).x;
				 float deltaY = event.getY() - puntiDinamici.get(2).y;
				 float distanza = (float) (Math.sqrt((deltaX * deltaX +  deltaY *  deltaY)));
				 
				 if(distanza >= 100){
					 
					 if(beta==23) {
						 beta =  0;
						 r=7;
					 }
					 else {
						 beta= 23;
						 r=7.6;
					 }
					
				 }
				 
				 
				 spostati = false;
				  
			  }else
			  
			  for(int i = 0; i < 2; i++){
					
					if( eventX >= (puntiDinamici.get(i).x - 50) && eventX <= (puntiDinamici.get(i).x + 50) && eventY >= (puntiDinamici.get(i).y - 50) && eventY <= (puntiDinamici.get(i).y + 50) ){
						
						new  DoTask(i).execute();
						
						
						
					}
					
				}
				
			  
			  
			  
		  }
		  
		  
		  if(event.getAction()==MotionEvent.ACTION_DOWN){
			  
			  if( eventX >= (puntiDinamici.get(2).x - 50) && eventX <= (puntiDinamici.get(2).x + 50) && eventY >= (puntiDinamici.get(2).y - 50) && eventY <= (puntiDinamici.get(2).y + 50) ){
				  
				  spostati = true;
				  
			  }
			  
		  }
		  
		
		
		
		  
		  
	  }
	
	  
	  
	return true;
	  
  }
  
  
  public class DoTask extends AsyncTask<Void, Void, Void> {
	  private int i;

		 public DoTask(int i) {
		// TODO Auto-generated constructor stub
			 this.i = i+1;
	}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
				
			HttpClient client = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("http://www.sportincontro.it/test/qrcode.php?change="+i);
			
				HttpResponse response;
				try {
					response = client.execute(httpPost);
				
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					
					
					if(puntiDinamiciColor[i-1] == getResources().getColor(R.color.result_points))  
						
						puntiDinamiciColor[i-1] =  getResources().getColor(R.color.viewfinder_laser);
					
					else   
						puntiDinamiciColor[i-1] = getResources().getColor(R.color.result_points);
					
					/*HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) 
					{
						builder.append(line);
						
					} //end while
					dati = builder.toString();*/
					
				} 
				
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	       
				
			
		 
			

			return null;
		}

		protected void onPostExecute(Void unused) {
			
			
		
		}
		
		
	}

}
