// Copyright (C) Yannick Le Roux.
// This file is part of Freeteuse.
//
//   Freeteuse is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Freeteuse is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with Freeteuse.  If not, see <http://www.gnu.org/licenses/>.

package bzh.leroux.yannick.freeteuse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import bzh.leroux.yannick.freeteuse.sniffers.BonjourSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.BrutusSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.FreeboxSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.Simulator;

class Home implements FreeboxSniffer.Listener
{
  public interface Listener
  {
    void onFreeboxSelected (Freebox freebox);
    void onFreeboxDetected (Freebox freebox);
  }

  private BrutusSniffer     mBrutusSniffer;
  private BonjourSniffer    mPlayerSniffer;
  private BonjourSniffer    mGatewaySniffer;
  private Simulator         mSimulator;
  private Activity          mActivity;
  private SharedPreferences mPreferences;
  private List<Freebox>     mBoxes;
  private Listener          mListener;
  private Painter           mPainter;
  private Logger            mLogger;

  // ---------------------------------------------------
  Home (Activity          activity,
        Listener          listener,
        SharedPreferences preferences)
  {
    mActivity    = activity;
    mListener    = listener;
    mPreferences = preferences;
    mPainter     = new Painter (activity);
    mBoxes       = new ArrayList<> ();
  }

  // ---------------------------------------------------
  private void recoverSavedBoxes ()
  {
    String freeboxPool = mPreferences.getString ("freebox_pool", null);

    if (freeboxPool != null)
    {
      Freebox focus = null;

      Log.d (Freeteuse.TAG, freeboxPool.replace ("},", "},\n"));

      try
      {
        JSONArray array = new JSONArray (freeboxPool);

        for (int i = 0; i < array.length (); i++)
        {
          Freebox freebox = new Freebox (array.getJSONObject (i));

          if (freebox.isConsistent ())
          {
            mPainter.useColor (freebox.getColor ());
            mBoxes.add (freebox);

            if (freebox.hasFocus () || (focus == null))
            {
              focus = freebox;
            }
          }
        }
      }
      catch (org.json.JSONException e)
      {
        e.printStackTrace ();
      }

      if (focus != null)
      {
        mListener.onFreeboxSelected (focus);
      }
    }

    //noinspection ConstantConditions
    if (BuildConfig.BUILD_TYPE.equals ("debug"))
    {
      CharSequence text = String.valueOf (mBoxes.size ());

      Toast toast = Toast.makeText (mActivity,
                                    text + " Freebox",
                                    Toast.LENGTH_SHORT);
      toast.show ();
    }
  }

  // ---------------------------------------------------
  void discloseBoxes ()
  {
    mLogger = new Logger (mActivity,
                          "0x59",
                          "Home");

    mBoxes.clear ();

    recoverSavedBoxes ();

    mBrutusSniffer = new BrutusSniffer (mActivity,this);
    mBrutusSniffer.start (24322);

    mPlayerSniffer = new BonjourSniffer (mActivity, this);
    mPlayerSniffer.start ("_hid._udp");
//    mBonjourSniffer.start ("_services._dns-sd._udp");

    mGatewaySniffer = new BonjourSniffer (mActivity, this);
    mGatewaySniffer.start ("_fbx-api._tcp");

    mSimulator = new Simulator (mActivity, this);
    mSimulator.start ();
  }

  // ---------------------------------------------------
  void concealBoxes ()
  {
    mBrutusSniffer.stop  ();
    mPlayerSniffer.stop ();
    mGatewaySniffer.stop ();
    mSimulator.stop      ();

    save ();

    mBoxes.clear ();
  }

