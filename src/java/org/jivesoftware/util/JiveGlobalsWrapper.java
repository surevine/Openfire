package org.jivesoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Mockable wrapper for JiveGlobals
 */
public class JiveGlobalsWrapper {

    /**
     * Returns a Jive property.
     *
     * Wraps {@link JiveGlobals#getProperty(String)}
     *
     * @param name the name of the property to return.
     * @return the property value specified by name or null if no matching property.
     */
    public String getProperty(String name) {
        return JiveGlobals.getProperty(name);
    }

    /**
     * Return all immediate children property values of a parent Jive property as a map of key/value pairs
     * or an empty map if there are no children.
     *
     * An adaptation of {@link JiveGlobals#getProperties(String)} that maps keys to values in the return
     *
     * @param parent the name of the parent property to return the children for.
     * @return all child property keys and values for the given parent.
     */
    public Map<String, String> getProperties(String parent) {

        final SortedSet<String> propertyNames = new TreeSet<>(JiveGlobals.getPropertyNames(parent));

        final Map<String, String> properties = new HashMap<>();

        for ( String propertyName : propertyNames ) {
            properties.put(propertyName, this.getProperty(propertyName));
        }

        return properties;
    }

    /**
     * Flags certain properties as being sensitive, based on property naming conventions.
     *
     * Wraps {@link JiveGlobals#isPropertySensitive(String)}
     *
     * @param key The name of the property
     * @return True if the property is considered sensitive, otherwise false
     */
    public boolean isPropertySensitive(String key) {
        return JiveGlobals.isPropertySensitive(key);
    }

    /**
     * Determines whether a property is configured for encryption.
     *
     * @param key The name of the property
     * @return True if the property is stored using encryption, otherwise false
     */
    public boolean isPropertyEncrypted(String key) {
        return JiveGlobals.isPropertyEncrypted(key);
    }
}