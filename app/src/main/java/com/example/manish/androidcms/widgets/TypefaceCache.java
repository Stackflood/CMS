package com.example.manish.androidcms.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.example.manish.androidcms.R;

import java.util.Hashtable;

public class TypefaceCache {
    private static final int VARIATION_NORMAL = 0;
    private static final int VARIATION_LIGHT = 1;

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<>();

    public static Typeface getTypeface(Context context) {
        return getTypeface(context, Typeface.NORMAL, VARIATION_NORMAL);
    }
    private static Typeface getTypeface(Context context,
                                        int fontStyle,
                                        int variation) {
        if (context == null) {
            return null;
        }

        final String typefaceName;
        if (variation == VARIATION_LIGHT) {
            switch (fontStyle) {
                case Typeface.BOLD:
                    typefaceName = "OpenSans-LightBold.ttf";
                    break;
                case Typeface.ITALIC:
                    typefaceName = "OpenSans-LightItalic.ttf";
                    break;
                case Typeface.BOLD_ITALIC:
                    typefaceName = "OpenSans-LightBoldItalic.ttf";
                    break;
                default:
                    typefaceName = "OpenSans-Light.ttf";
                    break;
            }
        } else {
            switch (fontStyle) {
                case Typeface.BOLD:
                    typefaceName = "OpenSans-Bold.ttf";
                    break;
                case Typeface.ITALIC:
                    typefaceName = "OpenSans-Italic.ttf";
                    break;
                case Typeface.BOLD_ITALIC:
                    typefaceName = "OpenSans-BoldItalic.ttf";
                    break;
                default:
                    typefaceName = "OpenSans-Regular.ttf";
                    break;
            }
        }

        return getTypefaceForTypefaceName(context, typefaceName);
    }

    protected static Typeface getTypefaceForTypefaceName(Context context, String typefaceName) {
        if (!mTypefaceCache.containsKey(typefaceName)) {
            Typeface typeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/"
                    + typefaceName);
            if (typeface != null) {
                mTypefaceCache.put(typefaceName, typeface);
            }
        }

        return mTypefaceCache.get(typefaceName);
    }

    /*
     * sets the typeface for a TextView (or TextView descendant such as EditText or Button) based on
     * the passed attributes, defaults to normal typeface
     */
    protected static void setCustomTypeface(Context context, TextView view, AttributeSet attrs) {
        if (context == null || view == null)
            return;

        // skip at design-time
        if (view.isInEditMode())
            return;

        // read custom fontVariation from attributes, default to normal
        int variation = TypefaceCache.VARIATION_NORMAL;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.WPTextView, 0, 0);

            if (a != null) {
                try {
                    variation = a.getInteger(R.styleable.WPTextView_fontVariation,
                            TypefaceCache.VARIATION_NORMAL);
                } finally {
                    a.recycle();
                }
            }
        }

        // determine the font style from the existing typeface
        final int fontStyle;
        if (view.getTypeface() != null) {
            boolean isBold = view.getTypeface().isBold();
            boolean isItalic = view.getTypeface().isItalic();
            if (isBold && isItalic) {
                fontStyle = Typeface.BOLD_ITALIC;
            } else if (isBold) {
                fontStyle = Typeface.BOLD;
            } else if (isItalic) {
                fontStyle = Typeface.ITALIC;
            } else {
                fontStyle = Typeface.NORMAL;
            }
        } else {
            fontStyle = Typeface.NORMAL;
        }

        Typeface typeface = getTypeface(context, fontStyle, variation);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }
}
