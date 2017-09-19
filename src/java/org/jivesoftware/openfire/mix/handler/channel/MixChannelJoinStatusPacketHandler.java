package org.jivesoftware.openfire.mix.handler.channel;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityPreference;
import org.jivesoftware.openfire.mix.exception.CannotJoinMixChannelException;
import org.jivesoftware.openfire.mix.handler.MixRequestContext;
import org.jivesoftware.openfire.mix.model.MixChannel;
import org.jivesoftware.openfire.mix.model.MixChannelParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MixChannelJoinStatusPacketHandler implements MixChannelPacketHandler {

	private static final Logger Log = LoggerFactory.getLogger(MixChannelJoinStatusPacketHandler.class);

	@Override
	public IQ processIQ(MixRequestContext context, MixChannel channel, IQ iq) {
		// Unpack packet
		Element joinNode = iq.getChildElement();

		if (iq.getType() != IQ.Type.get) {
			return null;
		}

		if ((joinNode == null) || (!joinNode.getQName().equals(QName.get("join", MixManager.MIX_NAMESPACE)))) {
			return null;
		}

		IQ result = IQ.createResultIQ(iq);
		Element joinElement = result.setChildElement("join", MixManager.MIX_NAMESPACE);

        MixChannelParticipant mcp = channel.getParticipantByRealJID(iq.getFrom());

        if (mcp == null) {
        	result.setType(IQ.Type.error);
        	return result;
		}

        for (String subscription : mcp.getSubscriptions()) {
            Element current = joinElement.addElement("subscribe");
            current.addAttribute("node", subscription);
        }
        joinElement.addAttribute("jid", mcp.getJid().toBareJID());

		DataForm form = new DataForm(DataForm.Type.result);

		FormField formType = form.addField("FORM_TYPE", "", FormField.Type.hidden);
		formType.addValue("urn:xmpp:mix:0");

		FormField jidVisibility = form.addField("JID Visibility", "", FormField.Type.text_single);
		switch (mcp.getJidVisibilityPreference()) {
			case NO_PREFERENCE:
				jidVisibility.addValue("no-preference");
				break;
			case PREFER_HIDDEN:
				jidVisibility.addValue("prefer-hidden");
				break;
			case ENFORCE_HIDDEN:
				jidVisibility.addValue("enforce-hidden");
				break;
			case ENFORCE_VISIBLE:
				jidVisibility.addValue("enforce-visible");
				break;
		}

		joinElement.add(form.getElement());

		return result;
	}

	@Override
	public boolean processPresence(MixRequestContext context, MixChannel channel, Presence presence) {
		return false;
	}

	@Override
	public boolean processMessage(MixRequestContext context, MixChannel channel, Message message) {
		return false;
	}
}
