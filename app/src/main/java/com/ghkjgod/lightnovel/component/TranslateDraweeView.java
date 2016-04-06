package com.ghkjgod.lightnovel.component;

import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;

public class TranslateDraweeView extends SimpleDraweeView {

    public TranslateDraweeView(Context context) {
        super(context);
    }

    public TranslateDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TranslateDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TranslateDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
    }

    // looks like overwrite this method can fix this issue
// but still don't figure out why
    public void animateTransform(Matrix matrix) {
        invalidate();
    }
}