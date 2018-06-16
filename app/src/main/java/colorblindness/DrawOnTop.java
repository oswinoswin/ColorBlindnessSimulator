package colorblindness;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class DrawOnTop extends View {
	Bitmap mBitmap;
	Paint mPaintBlack;
	Paint mPaintYellow;
	byte[] mYUVData;
	int[] mRGBData;
	int mImageWidth, mImageHeight;
	int[] mGrayHistogram;
	double[] mGrayCDF;
	int mState;
	
	static final int STATE_ORIGINAL = 0;
	static final int STATE_PROCESSED = 1;

    public DrawOnTop(Context context) {
        super(context);
        
        mPaintBlack = new Paint();
        mPaintBlack.setStyle(Paint.Style.FILL);
        mPaintBlack.setColor(Color.BLACK);
        mPaintBlack.setTextSize(25);
        
        mPaintYellow = new Paint();
        mPaintYellow.setStyle(Paint.Style.FILL);
        mPaintYellow.setColor(Color.YELLOW);
        mPaintYellow.setTextSize(25);
        
        mBitmap = null;
        mYUVData = null;
        mRGBData = null;
        mGrayHistogram = new int[256];
        mGrayCDF = new double[256];
        mState = STATE_ORIGINAL;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null)
        {
			int canvasWidth = canvas.getWidth();
			int canvasHeight = canvas.getHeight();
			int origImageWidth = 320;
			int origImageHeight = 240;
			int newImageWidth = (canvasHeight * origImageWidth) / origImageHeight;
			int marginWidth = Math.max((canvasWidth - newImageWidth)/2, 0);

			// Convert from YUV to RGB
        	if (mState == STATE_ORIGINAL)
        		decodeYUV420RGB(mRGBData, mYUVData, mImageWidth, mImageHeight);
        	else
        		decodeYUV420RGBAndChangeColors(mRGBData, mYUVData, mImageWidth, mImageHeight);
        	
        	// Draw bitmap
        	mBitmap.setPixels(mRGBData, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight);
        	Rect src = new Rect(0, 0, mImageWidth, mImageHeight);
        	Rect dst = new Rect(marginWidth, 0, canvasWidth-marginWidth, canvasHeight);
        	canvas.drawBitmap(mBitmap, src, dst, mPaintBlack);
        	
        	// Draw black borders        	        	
        	canvas.drawRect(0, 0, marginWidth, canvasHeight, mPaintBlack);
        	canvas.drawRect(canvasWidth - marginWidth, 0, 
        			canvasWidth, canvasHeight, mPaintBlack);
        	
        	// Draw label
        	String imageStateStr;
        	if (mState == STATE_ORIGINAL) 
        		imageStateStr = "Original Image";
        	else 
        		imageStateStr = "Processed Image";
        	canvas.drawText(imageStateStr, marginWidth+10-1, 30-1, mPaintBlack);
        	canvas.drawText(imageStateStr, marginWidth+10+1, 30-1, mPaintBlack);
        	canvas.drawText(imageStateStr, marginWidth+10+1, 30+1, mPaintBlack);
        	canvas.drawText(imageStateStr, marginWidth+10-1, 30+1, mPaintBlack);
        	canvas.drawText(imageStateStr, marginWidth+10, 30, mPaintYellow);
        	
        } // end if statement
        
        super.onDraw(canvas);
        
    } // end onDraw method

	private int calculateColors(int r, int g, int b){
    	int rgb;
        if (r < 0) r = 0; else if (r > 262143) r = 262143;
        if (g < 0) g = 0; else if (g > 262143) g = 262143;
        if (b < 0) b = 0; else if (b > 262143) b = 262143;

      /*  float R[] = {1.0f, 0.0f, 0.0f};
        float G[] = {0.0f, 1.0f, 0.0f};
        float B[] = {0.0f, 0.0f, 1.0f};
*/
       /* int R[] = {100, 0, 0};
        int G[] = {0, 100, 0};
        int B[] = {0, 0, 100};

*//*
        double R[] = {0.8, 0.2, 0};
        double G[] = {0.25833, 0.74167, 0};
        double B[] = {0, 0.14167, 0.85833};*/

        /*double R[] = {0.625, 0.375, 0};
        double G[] = {0.7, 0.3, 0};
        double B[] = {0, 0.3, 0.7};*/

        double R[] = {0.8, 0.2, 0};
        double G[] = {0.25833, 0.74167, 0};
        double B[] = {0, 0.14167, 0.85833};

        double fr, fg, fb;

        fr = r * R[0]  + g *R[1]  + b * R[2];
        fg = r * G[0]  + g * G[1]  + b *G[2];
        fb = r * B[0]  + g * B[1]  + b *B[2];

        r = (int) fr;
        g = (int) fg;
        b = (int) fb;


    	rgb = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    	return rgb;
	}



    private void decodeYUV420RGBAndChangeColors(int[] rgb, byte[] yuv420sp, int width, int height) {
        // Convert YUV to RGB
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);



                rgb[yp] = calculateColors(r, g, b);
            }
        }
    }

    private void decodeYUV420RGB(int[] rgb, byte[] yuv420sp, int width, int height) {
    	// Convert YUV to RGB
    	final int frameSize = width * height;
    	for (int j = 0, yp = 0; j < height; j++) {
    		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
    		for (int i = 0; i < width; i++, yp++) {
    			int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			if (y < 0) y = 0;
    			if ((i & 1) == 0) {
    				v = (0xff & yuv420sp[uvp++]) - 128;
    				u = (0xff & yuv420sp[uvp++]) - 128;
    			}
    			
    			int y1192 = 1192 * y;
    			int r = (y1192 + 1634 * v);
    			int g = (y1192 - 833 * v - 400 * u);
    			int b = (y1192 + 2066 * u);
    			
    			if (r < 0) r = 0; else if (r > 262143) r = 262143;
    			if (g < 0) g = 0; else if (g > 262143) g = 262143;
    			if (b < 0) b = 0; else if (b > 262143) b = 262143;
    			
    			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    		}
    	}
    }
    
    private void decodeYUV420RGBContrastEnhance(int[] rgb, byte[] yuv420sp, int width, int height) {
    	// Compute histogram for Y
    	final int frameSize = width * height;
    	int clipLimit = frameSize / 10;
    	for (int bin = 0; bin < 256; bin++)
    		mGrayHistogram[bin] = 0;
    	for (int j = 0, yp = 0; j < height; j++) {
    		for (int i = 0; i < width; i++, yp++) {
    			int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			if (y < 0) y = 0;
    			if (mGrayHistogram[y] < clipLimit)
    				mGrayHistogram[y]++;
    		}
    	}
    	double sumCDF = 0;
    	for (int bin = 0; bin < 256; bin++)
    	{
    		sumCDF += (double)mGrayHistogram[bin]/(double)frameSize;
    		mGrayCDF[bin] = sumCDF;
    	}
    	
    	// Convert YUV to RGB
    	for (int j = 0, yp = 0; j < height; j++) {
    		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
    		for (int i = 0; i < width; i++, yp++) {
    			int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			if (y < 0) y = 0;
    			if ((i & 1) == 0) {
    				v = (0xff & yuv420sp[uvp++]) - 128;
    				u = (0xff & yuv420sp[uvp++]) - 128;
    			}
    			y = (int)(mGrayCDF[y]*255 + 0.5);
    			
    			int y1192 = 1192 * y;
    			int r = (y1192 + 1634 * v);
    			int g = (y1192 - 833 * v - 400 * u);
    			int b = (y1192 + 2066 * u);
    			
    			//if (r < 0) r = 0; else if (r > 262143) r = 262143;


    			rgb[yp] = calculateColors(r, g, b);
//    			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    		}
    	}
    }}
