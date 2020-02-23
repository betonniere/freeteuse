package bzh.leroux.yannick.freeteuse.sniffers;

import android.content.Context;

import bzh.leroux.yannick.freeteuse.Freeteuse;

public class BrutusSniffer extends    FreeboxSniffer
                           implements Runnable
{
  private static final String TAG = Freeteuse.TAG + "BrutusSniffer";

  private Thread mThread;

  // ---------------------------------------------------
  public BrutusSniffer (Context  context,
                        Listener listener)
  {
    super (TAG,
            listener);

    // mThread  = new Thread (this);
  }

  // ---------------------------------------------------
  public void start (int usualPort)
  {
    if (mThread != null)
    {
      mThread.start ();
    }
  }

  // ---------------------------------------------------
  public void stop ()
  {
    if (mThread != null)
    {
      mThread.interrupt ();
    }
  }

  // ---------------------------------------------------
  public void run ()
  {
  }
}
