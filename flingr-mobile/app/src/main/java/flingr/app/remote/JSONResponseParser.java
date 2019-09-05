package flingr.app.remote;

import org.json.JSONException;
import org.json.JSONObject;

import flingr.app.entities.Connection;
import timber.log.Timber;

/**
 * Processes the returned JSON string from the AWS lambda request.
 */
public class JSONResponseParser
{

    // Keys for JSON response
    private static final String LAN_PORT = "lanPort";
    private static final String WAN_PORT = "wanPort";
    private static final String SELECT = "S";
    private static final String ITEM = "Item";
    private static final String ID = "id";
    private static final String IP_ADDR = "ipAddr";
    private static final String LAN_ADDR = "lanAddr";


    /**
     * Parses JSON from Flingr AWS lambda response for its connection values.
     *
     * @param responeString The JSON string.
     *
     * @return Connection object with returned information stored.
     */
    public static Connection parseJSON(String responeString)
    {
        if (responeString != null && !responeString.isEmpty())
        {
            try
            {
                JSONObject jsonObject = new JSONObject(responeString);

                JSONObject returnedItem = jsonObject.getJSONObject(ITEM);

                Connection response = new Connection();

                if (returnedItem.has(ID))
                {
                    JSONObject idObj = returnedItem.getJSONObject(ID);
                    if (idObj.has(SELECT))
                    {
                        response.setActivationCode(idObj.getString(SELECT));
                    }
                }

                if (returnedItem.has(LAN_PORT))
                {
                    JSONObject portObj = returnedItem.getJSONObject(LAN_PORT);
                    if (portObj.has(SELECT))
                    {
                        response.setLocalPort(portObj.getInt(SELECT));
                    }
                }

                if (returnedItem.has(WAN_PORT))
                {
                    JSONObject portObj = returnedItem.getJSONObject(WAN_PORT);
                    if (portObj.has(SELECT))
                    {
                        response.setWanPort(portObj.getInt(SELECT));
                    }
                }

                if (returnedItem.has(IP_ADDR))
                {
                    JSONObject ipAddrObj = returnedItem.getJSONObject(IP_ADDR);
                    if (ipAddrObj.has(SELECT))
                    {
                        response.setWanAddress(ipAddrObj.getString(SELECT));
                    }
                }

                if (returnedItem.has(LAN_ADDR))
                {
                    JSONObject ipAddrObj = returnedItem.getJSONObject(LAN_ADDR);
                    if (ipAddrObj.has(SELECT))
                    {
                        response.setLocalAddress(ipAddrObj.getString(SELECT));
                    }
                }

                return response;

            }
            catch (JSONException e)
            {
                Timber.e(e, "Could not parse JSON correctly");
            }
        }

        return null;
    }
}
