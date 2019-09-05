package flingr.app.remote;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

/**
 * Performs all of the heavy-lifting to hand-roll an AWS lambda request and response.
 *
 * This class creates a query for connection information based on a user provided activation code.
 */
class AWSRequestor
{
    private static final String RegionName = "us-east-1"; //This is the regionName
    private static final String ServiceName = "execute-api";
    private static final String Algorithm = "AWS4-HMAC-SHA256";
    private static final String ContentType = "application/json";
    private static final String Host = "20d3ektd5h.execute-api.us-east-1.amazonaws.com/test"; // Link to the AWS API Gateway
    private static final String SignedHeaders = "content-type;host;x-amz-date";
    //private static final String xApiKey = "sorry, private"; // api key (Can we store this somewhere safer? XD)
    private static final String accessKey = "sorry, private"; // (Can we store this somewhere safer? XD)
    private static final String secretKey = "sorry, private"; // (Can we store this somewhere safer? XD)


    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    static String getRegistrationInfo(String activationCode)
    {
        if (activationCode != null && !activationCode.isEmpty())
        {
            String canonicalQuery = "Key=" + activationCode;

            HttpURLConnection connection = requestGET(canonicalQuery);
            BufferedReader in = null;
            if (connection != null)
            {
                try
                {
                    connection.connect();

                    int status = connection.getResponseCode();

                    if (status == HttpURLConnection.HTTP_OK)
                    {
                        in = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()));

                        String inputLine;
                        StringBuilder content = new StringBuilder();
                        while ((inputLine = in.readLine()) != null)
                        {
                            content.append(inputLine);
                        }

                        return content.toString();

                    }
                }
                catch (IOException e)
                {
                    Timber.e(e, "Error connecting the HTTP request or retrieving response");
                }
                finally
                {
                    connection.disconnect();
                    if (in != null)
                    {
                        try
                        {
                            in.close();
                        }
                        catch (IOException e)
                        {
                            Timber.e(e, "Couldn't close HTTPUrlConnection input stream");
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressLint("SimpleDateFormat")
    private static HttpURLConnection requestGET(String canonicalQueryString)
    {
        String hashedRequestPayload = "";

        String authorization = sign(hashedRequestPayload,
                canonicalQueryString);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        String requestDate = sdf.format(date) + "Z";

        try
        {
            URL url = new URL("https://" + Host + "/FlingrRegistration" + "?" + canonicalQueryString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", ContentType);
            connection.setRequestProperty("X-Amz-Date", requestDate);
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty("x-amz-content-sha256", hashedRequestPayload);
            connection.setConnectTimeout(5000);
            connection.setConnectTimeout(5000);

            return connection;
        }
        catch (IOException e)
        {
            Timber.e(e, "Could not create HttpURLConnection object");
        }
        return null;
    }

    @SuppressLint("SimpleDateFormat")
    private static String sign(String hashRequestPayload, String canonicalQueryString)
    {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat requestDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

        String dateStamp = dateFormat.format(currentDate);
        String requestDate = requestDateFormat.format(currentDate) + "Z";

        String credentialScope =
                dateStamp + "/" + RegionName + "/" + ServiceName + "/aws4_request";

        String canonicalHeaders = "Content-Type" + ":" + ContentType.trim() + "\n"
                + "Host" + ":" + Host.trim() + "\n" + "X-Amz-Date" + ":"
                + requestDate.trim() + "\n";

        String canonicalRequest = "GET" + "\n" + "/FlingrRegistration"
                + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + SignedHeaders
                + "\n" + hashRequestPayload;


        byte[] hashedCanonicalRequestBytes = hash(canonicalRequest
                .getBytes(StandardCharsets.UTF_8));
        if (hashedCanonicalRequestBytes != null)
        {
            String hashedCanonicalRequest = bytesToHex(hashedCanonicalRequestBytes);
            String stringToSign = Algorithm + "\n" + requestDate + "\n" + credentialScope
                    + "\n" + hashedCanonicalRequest;

            byte[] signingKey = getSignatureKey(dateStamp);
            if (signingKey != null)
            {
                byte[] hashStringToSign = createHmac256(stringToSign, signingKey);
                if (hashStringToSign != null)
                {
                    String signature = bytesToHex(hashStringToSign);

                    return Algorithm + " Credential=" + accessKey
                            + "/" + dateStamp + "/" + RegionName + "/" + ServiceName
                            + "/aws4_request, SignedHeaders=" + SignedHeaders + ", Signature="
                            + signature;
                }
            }
        }
        return null;
    }

    private static byte[] hash(byte[] toHashBytes)
    {
        byte[] retVal = null;
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            retVal = messageDigest.digest(toHashBytes);
        }
        catch (NoSuchAlgorithmException e)
        {
            Timber.e(e, "Could not generate SHA-256 hash");
        }

        return retVal;
    }

    private static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] getSignatureKey(String dateStamp)
    {
        byte[] kDate = createHmac256(dateStamp, ("AWS4" + AWSRequestor.secretKey).getBytes(StandardCharsets.UTF_8));
        if (kDate != null)
        {
            byte[] kRegion = createHmac256(AWSRequestor.RegionName, kDate);
            if (kRegion != null)
            {
                byte[] kService = createHmac256(AWSRequestor.ServiceName, kRegion);
                if (kService != null)
                {
                    return createHmac256("aws4_request", kService);
                }
            }
        }
        return null;
    }

    private static byte[] createHmac256(String data, byte[] key)
    {
        byte[] retVal = null;
        try
        {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(new SecretKeySpec(key, "HmacSHA256"));
            retVal = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            Timber.e(e, "Could not create HMAC-256 content");
        }
        return retVal;
    }
}
