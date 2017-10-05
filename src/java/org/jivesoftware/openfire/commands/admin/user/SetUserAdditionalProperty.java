package org.jivesoftware.openfire.commands.admin.user;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.UserWrapper;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  An ad-hoc command to retrieve the additional properties of the user.
 */
public class SetUserAdditionalProperty extends AdHocCommand {

    private static final String ACCOUNT_JID_VAR = "accountjid";
    private static final String PROPERTY_KEY_VAR = "key";
    private static final String PROPERTY_VALUE_VAR = "value";

    private XMPPServer xmppServer;
    private UserWrapper propertyStore;

    @Override
	public String getCode() {
        return "http://jabber.org/protocol/admin#set-user-additional-property";
    }

    @Override
	public String getDefaultLabel() {
        return "Set an additional user property";
    }

    @Override
	public int getMaxStages(SessionData data) {
        return 1;
    }

    @Override
	public void execute(SessionData sessionData, Element command) {

        Map<String, List<String>> data = sessionData.getData();

        Element note = command.addElement("note");

        final boolean requestIsMissingAccountJID = !data.containsKey(ACCOUNT_JID_VAR);
        final boolean requestIsMissingKey = !data.containsKey(PROPERTY_KEY_VAR);
        final boolean requestIsMissingValue = !data.containsKey(PROPERTY_VALUE_VAR);

        if (requestIsMissingAccountJID || requestIsMissingKey || requestIsMissingValue) {
            note.addAttribute("type", "error");
            note.setText("accountjid, key, and value are required parameters.");
            return;
        }

        String accountJid = data.get(ACCOUNT_JID_VAR).get(0);
        String propertyKey = data.get(PROPERTY_KEY_VAR).get(0);
        String propertyValue = data.get(PROPERTY_VALUE_VAR).get(0);

        if (accountJid.isEmpty() || propertyKey.isEmpty()) {
            note.addAttribute("type", "error");
            note.setText("accountjid and key must not be empty.");
            return;
        }

        JID account;
        try {
            account = new JID(accountJid);
        }
        catch (IllegalArgumentException iae) {
            note.addAttribute("type", "error");
            note.setText("Invalid accountjid supplied.");
            return;
        }

        if (isRemoteUser(account)) {
            note.addAttribute("type", "error");
            note.setText("Can only set additional properties for local users.");
            return;
        }

        try {
            this.getPropertyStore().setProperty(account.getNode(), propertyKey, propertyValue);
        }
        catch (UserNotFoundException e) {
            note.addAttribute("type", "error");
            note.setText("User not found.");
            return;
        }

        note.addAttribute("type", "info");
        note.setText("Operation finished successfully");
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
        form.setTitle("Set an additional property value for a user");
        form.addInstruction("Fill out this form to set an additional property value for a user.");

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        field = form.addField();
        field.setType(FormField.Type.jid_single);
        field.setLabel("The Jabber ID for which to set the additional property");
        field.setVariable("accountjid");
        field.setRequired(true);

        field = form.addField();
        field.setType(FormField.Type.text_single);
        field.setLabel("The key of the property to set");
        field.setVariable("key");
        field.setRequired(true);

        field = form.addField();
        field.setType(FormField.Type.text_single);
        field.setLabel("The value of the property to set");
        field.setVariable(PROPERTY_VALUE_VAR);
        field.setRequired(true);

        command.add(form.getElement());
    }

    @Override
	protected List<Action> getActions(SessionData data) { return Collections.singletonList(Action.complete); }

    @Override
	protected Action getExecuteAction(SessionData data) { return Action.complete; }

    /**
     * Get the property store to update
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
     * @param propertyStore that this class will use to set properties
     */
    public void setPropertyStore(UserWrapper propertyStore) { this.propertyStore = propertyStore; }

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