package com.abdulrahman.littlesnap.fragments;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class DImageView extends AppCompatImageView {
    public DImageView(Context context) {
        super(context);
    }

    public DImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private class Pen{
        Path pen ;
        Paint paint ;

        Pen(int c , float width){
            pen = new Path();
        }
    }
}
