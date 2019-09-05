package flingr.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import flingr.app.services.FileUploadIntentService;

/**
 * Receives intents
 */
public class NotificationActionReceiver extends BroadcastReceiver
{
    public static final String CANCEL_FILEUPLOAD_ACTION
            = "flinger.app.receivers.NotificationActionReceiver.cancel_upload_action";

    public static final String DONE_FILEUPLOAD_ACTION
            = "flinger.app.receivers.NotificationActionReceiver.done_upload_action";
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null)
        {
            if (CANCEL_FILEUPLOAD_ACTION.equals(intent.getAction()))
            {
                FileUploadIntentService.cancelUpload();
            }
            else if (DONE_FILEUPLOAD_ACTION.equals(intent.getAction()))
            {
                FileUploadIntentService.doneWithNotification(context);
            }
        }
    }
}
