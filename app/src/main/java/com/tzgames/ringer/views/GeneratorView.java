package com.tzgames.ringer.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.tzgames.ringer.R;

import java.util.ArrayList;

/**
 * A custom view that draws rectangles moving from the right side to the left side of the screen,
 * with width proportional to how long the user tapped and held the screen. This view gives the
 * user a visual representation of how his generated vibrations look like. Callbacks of this view
 * are used to generate the actual long[] sequences.
 */
public class GeneratorView extends View {

    /** Delay in between frame draws */
    private static final int DRAW_MS = 40;

    /** Cached bitmap onto which to paint the canvas */
    private Bitmap mBitmap;

    /** Cached paint used to draw the rectangles */
    private final Paint mPaint;

    /** Cached handler used to schedule frame draws */
    private final Handler mHandler;

    /** Array of current touchBoxes that are drawn to canvas */
    private final ArrayList<TouchBox> touchBoxes;

    /** The latest / newest added touchbox */
    private TouchBox currentTouchBox = null;

    /** Callback listener for VibrationGenFragment to handle touches */
    private GeneratorTouchListener generatorTouchListener;

    /**
     * Class that represents a blue rectangle that whose width is proportional to the length
     * of a touch (and a vibration).
     */
    private class TouchBox {
        /** Current x position of rect */
        private float x;

        /** Current width of rect. If the user is pressing the screen, this is 0. Only once
         * user lets go of screen, the rect will have a fixed width. */
        private float width = 0;

        /** Private rect */
        private final Rect rect;

        /** How fast the rects move across the screen */
        private static final float SPEED = 20.0f;

        /**
         * Initializes rect dimensions so that starts off screen.
         */
        private TouchBox() {
            rect = new Rect();
            rect.bottom = getHeight();
            rect.top = 0;               // from top to bottom
            x = getWidth();             // width view. Position it to the far right out of bounds
        }

        /**
         * Update the coordinates of the rectangle and return it, ready to be drawn.
         * @return Return rect if ready to draw. If out of bounds, return null.
         */
        private Rect draw() {
            // move rect to right as time passes
            rect.left = (int) (x -= SPEED);

            // if rect has defined width, user already let go the tap, so draw it with its width
            if (width > 0) rect.right = (int)(x + width);

            // else, user is still pressing finger, draw it all the way to the right
            else rect.right = getWidth();

            // rect is outside of screen, so do not draw it
            if (width != 0 && x + width < 0) return null;
            else return rect;
        }

        /**
         * Called when user stops touching screen. Gives the rect its final, fixed width.
         */
        private void touchOver() {
            width = getWidth() - rect.left;
        }
    }

    /**
     * Constructor. Creates bitmap paint, frame update handler, and list of touch boxes.
     */
    public GeneratorView(Context ctx, AttributeSet set) {
        super(ctx, set);

        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.colorPrimaryLight));
        mPaint.setStyle(Paint.Style.FILL);

        mHandler = new Handler();
        touchBoxes = new ArrayList<>(3);
    }

    /**
     * Create a new bigmap with updated width & height. Called when view dimensions change.
     * @param w NewWidth
     * @param h NewHeight
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    /**
     * Called whenever view is invalidaded which happens every FRAME_MS miliseconds. This is
     * the loop that creates the illusion of fluid motion
     * @param canvas Canvas to draw the rectangles on
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, null);

        // draw all the boxes. Remove them if they are no longer inside view
        for (int i = 0; i < touchBoxes.size(); i++) {
            Rect r = touchBoxes.get(i).draw();
            if (r != null) canvas.drawRect(r, mPaint);
            else touchBoxes.remove(touchBoxes.get(i--));
        }

        // schedule next frame draw
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        }, DRAW_MS);
    }

    /**
     * Creates a box when user touches the screen and adds them to touchBoxes. Also calls
     * generatorTouchListener callbacks used in parent to update vibration timings
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                TouchBox t = new TouchBox();
                touchBoxes.add(t);
                currentTouchBox = t;
                generatorTouchListener.onTouchStarted(event.getEventTime());
                generatorTouchListener.setRecentStartTime(event.getEventTime());
                break;
            case MotionEvent.ACTION_UP:
                currentTouchBox.touchOver();
                generatorTouchListener.onTouchEnded(event.getEventTime());
                generatorTouchListener.setRecentEndTime(event.getEventTime());
                break;
        }
        return true;
    }

    /**
     * Function to bind GeneratorTouchListener callbacks
     * @param listener GeneratorTouchListener with overriden methods
     */
    public void setOnGeneratorTouchListener(GeneratorTouchListener listener) {
        generatorTouchListener = listener;
    }

    /**
     * Class that acts as an interface between visual vibrationview and the callbacks to parent
     * fragment about when user touches the screen.
     */
    public static abstract class GeneratorTouchListener {

        /** Most recent time point when user started touching screen */
        private long recentStartTime = 0;

        /** Most recent timepoint when user let go of screen */
        private long recentEndTime = 0;

        /** Setter */
        void setRecentEndTime(long t) { recentEndTime = t; }

        /** Setter */
        void setRecentStartTime(long t) { recentStartTime = t; }

        /** Getter */
        protected long getRecentEndTime() { return recentEndTime; }

        /** Getter */
        protected long getRecentStartTime() { return recentStartTime; }

        /**
         * Callback. Called when user started touching the screen.
         * @param end The time at which the user started touching the screen
         */
        public abstract void onTouchEnded(long end);

        /**
         * Callback. Called when user stopped touching the screen.
         * @param start The time at which the user stopped touching the screen
         */
        public abstract void onTouchStarted(long start);
    }
}