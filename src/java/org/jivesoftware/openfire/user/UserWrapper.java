package org.jivesoftware.openfire.user;

/**
 * Mockable wrapper for {@link User}
 */
public class UserWrapper {

    /**
     * Returns the value of the specified property for the given username.
     *
     * Wraps {@link User#getPropertyValue(String, String)}
     *
     * @param username the username of the user to get a specific property value.
     * @param propertyName the name of the property to return its value.
     * @return the value of the specified property for the given username.
     */
    public String getPropertyValue(String username, String propertyName) {
        return User.getPropertyValue(username, propertyName);
    }
}
