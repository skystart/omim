package com.mapswithme.maps.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapswithme.maps.R;
import com.mapswithme.util.UiUtils;

public class PlaceholderView extends FrameLayout
{
  @Nullable
  private ImageView mImage;
  @Nullable
  private TextView mTitle;
  @Nullable
  private TextView mSubtitle;

  private float mImageSizeFull;
  private float mImageSizeSmall;
  private float mPaddingImage;
  private float mPaddingNoImage;
  private float mScreenHeight;
  private float mScreenWidth;

  private int mOrientation;

  public PlaceholderView(Context context)
  {
    this(context, null, 0);
  }

  public PlaceholderView(Context context, @Nullable AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public PlaceholderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);

    init(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public PlaceholderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                         int defStyleRes)
  {
    super(context, attrs, defStyleAttr, defStyleRes);

    init(context);
  }

  private void init(Context context)
  {
    Resources res = getResources();
    mImageSizeFull = res.getDimension(R.dimen.placeholder_size);
    mImageSizeSmall = res.getDimension(R.dimen.placeholder_size_small);
    mPaddingImage = res.getDimension(R.dimen.placeholder_margin_top);
    mPaddingNoImage = res.getDimension(R.dimen.placeholder_margin_top_no_image);
    mScreenHeight = res.getDisplayMetrics().heightPixels;
    mScreenWidth = res.getDisplayMetrics().widthPixels;
    LayoutInflater.from(context).inflate(R.layout.placeholder, this, true);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    mImage = (ImageView) findViewById(R.id.image);
    mTitle = (TextView) findViewById(R.id.title);
    mSubtitle = (TextView) findViewById(R.id.subtitle);

    ViewCompat.setOnApplyWindowInsetsListener(this, new android.support.v4.view.OnApplyWindowInsetsListener()
    {
      @Override
      public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets)
      {
        int height = (int) (mOrientation == Configuration.ORIENTATION_LANDSCAPE
                            ? mScreenWidth : mScreenHeight);
        int[] location = new int[2];
        getLocationOnScreen(location);
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = height - insets.getSystemWindowInsetBottom() - location[1];
        setLayoutParams(lp);
        return insets;
      }
    });
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    //isInEditMode() need for correct editor visualization
    if (isInEditMode() || mImage == null)
    {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    if (mOrientation == Configuration.ORIENTATION_LANDSCAPE && !UiUtils.isTablet())
    {
      UiUtils.hide(mImage);
      setPadding(getPaddingLeft(), (int) mPaddingNoImage, getPaddingRight(), getPaddingBottom());
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    setPadding(getPaddingLeft(), (int) mPaddingImage, getPaddingRight(), getPaddingBottom());
    UiUtils.show(mImage);
    ViewGroup.LayoutParams lp = mImage.getLayoutParams();
    lp.width = (int) mImageSizeFull;
    lp.height = (int) mImageSizeFull;
    mImage.setLayoutParams(lp);

    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    if (getMeasuredHeight() > MeasureSpec.getSize(heightMeasureSpec))
    {
      lp.width = (int) mImageSizeSmall;
      lp.height = (int) mImageSizeSmall;
      mImage.setLayoutParams(lp);
      super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
      if (getMeasuredHeight() > MeasureSpec.getSize(heightMeasureSpec))
      {
        UiUtils.hide(mImage);
        setPadding(getPaddingLeft(), (int) mPaddingNoImage, getPaddingRight(), getPaddingBottom());
      }
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig)
  {
    mOrientation = newConfig.orientation;
  }

  public void setContent(@DrawableRes int imageRes, @StringRes int titleRes,
                         @StringRes int subtitleRes)
  {
    if (mImage != null)
      mImage.setImageResource(imageRes);
    if (mTitle != null)
      mTitle.setText(titleRes);
    if (mSubtitle != null)
      mSubtitle.setText(subtitleRes);
  }
}
