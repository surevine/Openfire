package org.jivesoftware.openfire.commands.admin.user;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.UserWrapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SetUserAdditionalPropertyTest {

    private static final String COMMANDS_NAMESPACE = "http://jabber.org/protocol/commands";

    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private UserWrapper mockUserWrapper;
    private XMPPServer mockXMPPServer;
    private SetUserAdditionalProperty setUserAdditionalPropertyCommand;

    private Map<String, List<String>> requestData;
    private SessionData mockSessionData;

    @Before
    public void setUp() {
        this.mockUserWrapper = context.mock(UserWrapper.class);
        this.mockXMPPServer = context.mock(XMPPServer.class);

        this.setUserAdditionalPropertyCommand = new SetUserAdditionalProperty();
        this.setUserAdditionalPropertyCommand.setPropertyStore(mockUserWrapper);
        this.setUserAdditionalPropertyCommand.setXMPPServer(mockXMPPServer);

        this.mockSessionData = context.mock(SessionData.class);
        this.requestData = new HashMap<>();
    }

    @Test
    public void testCanSetASingleProperty() throws UserNotFoundException {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToSet = "property.to.set";
        final String propertyValue = "property-value";

        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            allowing(mockXMPPServer).isLocal(with(a(JID.class)));
            will(returnValue(true));

            one(mockUserWrapper).setProperty(username, propertyToSet, propertyValue);
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an info note", "info", note.attributeValue("type"));
        assertEquals("Should send standard success message", "Operation finished successfully", note.getText());
    }

    @Test
    public void testShouldErrorIfUserNotFound() throws UserNotFoundException {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToSet = "property.to.set";
        final String propertyValue = "property-value";

        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            allowing(mockXMPPServer).isLocal(with(a(JID.class)));
            will(returnValue(true));

            one(mockUserWrapper).setProperty(username, propertyToSet, propertyValue);
            will(throwException(new UserNotFoundException()));

        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate user not found", note.getText().toLowerCase(), containsString("user not found"));
    }

    @Test
    public void testShouldErrorIfNoKeySupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyValue = "property-value";

        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("key"));
    }

    @Test
    public void testShouldErrorIfNoJIDSupplied() {

        final String propertyToSet = "property.to.set";
        final String propertyValue = "property-value";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("accountjid"));
    }

    @Test
    public void testShouldErrorIfNoValueSupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToSet = "property.to.set";

        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("value"));
    }

    @Test
    public void testShouldErrorIfEmptyJIDSupplied() {

        final String accountJid = "";
        final String propertyToSet = "property.to.set";
        final String propertyValue = "property-value";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention invalid JID", note.getText().toLowerCase(), containsString("invalid accountjid"));
    }

    @Test
    public void testShouldErrorIfRemoteUserSupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToSet = "property.to.set";
        final String propertyValue = "property-value";

        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToSet);
        }});
        requestData.put("value", new ArrayList<String>() {{
            add(propertyValue);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            allowing(mockXMPPServer).isLocal(with(a(JID.class)));
            will(returnValue(false));

        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention local users", note.getText().toLowerCase(), containsString("local users"));
    }


    @Test
    public void testStageInformationProvidesUsageHelp() {

        // Request stanza to populate
        IQ request = new IQ(IQ.Type.set);
        Element childElement = request.setChildElement("command", COMMANDS_NAMESPACE);

        setUserAdditionalPropertyCommand.addStageInformation(mockSessionData, childElement);

        final Element x = childElement.element("x");
        final String title = x.element("title").getText().toLowerCase();
        final String instructions = x.element("instructions").getText().toLowerCase();

        assertEquals("Stage information should indicate form", "form", x.attribute("type").getValue());
        assertThat("Title should mention user", title, containsString("user"));
        assertThat("Title should mention additional property", title, containsString("additional property"));
        assertThat("Title should mention set", title, containsString("set"));
        assertThat("Instructions should mention additional property", instructions, containsString("additional property"));
        assertThat("Instructions should mention set", instructions, containsString("set"));
    }

    @Test
    public void testShouldHaveOneStage() {
        assertEquals(1, setUserAdditionalPropertyCommand.getMaxStages(mockSessionData));
    }
    @Test
    public void testCodeShouldIndicateAdminSetSystemProperty() {
        assertThat(setUserAdditionalPropertyCommand.getCode(), endsWith("admin#set-user-additional-property"));
    }

    @Test
    public void testOnlyAllowedActionIsComplete() {
        List<AdHocCommand.Action> actions = setUserAdditionalPropertyCommand.getActions(mockSessionData);

        assertThat("Should have exactly one action", actions.size(), is(1));
        assertThat("Should have a Complete action", actions, hasItem(AdHocCommand.Action.complete));
    }

    @Test
    public void testExecuteActionShouldBeComplete() {
        AdHocCommand.Action action = setUserAdditionalPropertyCommand.getExecuteAction(mockSessionData);

        assertEquals("Execute command should be Complete", AdHocCommand.Action.complete, action);
    }
}