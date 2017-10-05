package org.jivesoftware.openfire.commands.admin;

import org.dom4j.Element;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.util.JiveGlobalsWrapper;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An ad-hoc command to set a system property.
 */
public class SetSystemProperty extends AdHocCommand {

    private JiveGlobalsWrapper propertyStore;

    @Override
    public String getCode() {
        return "http://jabber.org/protocol/admin#set-system-property";
    }

    @Override
    public String getDefaultLabel() {
        return "Set a system property value";
    }

    @Override
    public int getMaxStages(SessionData data) { return 1; }

    @Override
    public void execute(SessionData sessionData, Element command) {

        Map<String, List<String>> data = sessionData.getData();

        Element note = command.addElement("note");

        String propertyKey;
        String propertyValue;

        try {
            propertyKey = data.get("key").get(0);
            propertyValue = data.get("value").get(0);
        }
        catch (NullPointerException ne) {
            note.addAttribute("type", "error");
            note.setText("Key and Value are required parameters.");
            return;
        }

        this.getPropertyStore().setProperty(propertyKey, propertyValue);

        note.addAttribute("type", "info");
        note.setText("Operation finished successfully");
    }


    @Override
    protected void addStageInformation(SessionData data, Element command) {
        DataForm form = new DataForm(DataForm.Type.form);
        form.setTitle("Set a system property value");
        form.addInstruction("Fill out this form to set a system property. " +
                "If the property doesn't already exist, a new one will be created.");

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        field = form.addField();
        field.setType(FormField.Type.text_single);
        field.setLabel("The key of the system property to set");
        field.setVariable("key");
        field.setRequired(true);

        field = form.addField();
        field.setType(FormField.Type.text_single);
        field.setLabel("The value of the system property to set");
        field.setVariable("value");
        field.setRequired(true);

        command.add(form.getElement());
    }

    @Override
    protected List<Action> getActions(SessionData data) {
        return Collections.singletonList(Action.complete);
    }

    @Override
    protected Action getExecuteAction(SessionData data) {
        return Action.complete;
    }

    /**
     * Get the property store to update
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
     * @param propertyStore that this class will use to set properties
     */
    public void setPropertyStore(JiveGlobalsWrapper propertyStore) {
        this.propertyStore = propertyStore;
    }
}