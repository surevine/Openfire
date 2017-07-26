package org.jivesoftware.openfire.commands.admin;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class GetSystemPropertyTest {

    private static final String COMMANDS_NAMESPACE = "http://jabber.org/protocol/commands";

    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private GetSystemProperty getSystemPropertyCommand;
    private JiveGlobalsWrapper mockJiveGlobalsWrapper;
    private Map<String, List<String>> requestData;
    private SessionData mockSessionData;

    @Before
    public void setUp() {
        this.mockJiveGlobalsWrapper = context.mock(JiveGlobalsWrapper.class);

        this.getSystemPropertyCommand = new GetSystemProperty();
        this.getSystemPropertyCommand.setPropertyStore(mockJiveGlobalsWrapper);

        this.mockSessionData = context.mock(SessionData.class);
        this.requestData = new HashMap<>();
    }

    @Test
    public void testCanRequestASingleProperty() {

        // Test data
        requestData.put("key", new ArrayList<String>() {{
            add("system.property.to.lookup");
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));

            // Our mock property store
            one(mockJiveGlobalsWrapper).getProperty("system.property.to.lookup");
            will(returnValue("expected-property-value"));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("result", childElement.element("x").attributeValue("type"));
        assertEquals("expected-property-value", childElement.selectSingleNode("//*[name()='field'][@var='value']/*[name()='value']").getText());
    }

    @Test
    public void testStageInformationProvidesUsageHelp() {

        // Request stanza to populate
        IQ request = new IQ(IQ.Type.set);
        Element childElement = request.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.addStageInformation(mockSessionData, childElement);

        assertEquals("Stage information should indicate form", "form", childElement.element("x").attribute("type").getValue());
        assertThat("Title should mention system property", childElement.element("x").element("title").getText(), containsString("system property"));
        assertThat("Instructions should mention system property", childElement.element("x").element("instructions").getText(), containsString("system property"));
    }

    @Test
    public void testResponseIndicatesAdminFormType() {

        // Request stanza to populate
        IQ request = new IQ(IQ.Type.set);
        Element childElement = request.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.addStageInformation(mockSessionData, childElement);

        assertEquals("http://jabber.org/protocol/admin", childElement.selectSingleNode("//*[name()='field'][@var='FORM_TYPE']/*[name()='value']").getText());
    }

    @Test
    public void testCanRequestMultipleProperties() {

        // Test data
        final String propertyToLookupOne = "system.property.to.lookup.one";
        final String propertyToLookupTwo = "system.property.to.lookup.two";
        final String propertyOneValue = "expected-property-value-one";
        final String propertyTwoValue = "expected-property-value-two";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookupOne);
            add(propertyToLookupTwo);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperty(propertyToLookupOne);
            will(returnValue(propertyOneValue));

            one(mockJiveGlobalsWrapper).getProperty(propertyToLookupTwo);
            will(returnValue(propertyTwoValue));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals(propertyOneValue, childElement.selectSingleNode("//*[name()='item'][1]/*[name()='field'][1]/*[name()='value']").getText());
        assertEquals("Property value should be associated with correct key in response",
                propertyToLookupOne, childElement.selectSingleNode("//*[name()='item'][1]/*[name()='field'][2]/*[name()='value']").getText());

        assertEquals(propertyTwoValue, childElement.selectSingleNode("//*[name()='item'][2]/*[name()='field'][1]/*[name()='value']").getText());
        assertEquals("Property value should be associated with correct key in response",
                propertyToLookupTwo, childElement.selectSingleNode("//*[name()='item'][2]/*[name()='field'][2]/*[name()='value']").getText());
    }

    @Test
    public void testCanRequestAllChildProperties() {

        // Test data
        final String propertyToLookup = "system.property.to.lookup.*";

        final String propertyOneKey = "system.property.to.lookup.one";
        final String propertyTwoKey = "system.property.to.lookup.two";

        final String propertyOneValue = "expected-property-value-one";
        final String propertyTwoValue = "expected-property-value-two";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperties("system.property.to.lookup");
            will(returnValue(new HashMap<String, String>() {{
               put(propertyOneKey, propertyOneValue);
               put(propertyTwoKey, propertyTwoValue);
            }}));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals(propertyOneValue, childElement.selectSingleNode("//*[name()='item'][2]/*[name()='field'][1]/*[name()='value']").getText());
        assertEquals("Property value should be associated with correct key in response",
                propertyOneKey, childElement.selectSingleNode("//*[name()='item'][2]/*[name()='field'][2]/*[name()='value']").getText());

        assertEquals(propertyTwoValue, childElement.selectSingleNode("//*[name()='item'][1]/*[name()='field'][1]/*[name()='value']").getText());
        assertEquals("Property value should be associated with correct key in response",
                propertyTwoKey, childElement.selectSingleNode("//*[name()='item'][1]/*[name()='field'][2]/*[name()='value']").getText());
    }

    @Test
    public void testNonExistentKeyReturnsEmptyValue() {

        // Test data
        requestData.put("key", new ArrayList<String>() {{
            add("non.existent.property.key");
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperty("non.existent.property.key");
            will(returnValue(null));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("Non existent key should return null value", "", childElement.selectSingleNode("//*[name()='field'][@var='value']").getText());
    }

    @Test
    public void testValuesOfSensitivePropertiesAreNotReturned() {

        // Test data
        final String propertyToLookup = "system.property.to.lookup.*";

        final String passwdKey = "system.property.to.lookup.passwd";
        final String passwordKey = "system.property.to.lookup.password";
        final String cookiekeyKey = "system.property.to.lookup.cookiekey";
        final String secret = "secret";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperties("system.property.to.lookup");
            will(returnValue(new HashMap<String, String>() {{
                put(passwdKey, secret);
                put(passwordKey, secret);
                put(cookiekeyKey, secret);
            }}));

            one(mockJiveGlobalsWrapper).isPropertySensitive(passwdKey);
            will(returnValue(true));
            one(mockJiveGlobalsWrapper).isPropertySensitive(passwordKey);
            will(returnValue(true));
            one(mockJiveGlobalsWrapper).isPropertySensitive(cookiekeyKey);
            will(returnValue(true));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertThat("Sensitive property values should not be returned", childElement.asXML(), not(containsString(secret)));
    }

    @Test
    public void testValuesOfIndividualSensitivePropertiesAreNotLookedUp() {

        // Test data
        final String passwdKey = "system.property.to.lookup.passwd";
        final String passwordKey = "system.property.to.lookup.password";
        final String cookiekeyKey = "system.property.to.lookup.cookiekey";
        final String secret = "secret";

        requestData.put("key", new ArrayList<String>() {{
            add(passwdKey);
            add(passwordKey);
            add(cookiekeyKey);
        }});

        context.checking(new Expectations() {{
            // Our mock test data
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).isPropertySensitive(passwdKey);
            will(returnValue(true));
            one(mockJiveGlobalsWrapper).isPropertySensitive(passwordKey);
            will(returnValue(true));
            one(mockJiveGlobalsWrapper).isPropertySensitive(cookiekeyKey);
            will(returnValue(true));

            never(mockJiveGlobalsWrapper).getProperty(passwdKey);
            never(mockJiveGlobalsWrapper).getProperty(passwordKey);
            never(mockJiveGlobalsWrapper).getProperty(cookiekeyKey);
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertThat("Sensitive property values should not be returned", childElement.asXML(), not(containsString(secret)));
    }

    @Test
    public void testValuesOfIndividualEncryptedPropertiesAreNotLookedUp() {

        // Test data
        final String propertyToLookup = "system.property.to.lookup.encrypted";

        final String propertyValue = "decrypted-property-value";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            never(mockJiveGlobalsWrapper).getProperty(propertyToLookup);

            one(mockJiveGlobalsWrapper).isPropertySensitive(propertyToLookup);
            will(returnValue(false));
            one(mockJiveGlobalsWrapper).isPropertyEncrypted(propertyToLookup);
            will(returnValue(true));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertThat("Encrypted property values should not be returned", childElement.asXML(), not(containsString(propertyValue)));
    }

    @Test
    public void testValuesOfEncryptedPropertiesAreNotReturned() {

        // Test data
        final String propertyToLookup = "system.property.to.lookup.encrypted.*";

        final String propertyKey = "system.property.to.lookup.encrypted";
        final String propertyValue = "secret-property-value";

        requestData.put("key", new ArrayList<String>() {{
            add(propertyToLookup);
        }});

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperties(propertyKey);
            will(returnValue(new HashMap<String, String>() {{
                put(propertyKey, propertyValue);
            }}));

            one(mockJiveGlobalsWrapper).isPropertySensitive(propertyKey);
            will(returnValue(false));
            one(mockJiveGlobalsWrapper).isPropertyEncrypted(propertyKey);
            will(returnValue(true));
        }});

        // Result stanza to populate
        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertThat("Encrypted property values should not be returned", childElement.asXML(), not(containsString(propertyValue)));
    }

    @Test
    public void testMaxLengthValuesCanBeReturned() {

        requestData.put("key", new ArrayList<String>() {{
            add("system.property.to.lookup");
        }});

        // DB: propvalue | character varying(4000)
        final int propertyValueMaxLength = 4000;
        final String maxLengthValue = StringUtils.repeat("W", propertyValueMaxLength);

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperty("system.property.to.lookup");
            will(returnValue(maxLengthValue));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("Value returned was not correct", maxLengthValue, childElement.selectSingleNode("//*[name()='field'][@var='value']/*[name()='value']").getText());
    }

    @Test
    public void testMaxLengthKeysCanBeRequested() {

        // DB: name      | character varying(100)
        final int propertyKeyMaxLength = 100;
        final String maxLengthKey = StringUtils.repeat("W", propertyKeyMaxLength);

        requestData.put("key", new ArrayList<String>() {{
            add(maxLengthKey);
        }});

        final String expectedValue = "expected-value";

        context.checking(new Expectations() {{
            one(mockSessionData).getData();
            will(returnValue(requestData));

            one(mockJiveGlobalsWrapper).getProperty(maxLengthKey);
            will(returnValue(expectedValue));

            allowing(mockJiveGlobalsWrapper).isPropertySensitive(with(any(String.class)));
            will(returnValue(false));
            allowing(mockJiveGlobalsWrapper).isPropertyEncrypted(with(any(String.class)));
            will(returnValue(false));
        }});

        IQ result = new IQ(IQ.Type.result);
        Element childElement = result.setChildElement("command", COMMANDS_NAMESPACE);

        getSystemPropertyCommand.execute(mockSessionData, childElement);

        context.assertIsSatisfied();

        assertEquals("Long key did not return expected value", expectedValue, childElement.selectSingleNode("//*[name()='field'][@var='value']/*[name()='value']").getText());

    }
}