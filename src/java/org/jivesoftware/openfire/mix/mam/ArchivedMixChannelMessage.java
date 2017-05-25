package org.jivesoftware.openfire.mix.mam;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.hibernate.annotations.GenericGenerator;
import org.jivesoftware.openfire.labelling.SecurityLabel;
import org.jivesoftware.openfire.mix.mam.repository.TimeBasedChannelQuery;
import org.jivesoftware.openfire.mix.model.LocalMixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelMessage;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.Packet;

@Entity
@Table(name = "ofArchivedMixChannelMessage")
public class ArchivedMixChannelMessage {

	private static final Logger Log = LoggerFactory.getLogger(ArchivedMixChannelMessage.class);

	@Id @GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
	private String id;
	
	@Enumerated(EnumType.STRING)
	private Type type;
	
	private String subject;
	
	@Column(columnDefinition = "text")
	private String body;

	@Column(columnDefinition = "text")
	private String stanza;

	private String channel;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date archiveTimestamp;

	private String fromJID;
	
	@Transient
	private SimpleDateFormat sdf = new SimpleDateFormat(TimeBasedChannelQuery.MAM_DATE_FORMAT);

	public ArchivedMixChannelMessage() {
	}

	public ArchivedMixChannelMessage(MixChannelMessage archive) {
		Message message = archive.getMessage();
		MixChannelParticipant sender = archive.getSender();

		this.type = message.getType();
		this.subject = message.getSubject();
		this.body = message.getBody();

		Message stanza = message.createCopy();
		stanza.getElement().addAttribute("id", this.getId());
		stanza.getElement().addAttribute("from", message.getTo().toBareJID());
		stanza.getElement().addAttribute("to", null);
		stanza.addChildElement("nick", "urn:xmpp:mix:0").addText(sender.getNick());
		stanza.addChildElement("jid", "urn:xmpp:mix:0").addText(sender.getJid().toBareJID().toString());

		this.stanza = stanza.toXML();

		this.setChannel(message.getTo().getNode());
		this.setFromJID(sender.getJid().toBareJID().toString());
	}

	private void setFromJID(String node) {
		this.fromJID = node;
	}
	
	public String getFromJID() {
		return this.fromJID;
	}

	@PrePersist
	private void onPrePersist() {
		this.archiveTimestamp = new Date();
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getStanza() {
		return stanza;
	}

	public void setStanza(String stanza) {
		this.stanza = stanza;
	}

	public SecurityLabel getSecurityLabel() {
		try {
			Document stanzaDoc = DocumentHelper.parseText(stanza);
			Element stanzaElement = stanzaDoc.getRootElement();
			Element securityLabelElement = stanzaElement.element(SecurityLabel.QNAME);
			return new SecurityLabel(securityLabelElement);
		} catch (Exception e) {
			Log.error("Failed to parse Stanza XML", e);
		}
		return null;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Date getArchiveTimestamp() {
		return new Date(this.archiveTimestamp.getTime());
	}

	public void setArchiveTimestamp(Date archiveTimestamp) {
		this.archiveTimestamp = archiveTimestamp;
	}

	public Message formatMessageResponse(IQ queryIQ) {
		// create wrapper mam <message>
		Message msg = new Message();
		//msg.setType(Message.Type.groupchat);
		msg.setFrom(queryIQ.getTo());
		msg.setTo(queryIQ.getFrom());

		// add <result> with id set to the message id (from mam)
		Element result = msg.addChildElement("result", MessageArchiveService.MAM_NAMESPACE);
		result.addAttribute("id", this.getId());

		// add queryid if set in query iq
		String queryid = queryIQ.getChildElement().attributeValue("queryid");
		if (queryid != null) {
			result.addAttribute("queryid", queryid);
		}

		// add <forwarded> and <delay>
		Element forwarded = result.addElement("forwarded", "urn:xmpp:forward:0");
		Element delay = forwarded.addElement("delay", "urn:xmpp:delay");
		delay.addAttribute("stamp", sdf.format(this.getArchiveTimestamp()));

		// parse stanza from db, set id, and to
		String stanza = this.getStanza();
        try {
            Document stanzaDoc = DocumentHelper.parseText(stanza);
            Element stanzaEl = stanzaDoc.getRootElement();
			stanzaEl.setQName(QName.get("message", "jabber:client"));
            // override the message id with the mam archive id
            stanzaEl.addAttribute("id", this.getId());
            stanzaEl.addAttribute("to", queryIQ.getFrom().toString());

            forwarded.add(stanzaEl);
        } catch (Exception ex) {
            Log.error("Failed to parse payload XML", ex);
        }

		return msg;
	}

}
