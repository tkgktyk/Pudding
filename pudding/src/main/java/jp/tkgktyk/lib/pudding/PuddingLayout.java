/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tkgktyk.lib.pudding;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnOverscrollListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 * </p>
 */
public class PuddingLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {
    // Maps to ProgressBar.Large style
    static final int LARGE = 0;
    // Maps to ProgressBar default style
    static final int DEFAULT = 1;

    private static final String LOG_TAG = PuddingLayout.class.getSimpleName();

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private View mExternalTarget;
    private OnOverscrollListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;

    /* removed nested scroll support */
//    // If nested scrolling is enabled, the total amount that needed to be
//    // consumed by this as the nested scrolling parent is used in place of the
//    // overscroll determined by MOVE events in the onTouch handler
//    private float mTotalUnconsumed;
//    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
//    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
//    private final int[] mParentScrollConsumed = new int[2];
//    private final int[] mParentOffsetInWindow = new int[2];
//    private boolean mNestedScrollInProgress;

    private int mMediumAnimationDuration;
    private int mCurrentTargetOffset;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionX;
    private float mInitialDownX;
    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private CircleImageView mCircleView;
    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffset;

    private static final int DIRECTION_TOP = 1;
    private static final int DIRECTION_BOTTOM = 2;
    private static final int DIRECTION_LEFT = 3;
    private static final int DIRECTION_RIGHT = 4;

    private int mDirection;

    private Drawable mTopDrawable;
    private Drawable mBottomDrawable;
    private Drawable mLeftDrawable;
    private Drawable mRightDrawable;

    private TargetOffsetSetter mTargetOffsetSetter;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mCircleWidth;

    private int mCircleHeight;

    /* remove custom starting position */
    // Whether the client has set a custom starting position;
//    private boolean mUsingCustomStart;

    private AnimationListener mRefreshListener = new AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                if (mNotify && mListener != null) {
                    switch (mDirection) {
                        case DIRECTION_TOP:
                            mListener.onOverscrollTop();
                            break;
                        case DIRECTION_BOTTOM:
                            mListener.onOverscrollBottom();
                            break;
                        case DIRECTION_LEFT:
                            mListener.onOverscrollLeft();
                            break;
                        case DIRECTION_RIGHT:
                            mListener.onOverscrollRight();
                            break;
                    }
                    setRefreshing(false);
                }
            } else {
                mCircleView.setVisibility(View.GONE);
                // Return the circle to its start position
                if (mScale) {
                    setAnimationProgress(0 /* animation complete and view is hidden */);
                } else {
                    mTargetOffsetSetter.set(mOriginalOffset - mCurrentTargetOffset,
                            true /* requires update */);
                }
            }
            mCurrentTargetOffset = mTargetOffsetSetter.calculateCurrentOffset();
        }
    };

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *              progress spinner should appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mCircleView.setVisibility(View.GONE);
        mOriginalOffset = mCurrentTargetOffset = start;
        mSpinnerFinalOffset = end;
        /* remove custom starting position */
