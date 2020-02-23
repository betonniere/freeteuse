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

package bzh.leroux.yannick.freeteuse.sniffers;

import android.os.Handler;

import bzh.leroux.yannick.freeteuse.Freebox;

public class FreeboxSniffer
{
  public interface Listener
  {
    void onFreeboxDetected (Freebox        freebox,
                            FreeboxSniffer sniffer);
  }

  private Handler  mHandler;
  private Listener mListener;
  private String   mName;

  // ---------------------------------------------------
  FreeboxSniffer (String   name,
                  Listener listener)
  {
    mName     = name;
    mHandler  = new Handler ();
    mListener = listener;
  }

  // ---------------------------------------------------
  public String getName () {
    return mName;
  }

  // ---------------------------------------------------
  @SuppressWarnings("NullableProblems")
  @Override
  public String toString () {
    return mName;
  }

  // ---------------------------------------------------
  void onFreeboxDetected (final Freebox freebox)
  {
    if (mListener != null) {
      mHandler.post (new Runnable () {
        @Override
        public void run () {
          mListener.onFreeboxDetected (freebox,
                                       FreeboxSniffer.this);
        }
      });
    }
  }
}
