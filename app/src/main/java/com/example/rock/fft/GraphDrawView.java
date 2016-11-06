package com.example.rock.fft;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Rock on 2016.11.05.
 */

public class GraphDrawView extends View {
    public double[] x,y;
    Paint magnitude_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint backline_paint  = new Paint();
    Paint clear_paint = new Paint();
    Paint text_paint = new Paint();
    private boolean isClear = true;
    public double[] origin_hz, origin_mag;
    public double max_mag, min_mag, max_hz, min_hz;
    public GraphDrawView(Context context) {
        super(context);
        init();
    }

    public GraphDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraphDrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init(){
        magnitude_paint.setColor(Color.RED);
        magnitude_paint.setStyle(Paint.Style.STROKE);
        magnitude_paint.setStrokeWidth(5);

        backline_paint.setColor(Color.BLUE);
        clear_paint.setColor(Color.WHITE);
        text_paint.setColor(Color.BLUE);
        text_paint.setTextSize(30);
        this.setBackgroundColor(Color.WHITE);

    }
//
//
//    normalize(normal_freq_array);
//    normalize(normal_mag_array);
    private static double[] normalize(double[] datas) {

        double maximo = 0;
        for (int k = 0; k < datas.length; k++) {
            if (Math.abs(datas[k]) > maximo) {
                maximo = Math.abs(datas[k]);
            }
        }

        for (int k = 0; k < datas.length; k++) {
            datas[k] = datas[k] / maximo;
        }
        return datas;
    }

    public double maxValue(double[] in_array){
        double max_val = 0;
        for(int i = 0 ; i < in_array.length ; i ++){
            if(in_array[i] > max_val){
                max_val = in_array[i];
            }
        }
        return max_val;
    }
    public double minValue(double[] in_array){
        double min_val = 0;
        for(int i = 0 ; i < in_array.length ; i ++){
            if(in_array[i] < min_val){
                min_val = in_array[i];
            }
        }
        return min_val;
    }
    public void setX(double[] in_x) {
        origin_hz = in_x;
        this.x = new double[in_x.length];
        try {
            System.arraycopy(origin_hz, 0, this.x, 0, origin_hz.length);
        }catch (Exception e){
            e.printStackTrace();
            Log.d("error","error");
        }
        normalize(x);
        max_hz = maxValue(origin_hz);
        min_hz = minValue(origin_hz);
    }

    public void setY(double[] in_y) {
        origin_mag = in_y;
        this.y = new double[in_y.length];
        try {
            System.arraycopy(origin_mag, 0, this.y, 0, origin_mag.length);
        }catch (Exception e){
            e.printStackTrace();
            Log.d("error","error");
        }
        normalize(this.y);
        max_mag = maxValue(origin_mag);
        min_mag = minValue(origin_mag);
    }
    public void drawValues(){
        isClear = false;
        this.invalidate();
    }
    public void drawClear(){
        x = null;
        y = null;
        isClear = true;
        this.invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isClear == true){
            canvas.drawRect(0,0,canvas.getWidth(), canvas.getHeight(), clear_paint);
        }
        else{
            float height = (canvas.getHeight()) * 0.8f  ;
            float interval_height = canvas.getHeight() * 0.1f;
            float widith = (canvas.getWidth() ) * 0.8f ;
            float interval_width= canvas.getWidth() * 0.1f;

            for(int i = 0; i < x.length; i ++){
                canvas.drawLine((float) x[i] * widith + interval_width,     (height + interval_height) ,
                                             (float) x[i] * widith + interval_width,     (float) (1 - y[i]) * height + interval_height,       magnitude_paint);
            }
            float interval_horizontal_line = height / 10;
            float interval_vertical_line = widith / 10;
            float interval_gap_mag = (float) (max_mag - min_mag) / 10;
            float interval_gap_hz = (float)(max_hz - min_hz) / 10;

            for(int interval = 0 ; interval <= 10 ; interval ++){
                canvas.drawLine(interval_width , interval * interval_horizontal_line + interval_height,
                                            interval_width + widith,   interval * interval_horizontal_line + interval_height,
                                            backline_paint);
                String tmp_mag_value = String.format("%.2f", interval_gap_mag * (10 - interval) / 1000);
                canvas.drawText(tmp_mag_value,interval_width - 80, interval*interval_horizontal_line + interval_height, text_paint);
            }
            canvas.drawText("Magnitude(dbFS)", interval_width - 20, interval_height - 40, text_paint);

            for(int interval = 0 ; interval <=10 ; interval ++){
                canvas.drawLine(interval * interval_vertical_line + interval_width , interval_height ,
                                            interval * interval_vertical_line + interval_width, height + interval_height, backline_paint);
                String tmp_hz_value = String.format("%.2f", interval_gap_hz * interval / 1000);
                canvas.drawText(tmp_hz_value,interval* interval_vertical_line + interval_width - 40, height + interval_height + 40, text_paint);

            }
            canvas.drawText("Frequency(KHz)", (canvas.getWidth()- interval_width) /2, height + interval_height + 80, text_paint);

        }

    }
}
