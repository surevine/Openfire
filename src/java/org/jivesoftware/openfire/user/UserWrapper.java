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

    /**
     * Sets an additional property on a user. If property doesn't already exist, a new one will be created.
     *
     * @param username for which to set the property
     * @param key the key of the property being set.
     * @param value the value of the property being set.
     * @throws UserNotFoundException when user is not found
     */
    public void setProperty(String username, String key, String value) throws UserNotFoundException {
        User user = UserManager.getInstance().getUser(username);
        user.getProperties().put(key, value);
    }
}