  // ---------------------------------------------------
  private void save ()
  {
    SharedPreferences.Editor editor = mPreferences.edit ();
    JSONArray                array  = new JSONArray ();

    for (Freebox box : mBoxes)
    {
      JSONObject json = box.getJson ();

      if (json != null)
      {
        Log.e (Freeteuse.TAG, String.valueOf (json));
        array.put (json);
      }
    }

    editor.putString ("freebox_pool", String.valueOf (array));
    editor.commit ();
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxDetected (Freebox        freebox,
                                 FreeboxSniffer sniffer)
  {
    if (freebox.descriptionContains ("._fbx-api._tcp"))
    {
      String boxModel = freebox.getDescriptionField ("box_model");

      mLogger.Log ("- box_model = " + boxModel);

      try
      {
        changeMulticlickShortcuts (boxModel);
      }
      catch (JSONException | IOException e)
      {
        mLogger.Log ("- No shortcuts");
      }
    }
    else
    {
      for (Freebox box : mBoxes)
      {
        if (freebox.equals (box))
        {
          mLogger.Log ("- " + sniffer + "/" + freebox);

          box.detected ();
          mListener.onFreeboxDetected (box);
          return;
        }
      }

      if (freebox.getColor () == null)
      {
        freebox.setColor (mPainter.getColor ());
      }
      mBoxes.add (freebox);
      mListener.onFreeboxDetected (freebox);
    }
  }

  // ---------------------------------------------------
  void paintBoxButtons (Freebox     middle,
                        ImageButton leftSelector,
                        ImageButton middleSelector,
                        ImageButton rightSelector)
  {
    Freebox rightBox = getNextReachable     (middle);
    Freebox leftBox  = getPreviousReachable (middle);

    rightSelector.setVisibility (View.INVISIBLE);
    if (rightBox != null)
    {
      rightSelector.setVisibility    (View.VISIBLE);
      rightSelector.setImageResource (mPainter.getResourceId ("next",
                                                              rightBox.getColor ()));
    }

    middleSelector.setImageResource (mPainter.getResourceId ("free",
                                                             middle.getColor ()));

    leftSelector.setVisibility  (View.INVISIBLE);
    if (leftBox != null)
    {
      leftSelector.setVisibility    (View.VISIBLE);
      leftSelector.setImageResource (mPainter.getResourceId ("previous",
                                                             leftBox.getColor ()));
    }
  }

  // ---------------------------------------------------
  Freebox getNextReachable (Freebox of)
  {
    boolean found = false;

    for (Freebox box : mBoxes)
    {
      if (found && box.isReachable ())
      {
        return box;
      }

      if (box.equals (of))
      {
        found = true;
      }
    }

    return null;
  }

  // ---------------------------------------------------
  Freebox getPreviousReachable (Freebox of)
  {
    Freebox previous = null;

    for (Freebox box : mBoxes)
    {
      if (box.equals (of))
      {
        break;
      }

      if (box.isReachable ())
      {
        previous = box;
      }
    }

    return previous;
  }

  // ---------------------------------------------------
  private void changeMulticlickShortcuts (String boxModel) throws JSONException, IOException
  {
    if (boxModel != null)
    {
      String[]    shortModel = boxModel.split ("-");
      InputStream stream     = mActivity.getAssets ().open (shortModel[0] + "/shortcuts.json");
      int         size       = stream.available ();
      byte[]      buffer     = new byte[size];

      if (stream.read (buffer) != -1)
      {
        Resources resources     = mActivity.getResources();
        String    content       = new String    (buffer, StandardCharsets.UTF_8);
        JSONArray jsonShortcuts = new JSONArray (content);

        for (int i = 0; i < jsonShortcuts.length (); i++)
        {
          JSONObject jsonShortcut = jsonShortcuts.getJSONObject (i);
          String     shortcutName = jsonShortcut.keys ().next ();
          String     keySequence  = jsonShortcut.getString (shortcutName);

          if (!keySequence.isEmpty ())
          {
            int viewId = resources.getIdentifier (shortcutName,
                                                  "id",
                                                  mActivity.getPackageName ());

            if (viewId != 0)
            {
              View view = mActivity.findViewById (viewId);

              view.setTag ("onMultiClick:" + keySequence);
            }
          }
        }
      }
      stream.close ();
    }
  }
}
