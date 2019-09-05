package flingr.app.remote;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Objects;

import flingr.app.entities.Connection;
import timber.log.Timber;

/**
 * Generates an HTTP request to the Flingr AWS lambda service for connection information based on the
 * user provided activation code.
 */
public class QueryServerTask extends AsyncTask<String, Void, Connection>
{
    private static final int TIME_INTERVAL_MS = 10000;
    private static final int REQUEST_WAIT_MS = 500;

    private WeakReference<QueryServerCallback> callbackWeakRef;

    /**
     * Constructor
     *
     * @param callback The callback to be triggered when the request is finished or timed out.
     */
    public QueryServerTask(QueryServerCallback callback)
    {
        Objects.requireNonNull(callback);

        callbackWeakRef = new WeakReference<>(callback);
    }

    @Override
    protected Connection doInBackground(String...activationCode)
    {
        boolean success = false;
        Connection connection = null;

        long timeStarted = System.currentTimeMillis();

        while (isCancelled()
                || (!success && ((System.currentTimeMillis() - timeStarted) <= TIME_INTERVAL_MS)))
        {
            if (activationCode.length > 0 && activationCode[0] != null)
            {
                String jsonResult = AWSRequestor.getRegistrationInfo(activationCode[0]);

                connection = JSONResponseParser.parseJSON(jsonResult);

                if (connection != null && connection.isValid())
                {
                    success = true;
                }
            }

            if (!success)
            {
                try
                {
                    Thread.sleep(REQUEST_WAIT_MS);
                }
                catch (InterruptedException e)
                {
                    Timber.e(e, "Couldn't freeze async task");
                }
            }
        }

        return connection;
    }

    @Override
    protected void onPostExecute(Connection result)
    {
        QueryServerCallback callback = callbackWeakRef.get();
        if (callback != null)
        {
            callback.onQueryCompleted(result);
        }
    }
}
