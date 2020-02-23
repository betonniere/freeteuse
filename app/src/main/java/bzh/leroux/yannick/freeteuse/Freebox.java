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

import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Freebox
{
  public interface Listener
  {
    void onFreeboxStatus (String status);
  }

  private long    mRcu;
  private String  mAddress;
  private int     mPort;
  private String  mDescription;
  private boolean mHasFocus;
  private boolean mReachable;
  private String  mColor;
  private Thread  mStatusLooper;

  // ---------------------------------------------------
  Freebox (JSONObject json)
  {
    try
    {
      mPort     = json.getInt     ("port");
      mAddress  = json.getString  ("address");
      mHasFocus = json.getBoolean ("focus");
      mColor    = json.getString  ("color");
    }
    catch (JSONException e)
    {
      e.printStackTrace ();
    }
  }

  // ---------------------------------------------------
  public Freebox (String address,
                  int    port,
                  String description)
  {
    mReachable   = true;
    mAddress     = address;
    mPort        = port;
    mDescription = description;

    Log.d (Freeteuse.TAG, mAddress);
  }

  // ---------------------------------------------------
  boolean descriptionContains (String keyword)
  {
    if (mDescription != null)
    {
      return mDescription.contains (keyword);
    }

    return false;
  }

  // ---------------------------------------------------
  String getDescriptionField (String field)
  {
    if (mDescription != null)
    {
      Pattern pattern = Pattern.compile (field + "=(\\p{Print}*)");
      Matcher matcher = pattern.matcher (mDescription);

      if (matcher.find ())
      {
        return matcher.group (1);
      }
    }

    return null;
  }

  // ---------------------------------------------------
  @SuppressWarnings("NullableProblems")
  @Override
  public String toString () {
    return mAddress + ":" + mPort;
  }

  // ---------------------------------------------------
  boolean hasFocus ()
  {
    return mHasFocus;
  }

  // ---------------------------------------------------
  boolean isConsistent ()
  {
    return mAddress != null;
  }

  // ---------------------------------------------------
  boolean isReachable ()
  {
    return mReachable;
  }

  // ---------------------------------------------------
  void detected ()
  {
    mReachable = true;
  }

  // ---------------------------------------------------
  String getColor ()
  {
    return mColor;
  }

  // ---------------------------------------------------
  void setColor (String color)
  {
    mColor = color;
  }

  // ---------------------------------------------------
  boolean equals (Freebox freebox)
  {
    //noinspection SimplifiableIfStatement
    if (freebox == null)
    {
      return false;
    }

    return ((mPort == freebox.mPort) && mAddress.equals (freebox.mAddress));
  }

  // ---------------------------------------------------
  JSONObject getJson ()
  {
    JSONObject json = new JSONObject ();

    try
    {
      json.put ("port",    mPort);
      json.put ("address", mAddress);
      json.put ("focus",   mHasFocus);
      json.put ("color",   mColor);
    }
    catch (JSONException e)
    {
      e.printStackTrace ();
      return null;
    }

    return json;
  }

  // ---------------------------------------------------
  void connect (final Listener listener)
  {
    if ((mAddress != null) && (mPort != 0))
    {
      final Handler listenerHandler = new Handler ();

      mRcu = jniCreateRcu ();

      jniConnectRcu (mRcu,
                     mAddress,
                     mPort);

      mStatusLooper = new Thread (new Runnable ()
      {
        @Override
        public void run ()
        {
          while (true)
          {
            final String status = jniReadRcuStatus (mRcu);

            if (listener != null)
            {
              listenerHandler.post (new Runnable ()
              {
                @Override
                public void run ()
                {
                  listener.onFreeboxStatus (status);
                }
              });
            }

            if (status.equals ("EXIT"))
            {
              break;
            }
          }
        }
      }, "Freebox(" + mAddress + ")");
      mStatusLooper.start ();
    }
  }

  // ---------------------------------------------------
  void disconnect ()
  {
    if (mStatusLooper != null) {
      jniDisconnectRcu (mRcu);

      try {
        mStatusLooper.join ();

        jniDestroyRcu (mRcu);
        mRcu = 0;
      } catch (InterruptedException e) {
        e.printStackTrace ();
      }
    }
  }

  // ---------------------------------------------------
  void onSpecialKeyPressed (int keyCode)
  {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    {
      pressRcuKey ("0x01",
                   "0xe9",
                   false);
    }
    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
    {
      pressRcuKey ("0x01",
                   "0xea",
                   false);
    }
    else if (keyCode == KeyEvent.KEYCODE_DEL)
    {
      pressRcuKey ("0x02",
                   "0x2a",
                   false);
    }
  }

  // ---------------------------------------------------
  void onSpecialKeyReleased (int keyCode)
  {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    {
      releaseRcuKey ("0x01",
                     "0xe9");
    }
    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
    {
      releaseRcuKey ("0x01",
                     "0xea");
    }
    else if (keyCode == KeyEvent.KEYCODE_DEL)
    {
      releaseRcuKey ("0x02",
                     "0x2a");
    }
  }

  // ---------------------------------------------------
  void pressRcuKey (String  report_id,
                    String  key_code,
                    boolean with_release)
  {
    try
    {
      pressRcuKey (Integer.decode(report_id),
                   Integer.decode(key_code),
                   with_release);
    }
    catch (NumberFormatException e)
    {
      Log.e (Freeteuse.TAG, String.valueOf(e));
    }
  }

  // ---------------------------------------------------
  void pressRcuKey (int report_id,
                    int key_code,
                    boolean with_release)
  {
    jniPressRcuKey (mRcu,
                    report_id,
                    key_code,
                    with_release);
  }

  // ---------------------------------------------------
  void releaseRcuKey (String  report_id,
                      String  key_code)
  {
    jniReleaseRcuKey (mRcu,
                      Integer.decode(report_id),
                      Integer.decode(key_code));
  }

  // ---------------------------------------------------
  void grabFocus ()
  {
    mHasFocus = true;
  }

  // ---------------------------------------------------
  void releaseFocus ()
  {
    mHasFocus = false;
  }

  // ---------------------------------------------------
  private native long   jniCreateRcu     ();
  private native void   jniDestroyRcu    (long rcu);

  private native void   jniConnectRcu    (long rcu, String address, int port);
  private native void   jniDisconnectRcu (long rcu);

  private native void   jniPressRcuKey   (long rcu, int report_id, int key_code, boolean with_key_release);
  private native void   jniReleaseRcuKey (long rcu, int report_id, int key_code);

  private native String jniReadRcuStatus (long rcu);
}
