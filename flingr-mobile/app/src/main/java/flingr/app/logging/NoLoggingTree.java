package flingr.app.logging;

import timber.log.Timber;

public class NoLoggingTree extends Timber.Tree
{
    @Override
    protected void log(int priority, String tag, String message, Throwable t)
    {
        // Swallow the logs into a black hole.
    }
}
