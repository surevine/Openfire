package org.jivesoftware.openfire.commands.admin;

import org.dom4j.Element;
import org.jivesoftware.openfire.commands.AdHocCommand.Action;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.util.JiveGlobalsWrapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SetSystemPropertyTest {

    private static final String COMMANDS_NAMESPACE = "http://jabber.org/protocol/commands";

    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private SetSystemProperty setSystemPropertyCommand;
    private JiveGlobalsWrapper mockJiveGlobalsWrapper;
    private Map<String, List<String>> requestData;
    private SessionData mockSessionData;

    @Before
    public void setUp() {
        this.mockJiveGlobalsWrapper = context.mock(JiveGlobalsWrapper.class);

        this.setSystemPropertyCommand = new SetSystemProperty();
        this.setSystemPropertyCommand.setPropertyStore(mockJiveGlobalsWrapper);

        this.mockSessionData = context.mock(SessionData.class);
        this.requestData = new HashMap<>();
    }

    @Test
    public void testCanSetASystemProperty() {

        // Test data
        requestData.put("key", new ArrayList<String>() {{
            add("system.property.key");
        }});
        requestData.put("value", new ArrayList<String>() {{
            add("system-property-value");
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));

            // Our mock property store
            one(mockJiveGlobalsWrapper).setProperty("system.property.key", "system-property-value");
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an info note", "info", note.attributeValue("type"));
        assertEquals("Should send standard success message", "Operation finished successfully", note.getText());
    }

    @Test
    public void testShouldErrorIfNoKeySupplied() {

        // Test data
        requestData.put("value", new ArrayList<String>() {{
            add("system-property-value");
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("key"));
    }

    @Test
    public void testShouldErrorIfNoValueSupplied() {

        // Test data
        requestData.put("key", new ArrayList<String>() {{
            add("system.property.key");
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        setSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        final Element note = childElement.element("note");
        assertEquals("Should return an error note", "error", note.attributeValue("type"));
        assertThat("Error should indicate required", note.getText().toLowerCase(), containsString("required"));
        assertThat("Error should mention missing parameter", note.getText().toLowerCase(), containsString("value"));
    }

    @Test
    public void testStageInformationProvidesUsageHelp() {

        // Request stanza to populate
        IQ request = new IQ(IQ.Type.set);
        Element childElement = request.setChildElement("command", COMMANDS_NAMESPACE);

        setSystemPropertyCommand.addStageInformation(mockSessionData, childElement);

        final Element x = childElement.element("x");
        final String title = x.element("title").getText().toLowerCase();
        final String instructions = x.element("instructions").getText().toLowerCase();

        assertEquals("Stage information should indicate form", "form", x.attribute("type").getValue());
        assertThat("Title should mention system property", title, containsString("system property"));
        assertThat("Title should mention set", title, containsString("set"));
        assertThat("Instructions should mention system property", instructions, containsString("system property"));
        assertThat("Instructions should mention set", instructions, containsString("set"));
    }

    @Test
    public void testShouldHaveOneStage() {
        assertEquals(1, setSystemPropertyCommand.getMaxStages(mockSessionData));
    }
    @Test
    public void testCodeShouldIndicateAdminSetSystemProperty() {
        assertThat(setSystemPropertyCommand.getCode(), endsWith("admin#set-system-property"));
    }

    @Test
    public void testOnlyAllowedActionIsComplete() {
        List<Action> actions = setSystemPropertyCommand.getActions(mockSessionData);

        assertThat("Should have exactly one action", actions.size(), is(1));
        assertThat("Should have a Complete action", actions, hasItem(Action.complete));
    }

    @Test
    public void testExecuteActionShouldBeComplete() {
        Action action = setSystemPropertyCommand.getExecuteAction(mockSessionData);

        assertEquals("Execute command should be Complete", Action.complete, action);
    }
}