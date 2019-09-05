package flingr.app.utilities;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import timber.log.Timber;

/**
 * Static class that encrypts and decrypts strings.
 * <p>
 * The output of the {@link CryptographyHelper#encrypt(String)} is a comma delimited, Base64
 * string first of the initialization vector, then of data encrypted using AES/CBC/PKCS7
 * <p>
 * The Android Key Store is used as the store provider.
 */
public class CryptographyHelper
{
    // Key store provider, which in this case is Android's default
    private static final String KEY_STORE_PROVIDER = "AndroidKeyStore";
    // The alias the key will be saved under in the key store provider
    private static final String ALIAS = "FlingrAlias";

    // The cipher transformation used
    private static final String TRANSFORMATION = KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7;

    private static final String CSV_REGEX = "\\s*,\\s*";

    /**
     * Encrypts a string using the transformation defined in
     * {@link CryptographyHelper#TRANSFORMATION}.
     * <p>
     * If no existing key is found in the key provider, this function will attempt to create one.
     *
     * @param content The string of information to be encrypted.
     * @return If successful, a comma delimited string of two Base64 strings in the format
     * <Base64 initialization vector>,<Base64 encrypted input parameter>. Returns null if
     * any failure occurs.
     */
    public static String encrypt(String content)
    {
        String retVal = null;

        SecretKey key = getSecretKey(true);
        if (key != null)
        {
            try
            {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, key);

                String IV = Base64.encodeToString(cipher.getIV(), Base64.DEFAULT);

                byte[] encryptedContent = cipher.doFinal(content.getBytes());
                retVal = IV + ","
                        + Base64.encodeToString(encryptedContent, Base64.DEFAULT);
            }
            catch (Exception e)
            {
                Timber.e(e, "Error initializing the algorithm or encrypting the content");
            }
        }
        return retVal;
    }

    /**
     * Takes in a comma delimited string in the format
     * <Base64 initialization vector>, <Base64 encrypted data> and decrypts to plaintext.
     *
     * @param content The encrypted content and initialization vector in Base64 format.
     * @return The plaintext version of the encrypted parameter, null if a failure occurred.
     */
    public static String decrypt(String content)
    {
        String retVal = null;

        SecretKey key = getSecretKey(false);
        if (key != null)
        {
            try
            {
                String[] IV_and_encryptedData = content.split(CSV_REGEX);
                if (IV_and_encryptedData.length == 2)
                {
                    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.DECRYPT_MODE, key,
                            new IvParameterSpec(Base64.decode(IV_and_encryptedData[0], Base64.DEFAULT)));
                    byte[] encryptedData = Base64.decode(IV_and_encryptedData[1], Base64.DEFAULT);

                    retVal = new String(cipher.doFinal(encryptedData));
                }

            }
            catch (Exception e)
            {
                Timber.e(e, "Error initializing the algorithm or encrypting the content");
            }
        }
        return retVal;
    }

    /**
     * Attempts to remove an existing secret key from the key store provider defined
     * in {@link CryptographyHelper#KEY_STORE_PROVIDER}.
     *
     * @return True if a key was removed from the key store.
     */
    public static boolean removeExistingKeyEntry()
    {
        boolean retVal = false;

        KeyStore keyStore = getAndroidKeyStore();
        if (keyStore != null)
        {
            try
            {
                KeyStore.Entry keyEntry = keyStore.getEntry(ALIAS, null);
                if (keyEntry != null)
                {
                    keyStore.deleteEntry(ALIAS);
                    retVal = true;
                }
            }
            catch (Exception e)
            {
                Timber.e(e, "Unable to delete key entry");
            }
        }
        return retVal;
    }

    /**
     * Retrieves an existing key in the key store provider under {@link CryptographyHelper#ALIAS},
     * or creates a new key in the provider.
     *
     * @param createIfNotFound If none are found, create a new key in the key store provider
     *
     * @return A secret key to be used for encrypting and decrypting content, null if a failure occurred.
     */
    private static SecretKey getSecretKey(boolean createIfNotFound)
    {
        KeyStore keyStore = getAndroidKeyStore();

        try
        {
            KeyStore.Entry foundKey = keyStore.getEntry(ALIAS, null);

            return (foundKey instanceof KeyStore.SecretKeyEntry)
                    ? ((KeyStore.SecretKeyEntry) foundKey).getSecretKey()
                    : ((createIfNotFound) ? createSecretKey() : null);
        }
        catch (Exception e)
        {
            Timber.e(e, "Unable to recall a previously generated secret key");
        }

        return null;
    }

    /**
     * Creates a new secret key that will use a AES/CBC/PKCS7 transformation.
     *
     * @return A new secret key, null if a failure occured.
     */
    private static SecretKey createSecretKey()
    {
        KeyGenerator keyGenerator;
        try
        {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER);

            if (keyGenerator != null)
            {
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build());

                return keyGenerator.generateKey();
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Unable to create a secret key");
        }

        return null;
    }

    /**
     * Returns an instance of the key store provider by the name of
     * {@link CryptographyHelper#KEY_STORE_PROVIDER}.
     *
     * @return An instance of the requested key store provider, null if a failure occurred.
     */
    private static KeyStore getAndroidKeyStore()
    {
        KeyStore keyStore = null;
        try
        {
            keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER);
            keyStore.load(null);

        }
        catch (Exception e)
        {
            Timber.e(e,"Unable to get the AndroidKeyStore");
        }

        return keyStore;
    }

}
