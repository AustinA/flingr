package flingr.app.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class for storing parameters to establish a connection to an external device.
 */
public class Connection implements Serializable
{
    private String activationCode;
    private String colloquialName;
    private String wanAddress;
    private Integer wanPort;
    private String localAddress;
    private Integer localPort;
    private String userName;
    private String userPassword;

    /**
     * Default constructor
     */
    public Connection()
    {

    }

    /**
     * Constructor, ideally used for testing only
     *
     * @param activationCode Alphanumeric code to query Flingr servers for up to date connection information.
     * @param colloquialName User set name for connection.
     * @param ipAddress The WAN IP address.
     * @param port The WAN port.
     * @param userName The user name.
     * @param userPassword The user's password.
     */
    public Connection(String activationCode, String colloquialName, String ipAddress,
                      Integer port, String userName, String userPassword)
    {
        this.activationCode = activationCode;
        this.colloquialName = colloquialName;
        this.wanAddress = ipAddress;
        this.wanPort = port;

        this.userName = userName;
        this.userPassword = userPassword;
    }

    /**
     * Gets local address.
     *
     * @return The local address.
     */
    public String getLocalAddress()
    {
        return localAddress;
    }

    /**
     * Sets the local address
     * @param localAddress The local address.
     */
    public void setLocalAddress(String localAddress)
    {
        this.localAddress = localAddress;
    }

    /**
     * The local port.
     *
     * @return The local port.
     */
    public Integer getLocalPort()
    {
        return localPort;
    }

    /**
     * Sets the local port.
     * @param localPort The local port.
     */
    public void setLocalPort(Integer localPort)
    {
        this.localPort = localPort;
    }

    /**
     * Sets the activation code.
     *
     * @param activationCode The activation code.
     */
    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    /**
     * Sets the WAN address.
     *
     * @param wanAddress The wan address
     */
    public void setWanAddress(String wanAddress) {
        this.wanAddress = wanAddress;
    }

    /**
     * Sets the WAN port
     *
     * @param wanPort The WAN port.
     */
    public void setWanPort(Integer wanPort) {
        this.wanPort = wanPort;
    }

    /**
     * Gets the activation code.
     *
     * @return The activation code.
     */
    public String getActivationCode()
    {
        return activationCode;
    }

    /**
     * Gets colloquial name.
     *
     * @return The colloquial name.
     */
    public String getColloquialName()
    {
        return colloquialName;
    }

    /**
     * Gets the WAN address.
     *
     * @return The WAN address.
     */
    public String getWanAddress()
    {
        return wanAddress;
    }

    /**
     * Getes the WAN port.
     *
     * @return The WAN port.
     */
    public Integer getWanPort()
    {
        return wanPort;
    }

    /**
     * Creates formatted string summary of this object.
     *
     * @return The information summary string.
     */
    public String getInfoSummary()
    {
        if (wanAddress == null || wanPort == null || localAddress == null || localPort == null)
        {
            return "";
        }

        return "WAN: " + wanAddress + ":" + wanPort + "  |  LAN: "
            + localAddress + ":" + localPort;
    }

    /**
     * Creates string representing user name
     *
     * @return Formatted username string.
     */
    public String getUserNameSummary()
    {
        if (userName == null)
        {
            return "";
        }

        return "User:  " + userName;
    }

    /**
     * Gets the user name.
     *
     * @return The user name.
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Gets the user password.
     *
     * @return The user password.
     */
    public String getUserPassword()
    {
        return userPassword;
    }


    /**
     * Validates if this object contains enough information to create a basic connection from.
     *
     * @return True if the connection is valid.
     */
    public boolean isValid()
    {
        if (wanAddress != null && !wanAddress.isEmpty())
        {
            if (wanPort != null)
            {
                return activationCode != null && !activationCode.isEmpty();
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(activationCode, colloquialName, wanAddress, wanPort, userName);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Connection)
        {
            Connection theOther = (Connection) other;
            return Objects.equals(activationCode, theOther.activationCode)
                    && Objects.equals(colloquialName, theOther.colloquialName)
                    && Objects.equals(wanAddress, theOther.wanAddress)
                    && Objects.equals(wanPort, theOther.wanPort)
                    && Objects.equals(localAddress, theOther.localAddress)
                    && Objects.equals(localPort, theOther.localPort)
                    && Objects.equals(userName, theOther.userName);
        }
        else
        {
            return false;
        }
    }
}
