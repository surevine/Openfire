package org.jivesoftware.openfire.commands.admin.user;

import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class GetUserAdditionalPropertyTest {

    private static final String COMMANDS_NAMESPACE = "http://jabber.org/protocol/commands";

    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private UserWrapper mockUserWrapper;
    private XMPPServer mockXMPPServer;
    private GetUserAdditionalProperty getUserAdditionalPropertyCommand;

    private Map<String, List<String>> requestData;
    private SessionData mockSessionData;

    @Before
    public void setUp() {
        this.mockUserWrapper = context.mock(UserWrapper.class);
        this.mockXMPPServer = context.mock(XMPPServer.class);

        this.getUserAdditionalPropertyCommand = new GetUserAdditionalProperty();
        this.getUserAdditionalPropertyCommand.setPropertyStore(mockUserWrapper);
        this.getUserAdditionalPropertyCommand.setXMPPServer(mockXMPPServer);

        this.mockSessionData = context.mock(SessionData.class);
        this.requestData = new HashMap<>();
    }

    @Test
    public void testCanRequestASingleProperty() throws Exception {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToLookup = "property.to.lookup";
        final String expectedPropertyValue = "expected-property-value";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));

            // Our mock property store
            one(mockUserWrapper).getPropertyValue(username, propertyToLookup);
            will(returnValue(expectedPropertyValue));

            allowing(mockXMPPServer).isLocal(with(any(JID.class)));
            will(returnValue(true));

        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("result", childElement.element("x").attributeValue("type"));
        assertEquals(expectedPropertyValue, childElement.selectSingleNode("//*[name()='field'][@var='value']/*[name()='value']").getText());
    }

    @Test
    public void testResponseContainsReportedTableHeader() throws Exception {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToLookup = "property.to.lookup";
        final String expectedPropertyValue = "expected-property-value";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));

            // Our mock property store
            one(mockUserWrapper).getPropertyValue(username, propertyToLookup);
            will(returnValue(expectedPropertyValue));

            allowing(mockXMPPServer).isLocal(with(any(JID.class)));
            will(returnValue(true));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element x = childElement.element("x");
        assertEquals("result", x.attributeValue("type"));
        final Element reported = x.element("reported");
        assertNotNull("Response should contain reported element", reported);
        assertEquals("Reported element should contain 2 fields", 2,  reported.elements().size());
        assertEquals("Reported element should contain key field", "key",  ((Element) reported.elements().get(0)).attribute("var").getValue());
        assertEquals("Reported element should contain value field", "value",  ((Element) reported.elements().get(1)).attribute("var").getValue());
    }

    @Test
    public void testShouldErrorIfNoKeySupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("key"));
    }

    @Test
    public void testShouldErrorIfEmptyKeySupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToLookup = "";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention empty", note.getText().toLowerCase(), containsString("empty"));
        assertThat("Error should mention key", note.getText().toLowerCase(), containsString("key"));
    }

    @Test
    public void testShouldErrorIfNoJIDSupplied() {

        final String propertyToLookup = "property.to.lookup";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("accountjid"));
    }

    @Test
    public void testShouldErrorIfEmptyJIDSupplied() {

        final String accountJid = "";
        final String propertyToLookup = "property.to.lookup";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention empty", note.getText().toLowerCase(), containsString("empty"));
        assertThat("Error should mention accountjid", note.getText().toLowerCase(), containsString("accountjid"));
    }

    @Test
    public void testShouldErrorIfInvalidJIDSupplied() {

        final String accountJid = "!!!!!!!!";
        final String propertyToLookup = "property.to.lookup";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention invalid JID", note.getText().toLowerCase(), containsString("invalid accountjid"));
    }

    @Test
    public void testShouldErrorIfRemoteUserSupplied() {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToLookup = "property.to.lookup";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            allowing(mockXMPPServer).isLocal(with(any(JID.class)));
            will(returnValue(false));

        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should mention local users", note.getText().toLowerCase(), containsString("local users"));
    }

    @Test
    public void testNonExistentKeyReturnsEmptyValue() throws Exception {

        final String username = "user";
        final String accountJid = username + "@localhost";
        final String propertyToLookup = "non.existent.property.to.lookup";

        // Test data
        requestData.put("accountjid", new ArrayList<String>() {{
            add(accountJid);
        }});
        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockUserWrapper).getPropertyValue(username, propertyToLookup);
            will(returnValue(null));

            allowing(mockXMPPServer).isLocal(with(any(JID.class)));
            will(returnValue(true));

        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("Non existent key should return null value", "", childElement.selectSingleNode("//*[name()='field'][@var='value']").getText());
    }


    @Test
    public void testStageInformationProvidesUsageHelp() {

        // Request stanza to populate
        IQ request = new IQ(IQ.Type.set);
        Element childElement = request.setChildElement("command", COMMANDS_NAMESPACE);

        getUserAdditionalPropertyCommand.addStageInformation(mockSessionData, childElement);

        final Element x = childElement.element("x");
        final String title = x.element("title").getText().toLowerCase();
        final String instructions = x.element("instructions").getText().toLowerCase();

        assertEquals("Stage information should indicate form", "form", x.attribute("type").getValue());
        assertThat("Title should mention user", title, containsString("user"));
        assertThat("Title should mention additional property", title, containsString("additional property"));
        assertThat("Title should mention get", title, containsString("get"));
        assertThat("Instructions should mention additional property", instructions, containsString("additional property"));
        assertThat("Instructions should mention get", instructions, containsString("get"));
    }

    @Test
    public void testShouldHaveOneStage() {
        assertEquals(1, getUserAdditionalPropertyCommand.getMaxStages(mockSessionData));
    }
    @Test
    public void testCodeShouldIndicateAdminSetSystemProperty() {
        assertThat(getUserAdditionalPropertyCommand.getCode(), endsWith("admin#get-user-additional-property"));
    }

    @Test
    public void testOnlyAllowedActionIsComplete() {
        List<AdHocCommand.Action> actions = getUserAdditionalPropertyCommand.getActions(mockSessionData);

        assertThat("Should have exactly one action", actions.size(), is(1));
        assertThat("Should have a Complete action", actions, hasItem(AdHocCommand.Action.complete));
    }

    @Test
    public void testExecuteActionShouldBeComplete() {
        AdHocCommand.Action action = getUserAdditionalPropertyCommand.getExecuteAction(mockSessionData);

        assertEquals("Execute command should be Complete", AdHocCommand.Action.complete, action);
    }
}
