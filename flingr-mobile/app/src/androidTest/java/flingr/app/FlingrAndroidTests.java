package flingr.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import flingr.app.entities.Connection;
import flingr.app.managers.ConnectionManager;
import flingr.app.remote.JSONResponseParser;
import flingr.app.utilities.CryptographyHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class FlingrAndroidTests
{
    private static final String TEST_STRING = "THIS MESSAGE NEEDS TO BE ENCRYPTED/DECRYPTED";

    private List<Connection> connectionList;

    @Before
    public void initializeVars()
    {
        // Confirm the context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("flingr.app", appContext.getPackageName());

        connectionList = new ArrayList<Connection>()
        {
            {
                // Create an example list of Previous Connections
                add(new Connection("12345",
                        "Test1", "1.1.1.1", 1234, "tester1", "password1"));
                add(new Connection("2222", "Test2",
                        "2.2.2.2", 2222, "tester2", "password2"));
                add(new Connection("33333", "Test2",
                        "3.3.3.3", 3333, "tester3", "password3"));
            }
        };
    }

    @Test
    public void testEncryptionAndDecryption()
    {
        // Clear any existing keys in the android key store
        CryptographyHelper.removeExistingKeyEntry();

        // Create a new secret key and generate encrypted text
        String encryptedText = CryptographyHelper.encrypt(TEST_STRING);

        assertNotNull(encryptedText);
        assertFalse(encryptedText.isEmpty());

        // Decrypt the text
        String decryptedText = CryptographyHelper.decrypt(encryptedText);

        assertNotNull(decryptedText);
        assertEquals("The decrypted text did not match what was encrypted.",
                TEST_STRING, decryptedText);

        // Load existing key from keystore and generate encrypted text
        encryptedText = CryptographyHelper.encrypt(TEST_STRING);

        assertNotNull(encryptedText);
        assertFalse(encryptedText.isEmpty());

        // Decrypt the text
        decryptedText = CryptographyHelper.decrypt(encryptedText);

        assertNotNull(decryptedText);
        assertEquals("The decrypted text did not match what was encrypted.",
                TEST_STRING, decryptedText);

        // Remove the key from the key store
        assertTrue(CryptographyHelper.removeExistingKeyEntry());

        // And make sure we can't decrypt something without a key in the store
        decryptedText = CryptographyHelper.decrypt(encryptedText);

        assertNull(decryptedText);
    }

    @Test
    public void testSavingAndRestoringPreviousConnections()
    {
        Context context = InstrumentationRegistry.getTargetContext();

        // Add previous connections
        ConnectionManager.getInstance().addConnections(connectionList);
        assertEquals("Expected size of previous connections is wrong",
                ConnectionManager.getInstance().numOfConnections(),
                3);

        SharedPreferences sharedPrefs =
                context.getSharedPreferences(ConnectionManager.FLINGR_PREFS_LABEL,
                        Context.MODE_PRIVATE);


        // Remove any previous values stored
        sharedPrefs.edit().remove(ConnectionManager.CONNECTIONS_LIST).commit();

        // Attempt to save the previous connections to Shared Preferences
        ConnectionManager.getInstance().saveToPreferences(context);

        // Clear the connections
        ConnectionManager.getInstance().clearConnections();


        // Attempt to repopulate the previous connections list
        ConnectionManager.getInstance().readFromPreferences(context);

        assertEquals("Number of objects de-serialized are not equal", 3,
                ConnectionManager.getInstance().numOfConnections());
    }

    @Test
    public void testJson()
    {

        String jsonString = "{\"Item\": {\"lanPort\": {\"S\": \"22\"}, \"wanPort\": {\"S\": \"5718\"}, \"ipAddr\": {\"S\": \"123.456.78.9\"}, \"lanAddr\": {\"S\": \"192.168.1.23\"}, \"timestamp\": {\"S\": \"1552160915\"}, \"id\": {\"S\": \"4OPRA9\"}}, \"ResponseMetadata\": {\"RetryAttempts\": 0, \"HTTPStatusCode\": 200, \"RequestId\": \"IHF98DVC9GAK3LT62M7AN6FF3FVV4KQNSO5AEMVJF66Q9ASUAAJG\", \"HTTPHeaders\": {\"x-amzn-requestid\": \"IHF98DVC9GAK3LT62M7AN6FF3FVV4KQNSO5AEMVJF66Q9ASUAAJG\", \"content-length\": \"167\", \"server\": \"Server\", \"connection\": \"keep-alive\", \"x-amz-crc32\": \"253008430\", \"date\": \"Tue, 12 Mar 2019 00:35:25 GMT\", \"content-type\": \"application/x-amz-json-1.0\"}}}";

        Connection response = JSONResponseParser.parseJSON(jsonString);

        assertNotNull(response);

        assertEquals(Integer.valueOf(22), response.getLocalPort());
        assertEquals(Integer.valueOf(5718), response.getWanPort());
        assertEquals("123.456.78.9", response.getWanAddress());
        assertEquals("192.168.1.23", response.getLocalAddress());
        assertEquals("4OPRA9", response.getActivationCode());
    }
}
