package flingr.app.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import flingr.app.FlingrApplication;
import flingr.app.R;
import flingr.app.entities.Connection;
import flingr.app.receivers.NotificationActionReceiver;
import flingr.app.utilities.Serializer;

/**
 * A service to upload files to a Flingr server.
 */
public class FileUploadIntentService extends IntentService
{
    private static final String ACTION_SEND_FILE = "flingr.app.services.action.sendfile";

    private static final String EXTRA_FILEURI = "flingr.app.services.extra.fileuri";
    private static final String EXTRA_FILENAME = "flingr.app.services.extra.filename";
    private static final String EXTRA_CONNECTION = "flingr.app.services.extra.connection";

    private static final int STATUS_NOTIFICATION_ID = 958723;
    private static final int DONE_NOTIFICATION_ID = 958724;

    private static volatile boolean isCancelled = false;

    private static final int TIMEOUT = 5000;

    private NotificationCompat.Builder statusNotBuilder;
    private NotificationCompat.Builder doneNotBuilder;

    /**
     * Default constructor.
     */
    public FileUploadIntentService()
    {
        super("FileUploadIntentService");
    }

    /**
     * Static method to start the service.
     *
     * @param context  Context that sends the intent.
     * @param fileUri  The URI of the file to send.
     * @param fileName Name of the file.
     */
    public static void startService(Context context, Connection connection, Uri fileUri, String fileName)
    {
        Intent intent = new Intent(context, FileUploadIntentService.class);
        intent.setAction(ACTION_SEND_FILE);
        intent.putExtra(EXTRA_FILEURI, fileUri);
        intent.putExtra(EXTRA_FILENAME, fileName);
        intent.putExtra(EXTRA_CONNECTION, Serializer.serialize(connection));
        context.startService(intent);
    }

    /**
     * Sets a flag to cancel the upload.
     */
    public static void cancelUpload()
    {
        isCancelled = true;
    }

    /**
     * Clears the done notification posted after an operation was cancelled or completed.
     *
     * @param context An instance of a lifecycle object to get the Notification Manager from.
     */
    public static void doneWithNotification(Context context)
    {
        context.getSystemService(NotificationManager.class).cancel(DONE_NOTIFICATION_ID);
    }

