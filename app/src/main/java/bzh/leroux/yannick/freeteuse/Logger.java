package bzh.leroux.yannick.freeteuse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Logger {

  private static List<Logger> mLoggers = new ArrayList<> ();
  private static AlertDialog  mAlert;

  private List<String> mLogs;
  private Context      mContext;
  private String       mName;
  private CheatCode    mCheatCode;

  // ---------------------------------------------------
  Logger (Context context,
          String  keySelector,
          String  name)
  {
    mContext   = context;
    mName      = name;
    mLogs      = new ArrayList<> ();
    mCheatCode = new CheatCode (keySelector);

    mLoggers.add (this);
  }

  // ---------------------------------------------------
  @SuppressWarnings("NullableProblems")
  @Override
  public String toString () {
    return mName;
  }

  // ---------------------------------------------------
  public static void stop () {
    hideAlert ();

    mLoggers.clear ();
  }

  // ---------------------------------------------------
  public void Log (String log)
  {
    Log.d (Freeteuse.TAG, "LOGGER: " + log);
    mLogs.add (log);
  }

  // ---------------------------------------------------
  static void onClick(String tag)
  {
    for (Logger logger: mLoggers)
    {
      if (logger.mCheatCode.discovered (tag))
      {
        logger.display ();
      }
    }
  }

  // ---------------------------------------------------
  private void display ()
  {
    hideAlert ();

    {
      StringBuilder       description = new StringBuilder ();
      AlertDialog.Builder builder     = new AlertDialog.Builder (mContext);

      builder.setTitle (mName);

      for (String log: mLogs)
      {
        description.append (log);
        description.append ("\n");
      }

      builder.setMessage (description);

      builder.setPositiveButton ("Fermer", new DialogInterface.OnClickListener()
      {
        public void onClick (DialogInterface dialog,
                             int              which)
        {
          hideAlert ();
        }
      });

      mAlert = builder.create ();
      mAlert.show ();
    }
  }

  // ---------------------------------------------------
  private static void hideAlert ()
  {
    if (mAlert != null)
    {
      mAlert.dismiss ();
      mAlert = null;
    }
  }
}
