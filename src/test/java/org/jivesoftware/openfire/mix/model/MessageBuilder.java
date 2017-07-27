package org.jivesoftware.openfire.mix.model;

import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MessageBuilder {

    private Message message;

    public MessageBuilder() {
        this.message = new Message();
    }

    public MessageBuilder from(JID from) {
        message.setFrom(from);
        return this;
    }

    public MessageBuilder to(JID to) {
        message.setTo(to);
        return this;
    }

    public MessageBuilder type(Message.Type type) {
        message.setType(type);
        return this;
    }

    public MessageBuilder id(String id) {
        message.setID(id);
        return this;
    }

    public MessageBuilder body(String body) {
        message.setBody(body);
        return this;
    }

    public MessageBuilder subject(String s) {
        message.setSubject(s);
        return this;
    }

    public Message build() {
        return message;
    }


}
