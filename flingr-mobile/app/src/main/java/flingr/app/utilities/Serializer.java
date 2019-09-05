package flingr.app.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import timber.log.Timber;

/**
 * Quick and dirty Java 7+ Object serializer helper.
 */
public class Serializer
{

    /**
     * Turns a non-null Java object properly initialized in the virtual machine's class loader into a byte
     * array.
     *
     * @param object The object to serialize.
     *
     * @return A byte array representing the Java object.
     */
    public static byte[] serialize(Object object)
    {
        if (object != null)
        {
            try
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(object);
                return bos.toByteArray();
            }
            catch (Exception e)
            {
                Timber.e(e, "Unable to successfully serialize object");
            }
        }
        return null;
    }

    /**
     * Creates a Java object from a byte array.
     *
     * @param bytes The bytes to turn into a Java object.
     * @param <T> The type of object to turn the byte array into.
     *
     * @return An object, if successfully deserialized.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes)
    {
        if (bytes != null)
        {
            try
            {
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = new ObjectInputStream(bis);
                return (T) in.readObject();
            }
            catch (Exception e)
            {
                Timber.e(e, "Unable to deserialize object");
            }
        }
        return null;
    }
}
