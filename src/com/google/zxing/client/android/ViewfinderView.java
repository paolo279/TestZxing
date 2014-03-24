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
import android.graphics.Rect;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

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
  private final int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;
  
  
  long start = 0;
  long tempo = 0;
  boolean prova;

  private ResultPoint[] risultato;
  private ResultPoint[] salvarisultato;
  
  private  List<ResultPoint> primoPunto;
  private  List<ResultPoint> secondoPunto;
  private  List<ResultPoint> terzoPunto;
  

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
    salvarisultato = null;
    
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

      
      //veniva disegnato in laser al centro della cornice
      
      // Draw a red "laser scanner" line through the middle to show decoding is active
      //     paint.setColor(laserColor);
      //paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      //scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
      //int middle = frame.height() / 2 + frame.top;
     // canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
      
      
      
      Rect previewFrame = cameraManager.getFramingRectInPreview();
      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();

      
     
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
      }
      
      
      // incrementa lo start: se dopo x postInvalidate (circa 300 ms) che non vengono ricevuti nuovi punti 
      // imposta il risultato a null e i punti non vengono più disegnati a schermo
      start++;
      
      if(start>6)  risultato = null;
      
     
 		  
      
      
    	  
      //vecchio metodo commentato 
      
   // List<ResultPoint> currentPossible = possibleResultPoints;
      //List<ResultPoint> currentLast = lastPossibleResultPoints;
      
      
     /*   possibleResultPoints = new ArrayList<ResultPoint>(5);
     /   lastPossibleResultPoints = currentPossible;
        
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);

       
       
        
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              POINT_SIZE, paint);
         //   canvas.drawLine(frameLeft + (int) (point.getX() * scaleX) - 50,frameTop + (int) (point.getY() * scaleY) - 50, frameLeft + (int) (point.getX() * scaleX) + 50, frameTop + (int) (point.getY() * scaleY) + 50, paint);
          }
        }*/
 
    	  
      } 
      
 
        
       /* synchronized (currentLast) {
          float radius = POINT_SIZE;  // / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              radius, paint);
          		}
        	}
   */
    
    
    
    

      // dopo aver finito setta un tempo di ANIMATION_DELAY e richiama l'onDraw all'interno della cornice
    
     postInvalidateDelayed(40L,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
    
  
//  }
  
  
  

  

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
  
  
  
  public void drawPunto(ResultPoint[] punti) {
	  paint.setStrokeWidth(15.0f);
	    risultato = punti;
	    invalidate();
	  }
  
  

  //i possibili result point vengono inviati !!
  public void addPossibleResultPoint(ResultPoint point[]) {
	  
   risultato = point;
   start=0;
   
   
   if(tempo != 0){
	   
	   Log.d(VIEW_LOG_TAG, "Punti trovati in " + (System.currentTimeMillis() - tempo) + " ms");
	   tempo = 0;
   }
   
   tempo =  System.currentTimeMillis();
   
   
   
  
   
   //possibleResultPoints.add(point[0]);
   //possibleResultPoints.add(point[1]);
   //possibleResultPoints.add(point[2]);

   
	/*List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
    	
  
    	points.add(point);

    
      int size = points.size();
     
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }*/
  }

}
