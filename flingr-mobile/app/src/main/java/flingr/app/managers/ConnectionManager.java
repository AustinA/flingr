package flingr.app.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Collection;

import flingr.app.entities.Connection;
import flingr.app.utilities.Serializer;

/**
 * Reads from and writes to a list of previous successful connections {@link Connection}
 * to a "database" backed by {@link android.content.SharedPreferences}.
 */
public class ConnectionManager
{
    public static final String FLINGR_PREFS_LABEL = "FlingrPreferences";
    public static final String CONNECTIONS_LIST = "FlingrConnectionsList";


    private static final ArrayList<Connection> connections = new ArrayList<>();

    private static final ConnectionManager connectionManager = new ConnectionManager();

    public static ConnectionManager getInstance()
    {
        return connectionManager;
    }

    /**
     * Private constructor.
     *
     * .
     */
    private ConnectionManager()
    {
    }

    /**
     * Returns numbers of connections stored.
     *
     * @return Number of connections stored.
     */
    public int numOfConnections()
    {
        return connections.size();
    }

    /**
     * Adds a connection.
     *
     * @param connection New connection to add.
     */
    public void addConnection(Connection connection)
    {
        if (connection != null && !connections.contains(connection))
        {
            connections.add(connection);
        }
    }

    /**
     * Adds connections
     *
     * @param aConnections New connections to add.
     */
    public void addConnections(Collection<Connection> aConnections)
    {
        if (aConnections != null)
        {
            connections.addAll(aConnections);
        }
    }

    /**
     * Gets connections.
     *
     * @return List of connections
     */
    public ArrayList<Connection> getConnections()
    {
        return connections;
    }

    /**
     * Removes connection.
     *
     * @param index Index of connection to remove.
     *
     */
    public void removeConnection(int index)
    {
        if (index >= 0 && index < connections.size())
        {
            connections.remove(index);
        }
    }

    /**
     * Saves list of connections to Shared Preferences
     */
    public void saveToPreferences(Context context)
    {
        if (context != null)
        {
            byte[] serializedObj = Serializer.serialize(connections);
            if (serializedObj != null)
            {
                String base64 = Base64.encodeToString(serializedObj, Base64.DEFAULT);
                if (base64 != null)
                {
                    SharedPreferences sharedPreferences =
                            context.getSharedPreferences(FLINGR_PREFS_LABEL, Context.MODE_PRIVATE);
                    if (sharedPreferences != null)
                    {
                        sharedPreferences.edit().putString(CONNECTIONS_LIST, base64).apply();
                    }
                }
            }
        }
    }

    /**
     * Reads list of connections from Shared Preferences.
     */
    public void readFromPreferences(Context context)
    {
        if (context != null)
        {
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(FLINGR_PREFS_LABEL, Context.MODE_PRIVATE);
            if (sharedPreferences != null)
            {
                String base64List = sharedPreferences.getString(CONNECTIONS_LIST, null);
                if (base64List != null)
                {
                    byte[] serializedPreviousList = Base64.decode(base64List, Base64.DEFAULT);
                    if (serializedPreviousList != null)
                    {
                        ArrayList<Connection> sharedPrefConnection =
                                Serializer.deserialize(serializedPreviousList);

                        if (sharedPrefConnection != null)
                        {
                            connections.clear();
                            connections.addAll(sharedPrefConnection);
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears Connections from storage
     */
    public void clearConnections()
    {
        connections.clear();
    }
}
