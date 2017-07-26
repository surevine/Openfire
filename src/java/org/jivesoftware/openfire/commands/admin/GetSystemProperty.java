package org.jivesoftware.openfire.commands.admin;

import org.dom4j.Element;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.util.JiveGlobalsWrapper;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An ad-hoc command to retrieve a system property.
 */
public class GetSystemProperty extends AdHocCommand {

    private JiveGlobalsWrapper propertyStore;

    @Override
    public String getCode() {
        return "http://jabber.org/protocol/admin#get-system-property";
    }

    @Override
    public String getDefaultLabel() {
        return "Get a system property value";
    }

    @Override
    public int getMaxStages(SessionData data) { return 1; }

    @Override
    public void execute(SessionData sessionData, Element command) {

        Map<String, List<String>> data = sessionData.getData();

        List<String> requestedKeys = data.get("key");

        DataForm form = new DataForm(DataForm.Type.result);

        form.addReportedField("key", "System property key", FormField.Type.text_single);
        form.addReportedField("value", "System property value", FormField.Type.text_single);

        for (String key : requestedKeys) {
            if (isWildcard(key)) {
                String parentKey = stripWildcardPart(key);

                Map<String, String> propertyValues = lookupProperties(parentKey);

                for (Map.Entry<String, String> property : propertyValues.entrySet()) {
                    if (isPropertySensitive(property.getKey())) {
                        continue; // do not add sensitive child key to response
                    } else {
                        Map<String, Object> fields = new HashMap<>();
                        fields.put("key", property.getKey());
                        fields.put("value", property.getValue());
                        form.addItemFields(fields);
                    }
                }
            } else {
                if (isPropertySensitive(key)) {
                    // do not look up sensitive key
                } else {
                    String propertyValue = lookupProperty(key);

                    Map<String,Object> fields = new HashMap<>();
                    fields.put("key", key);
                    fields.put("value", propertyValue);
                    form.addItemFields(fields);
                }

            }
        }

        command.add(form.getElement());
    }

    private boolean isPropertySensitive(String key) {
        return this.getPropertyStore().isPropertySensitive(key) || this.getPropertyStore().isPropertyEncrypted(key);
    }

    /**
     * Lookup a single system property
     *
     * @param key to find the value of
     * @return the value of any matching property
     */
    private String lookupProperty(String key) {
        return this.getPropertyStore().getProperty(key);
    }

    /**
     * Get key/value pairs of the specified parent's immediate children
     *
     * @param parent key to find children key/value pairs of
     * @return parent's children key/value pairs
     */
    private Map<String, String> lookupProperties(String parent) {
        return this.getPropertyStore().getProperties(parent);
    }

    @Override
    protected void addStageInformation(SessionData data, Element command) {
        DataForm form = new DataForm(DataForm.Type.form);
        form.setTitle("Requesting system property values");
        form.addInstruction("Fill out this form to request system property values.");

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        field = form.addField();
        field.setType(FormField.Type.text_multi);
        field.setLabel("The system property keys to which the required values are mapped");
        field.setVariable("key");
        field.setRequired(true);
        command.add(form.getElement());
    }

    @Override
    protected List<Action> getActions(SessionData data) {
        return Collections.singletonList(Action.complete);
    }

    @Override
    protected Action getExecuteAction(SessionData data) {
        return AdHocCommand.Action.complete;
    }

    /**
     * Get the property store to lookup from
     *
     * Provides a default implementation
     *
     * @return the property store
     */
    private JiveGlobalsWrapper getPropertyStore() {
        if (null == this.propertyStore) {
            this.propertyStore = new JiveGlobalsWrapper();
        }

        return this.propertyStore;
    }

    /**
     * Allows the default property store to be switched out, e.g. for testing
     *
     * @param propertyStore that this class will use to lookup properties
     */
    public void setPropertyStore(JiveGlobalsWrapper propertyStore) {
        this.propertyStore = propertyStore;
    }

    /**
     * Strips the wildcard characters to return just the parent key
     *
     * @param key
     * @return the parent of the wildcard
     */
    private String stripWildcardPart(String key) {
        return key.substring(0, key.length()-2);
    }

    /**
     * Is the key a wildcard specifier
     *
     * @param key
     * @return true if key specifies a wildcard match
     */
    private boolean isWildcard(String key) {
        return key.endsWith(".*");
    }
}