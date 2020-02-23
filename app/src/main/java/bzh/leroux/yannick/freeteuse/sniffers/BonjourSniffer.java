package bzh.leroux.yannick.freeteuse.sniffers;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import bzh.leroux.yannick.freeteuse.Freebox;
import bzh.leroux.yannick.freeteuse.Freeteuse;

public class BonjourSniffer extends FreeboxSniffer
{
  private static final String TAG = Freeteuse.TAG + "BonjourSniffer";

  private static ResolverQueue mResolverQueue;
  private static NsdManager    mNsdManager;

  private NsdManager.DiscoveryListener mDiscoveryListener;
  private String                       mServiceType;

  // ---------------------------------------------------
  private class ResolverQueue
  {
    private Queue<NsdServiceInfo> mQueue;
    private boolean               mInProgress;
    private NsdManager            mNsdManager;

    ResolverQueue (NsdManager manager)
    {
      mQueue      = new LinkedList<> ();
      mNsdManager = manager;
    }

    void add (NsdServiceInfo serviceInfo)
    {
      mQueue.add (serviceInfo);
      resolveNext ();
    }

    void resolveNext ()
    {
      if (!(mInProgress || mQueue.isEmpty ()))
      {
        mInProgress = true;
        mNsdManager.resolveService (mQueue.poll (),
                                    new BonjourResolveListener ());
      }
    }

    void resolutionReceived ()
    {
      mInProgress = false;
      resolveNext ();
    }
  }

  // ---------------------------------------------------
  private class BonjourResolveListener implements NsdManager.ResolveListener
  {
    @Override
    public void onResolveFailed (NsdServiceInfo serviceInfo,
                                 int            errorCode)
    {
      Log.e (TAG, "Resolve failed (" + errorCode + ")");
      mResolverQueue.resolutionReceived ();
    }

    @Override
    public void onServiceResolved (final NsdServiceInfo serviceInfo) {
      Log.d (TAG, "Service resolved :" + serviceInfo);
      final Freebox freebox = new Freebox (serviceInfo.getHost ().getHostAddress (),
                                           serviceInfo.getPort (),
                                           serviceInfo.toString ());

      onFreeboxDetected (freebox);

      mResolverQueue.resolutionReceived ();
    }
  }

  // ---------------------------------------------------
  public BonjourSniffer (Context                 context,
                         FreeboxSniffer.Listener listener)
  {
    super (TAG,
            listener);

    if (mNsdManager == null)
    {
      mNsdManager = (NsdManager) context.getSystemService (Context.NSD_SERVICE);

      if (mNsdManager != null)
      {
        mResolverQueue = new ResolverQueue (mNsdManager);
      }
    }
  }

  // ---------------------------------------------------
  public void start (String serviceType)
  {
    mServiceType = serviceType;

    initializeDiscoveryListener ();

    if (mNsdManager != null)
    {
      mNsdManager.discoverServices (serviceType,
                                    NsdManager.PROTOCOL_DNS_SD,
                                    mDiscoveryListener);
    }
  }

  // ---------------------------------------------------
  public void stop ()
  {
    if (mNsdManager != null)
    {
      try
      {
        mNsdManager.stopServiceDiscovery (mDiscoveryListener);
      }
      catch (IllegalArgumentException ignored)
      {
      }
    }
  }

  // ---------------------------------------------------
  private void initializeDiscoveryListener ()
  {
    mDiscoveryListener = new NsdManager.DiscoveryListener ()
    {
      @Override
      public void onStartDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Discovery failed");
      }

      @Override
      public void onStopDiscoveryFailed (String serviceType, int errorCode)
      {
        Log.e (TAG, "Stopping discovery failed");
      }

      @Override
      public void onDiscoveryStarted (String serviceType)
      {
        Log.d (TAG, "Discovery started");
      }

      @Override
      public void onDiscoveryStopped (String serviceType)
      {
        Log.d (TAG, "Discovery stopped");
      }

      @Override
      public void onServiceFound (NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service found: << " + serviceInfo.getServiceName () + " >>");
        if (serviceInfo.getServiceType ().startsWith (mServiceType))
        {
          mResolverQueue.add (serviceInfo);
        }
      }

      @Override
      public void onServiceLost (NsdServiceInfo serviceInfo)
      {
        Log.d (TAG, "Service lost: " + serviceInfo.getServiceName ());
      }
    };
  }
}
