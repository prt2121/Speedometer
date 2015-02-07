package com.prt2121.speedo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class Speedometer extends View implements SpeedChangeListener {

    private static final float DEFAULT_MAX_SPEED = 100;

    private static final int DEFAULT_SIZE = 300;

    private static final int READING_BOTTOM_MARGIN = 10;

    private final RectF mOval = new RectF();

    // Speedometer internal state
    private float mMaxSpeed;

    private float mCurrentSpeed;

    // Scale drawing tools
    // paint of the colored area from -180 up to the current speed
    private Paint mOnPaint;

    // the grey or default paint
    private Paint mOffPaint;

    // paint for number on the scale
    private Paint mScalePaint;

    // paint for the reading digit
    private Paint mReadingPaint;

    private Path mOnPath;

    private Path mOffPath;

    private int mOnColor = Color.argb(255, 0xff, 0xeb, 0x3b);

    private int mOffColor = Color.argb(0x40, 0x00, 0x00, 0x00);

    private int mScaleColor = Color.argb(255, 0x21, 0x96, 0xF3);

    private float mScaleSize = 12f;

    // the max number of scale's number drawn on the curve.
    private int numberOfSteps = 10;

    private float mReadingSize = 50f;

    private float mCenterX;

    private float mCenterY;

    private float mRadius;

    private double mHalfCircumference;

    private float mDistanceIncrement;

    private float mScaleIncrement;

    public Speedometer(Context context) {
        super(context);
    }

    public Speedometer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.Speedometer, 0, 0);
        try {
            mMaxSpeed = a.getFloat(R.styleable.Speedometer_spd_maxSpeed, DEFAULT_MAX_SPEED);
            mCurrentSpeed = a.getFloat(R.styleable.Speedometer_spd_currentSpeed, mCurrentSpeed);
            mOnColor = a.getColor(R.styleable.Speedometer_spd_onColor, mOnColor);
            mOffColor = a.getColor(R.styleable.Speedometer_spd_offColor, mOffColor);
            mScaleColor = a.getColor(R.styleable.Speedometer_spd_scaleColor, mScaleColor);
            mScaleSize = dpToPixel(a.getDimension(R.styleable.Speedometer_spd_scaleTextSize, mScaleSize));
            mReadingSize = dpToPixel(a.getDimension(R.styleable.Speedometer_spd_readingTextSize, mReadingSize));
            numberOfSteps = a.getInt(R.styleable.Speedometer_spd_maxNumberScaleSteps, numberOfSteps);
        } finally {
            a.recycle();
        }
        initDrawingTools();
    }

    public float getCurrentSpeed() {
        return mCurrentSpeed;
    }

    public void setCurrentSpeed(float currentSpeed) {
        if (currentSpeed > this.mMaxSpeed) {
            this.mCurrentSpeed = mMaxSpeed;
        } else if (currentSpeed < 0) {
            this.mCurrentSpeed = 0;
        } else {
            this.mCurrentSpeed = currentSpeed;
        }
        this.invalidate();
    }

    public float getMaxSpeed() {
        return mMaxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        if (maxSpeed > 0) {
            mMaxSpeed = maxSpeed;
            this.invalidate();
        }
    }

    @Override
    public void onSpeedChanged(float newSpeedValue) {
        this.setCurrentSpeed(newSpeedValue);
        this.invalidate();
    }

    private int dpToPixel(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics());
    }

    private void initDrawingTools() {
        mOnPaint = new Paint();
        mOnPaint.setStyle(Paint.Style.STROKE);
        mOnPaint.setColor(mOnColor);
        mOnPaint.setStrokeWidth(35f);
        mOnPaint.setAntiAlias(true);

        mOffPaint = new Paint(mOnPaint);
        mOffPaint.setColor(mOffColor);
        mOffPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mScalePaint = new Paint(mOffPaint);
        mScalePaint.setStrokeWidth(2f);
        mScalePaint.setTextSize(mScaleSize);
        mScalePaint.setColor(mScaleColor);

        mReadingPaint = new Paint(mScalePaint);
        mReadingPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mReadingPaint.setTextSize(mReadingSize);
        mReadingPaint.setTypeface(Typeface.SANS_SERIF);
        mReadingPaint.setColor(Color.BLACK);

        mOnPath = new Path();
        mOffPath = new Path();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        // TODO make 1.4 customizable
        mRadius = (float) (height / 1.4);
        mOval.set(mCenterX - mRadius,
                mCenterY - mRadius,
                mCenterX + mRadius,
                mCenterY + mRadius);

        mHalfCircumference = mRadius * Math.PI;
        mDistanceIncrement = (float) mHalfCircumference / numberOfSteps;
        mScaleIncrement = mMaxSpeed / numberOfSteps;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        // set the height to be half of the width
        int height, width;
        if (chosenWidth / 2 > chosenHeight) {
            height = chosenHeight;
            width = height * 2;
        } else {
            width = chosenWidth;
            height = width / 2;
        }

        mCenterX = width / 2;
        mCenterY = height;
        setMeasuredDimension(width, height);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // mode == MeasureSpec.UNSPECIFIED
            return DEFAULT_SIZE;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawScaleBackground(canvas);
        drawOn(canvas);
        drawScaleNumber(canvas);
        drawReading(canvas);
    }

    /**
     * Draws background and the OFF curve
     */
    private void drawScaleBackground(Canvas canvas) {
        //canvas.drawColor(getResources().getColor(R.color.primary));
        canvas.drawARGB(255, 255, 255, 255); // TODO make the background customizable
        mOffPath.reset();
        for (int i = -180; i < 0; i += 4) {
            mOffPath.addArc(mOval, i, 4f);
        }
        canvas.drawPath(mOffPath, mOffPaint);
    }

    private void drawOn(Canvas canvas) {
        mOnPath.reset();
        for (int i = -180; i < (mCurrentSpeed / mMaxSpeed) * 180 - 180; i += 4) {
            mOnPath.addArc(mOval, i, 4f);
        }
        canvas.drawPath(mOnPath, mOnPaint);
    }

    private void drawScaleNumber(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(-180, mCenterX, mCenterY);
        Path circle = new Path();


        for (int i = 1; i <= numberOfSteps; i++) {
            circle.addCircle(mCenterX, mCenterY, mRadius, Path.Direction.CW);
            canvas.drawTextOnPath(String.format("%d", (int) (i * mScaleIncrement)),
                    circle,
                    (float) i * mDistanceIncrement,
                    -30f,
                    mScalePaint);
        }
        canvas.restore();
    }

    private void drawReading(Canvas canvas) {
        Path path = new Path();
        String message = String.format("%d", (int) this.mCurrentSpeed);
        float[] widths = new float[message.length()];
        mReadingPaint.getTextWidths(message, widths);

        float advance = 0;
        for (double width : widths) {
            advance += width;
        }
        path.moveTo(mCenterX - advance / 2, mCenterY - READING_BOTTOM_MARGIN);
        path.lineTo(mCenterX + advance / 2, mCenterY - READING_BOTTOM_MARGIN);
        canvas.drawTextOnPath(message, path, 0f, 0f, mReadingPaint);
    }

}