    /**
     * Receives intent from sender to process.
     *
     * @param intent The received intent.
     */
    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_SEND_FILE.equals(action))
            {
                isCancelled = false;
                final Uri fileUri = intent.getParcelableExtra(EXTRA_FILEURI);
                final String fileName = intent.getStringExtra(EXTRA_FILENAME);
                final Connection connection =
                        Serializer.deserialize(intent.getByteArrayExtra(EXTRA_CONNECTION));
                initializeFileSend(connection, fileUri, fileName);
            }
        }
    }

    /**
     * Initialize sending of the file.
     *
     * @param connection Connection information.
     * @param fileUri    The uri of the file to send.
     * @param fileName   The name of the file to send.
     */
    private void initializeFileSend(Connection connection, Uri fileUri, String fileName)
    {
        // Build the intent structure
        Intent broadcastIntent = new Intent(this, NotificationActionReceiver.class);
        broadcastIntent.setAction(NotificationActionReceiver.CANCEL_FILEUPLOAD_ACTION);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0,
                broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set notification settings that won't change across posts
        statusNotBuilder = new NotificationCompat.Builder(this, FlingrApplication.CHANNEL_ID)
                .addAction(R.mipmap.ic_launcher, "Cancel", actionIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setContentTitle("Uploading " + fileName)
                .setOnlyAlertOnce(true);

        // Build the intent structure
        Intent doneBroadcastIntent = new Intent(this, NotificationActionReceiver.class);
        doneBroadcastIntent.setAction(NotificationActionReceiver.DONE_FILEUPLOAD_ACTION);
        PendingIntent doneActionIntent = PendingIntent.getBroadcast(this, 0,
                doneBroadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set notification settings that won't change across posts
        doneNotBuilder = new NotificationCompat.Builder(this, FlingrApplication.CHANNEL_ID)
                .addAction(R.mipmap.ic_launcher, "Close", doneActionIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setContentTitle("Flingr")
                .setOnlyAlertOnce(true);

        // Initiate SSH connection and send file
        sendFile(connection, fileUri, fileName);
    }

    /**
     * Business logic to send a file using JSCH.
     *
     * @param connection The connection to be established's information.
     * @param fileUri The URI to the file to send.
     * @param fileName The name of the file to send.
     */
    private void sendFile(Connection connection, Uri fileUri, String fileName)
    {
        Session session = null;

        try
        {
            // Create a new instance of JSch
            JSch jSch = new JSch();

            // Try the local connection first
            session = getLocalSession(jSch, connection);

            // If the local connection didn't work, try a WAN one
            if (session == null)
            {
                // Create the WAN session
                session = getWANSession(jSch, connection);

                // Try and send the file
                sftpFileSend(session, fileUri, fileName);
            }
            else
            {
                // Try and send the file
                boolean fileSent = sftpFileSend(session, fileUri, fileName);
                // If the file send fails, try WAN
                if (!fileSent)
                {
                    session = getWANSession(jSch, connection);
                    sftpFileSend(session, fileUri, fileName);
                }
            }
        }
        finally
        {
            if (session != null)
            {
                session.disconnect();
            }
        }
    }

    /**
     * Try to create a local session
     *
     * @param jsch Jsch instance.
     * @param connection Connection information.
     *
     * @return The local session, if properly instantiated.
     */
    private Session getLocalSession(JSch jsch, Connection connection)
    {
        Session session;
        try
        {
            session = jsch.getSession(connection.getUserName(), connection.getLocalAddress(),
                    connection.getLocalPort());
            session.setPassword(connection.getUserPassword());
            session.setTimeout(TIMEOUT);

            // There be dragons here...but this is just a working demonstration
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
        }
        catch (JSchException | NullPointerException e) {
            session = null;
        }

        return session;
    }

    /**
     * Try to create a WAN session.
     *
     * @param jsch Jsch instance.
     * @param connection Connection information.
     *
     * @return The WAN session, if properly instantiated.
     */
    private Session getWANSession(JSch jsch, Connection connection)
    {
        Session session;
        try
        {
            // Create session
            session = jsch.getSession(connection.getUserName(), connection.getWanAddress(),
                    connection.getWanPort());
            session.setPassword(connection.getUserPassword());
            // Sure would be nice if we knew if the timeout was in seconds or milliseconds
            session.setTimeout(TIMEOUT);

            // There be dragons here...but this is just a working demonstration
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
        }
        catch (JSchException | NullPointerException e)
        {
            session = null;
        }

        return session;
    }

    /**
     * Try to send the file using a sftp channel.
     *
     * @param session Session instance.
     * @param fileUri Uri of the file to send.
     * @param fileName Name of the file to be sent.
     *
     * @return True if successful.
     */
    private boolean sftpFileSend(Session session, Uri fileUri, String fileName)
    {
        boolean retVal = false;
        ChannelSftp sftpChannel = null;
        try
        {
            if (session != null && session.isConnected())
            {
                try
                {
                    sftpChannel = (ChannelSftp) session.openChannel("sftp");
                    sftpChannel.connect(TIMEOUT);

                    try
                    {
                        InputStream inputStream
                                = getContentResolver().openInputStream(fileUri);

                        if (inputStream != null)
                        {
                            try
                            {
                                if (sftpChannel.isConnected())
                                {
                                    sftpChannel.put(inputStream, fileName,
                                            new TransferProgressUpdater(sftpChannel, fileName,
                                                    inputStream.available()), ChannelSftp.OVERWRITE);

                                    retVal = true;
                                }
                                else
                                {
                                    postDoneNotification("Could not establish SFTP channel");
                                }
                            }
                            catch (SftpException | IOException e)
                            {
                                postDoneNotification("Could not transfer file");
                            }
                        }
                        else
                        {
                            throw new FileNotFoundException();
                        }
                    }
                    catch (FileNotFoundException e)
                    {
                        postDoneNotification("Unable to open selected file");
                    }
                }
                catch (JSchException e)
                {
                    postDoneNotification("Unable to establish an SFTP channel");
                }
            }
            else
            {
                postDoneNotification("Unable to establish a connection");
            }
        }
        finally
        {
           if (sftpChannel != null)
           {
               sftpChannel.disconnect();
           }
        }
        return retVal;
    }

    /**
     * Updates a notification builder with new content.
     *
     * @param contentText Text body of notification
     */
    private void updateNotification(String contentText)
    {
        if (statusNotBuilder != null)
        {
            Notification notification = statusNotBuilder.setContentText(contentText)
                    .build();

            // Post the notification
            getSystemService(NotificationManager.class).notify(STATUS_NOTIFICATION_ID, notification);
        }
    }

    /**
     * Posts an operation done notification.
     *
     * @param contentText Text body of notification.
     */
    private void postDoneNotification(String contentText)
    {
        if (doneNotBuilder != null)
        {
            Notification notification = doneNotBuilder.setContentText(contentText)
                    .build();

            // Get rid of the status notification
            getSystemService(NotificationManager.class).cancel(STATUS_NOTIFICATION_ID);
            // Post the notification
            getSystemService(NotificationManager.class).notify(DONE_NOTIFICATION_ID, notification);
        }
    }


    /**
     * Monitor that provides that periodically updates the current percentage of file that
     * has been transferred.
     */
    private class TransferProgressUpdater implements SftpProgressMonitor
    {
        private final long fileBytesMax;
        private final String fileName;
        private final ChannelSftp channelSftp;


        TransferProgressUpdater(ChannelSftp channelSftp, String fileName, long fileSize)
        {
            fileBytesMax = fileSize;
            this.fileName = fileName;
            this.channelSftp = channelSftp;
        }

        @Override
        public void init(int op, String src, String dest, long max)
        {
            // Don't do anything
        }

        @Override
        public boolean count(long count)
        {
            if (isCancelled)
            {
                return false;
            }
            else
            {
                long percentComplete = (count / fileBytesMax) * 100L;
                updateNotification(String.valueOf(percentComplete) + "% complete");
                return true;
            }
        }

        @Override
        public void end()
        {
            if (isCancelled)
            {
                postDoneNotification(fileName + " upload cancelled");

                try
                {
                    if (channelSftp != null) {
                        channelSftp.rm(fileName);
                    }
                }
                catch (SftpException e)
                {
                    updateNotification("Unable to delete file artifact from server" +
                            " after upload was cancelled");
                }
            }
            else
            {
                postDoneNotification(fileName + " uploaded successfully");
            }
        }
    }
}
