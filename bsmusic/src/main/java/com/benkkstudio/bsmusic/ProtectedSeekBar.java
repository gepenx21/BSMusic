package com.benkkstudio.bsmusic;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatSeekBar;

public class ProtectedSeekBar extends AppCompatSeekBar {

    private Drawable mThumb;

    public ProtectedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ProtectedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProtectedSeekBar(Context context) {
        super(context);
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        mThumb = thumb;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if( event.getX() < mThumb.getBounds().left ||
                    event.getX() > mThumb.getBounds().right ||
                    event.getY() > mThumb.getBounds().bottom ||
                    event.getY() < mThumb.getBounds().top) {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }
}