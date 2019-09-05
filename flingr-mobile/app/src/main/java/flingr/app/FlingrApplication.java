package flingr.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import flingr.app.logging.NoLoggingTree;
import timber.log.Timber;

public class FlingrApplication extends Application
{
    public static final String CHANNEL_ID = "FileUploadIntentService";

    @Override
    public void onCreate()
    {
        super.onCreate();
        if (BuildConfig.DEBUG)
        {
            Timber.plant(new Timber.DebugTree());
        }
        else
        {
            Timber.plant(new NoLoggingTree());
        }

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager
                    = getSystemService(NotificationManager.class);

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null)
            {
                NotificationChannel notChannel = new NotificationChannel(CHANNEL_ID, "Flingr",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(notChannel);
            }
        }
    }
}
