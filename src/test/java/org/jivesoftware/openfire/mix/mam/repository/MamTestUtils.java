package org.jivesoftware.openfire.mix.mam.repository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.mam.MessageArchiveService;
import org.jivesoftware.openfire.mix.mam.MessageArchiveServiceImplTest;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;

public class MamTestUtils {
	
	public static final String QUERY_ELEM_NAME = "query";

	public static final String TEST_USER = "hag66";

	public static final String TEST_MIX_CHANNEL_NAME = "coven";

	public static final String TEST_SERVICE_DOMAIN = "shakespeare.example";

	public static final String TEST_MIX_DOMAIN = "mix." + TEST_SERVICE_DOMAIN;

	public static final JID TEST_USERS_JID = new JID(TEST_USER, TEST_SERVICE_DOMAIN, null);

	public static final JID MIX_CHANNEL_JID = new JID(TEST_MIX_CHANNEL_NAME, TEST_MIX_DOMAIN, null);

	public static IQ getTimeBoundQuery() {

		Calendar today = Calendar.getInstance();
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DAY_OF_MONTH, -1);
		
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD'T'HH:mm:ssZ");
		
		IQ iq = MamTestUtils.getBaseQuery();
		Element x = iq.getChildElement().addElement("x", "jabber:x:data");
		x.addAttribute("type", "submit");
		Element field = x.addElement("field").addAttribute("var", "FORM_TYPE").addAttribute("type", "hidden");
		field.addElement("value").addText(MessageArchiveService.MAM_NAMESPACE);
		x.addElement("field").addAttribute("var", "start").addElement("value").addText(sdf.format(yesterday.getTime()));
		x.addElement("field").addAttribute("var", "end").addElement("value").addText(sdf.format(today.getTime()));
		
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
		
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD'T'HH:mm:ssZ");
		
		IQ iq = MamTestUtils.getBaseQuery();
		Element x = iq.getChildElement().addElement("x", "jabber:x:data");
		x.addAttribute("type", "submit");
		Element field = x.addElement("field").addAttribute("var", "FORM_TYPE").addAttribute("type", "hidden");
		field.addElement("value").addText(MessageArchiveService.MAM_NAMESPACE);
		x.addElement("field").addAttribute("var", "start").addElement("value").addText(sdf.format(yesterday.getTime()));
		
		return iq;
	}
	

	public static ArchivedMixChannelMessage getTestMessage() {
		return getTestMessage(TEST_USERS_JID);
	}
	
	public static ArchivedMixChannelMessage getTestMessage(JID from) {
		Message msg = new Message();
		msg.setBody(
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ligula erat, ullamcorper at ullamcorper e");
		msg.setType(Type.groupchat);
		msg.setSubject("Lorem ipsum");
		msg.setTo(MIX_CHANNEL_JID);
		msg.setFrom(from);

		ArchivedMixChannelMessage amcm = new ArchivedMixChannelMessage(msg);
		
		// Both of these below are performed by the database, but need to be done here.
		amcm.setId(UUID.randomUUID().toString());
		amcm.setArchiveTimestamp(new Date());

		return amcm;	
	}
}
