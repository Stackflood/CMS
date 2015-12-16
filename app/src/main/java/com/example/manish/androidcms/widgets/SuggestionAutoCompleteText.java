package com.example.manish.androidcms.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;

import com.example.manish.androidcms.util.SuggestionTokenizer;

import org.wordpress.persistentedittext.PersistentEditTextHelper;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
    PersistentEditTextHelper mPersistentEditTextHelper;

    public SuggestionAutoCompleteText(Context context) {
        super(context, null);
        init(context, null);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypefaceCache.setCustomTypeface(context, this, attrs);
        setTokenizer(new SuggestionTokenizer());
        setThreshold(1);
        mPersistentEditTextHelper = new PersistentEditTextHelper(context);
        // When TYPE_TEXT_FLAG_AUTO_COMPLETE is set, autocorrection is disabled.
        setRawInputType(getInputType() & ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE);
    }

    public PersistentEditTextHelper getAutoSaveTextHelper() {
        return mPersistentEditTextHelper;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().loadString(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().saveString(this);
    }
}
