package org.jivesoftware.openfire.commands.admin.user;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.openfire.user.UserWrapper;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  An ad-hoc command to retrieve the additional properties of the user.
 */
public class GetUserAdditionalProperty extends AdHocCommand {

    private XMPPServer xmppServer;
    private UserWrapper propertyStore;

    @Override
	public String getCode() {
        return "http://jabber.org/protocol/admin#get-user-additional-property";
    }

    @Override
	public String getDefaultLabel() {
        return "Get additional user properties";
    }

    @Override
	public int getMaxStages(SessionData data) {
        return 1;
    }

    @Override
	public void execute(SessionData sessionData, Element command) {

        Map<String, List<String>> data = sessionData.getData();

        String accountJid;
        String propertyKey;

        try {
            accountJid = data.get("accountjid").get(0);
            propertyKey = data.get("key").get(0);
        }
        catch (NullPointerException ne) {
            Element note = command.addElement("note");
            note.addAttribute("type", "error");
            note.setText("accountjid and key are required parameters.");
            return;
        }

        JID account;
        try {
            account = new JID(accountJid);
        }
        catch (IllegalArgumentException iae) {
            Element note = command.addElement("note");
            note.addAttribute("type", "error");
            note.setText("Invalid accountjid supplied.");
            return;
        }

        if (isRemoteUser(account)) {
            Element note = command.addElement("note");
            note.addAttribute("type", "error");
            note.setText("Can only get additional properties for local users.");
            return;
        }

        DataForm form = new DataForm(DataForm.Type.result);

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        String propertyValue = lookupProperty(account.getNode(), propertyKey);

        Map<String,Object> fields = new HashMap<>();
        fields.put("key", propertyKey);
        fields.put("value", propertyValue);
        form.addItemFields(fields);

        command.add(form.getElement());
    }

    /**
     * Lookup a single user-specific property
     *
     * @param username user to whom the property belongs
     * @param key to find the value of
     * @return the value of any matching property
     */
    private String lookupProperty(String username, String key) {
        return getPropertyStore().getPropertyValue(username, key);
    }

    /**
     * Returns true if the given address is remote (not managed by this server domain).
     *
     * @see XMPPServer#isLocal(JID)
     *
     * @param account the account to check
     * @return true if the user is not local
     */
    private boolean isRemoteUser(JID account) {
        return !this.getXMPPServer().isLocal(account);
    }

    @Override
	protected void addStageInformation(SessionData data, Element command) {
        DataForm form = new DataForm(DataForm.Type.form);
        form.setTitle("Get a user's additional properties");
        form.addInstruction("Fill out this form to get a user's additional properties.");

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        field = form.addField();
        field.setType(FormField.Type.jid_single);
        field.setLabel("The Jabber ID for which to lookup additional properties");
        field.setVariable("accountjid");
        field.setRequired(true);

        field = form.addField();
        field.setType(FormField.Type.text_single);
        field.setLabel("The system property key to fetch the value of");
        field.setVariable("key");
        field.setRequired(true);

        // Add the form to the command
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
     * Get the property store to lookup from
     *
     * Provides a default implementation
     *
     * @return the property store
     */
    private UserWrapper getPropertyStore() {
        if (null == this.propertyStore) {
            this.propertyStore = new UserWrapper();
        }

        return this.propertyStore;
    }

    /**
     * Allows the default property store to be switched out, e.g. for testing
     *
     * @param propertyStore that this class will use to lookup properties
     */
    public void setPropertyStore(UserWrapper propertyStore) {
        this.propertyStore = propertyStore;
    }

    /**
     * Get the local XMPP Server
     *
     * Provides a default implementation
     *
     * @return the XMPP server instance
     */
    private XMPPServer getXMPPServer() {
        if (null == this.xmppServer) {
            this.xmppServer = XMPPServer.getInstance();
        }

        return xmppServer;
    }

    /**
     * Allows the default XMPP Server to be switched out, e.g. for testing
     *
     * @param xmppServer to use for checking isLocal user
     */
    public void setXMPPServer(XMPPServer xmppServer) { this.xmppServer = xmppServer; }
}