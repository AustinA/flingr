package flingr.app.remote;

import flingr.app.entities.Connection;

/**
 * Callback that is called from a {@link QueryServerTask#onPostExecute(Connection)} to process
 * the HTTP response from the AWS lambda service.
 */
public interface QueryServerCallback
{
    /**
     * Passes query results to object implementer.
     *
     * @param response The response from the AWS lambda service.
     */
    void onQueryCompleted(Connection response);
}
