package flingr.app.utilities;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

/**
 * Create an entire class dedicated to parsing Intent contents for information pertaining to a
 * shared file received from another application.
 * <p>
 * Number of variations to receive shared files in Android so far:  3
 */
public class FileSharingIntentParser
{
    private static final String TEXT_PLAIN_MIME_TYPE = "text/plain";
    private static final String IMAGE_X_MIME_TYPE = "image/";

    private static SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyhhmmss", Locale.US);

    private Context context;

    /**
     * Default constructor.
     *
     * @param context An Android life-cycle object.
     */
    public FileSharingIntentParser(Context context)
    {
        Objects.requireNonNull(context);
        this.context = context;
    }

    /**
     * Parses a received Intent for pertinent information to handle a shared file.
     *
     * @param theIntent The received intent.
     * @return A entity object with the desired information
     * @throws NullPointerException if parameter is null
     */
    public SharedFileInfo parseIntent(Intent theIntent) throws NullPointerException
    {
        Objects.requireNonNull(theIntent);

        String intentAction = theIntent.getAction();
        String intentFileType = theIntent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && intentFileType != null)
        {

            /*
             * Handle "simple data" from other apps
             * see https://developer.android.com/training/sharing/receive
             */

            // Handle "simple" images
            if (intentFileType.contains(IMAGE_X_MIME_TYPE))
            {
                Parcelable someParcelable = theIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (someParcelable instanceof Uri)
                {
                    SharedFileInfo sharedFileInfo = new SharedFileInfo();
                    sharedFileInfo.fileUri = (Uri) someParcelable;
                    sharedFileInfo.fileName = getFileNameFromUri(sharedFileInfo.fileUri);
                    sharedFileInfo.mimeType = intentFileType;
                    return sharedFileInfo;
                }

            }
            // Handle "simple" plain text"
            else if (Objects.equals(intentFileType, TEXT_PLAIN_MIME_TYPE))
            {
                String sharedPlainText = theIntent.getStringExtra(Intent.EXTRA_TEXT);

                if (sharedPlainText != null)
                {
                    try
                    {
                        String fileName = sdf.format(new Date()) + ".txt";
                        FileOutputStream fop = context.openFileOutput(
                                fileName, Context.MODE_PRIVATE);

                        PrintStream printStream = new PrintStream(fop);
                        printStream.print(sharedPlainText);
                        printStream.close();

                        File outputFile = context.getFilesDir();

                        SharedFileInfo sharedFileInfo = new SharedFileInfo();
                        sharedFileInfo.mimeType = intentFileType;
                        sharedFileInfo.fileUri = Uri.fromFile(outputFile).buildUpon().appendPath(fileName).build();
                        sharedFileInfo.fileName = fileName;

                        return sharedFileInfo;

                    }
                    catch (FileNotFoundException e)
                    {
                        Timber.e(e, "Unable to open file for some reason");
                    }
                }
            }

            /*
             * See https://developer.android.com/training/secure-file-sharing/retrieve-info
             */
            Uri dataUri = theIntent.getData();
            if (dataUri != null)
            {
                SharedFileInfo sharedFileInfo = new SharedFileInfo();
                sharedFileInfo.fileUri = dataUri;
                sharedFileInfo.mimeType = intentFileType;
                sharedFileInfo.fileName = getFileNameFromUri(sharedFileInfo.fileUri);

                return sharedFileInfo;
            }


            /*
             * See https://developer.android.com/reference/android/content/ClipData?hl=en
             */
            ClipData clipData = theIntent.getClipData();
            if (clipData != null)
            {
                SharedFileInfo sharedFileInfo = new SharedFileInfo();

                // TODO  Only support one clipboard item at a time for now
                sharedFileInfo.mimeType = clipData.getDescription().getMimeType(0);

                ClipData.Item clipboardItem = clipData.getItemAt(0);
                sharedFileInfo.fileUri = clipboardItem.getUri();
                sharedFileInfo.fileName = getFileNameFromUri(clipboardItem.getUri());
                return sharedFileInfo;
            }
        }
        return null;
    }

    /**
     * Get the file name from a URI.  Assumes its a content:// scheme first using a
     * content resolver and DISPLAY_NAME, then try and pull the file name from the last
     * path segment.
     *
     * @param someUri Uri to get file name from.
     *
     * @return The file name, or null if one could not be determined.
     */
    private String getFileNameFromUri(Uri someUri)
    {
        String retVal = null;
        if (someUri != null)
        {
            ContentResolver contentResolver = context.getContentResolver();
            try (Cursor cursor = contentResolver
                    .query(someUri, null, null, null, null))
            {
                if (cursor != null)
                {
                    int fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (cursor.moveToFirst())
                    {
                        retVal = cursor.getString(fileNameIndex);
                    }
                }
            }

            if (retVal == null || retVal.isEmpty())
            {
                retVal = someUri.getLastPathSegment();
            }
        }
        return retVal;
    }


    /**
     * A entity representation of the pertinent information received from an App that shares a file.
     */
    public class SharedFileInfo
    {
        private Uri fileUri;
        private String mimeType;
        private String fileName;

        public Uri getFileUri()
        {
            return fileUri;
        }

        public String getMimeType()
        {
            return mimeType;
        }

        public String getFileName()
        {
            return fileName;
        }

    }
}
