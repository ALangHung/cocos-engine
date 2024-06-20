/****************************************************************************
Copyright (c) 2010-2012 cocos2d-x.org
Copyright (c) 2013-2016 Chukong Technologies Inc.
Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package com.cocos.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class CocosEditBoxActivity extends Activity {

    public static final String TAG = "CocosEditBoxActivity";

    // a color of dark green, was used for confirm button background
    private static final int DARK_GREEN = Color.parseColor("#1fa014");
    private static final int DARK_GREEN_PRESS = Color.parseColor("#008e26");

    private static CocosEditBoxActivity sThis = null;
    private Cocos2dxEditText mEditText = null;
    private Button mButton = null;
    private String mButtonTitle = null;
    private boolean mConfirmHold = true;
    private int mEditTextID = 1;
    private int mButtonLayoutID = 2;
    private static float keyboardHeightRate = 0;
    private float boxY = 0;
    private float boxX = 0;
    private RelativeLayout myLayout;
    public boolean perpareToOpenKeyboard = false;

    /***************************************************************************************
     Inner class.
     **************************************************************************************/
    class Cocos2dxEditText extends EditText {
        private final String TAG = "Cocos2dxEditBox";
        private boolean mIsMultiLine = false;
        private TextWatcher mTextWatcher = null;

        private int mScreenHeight;
        private boolean mCheckKeyboardShowNormally = false;



        public  Cocos2dxEditText(Activity context){
            super(context);
            mScreenHeight = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).
                    getDefaultDisplay().getHeight();

            mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Pass text to c++.
                    CocosEditBoxActivity.this.onKeyboardInput(s.toString());
                }
            };
            registKeyboardVisible();
        }

        /***************************************************************************************
         Public functions.
         **************************************************************************************/

        public void show(String defaultValue, int maxLength, boolean isMultiline, boolean confirmHold, String confirmType, String inputType, int x, int y, int width, int height, float uvX, float uvY, float uvWidth, float uvHeight, String fontPath, float fontSize, int fontColor, int textHorizontalAlignment, int textVerticalAlignment) {
            mIsMultiLine = isMultiline;

            Typeface typeface = TypefaceCache.getTypeface(getContext(), fontPath);
            this.setTypeface(typeface);

            this.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength) });
            this.setTextColor(fontColor);
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

            int gravity = 0;
            switch (textHorizontalAlignment) {
                case 0:
                    gravity = Gravity.LEFT;
                    break;
                case 1:
                    gravity = Gravity.CENTER_HORIZONTAL;
                    break;
                case 2:
                    gravity = Gravity.RIGHT;
                    break;
            }
            switch (textVerticalAlignment) {
                case 0:
                    gravity |= Gravity.TOP;
                    break;
                case 1:
                    gravity |= Gravity.CENTER_VERTICAL;
                    break;
                case 2:
                    gravity |= Gravity.BOTTOM;
                    break;
            }
            this.setGravity(gravity);

            boxX = uvX;
            boxY = uvY;
            this.setText(defaultValue);
            if (this.getText().length() >= defaultValue.length()) {
                this.setSelection(defaultValue.length());
            } else {
                this.setSelection(this.getText().length());
            }
            this.setConfirmType(confirmType);
            this.setInputType(inputType, mIsMultiLine);
            this.setVisibility(View.VISIBLE);

            // Open soft keyboard manually. Should request focus to open soft keyboard.
            this.requestFocus();

            this.addListeners();
        }

        public void hide() {
            mEditText.setVisibility(View.INVISIBLE);
            this.removeListeners();
        }

        /***************************************************************************************
         Private functions.
         **************************************************************************************/

        private void setConfirmType(final String confirmType) {
            if (confirmType.contentEquals("done")) {
                this.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                mButtonTitle = getResources().getString(R.string.done);
            } else if (confirmType.contentEquals("next")) {
                this.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                mButtonTitle = getResources().getString(R.string.next);
            } else if (confirmType.contentEquals("search")) {
                this.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                mButtonTitle = getResources().getString(R.string.search);
            } else if (confirmType.contentEquals("go")) {
                this.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                mButtonTitle = getResources().getString(R.string.go);
            } else if (confirmType.contentEquals("send")) {
                this.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                mButtonTitle = getResources().getString(R.string.send);
            } else{
                mButtonTitle = null;
                Log.e(TAG, "unknown confirm type " + confirmType);
            }
        }

        private void setInputType(final String inputType, boolean isMultiLine){
            mCheckKeyboardShowNormally = false;
            if (inputType.contentEquals("text")) {
                if (isMultiLine)
                    this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                else
                    this.setInputType(InputType.TYPE_CLASS_TEXT);
            }
            else if (inputType.contentEquals("email"))
                this.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            else if (inputType.contentEquals("number"))
                this.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            else if (inputType.contentEquals("phone"))
                this.setInputType(InputType.TYPE_CLASS_PHONE);
            else if (inputType.contentEquals("password")) {
                if (Build.BRAND.equalsIgnoreCase("oppo")) {
                    mCheckKeyboardShowNormally = true;
                }
                this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            else
                Log.e(TAG, "unknown input type " + inputType);
        }

        private void addListeners() {

            this.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (! mIsMultiLine) {
                        CocosEditBoxActivity.this.hide();
                    }

                    return false; // pass on to other listeners.
                }
            });


            this.addTextChangedListener(mTextWatcher);
        }

        private void removeListeners() {
            this.setOnEditorActionListener(null);
            this.removeTextChangedListener(mTextWatcher);
        }

        private boolean isSystemAdjustUIWhenPopKeyboard(int bottom) {
            int bottomOffset = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bottomOffset = getWindow().getDecorView().getRootWindowInsets().getSystemWindowInsetBottom();
            }
            // view will be scrolled to the target position by system,
            if (Math.abs(bottom - bottomOffset) < 10) {
                return true;
            }
            return false;
        }

        private void registKeyboardVisible() {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Rect r = new Rect();
                    getWindowVisibleDisplayFrame(r);
                    int heightDiff = getRootView().getHeight() - (r.bottom - r.top);
                    Log.i(TAG, "heightDiff: " + heightDiff);
                    Log.i(TAG, "mScreenHeight: " + mScreenHeight);
                    if (heightDiff > mScreenHeight/4) {
                        Log.i(TAG, "onGlobalLayout true");

                        if (CocosEditBoxActivity.this.perpareToOpenKeyboard) {
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                            int screenWidth = displayMetrics.widthPixels;
                            int screenHeight = displayMetrics.heightPixels;
                            Log.i(TAG, "screenWidth: " + screenWidth);
                            Log.i(TAG, "screenHeight: " + screenHeight);
                            Log.i(TAG, "heightDiff: " + heightDiff);

                            keyboardHeightRate = (float) heightDiff / screenHeight;

                            Log.i(TAG, "heightDiffDiff: " + screenHeight * (boxY - keyboardHeightRate));
                            if (boxY < keyboardHeightRate) {
                                onKeyboardHeightRateChange(screenHeight * (boxY - keyboardHeightRate));
                            }else {
                                myLayout.setTranslationY(screenHeight * (keyboardHeightRate - boxY));
                            }
                            myLayout.setTranslationX(boxX * screenWidth);
                        }

                        if (!isSystemAdjustUIWhenPopKeyboard(heightDiff)) {
                            getRootView().scrollTo(0, heightDiff);
                        }
                    } else {
                        Log.i(TAG, "onGlobalLayout false");

                        getRootView().scrollTo(0, 0);
                        onKeyboardHeightRateChange(0);
                        if (mCheckKeyboardShowNormally && CocosEditBoxActivity.this.perpareToOpenKeyboard) {
                            Toast.makeText(CocosEditBoxActivity.this, R.string.tip_disable_safe_input_type, Toast.LENGTH_SHORT).show();
                        }
                        if (!CocosEditBoxActivity.this.perpareToOpenKeyboard) {
                            CocosEditBoxActivity.this.hide();
                        }
                    }
                }
            });
        }
    }

    private void onKeyboardHeightRateChange(float keyboardHeight) {
        Log.i(TAG, "onKeyboardHeightRateChange keyboardHeight: " + keyboardHeight);
        Intent intent = new Intent("com.example.ACTION_DATA_CHANGED");
        intent.putExtra("keyboardHeightRate", keyboardHeight);
        sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        CocosEditBoxActivity.sThis = this;

        ViewGroup.LayoutParams frameLayoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        RelativeLayout frameLayout = new RelativeLayout(this);
        frameLayout.setLayoutParams(frameLayoutParams);
        frameLayout.setClickable(true);
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CocosEditBoxActivity.this.hide();
            }
        });
        setContentView(frameLayout);

        this.addItems(frameLayout);

        Intent intent = getIntent();
        Bundle extras = null;
        if (null != intent) {
            extras = intent.getExtras();
        }
        if(extras == null){
            show("",
                10,
                false,
                false,
                "done",
                "text",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "",
                20,
                0,
                0,
                0);
        } else {
            show(extras.getString("defaultValue"),
                extras.getInt("maxLength"),
                extras.getBoolean("isMultiline"),
                extras.getBoolean("confirmHold"),
                extras.getString("confirmType"),
                extras.getString("inputType"),
                extras.getInt("x"),
                extras.getInt("y"),
                extras.getInt("width"),
                extras.getInt("height"),
                extras.getFloat("uvX"),
                extras.getFloat("uvY"),
                extras.getFloat("uvWidth"),
                extras.getFloat("uvHeight"),
                extras.getString("fontPath"),
                extras.getFloat("fontSize"),
                extras.getInt("fontColor"),
                extras.getInt("textHorizontalAlignment"),
                extras.getInt("textVerticalAlignment"));
        }
    }

    /***************************************************************************************
     Public functions.
     **************************************************************************************/

    /***************************************************************************************
     Private functions.
     **************************************************************************************/
    private void addItems(RelativeLayout layout) {
        myLayout = new RelativeLayout(this);
        myLayout.setBackgroundColor(Color.argb(0, 244, 244, 244));

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layout.addView(myLayout, layoutParams);

        this.addEditText(myLayout);
        this.addButton(myLayout);
    }

    private int dpToPixel(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        return px;
    }
    private void addEditText(RelativeLayout layout) {
        mEditText = new Cocos2dxEditText(this);
        mEditText.setVisibility(View.VISIBLE);
        mEditText.setGravity(Gravity.CENTER_VERTICAL);
//        mEditText.setBackground(getRoundRectShape(18, Color.WHITE, Color.WHITE));
        mEditText.setBackground(getRoundRectShape(18, Color.TRANSPARENT, Color.TRANSPARENT));
//        mEditText.setBackground(getRoundRectShape(0, Color.YELLOW, Color.YELLOW));
        mEditText.setId(mEditTextID);
//        int bottomPadding = dpToPixel(4);
//        int leftPadding = dpToPixel(3);
//        mEditText.setPadding(leftPadding,bottomPadding,leftPadding,bottomPadding);
        mEditText.setPadding(0, 0, 0, 0);

        int viewWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        int viewHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        Intent intent = getIntent();
        Bundle extras = null;
        if (null != intent) {
            extras = intent.getExtras();
        }
        if (extras != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            float uvWidth = extras.getFloat("uvWidth");
            float uvHeight = extras.getFloat("uvHeight");
            viewWidth = (int) (screenWidth * uvWidth);
            viewHeight = (int) (screenHeight * uvHeight);
        }
        RelativeLayout.LayoutParams editParams = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        editParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        editParams.addRule(RelativeLayout.LEFT_OF, mButtonLayoutID);
        editParams.addRule(RelativeLayout.ALIGN_LEFT);
//        int bottomMargin = dpToPixel(5);
//        int leftMargin = dpToPixel(4);
//        editParams.setMargins(leftMargin, bottomMargin, bottomMargin, bottomMargin);
        editParams.setMargins(0, 0, 0, 0);
        layout.addView(mEditText, editParams);
    }

    private void addButton(RelativeLayout layout) {
        mButton = new Button(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mButton.setTextColor(Color.TRANSPARENT);
        mButton.setTextSize(16);
        mButton.setBackground(getRoundRectShape(18, Color.TRANSPARENT, Color.TRANSPARENT));
        int paddingLeft = dpToPixel(5);
        mButton.setPadding(paddingLeft,0,paddingLeft,0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mEditTextID);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mEditTextID);
        layoutParams.rightMargin = dpToPixel(4);
        layout.addView(mButton, layoutParams);
        mButton.setGravity(Gravity.CENTER);
        mButton.setId(mButtonLayoutID);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CocosEditBoxActivity.this.onKeyboardConfirm(mEditText.getText().toString());

                if (!CocosEditBoxActivity.this.mConfirmHold)
                    CocosEditBoxActivity.this.hide();
            }
        });
    }

    private Drawable getRoundRectShape(int radius, int normalColor, int pressColor) {
        float[] outerRadii = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
        RoundRectShape roundRectShape = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable shapeDrawableNormal = new ShapeDrawable();
        shapeDrawableNormal.setShape(roundRectShape);
        shapeDrawableNormal.getPaint().setStyle(Paint.Style.FILL);
        shapeDrawableNormal.getPaint().setColor(normalColor);
        ShapeDrawable shapeDrawablePress = new ShapeDrawable();
        shapeDrawablePress.setShape(roundRectShape);
        shapeDrawablePress.getPaint().setStyle(Paint.Style.FILL);
        shapeDrawablePress.getPaint().setColor(pressColor);
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, shapeDrawablePress);
        drawable.addState(new int[]{}, shapeDrawableNormal);
        return drawable;
    }


    private void hide() {
        Utils.hideVirtualButton();
        this.closeKeyboard();
        finish();
    }

    public void show(String defaultValue, int maxLength, boolean isMultiline, boolean confirmHold, String confirmType, String inputType, int x, int y, int width, int height, float uvX, float uvY, float uvWidth, float uvHeight, String fontPath, float fontSize, int fontColor, int textHorizontalAlignment, int textVerticalAlignment) {
        Log.i(TAG, "defaultValue: " + defaultValue);
        Log.i(TAG, "maxLength: " + maxLength);
        Log.i(TAG, "isMultiline: " + isMultiline);
        Log.i(TAG, "confirmHold: " + confirmHold);
        Log.i(TAG, "confirmType: " + confirmType);
        Log.i(TAG, "inputType: " + inputType);
        Log.i(TAG, "x: " + x);
        Log.i(TAG, "y: " + y);
        Log.i(TAG, "width: " + width);
        Log.i(TAG, "height: " + height);
        Log.i(TAG, "uvX: " + uvX);
        Log.i(TAG, "uvY: " + uvY);
        Log.i(TAG, "uvWidth: " + uvWidth);
        Log.i(TAG, "uvHeight: " + uvHeight);
        Log.i(TAG, "fontPath: " + fontPath);
        Log.i(TAG, "fontSize: " + fontSize);
        Log.i(TAG, "fontColor: " + fontColor);
        Log.i(TAG, "textHorizontalAlignment: " + textHorizontalAlignment);
        Log.i(TAG, "textVerticalAlignment: " + textVerticalAlignment);

        // ABGR to ARGB conversion
        int alpha = 0xFF;
        Log.i(TAG, "fontColor alpha: " + alpha);
        int blue = (fontColor >> 16) & 0xFF;
        Log.i(TAG, "fontColor blue: " + blue);
        int green = (fontColor >> 8) & 0xFF;
        Log.i(TAG, "fontColor green: " + green);
        int red = fontColor & 0xFF;
        Log.i(TAG, "fontColor red: " + red);

        // Create Color object
        int colorInt = (alpha << 24) | (red << 16) | (green << 8) | blue;
        Log.i(TAG, "fontColor colorInt: " + colorInt);

        mConfirmHold = confirmHold;
        mEditText.show(defaultValue, maxLength, isMultiline, confirmHold, confirmType, inputType, x, y, width, height, uvX, uvY, uvWidth, uvHeight, fontPath, fontSize, colorInt, textHorizontalAlignment, textVerticalAlignment);
        mButton.setText(mButtonTitle);
        if (TextUtils.isEmpty(mButtonTitle)) {
            mButton.setVisibility(View.INVISIBLE);
        } else {
            mButton.setVisibility(View.VISIBLE);
        }

        this.openKeyboard();
    }

    private void closeKeyboard() {
        Log.i(TAG, "closeKeyboard");
        this.perpareToOpenKeyboard = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);

        this.onKeyboardComplete(mEditText.getText().toString());
    }

    private void openKeyboard() {
        Log.i(TAG, "openKeyboard");
        this.perpareToOpenKeyboard = true;
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    /***************************************************************************************
     Functions invoked by CPP.
     **************************************************************************************/

    private static void showNative(String defaultValue, int maxLength, boolean isMultiline, boolean confirmHold, String confirmType, String inputType, int x, int y, int width, int height, float uvX, float uvY, float uvWidth, float uvHeight, String fontPath, float fontSize, int fontColor, int textHorizontalAlignment, int textVerticalAlignment) {
        GlobalObject.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(GlobalObject.getActivity(), CocosEditBoxActivity.class);
                i.putExtra("defaultValue", defaultValue);
                i.putExtra("maxLength", maxLength);
                i.putExtra("isMultiline", isMultiline);
                i.putExtra("confirmHold", confirmHold);
                i.putExtra("confirmType", confirmType);
                i.putExtra("inputType", inputType);
                i.putExtra("x", x);
                i.putExtra("y", y);
                i.putExtra("width", width);
                i.putExtra("height", height);
                i.putExtra("uvX", uvX);
                i.putExtra("uvY", uvY);
                i.putExtra("uvWidth", uvWidth);
                i.putExtra("uvHeight", uvHeight);
                i.putExtra("fontPath", fontPath);
                i.putExtra("fontSize", fontSize);
                i.putExtra("fontColor", fontColor);
                i.putExtra("textHorizontalAlignment", textHorizontalAlignment);
                i.putExtra("textVerticalAlignment", textVerticalAlignment);
                GlobalObject.getActivity().startActivity(i);
            }
        });
    }

    private static void hideNative() {
        if (null != CocosEditBoxActivity.sThis) {
            GlobalObject.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CocosEditBoxActivity.sThis.hide();
                }
            });
        }
    }

    /***************************************************************************************
     Native functions invoked by UI.
     **************************************************************************************/
    private void onKeyboardInput(String text) {
        CocosHelper.runOnGameThread(new Runnable() {
            @Override
            public void run() {
                CocosEditBoxActivity.onKeyboardInputNative(text);
            }
        });
    }

    private void onKeyboardComplete(String text) {
        CocosHelper.runOnGameThread(new Runnable() {
            @Override
            public void run() {
                CocosEditBoxActivity.onKeyboardCompleteNative(text);
            }
        });
    }

    private void onKeyboardConfirm(String text) {
        CocosHelper.runOnGameThread(new Runnable() {
            @Override
            public void run() {
                CocosEditBoxActivity.onKeyboardConfirmNative(text);
            }
        });
    }

    private static native void onKeyboardInputNative(String text);
    private static native void onKeyboardCompleteNative(String text);
    private static native void onKeyboardConfirmNative(String text);
}

class TypefaceCache {
    private static final String TAG = "TypefaceCache";
    private static Map<String, Typeface> typefaceCache = new HashMap<>();

    public static Typeface getTypeface(Context context, String fontPath) {
        if (typefaceCache.containsKey(fontPath)) {
            return typefaceCache.get(fontPath);
        } else {
            try {
                Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
                typefaceCache.put(fontPath, typeface);
                return typeface;
            } catch (Exception e) {
                Log.e(TAG, "Error loading font: " + fontPath, e);
                return Typeface.DEFAULT;
            }
        }
    }
}
