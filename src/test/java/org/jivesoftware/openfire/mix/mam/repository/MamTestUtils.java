package org.jivesoftware.openfire.mix.mam.repository;

import java.util.Calendar;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.mix.mam.MessageArchiveService;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class MamTestUtils {
	
	public static final String QUERY_ELEM_NAME = "query";

	public static final String TEST_USER = "hag66";

	public static final String TEST_MIX_CHANNEL_NAME = "coven";

	public static final String TEST_SERVICE_DOMAIN = "shakespeare.example";

	public static final String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	public static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);

	public static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);

	private static final XMPPDateTimeFormat XMPP_DATE_TIME_FORMAT = new XMPPDateTimeFormat();

	public static IQ getTimeBoundQuery() {

		Calendar today = Calendar.getInstance();
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DAY_OF_MONTH, -1);

		
		IQ iq = MamTestUtils.getBaseQuery();
		Element x = iq.getChildElement().addElement("x", "jabber:x:data");
		x.addAttribute("type", "submit");
		Element field = x.addElement("field").addAttribute("var", "FORM_TYPE").addAttribute("type", "hidden");
		field.addElement("value").addText(MessageArchiveService.MAM_NAMESPACE);
		x.addElement("field").addAttribute("var", "start").addElement("value").addText(MamTestUtils.XMPP_DATE_TIME_FORMAT.format(yesterday.getTime()));
		x.addElement("field").addAttribute("var", "end").addElement("value").addText(MamTestUtils.XMPP_DATE_TIME_FORMAT.format(today.getTime()));
		
		return iq;
	}

	public static IQ getFilterQuery() {
		
		IQ iq = MamTestUtils.getBaseQuery();
		Element x = iq.getChildElement().addElement("x", "jabber:x:data");
		x.addAttribute("type", "submit");
		Element field = x.addElement("field").addAttribute("var", "FORM_TYPE").addAttribute("type", "hidden");
		field.addElement("value").addText(MessageArchiveService.MAM_NAMESPACE);
		x.addElement("field").addAttribute("var", "with").addElement("value").addText(TEST_USERS_JID.toBareJID());
		
		return iq;
	}
	
	public static IQ getBaseQuery() {
		IQ queryRequest = new IQ(IQ.Type.set);

		queryRequest.setTo(MIX_CHANNEL_JID);
		queryRequest.setFrom(TEST_USERS_JID);

		Element query = DocumentFactory.getInstance().createElement("query", MessageArchiveService.MAM_NAMESPACE);
		query.addAttribute("queryid", "f28");

		queryRequest.setChildElement(query);
		
		return queryRequest;
	}

	public static IQ getAfterQuery() {

		Calendar today = Calendar.getInstance();
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DAY_OF_MONTH, -1);
		
		IQ iq = MamTestUtils.getBaseQuery();
		Element x = iq.getChildElement().addElement("x", "jabber:x:data");
		x.addAttribute("type", "submit");
		Element field = x.addElement("field").addAttribute("var", "FORM_TYPE").addAttribute("type", "hidden");
		field.addElement("value").addText(MessageArchiveService.MAM_NAMESPACE);
		x.addElement("field").addAttribute("var", "start").addElement("value").addText(MamTestUtils.XMPP_DATE_TIME_FORMAT.format(yesterday.getTime()));
		
		return iq;
	}
}
