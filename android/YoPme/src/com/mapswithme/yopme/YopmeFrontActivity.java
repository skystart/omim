package com.mapswithme.yopme;

import com.mapswithme.maps.api.MWMPoint;
import com.mapswithme.maps.api.MWMResponse;
import com.mapswithme.maps.api.MapsWithMeApi;
import com.mapswithme.maps.api.MwmRequest;
import com.mapswithme.yopme.BackscreenActivity.Mode;
import com.mapswithme.yopme.map.MapDataProvider;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class YopmeFrontActivity extends Activity
                                implements OnClickListener, SensorEventListener
{
  private TextView   mSelectedLocation;
  private View mMenu;

  private Mode mMode;
  private MWMPoint mPoint;
  private final static String KEY_MODE = "key.mode";
  private final static String KEY_POINT = "key.point";

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    outState.putSerializable(KEY_MODE, mMode);
    outState.putSerializable(KEY_POINT, mPoint);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_yopme_main);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayShowHomeEnabled(false);
    actionBar.setDisplayUseLogoEnabled(false);
    actionBar.setDisplayShowCustomEnabled(true);
    actionBar.setCustomView(R.layout.action_bar_view);

    // sencors set up
    sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accel = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    setUpView();

    //restore
    if (savedInstanceState != null)
    {
      mMode  = (Mode) savedInstanceState.getSerializable(KEY_MODE);
      mPoint = (MWMPoint) savedInstanceState.getSerializable(KEY_POINT);

     if (Mode.LOCATION == mMode)
       setLocationView();
     else if (Mode.POI == mMode)
       mSelectedLocation.setText(mPoint.getName());
    }

    setUpListeners();

    if (isIntroNeeded())
      showIntro();
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    sm.unregisterListener(this);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);

    if (intent.hasExtra(EXTRA_PICK) && intent.getBooleanExtra(EXTRA_PICK, false))
    {
      final MWMResponse response = MWMResponse.extractFromIntent(this, intent);
      if (response.hasPoint())
      {
        mPoint = response.getPoint();
        mSelectedLocation.setText(mPoint.getName());
        BackscreenActivity.startInMode(this, Mode.POI, mPoint, response.getZoomLevel());

        Toast.makeText(this, R.string.toast_poi, Toast.LENGTH_LONG).show();
      }

    }
  }

  private void setUpView()
  {
    mSelectedLocation   = (TextView) findViewById(R.id.selectedLocation);
    mMenu = findViewById(R.id.menu);
  }

  private void setUpListeners()
  {
    findViewById(R.id.poi).setOnClickListener(this);
    findViewById(R.id.me).setOnClickListener(this);
    mMenu.setOnClickListener(this);
  }

  @Override
  public void onClick(View v)
  {
    if (!mClickable)
    {
      Log.d("SENSOR", "SKIPING CLICK");
      return;
    }

    if (R.id.me == v.getId())
    {
      BackscreenActivity.startInMode(getApplicationContext(), Mode.LOCATION, null, MapDataProvider.COMFORT_ZOOM);
      Toast.makeText(this, R.string.toast_your_location, Toast.LENGTH_LONG).show();
      setLocationView();
    }
    else if (R.id.poi == v.getId())
    {
      final MwmRequest request = new MwmRequest()
                                   .setCustomButtonName(getString(R.string.pick_point_button_name))
                                   .setPendingIntent(getPickPointPendingIntent())
                                   .setTitle(getString(R.string.app_name))
                                   .setPickPointMode(true);
      MapsWithMeApi.sendRequest(this, request);
    }
    else if (R.id.menu == v.getId())
    {
      final PopupMenu popupMenu = new PopupMenu(this, mMenu);
      popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener()
      {
        @Override
        public boolean onMenuItemClick(MenuItem item)
        {
          final int itemId = item.getItemId();
          if (itemId == R.id.menu_help)
          {
            startActivity(new Intent(getApplicationContext(), ReferenceActivity.class));
            return true;
          }
          else if (itemId == R.id.menu_settings)
          {
            startActivity(new Intent(getApplicationContext(), YopmePreference.class));
            return true;
          }
          else if (itemId == R.id.menu_about)
          {
            final SpannableString linkifiedAbout = new SpannableString(getString(R.string.about));
            Linkify.addLinks(linkifiedAbout, Linkify.ALL);

            new AlertDialog.Builder(YopmeFrontActivity.this)
              .setTitle(R.string.about_title)
              .setMessage(linkifiedAbout)
              .create()
              .show();

            return true;
          }
          return false;
        }
      });
      popupMenu.inflate(R.menu.yopme_main);
      popupMenu.show();
    }
  }

  private final static String EXTRA_PICK = ".pick_point";
  private PendingIntent getPickPointPendingIntent()
  {
    final Intent i = new Intent(this, YopmeFrontActivity.class);
    i.putExtra(EXTRA_PICK, true);
    return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private void setLocationView()
  {
    mSelectedLocation.setText(getString(R.string.poi_label));
  }

  private void showIntro()
  {
    final View intro =  View.inflate(this, R.layout.help, null);
    intro.setBackgroundColor(0x80000000);
    ((ViewGroup)getWindow().getDecorView()).addView(intro);
    intro.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        intro.setVisibility(View.GONE);
        markIntroShown();
      }
    });
  }

  private final static String PREFS = "prefs.xml";
  private final static String KEY_INTRO = "intro";

  private boolean isIntroNeeded()
  {
    return getSharedPreferences(PREFS, 0).getBoolean(KEY_INTRO, true);
  }

  private void markIntroShown()
  {
    getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_INTRO, false).apply();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {
  }

  private long mLastTimeStamp;
  private boolean mClickable = true;
  private SensorManager sm;
  private Sensor accel;

  @Override
  public void onSensorChanged(SensorEvent event)
  {
    // in nanos
    final long interval = 1000;
    // in radians/second
    final float lowerBound = 1.0f;

    final float yRotationSpeed = event.values[1];
    if (Math.abs(yRotationSpeed) > lowerBound)
    {
      mLastTimeStamp = event.timestamp;
      mClickable = false;
    }
    else if (event.timestamp - mLastTimeStamp > interval * 1000000)
      mClickable = true;
  }
}
