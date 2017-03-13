package org.jivesoftware.openfire.mix.mam;

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

import org.dom4j.Element;
import org.hibernate.annotations.GenericGenerator;
import org.jivesoftware.openfire.mix.mam.repository.TimeBasedChannelQuery;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.Packet;

@Entity
@Table(name = "ofArchivedMixChannelMessage")
public class ArchivedMixChannelMessage {
	
	@Id @GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
	private String id;
	
	@Enumerated(EnumType.STRING)
	private Type type;
	
	private String subject;
	
	@Column(columnDefinition = "text")
	private String body;

	private String channel;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date archiveTimestamp;

	private String fromJID;
	
	@Transient
	private SimpleDateFormat sdf = new SimpleDateFormat(TimeBasedChannelQuery.MAM_DATE_FORMAT);

	public ArchivedMixChannelMessage() {
		
	}

	public ArchivedMixChannelMessage(Message archive) {
		this.type = archive.getType();
		this.subject = archive.getSubject();
		this.body = archive.getBody();
		this.setChannel(archive.getTo().getNode());
		this.setFromJID(archive.getFrom().getNode());
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
		Message msg = new Message();
		msg.setType(Message.Type.groupchat);
		msg.setTo(queryIQ.getFrom());

		Element result = msg.addChildElement("result", MessageArchiveService.MAM_NAMESPACE);
		result.addAttribute("id", this.id);

		String queryid = queryIQ.getChildElement().attributeValue("queryid");

		if (queryid != null) {
			result.addAttribute("queryid", queryid);
		}
		Element forwarded = result.addElement("forwarded", "urn:xmpp:forward:0");
		Element delay = forwarded.addElement("delay", "urn:xmpp:delay");
		delay.addAttribute("stamp", sdf.format(this.getArchiveTimestamp()));
		Element message = forwarded.addElement("message", "jabber:client");
		message.addAttribute("from", this.getFromJID());
		message.addAttribute("type", Message.Type.groupchat.name());
		Element body = message.addElement("body");
		body.setText(this.getBody());
		
		return msg;
	}

}