//        mUsingCustomStart = true;
        mCircleView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != LARGE && size != DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == LARGE) {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
    }

    public Drawable getTopDrawable() {
        return mTopDrawable;
    }

    public void setTopDrawable(Drawable drawable) {
        mTopDrawable = drawable;
    }

    public Drawable getBottomDrawable() {
        return mBottomDrawable;
    }

    public void setBottomDrawable(Drawable drawable) {
        mBottomDrawable = drawable;
    }

    public Drawable getLeftDrawable() {
        return mLeftDrawable;
    }

    public void setLeftDrawable(Drawable drawable) {
        mLeftDrawable = drawable;
    }

    public Drawable getRightDrawable() {
        return mRightDrawable;
    }

    public void setRightDrawable(Drawable drawable) {
        mRightDrawable = drawable;
    }

    public void setExternalTarget(View target) {
        mExternalTarget = target;
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public PuddingLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public PuddingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
        /* removed nested scroll support */
//        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
//
//        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
//        setNestedScrollingEnabled(true);
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
        mCircleView.setVisibility(View.GONE);
        addView(mCircleView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnOverscrollListener(OnOverscrollListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            int endTarget = 0;
            /* remove custom starting position */
//            if (!mUsingCustomStart) {
//                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffset);
//            } else {
//                endTarget = (int) mSpinnerFinalOffset;
//            }
            endTarget = (int) (mSpinnerFinalOffset + mOriginalOffset);
            mTargetOffsetSetter.set(endTarget - mCurrentTargetOffset,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void startScaleUpAnimation(AnimationListener listener) {
        mCircleView.setVisibility(View.VISIBLE);
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
        } else {
            ViewCompat.setScaleX(mCircleView, progress);
            ViewCompat.setScaleY(mCircleView, progress);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffset, mRefreshListener);
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mCircleView.setAnimationListener(listener);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownAnimation);
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes) {
        setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
        mCircleView.setBackgroundColor(color);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            if (mExternalTarget != null) {
                mTarget = mExternalTarget;
            } else {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (!child.equals(mCircleView)) {
                        mTarget = child;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        } else if (mExternalTarget == null) {
            final View child = mTarget;
            final int childLeft = getPaddingLeft();
            final int childTop = getPaddingTop();
            final int childWidth = width - getPaddingLeft() - getPaddingRight();
            final int childHeight = height - getPaddingTop() - getPaddingBottom();
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        switch (mDirection) {
            case DIRECTION_TOP:
                mCircleView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffset,
                        (width / 2 + circleWidth / 2), mCurrentTargetOffset + circleHeight);
                break;
            case DIRECTION_BOTTOM:
                mCircleView.layout((width / 2 - circleWidth / 2), height - (mCurrentTargetOffset + circleHeight),
                        (width / 2 + circleWidth / 2), height - mCurrentTargetOffset);
                break;
            case DIRECTION_LEFT:
                mCircleView.layout(mCurrentTargetOffset, (height / 2 - circleHeight / 2),
                        mCurrentTargetOffset + circleWidth, (height / 2 + circleHeight / 2));
                break;
            case DIRECTION_RIGHT:
                mCircleView.layout(width - (mCurrentTargetOffset + circleWidth), (height / 2 - circleHeight / 2),
                        width - mCurrentTargetOffset, (height / 2 + circleHeight / 2));
                break;
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        } else if (mExternalTarget == null) {
            mTarget.measure(MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        }
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
        /* remove custom starting position */
//        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
        if (!mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffset = mOriginalOffset = -mCircleView.getMeasuredHeight();
        }
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout. This is not valid until a measure pass has
     * completed.
     *
     * @return Diameter in pixels of the progress circle view.
     */
//    public int getProgressCircleDiameter() {
//        return mCircleView != null ? mCircleView.getMeasuredHeight() : 0;
//    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        return canScrollVertically(this, -1);
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll down. Override this if the child view is a custom view.
     */
    public boolean canChildScrollDown() {
        return canScrollVertically(this, 1);
    }

    private boolean canScrollVertically(ViewGroup target, int direction) {
        int count = target.getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = target.getChildAt(i);
            if (child.getVisibility() == VISIBLE
                    && ViewCompat.canScrollVertically(child, direction)) {
                return true;
            }
        }
        for (int i = 0; i < count; ++i) {
            View child = target.getChildAt(i);
            if (child.getVisibility() == VISIBLE
                    && child instanceof ViewGroup) {
                if (canScrollVertically((ViewGroup) child, direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll left. Override this if the child view is a custom view.
     */
    public boolean canChildScrollLeft() {
        return canScrollHorizontally(this, -1);
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll right. Override this if the child view is a custom view.
     */
    public boolean canChildScrollRight() {
        return canScrollHorizontally(this, 1);
    }

    private boolean canScrollHorizontally(ViewGroup target, int direction) {
        int count = target.getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = target.getChildAt(i);
            if (child.getVisibility() == VISIBLE
                    && ViewCompat.canScrollHorizontally(child, direction)) {
                return true;
            }
        }
        for (int i = 0; i < count; ++i) {
            View child = target.getChildAt(i);
            if (child.getVisibility() == VISIBLE
                    && child instanceof ViewGroup) {
                if (canScrollHorizontally((ViewGroup) child, direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || mRefreshing) {
        /* removed nested scroll support */
//                || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
//                setTargetOffsetTopAndBottom(mOriginalOffset - mCircleView.getTop(), true);
//                setTargetOffsetLeftAndRight(mOriginalOffsetLeft - mCircleView.getLeft(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    return false;
                }
                mInitialDownX = MotionEventCompat.getX(ev, index);
                mInitialDownY = MotionEventCompat.getY(ev, index);
                break;
            }

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    return false;
                }
                if (!mIsBeingDragged) {
                    final float x = MotionEventCompat.getX(ev, index);
                    final float y = MotionEventCompat.getY(ev, index);
                    final float xDiff = x - mInitialDownX;
                    final float yDiff = y - mInitialDownY;
                    // give priority to vertical swipe
                    if (yDiff > mTouchSlop) {
                        if (!canChildScrollUp() && mTopDrawable != null) {
                            mCircleView.setImageDrawable(mTopDrawable);
                            mTargetOffsetSetter = mTargetOffsetTopSetter;
                            mTargetOffsetSetter.set(mOriginalOffset - mTargetOffsetSetter.calculateCurrentOffset(), true);
                            mInitialMotionY = mInitialDownY + mTouchSlop;
                            mIsBeingDragged = true;
                            mDirection = DIRECTION_TOP;
                        } else {
                            requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (-yDiff > mTouchSlop) {
                        if (!canChildScrollDown() && mBottomDrawable != null) {
                            mCircleView.setImageDrawable(mBottomDrawable);
                            mTargetOffsetSetter = mTargetOffsetBottomSetter;
                            mTargetOffsetSetter.set(mOriginalOffset - mTargetOffsetSetter.calculateCurrentOffset(), true);
                            mInitialMotionY = mInitialDownY - mTouchSlop;
                            mIsBeingDragged = true;
                            mDirection = DIRECTION_BOTTOM;
                        } else {
                            requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (xDiff > mTouchSlop) {
                        if (!canChildScrollLeft() && mLeftDrawable != null) {
                            mCircleView.setImageDrawable(mLeftDrawable);
                            mTargetOffsetSetter = mTargetOffsetLeftSetter;
                            mTargetOffsetSetter.set(mOriginalOffset - mTargetOffsetSetter.calculateCurrentOffset(), true);
                            mInitialMotionX = mInitialDownX + mTouchSlop;
                            mIsBeingDragged = true;
                            mDirection = DIRECTION_LEFT;
                        } else {
                            requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (-xDiff > mTouchSlop) {
                        if (!canChildScrollRight() && mRightDrawable != null) {
                            mCircleView.setImageDrawable(mRightDrawable);
                            mTargetOffsetSetter = mTargetOffsetRightSetter;
                            mTargetOffsetSetter.set(mOriginalOffset - mTargetOffsetSetter.calculateCurrentOffset(), true);
                            mInitialMotionX = mInitialDownX - mTouchSlop;
                            mIsBeingDragged = true;
                            mDirection = DIRECTION_RIGHT;
                        } else {
                            requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    /* removed nested scroll support */
//    @Override
//    public void requestDisallowInterceptTouchEvent(boolean b) {
//        // if this is a List < L or another view that doesn't support nested
//        // scrolling, ignore this request so that the vertical scroll event
//        // isn't stolen
//        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
//                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
//            // Nope.
//        } else {
//            super.requestDisallowInterceptTouchEvent(b);
//        }
//    }

//    // NestedScrollingParent
//
//    @Override
//    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
//        return isEnabled() && !mReturningToStart && !mRefreshing
//                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
//    }
//
//    @Override
//    public void onNestedScrollAccepted(View child, View target, int axes) {
//        // Reset the counter of how much leftover scroll needs to be consumed.
//        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
//        // Dispatch up to the nested parent
//        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
//        mTotalUnconsumed = 0;
//        mNestedScrollInProgress = true;
//    }
//
//    @Override
//    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
//        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
//        // before allowing the list to scroll
//        if (dy > 0 && mTotalUnconsumed > 0) {
//            if (dy > mTotalUnconsumed) {
//                consumed[1] = dy - (int) mTotalUnconsumed;
//                mTotalUnconsumed = 0;
//            } else {
//                mTotalUnconsumed -= dy;
//                consumed[1] = dy;
//
//            }
//            moveSpinner(mTotalUnconsumed);
//        }
//
//        // If a client layout is using a custom start position for the circle
//        // view, they mean to hide it again before scrolling the child view
//        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
//        // the circle so it isn't exposed if its blocking content is moved
//        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
//                && Math.abs(dy - consumed[1]) > 0) {
//            mCircleView.setVisibility(View.GONE);
//        }
//
//        // Now let our nested parent consume the leftovers
//        final int[] parentConsumed = mParentScrollConsumed;
//        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
//            consumed[0] += parentConsumed[0];
//            consumed[1] += parentConsumed[1];
//        }
//    }
//
//    @Override
//    public int getNestedScrollAxes() {
//        return mNestedScrollingParentHelper.getNestedScrollAxes();
//    }
//
//    @Override
//    public void onStopNestedScroll(View target) {
//        mNestedScrollingParentHelper.onStopNestedScroll(target);
//        mNestedScrollInProgress = false;
//        // Finish the spinner for nested scrolling if we ever consumed any
//        // unconsumed nested scroll
//        if (mTotalUnconsumed > 0) {
//            finishSpinner(mTotalUnconsumed);
//            mTotalUnconsumed = 0;
//        }
//        // Dispatch up our nested parent
//        stopNestedScroll();
//    }
//
//    @Override
//    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
//                               final int dxUnconsumed, final int dyUnconsumed) {
//        // Dispatch up to the nested parent first
//        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
//                mParentOffsetInWindow);
//
//        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
//        // sometimes between two nested scrolling views, we need a way to be able to know when any
//        // nested scrolling parent has stopped handling events. We do that by using the
//        // 'offset in window 'functionality to see if we have been moved from the event.
//        // This is a decent indication of whether we should take over the event stream or not.
//        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
//        if (dy < 0) {
//            mTotalUnconsumed += Math.abs(dy);
//            moveSpinner(mTotalUnconsumed);
//        }
//    }
//
//    // NestedScrollingChild
//
//    @Override
//    public void setNestedScrollingEnabled(boolean enabled) {
//        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
//    }
//
//    @Override
//    public boolean isNestedScrollingEnabled() {
//        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
//    }
//
//    @Override
//    public boolean startNestedScroll(int axes) {
//        return mNestedScrollingChildHelper.startNestedScroll(axes);
//    }
//
//    @Override
//    public void stopNestedScroll() {
//        mNestedScrollingChildHelper.stopNestedScroll();
//    }
//
//    @Override
//    public boolean hasNestedScrollingParent() {
//        return mNestedScrollingChildHelper.hasNestedScrollingParent();
//    }
//
//    @Override
//    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
//                                        int dyUnconsumed, int[] offsetInWindow) {
//        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
//                dxUnconsumed, dyUnconsumed, offsetInWindow);
//    }
//
//    @Override
//    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
//        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
//    }
//
//    @Override
//    public boolean onNestedPreFling(View target, float velocityX,
//                                    float velocityY) {
//        return dispatchNestedPreFling(velocityX, velocityY);
//    }
//
//    @Override
//    public boolean onNestedFling(View target, float velocityX, float velocityY,
//                                 boolean consumed) {
//        return dispatchNestedFling(velocityX, velocityY, consumed);
//    }
//
//    @Override
//    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
//        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
//    }
//
//    @Override
//    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
//        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
//    }

    private void moveSpinner(float overscroll) {
        overscroll = Math.abs(overscroll);
        float originalDragPercent = overscroll / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overscroll) - mTotalDragDistance;
        /* remove custom starting position */
//        float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset - mOriginalOffset
//                : mSpinnerFinalOffset;
        float slingshotDist = mSpinnerFinalOffset;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = mOriginalOffset + (int) ((slingshotDist * dragPercent) + extraMove);
        // where 1.0f is a full circle
        if (mCircleView.getVisibility() != View.VISIBLE) {
            mCircleView.setVisibility(View.VISIBLE);
        }
        if (!mScale) {
            ViewCompat.setScaleX(mCircleView, 1f);
            ViewCompat.setScaleY(mCircleView, 1f);
        }
        if (overscroll < mTotalDragDistance) {
            if (mScale) {
                setAnimationProgress(overscroll / mTotalDragDistance);
            }
        }
        float rotation = Math.max(-1f, Math.min(1f, originalDragPercent)) * 360;
        mTargetOffsetSetter.set(rotation, targetY - mCurrentTargetOffset,
                true /* requires update */);
    }

    private void finishSpinner(float overscroll) {
        overscroll = Math.abs(overscroll);
        if (overscroll > mTotalDragDistance) {
            setRefreshing(true, true /* notify */);
        } else {
            // cancel refresh
            mRefreshing = false;
            AnimationListener listener = null;
            if (!mScale) {
                listener = new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            animateOffsetToStartPosition(mCurrentTargetOffset, listener);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart) {
        /* removed nested scroll support */
//                || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                if (mIsBeingDragged) {
                    final float x = MotionEventCompat.getX(ev, pointerIndex);
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    final float overscrollLeft = (x - mInitialMotionX) * DRAG_RATE;
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    switch (mDirection) {
                        case DIRECTION_TOP:
                            if (overscrollTop > 0) {
                                moveSpinner(overscrollTop);
                            } else {
                                return false;
                            }
                            break;
                        case DIRECTION_BOTTOM:
                            if (overscrollTop < 0) {
                                moveSpinner(overscrollTop);
                            } else {
                                return false;
                            }
                            break;
                        case DIRECTION_LEFT:
                            if (overscrollLeft > 0) {
                                moveSpinner(overscrollLeft);
                            } else {
                                return false;
                            }
                            break;
                        case DIRECTION_RIGHT:
                            if (overscrollLeft < 0) {
                                moveSpinner(overscrollLeft);
                            } else {
                                return false;
                            }
                            break;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                int pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollLeft = (x - mInitialMotionX) * DRAG_RATE;
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                switch (mDirection) {
                    case DIRECTION_TOP:
                    case DIRECTION_BOTTOM:
                        finishSpinner(overscrollTop);
                        break;
                    case DIRECTION_LEFT:
                    case DIRECTION_RIGHT:
                        finishSpinner(overscrollLeft);
                        break;
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
            }
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            /* remove custom starting position */
//            if (!mUsingCustomStart) {
//                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffset));
//            } else {
//                endTarget = (int) mSpinnerFinalOffset;
//            }
            int offset = 0;
            endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffset));
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            offset = targetTop - mTargetOffsetSetter.calculateCurrentOffset();
            mTargetOffsetSetter.set(offset, false /* requires update */);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        int offset = 0;
        targetTop = (mFrom + (int) ((mOriginalOffset - mFrom) * interpolatedTime));
        offset = targetTop - mTargetOffsetSetter.calculateCurrentOffset();
        mTargetOffsetSetter.set(offset, false /* requires update */);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = 1f;
        } else {
            mStartingScale = ViewCompat.getScaleX(mCircleView);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownToStartAnimation);
    }

    private abstract class TargetOffsetSetter {
        void set(int offset, boolean requiresUpdate) {
            mCircleView.bringToFront();
            setImpl(offset);
            mCurrentTargetOffset = calculateCurrentOffset();
            if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
                invalidate();
            }
        }

        void set(float rotation, int offset, boolean requiresUpdate) {
            mCircleView.setRotation(rotation);
            set(offset, requiresUpdate);
        }

        abstract void setImpl(int offset);

        abstract int calculateCurrentOffset();
    }

    private final TargetOffsetSetter mTargetOffsetTopSetter = new TargetOffsetSetter() {
        @Override
        void setImpl(int offset) {
            Log.d(LOG_TAG, "top = " + offset);
            mCircleView.offsetTopAndBottom(offset);
        }

        @Override
        int calculateCurrentOffset() {
            return mCircleView.getTop();
        }
    };

    private final TargetOffsetSetter mTargetOffsetBottomSetter = new TargetOffsetSetter() {
        @Override
        void setImpl(int offset) {
            Log.d(LOG_TAG, "bottom = " + offset);
            mCircleView.offsetTopAndBottom(-offset);
        }

        @Override
        int calculateCurrentOffset() {
            return getHeight() - mCircleView.getBottom();
        }
    };

    private final TargetOffsetSetter mTargetOffsetLeftSetter = new TargetOffsetSetter() {
        @Override
        void setImpl(int offset) {
            Log.d(LOG_TAG, "left = " + offset);
            mCircleView.offsetLeftAndRight(offset);
        }

        @Override
        int calculateCurrentOffset() {
            return mCircleView.getLeft();
        }
    };

    private final TargetOffsetSetter mTargetOffsetRightSetter = new TargetOffsetSetter() {
        @Override
        void setImpl(int offset) {
            Log.d(LOG_TAG, "right = " + offset);
            mCircleView.offsetLeftAndRight(-offset);
        }

        @Override
        int calculateCurrentOffset() {
            return getWidth() - mCircleView.getRight();
        }
    };

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnOverscrollListener {
        void onOverscrollTop();

        void onOverscrollBottom();

        void onOverscrollLeft();

        void onOverscrollRight();
    }
}
