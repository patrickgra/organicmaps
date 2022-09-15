package com.mapswithme.maps.widget.placepage;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.bookmarks.data.MapObject;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.LocationListener;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.bottomsheet.MenuBottomSheetItem;
import com.mapswithme.util.log.Logger;

import java.util.ArrayList;
import java.util.Objects;

public class RichPlacePageController implements PlacePageController, LocationListener,
                                                View.OnLayoutChangeListener,
                                                Closable
{
  private static final String TAG = RichPlacePageController.class.getSimpleName();

  private static final float ANCHOR_RATIO = 0.3f;
  private static final float PREVIEW_PLUS_RATIO = 0.45f;
  private static final int ANIM_CHANGE_PEEK_HEIGHT_MS = 100;
  @SuppressWarnings("NullableProblems")
  @NonNull
  private BottomSheetBehavior<View> mPlacePageBehavior;
  @SuppressWarnings("NullableProblems")
  @NonNull
  private View mButtonsLayout;
  @SuppressWarnings("NullableProblems")
  @NonNull
  private PlacePageView mPlacePage;
  private int mViewportMinHeight;
  private boolean mPeekHeightAnimating;
  @NonNull
  private final SlideListener mSlideListener;
  @Nullable
  private final RoutingModeListener mRoutingModeListener;
  @NonNull
  private final BottomSheetChangedListener mBottomSheetChangedListener = new BottomSheetChangedListener()
  {
    @Override
    public void onSheetHidden()
    {
      onHiddenInternal();
    }

    @Override
    public void onSheetDirectionIconChange()
    {
      // No op.
    }

    @Override
    public void onSheetDetailsOpened()
    {
      // No op.
    }

    @Override
    public void onSheetCollapsed()
    {
      mPlacePage.resetScroll();
      setPeekHeight();
    }

    @Override
    public void onSheetSliding(int top)
    {
      mSlideListener.onPlacePageSlide(top);
    //  mPlacePageTracker.onMove();
    }

    @Override
    public void onSheetSlideFinish()
    {
      PlacePageUtils.moveViewportUp(mPlacePage, mViewportMinHeight);
    }
  };

  @NonNull
  private final BottomSheetBehavior.BottomSheetCallback mSheetCallback
      = new DefaultBottomSheetCallback(mBottomSheetChangedListener);

  private boolean mDeactivateMapSelection = true;

  private void onHiddenInternal()
  {
    if (mDeactivateMapSelection)
      Framework.nativeDeactivatePopup();
    mDeactivateMapSelection = true;
    PlacePageUtils.moveViewportUp(mPlacePage, mViewportMinHeight);
    UiUtils.invisible(mButtonsLayout);
  }

  RichPlacePageController(@NonNull SlideListener listener,
                          @Nullable RoutingModeListener routingModeListener)
  {
    mSlideListener = listener;
    mRoutingModeListener = routingModeListener;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void initialize(@Nullable Activity activity)
  {
    Objects.requireNonNull(activity);
    Resources res = activity.getResources();
    mViewportMinHeight = res.getDimensionPixelSize(R.dimen.viewport_min_height);
    mPlacePage = activity.findViewById(R.id.placepage);
    mPlacePageBehavior = BottomSheetBehavior.from(mPlacePage);
    mPlacePageBehavior.addBottomSheetCallback(mSheetCallback);
    mPlacePageBehavior.setHideable(true);
    mPlacePageBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    PlacePageGestureListener ppGestureListener = new PlacePageGestureListener(mPlacePageBehavior);
    GestureDetectorCompat gestureDetector = new GestureDetectorCompat(activity, ppGestureListener);
    mPlacePage.addPlacePageGestureListener(ppGestureListener);
    mPlacePage.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    mPlacePage.addOnLayoutChangeListener(this);
    mPlacePage.addClosable(this);
    mPlacePage.setRoutingModeListener(mRoutingModeListener);

    mButtonsLayout = activity.findViewById(R.id.pp_buttons_layout);
    ViewGroup buttons = mButtonsLayout.findViewById(R.id.container);
    mPlacePage.initButtons(buttons);
    UiUtils.bringViewToFrontOf(mButtonsLayout, mPlacePage);
    LocationHelper.INSTANCE.addListener(this);
  }

  public int getPlacePageWidth()
  {
    return mPlacePage.getWidth();
  }

  @Override
  @Nullable
  public ArrayList<MenuBottomSheetItem> getMenuBottomSheetItems()
  {
    return mPlacePage.getMenuBottomSheetItems();
  }

  @Override
  public void destroy()
  {
    LocationHelper.INSTANCE.removeListener(this);
  }

  @Override
  public void openFor(@NonNull PlacePageData data)
  {
    mDeactivateMapSelection = true;
    MapObject object = (MapObject) data;
    mPlacePage.setMapObject(object, (isSameObject) -> {
      @BottomSheetBehavior.State
      int state = mPlacePageBehavior.getState();
      if (isSameObject && !PlacePageUtils.isHiddenState(state))
        return;

      mPlacePage.resetScroll();

      if (object.getOpeningMode() == MapObject.OPENING_MODE_DETAILS)
      {
        mPlacePageBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        return;
      }

      UiUtils.show(mButtonsLayout);
      openPlacePage();
    });

  }

  private void openPlacePage()
  {
    mPlacePage.post(() -> {
      setPeekHeight();
      mPlacePageBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    });
  }

  private void setPeekHeight()
  {
    if (mPeekHeightAnimating)
    {
      Logger.d(TAG, "Peek animation in progress, ignore.");
      return;
    }

    final int peekHeight = calculatePeekHeight();
    if (peekHeight == mPlacePageBehavior.getPeekHeight())
      return;

    @BottomSheetBehavior.State
    int currentState = mPlacePageBehavior.getState();
    if (PlacePageUtils.isSettlingState(currentState) || PlacePageUtils.isDraggingState(currentState))
    {
      Logger.d(TAG, "Sheet state inappropriate, ignore.");
      return;
    }

    if (PlacePageUtils.isCollapsedState(currentState) && mPlacePageBehavior.getPeekHeight() > 0)
    {
      setPeekHeightAnimatedly(peekHeight);
      return;
    }

    mPlacePageBehavior.setPeekHeight(peekHeight);
  }

  private void setPeekHeightAnimatedly(int peekHeight)
  {
    int delta = peekHeight - mPlacePageBehavior.getPeekHeight();
    ObjectAnimator animator = ObjectAnimator.ofFloat(mPlacePage, "translationY", -delta);
    animator.setDuration(ANIM_CHANGE_PEEK_HEIGHT_MS);
    animator.addListener(new UiUtils.SimpleAnimatorListener()
    {
      @Override
      public void onAnimationStart(Animator animation)
      {
        mPeekHeightAnimating = true;
        mPlacePage.setScrollable(false);
        mPlacePageBehavior.setDraggable(false);
      }

      @Override
      public void onAnimationEnd(Animator animation)
      {
        mPlacePage.setTranslationY(0);
        mPeekHeightAnimating = false;
        mPlacePage.setScrollable(true);
        mPlacePageBehavior.setDraggable(true);
        mPlacePageBehavior.setPeekHeight(peekHeight);
      }
    });
    animator.addUpdateListener(animation -> onUpdateTranslation());

    animator.start();
  }

  private void onUpdateTranslation()
  {
    mSlideListener.onPlacePageSlide((int) (mPlacePage.getTop() + mPlacePage.getTranslationY()));
  }

  private int calculatePeekHeight()
  {
    // Buttons layout padding is the navigation bar height.
    // Bottom sheets are displayed above it so we need to remove it from the computed size
    final int organicPeekHeight = mPlacePage.getPreviewHeight() +
                                  mButtonsLayout.getHeight() - mButtonsLayout.getPaddingBottom();
    final MapObject object = mPlacePage.getMapObject();
    if (object != null)
    {
      @MapObject.OpeningMode
      int mode = object.getOpeningMode();
      if (mode == MapObject.OPENING_MODE_PREVIEW_PLUS)
      {
        View parent = (View) mPlacePage.getParent();
        int promoPeekHeight = (int) (parent.getHeight() * PREVIEW_PLUS_RATIO);
        return promoPeekHeight <= organicPeekHeight ? organicPeekHeight : promoPeekHeight;
      }
    }

    return organicPeekHeight;
  }

  @Override
  public void close(boolean deactivateMapSelection)
  {
    mDeactivateMapSelection = deactivateMapSelection;
    mPlacePageBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    mPlacePage.reset();
  }

  @Override
  public boolean isClosed()
  {
    return PlacePageUtils.isHiddenState(mPlacePageBehavior.getState());
  }

  @Override
  public void onLocationUpdated(Location location)
  {
    mPlacePage.refreshLocation(location);
  }

  @Override
  public void onCompassUpdated(long time, double north)
  {
    @BottomSheetBehavior.State
    int currentState = mPlacePageBehavior.getState();
    if (PlacePageUtils.isHiddenState(currentState) || PlacePageUtils.isDraggingState(currentState)
        || PlacePageUtils.isSettlingState(currentState))
      return;

    mPlacePage.refreshAzimuth(north);
  }

  @Override
  public void onLocationError(int errorCode)
  {
    // Do nothing by default.
  }

  @Override
  public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int
      oldTop, int oldRight, int oldBottom)
  {
    if (mPlacePageBehavior.getPeekHeight() == 0)
    {
      Logger.d(TAG, "Layout change ignored, peek height not calculated yet");
      return;
    }

    mPlacePage.post(this::setPeekHeight);

    if (PlacePageUtils.isHiddenState(mPlacePageBehavior.getState()))
      return;

    PlacePageUtils.moveViewportUp(mPlacePage, mViewportMinHeight);
  }

  @Override
  public void onSave(@NonNull Bundle outState)
  {
    outState.putParcelable(PlacePageUtils.EXTRA_PLACE_PAGE_DATA, mPlacePage.getMapObject());
  }

  @Override
  public void onRestore(@NonNull Bundle inState)
  {
    if (mPlacePageBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
      return;

    if (!Framework.nativeHasPlacePageInfo())
    {
      close(false);
      return;
    }

    MapObject object = inState.getParcelable(PlacePageUtils.EXTRA_PLACE_PAGE_DATA);
    if (object == null)
      return;

    @BottomSheetBehavior.State
    int state = mPlacePageBehavior.getState();
    mPlacePage.setMapObject(object, (isSameObject) -> {
      restorePlacePageState(object, state);
    });
  }

  private void restorePlacePageState(@NonNull MapObject object, @BottomSheetBehavior.State int state)
  {
    mPlacePage.post(() -> {
      mPlacePageBehavior.setState(state);
      UiUtils.show(mButtonsLayout);
      setPeekHeight();
    });
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState)
  {
    // No op.
  }

  @Override
  public void onActivityStarted(Activity activity)
  {
    // No op.
  }

  @Override
  public void onActivityResumed(Activity activity)
  {
    // No op.
  }

  @Override
  public void onActivityPaused(Activity activity)
  {
    // No op.
  }

  @Override
  public void onActivityStopped(Activity activity)
  {
    // No op.
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState)
  {
    // No op.
  }

  @Override
  public void onActivityDestroyed(Activity activity)
  {
    // No op.
  }

  @Override
  public void closePlacePage()
  {
    close(true);
  }

  @Override
  public boolean support(@NonNull PlacePageData data)
  {
    return data instanceof MapObject;
  }
}
