package org.jivesoftware.openfire.mix.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.jivesoftware.openfire.mix.MixManager;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.constants.ChannelJidVisibilityMode;
import org.jivesoftware.openfire.mix.exception.MixFormValidationException;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.DataForm.Type;
import org.xmpp.forms.FormField;

public class MixChannelConfigurationNodeItem implements MixChannelNodeItem {
	private static final Logger LOG = LoggerFactory.getLogger(MixChannelConfigurationNodeItem.class);
	
	public static final String FORM_TYPE = MixManager.MIX_NAMESPACE;
	
	public static final String FIELD_FORM_TYPE = "FORM_TYPE";
	public static final String FIELD_JID_VISIBILITY = "jid-visibility";
	
	private MixChannel mixChannel;
	
	private DataForm form;
	
	private String id;

	public MixChannelConfigurationNodeItem(MixChannel mixChannel) {
		this.mixChannel = mixChannel;
		id = XMPPDateTimeFormat.format(new Date());
		
		form = new DataForm(Type.form);
		
		/* Form Type Field */
		FormField typeField = form.addField();
		typeField.setVariable(FIELD_FORM_TYPE);
		typeField.setType(FormField.Type.hidden);
		typeField.addValue(FORM_TYPE);
		
		/* JID Visibility Field */
		FormField jidVisibilityField = form.addField(FIELD_JID_VISIBILITY, "JID Visibility", FormField.Type.list_single);
		
		jidVisibilityField.addOption("Always Hidden", ChannelJidVisibilityMode.MANDATORY_HIDDEN.getXmppValue());
		jidVisibilityField.addOption("Hidden by Default", ChannelJidVisibilityMode.HIDDEN.getXmppValue());
		jidVisibilityField.addOption("Visible by Default", ChannelJidVisibilityMode.HIDDEN.getXmppValue());
		jidVisibilityField.addOption("Always Visible", ChannelJidVisibilityMode.MANDATORY_VISIBLE.getXmppValue());
		
		jidVisibilityField.addValue(mixChannel.getJidVisibilityMode().getXmppValue());
	}
	
	public MixChannelConfigurationNodeItem(MixChannel mixChannel, DataForm newForm) {
		this.mixChannel = mixChannel;
		
		if((newForm.getField(FIELD_FORM_TYPE) == null) || !FORM_TYPE.equals(newForm.getField(FIELD_FORM_TYPE).getFirstValue())) {
			// The form doesn't have a form 
		}
		this.form = newForm;
	}
	
	private Map<String, String> validate() {
		Map<String, String> messages = new HashMap<>();
		
		if((form.getField(FIELD_FORM_TYPE) == null) || !FORM_TYPE.equals(form.getField(FIELD_FORM_TYPE).getFirstValue())) {
			messages.put(FIELD_FORM_TYPE, "The form must be of type " + FORM_TYPE);
		}

		try {
			FormField jidVisibilityField = form.getField(FIELD_JID_VISIBILITY);
			
			if(jidVisibilityField != null) {
				ChannelJidVisibilityMode visibility = ChannelJidVisibilityMode.fromXmppValue(jidVisibilityField.getFirstValue());
			}
		} catch(IndexOutOfBoundsException e) {
			// TODO: Do something better here - ignoring this seems wrong. We should send an error back.
			LOG.warn("Ignoring invalid Channel JID Visibility Value");
		}
		
		return messages;
	}
	
	/**
	 * Applies the configuration {@link DataForm} to the {@link MixChannel}
	 */
	public void applyConfigurationToChannel() throws MixFormValidationException {
		Map<String, String> validationMessages = validate();
		
		if(!validationMessages.isEmpty()) {
			throw new MixFormValidationException(validationMessages);
		}
		
		FormField jidVisibilityField = form.getField(FIELD_JID_VISIBILITY);
		
		if(jidVisibilityField != null) {
			try {
				ChannelJidVisibilityMode visibility = ChannelJidVisibilityMode.fromXmppValue(jidVisibilityField.getFirstValue());
				
				mixChannel.setJidVisibilityMode(visibility);
			} catch(IndexOutOfBoundsException e) {
				// TODO: Do something better here - ignoring this seems wrong. We should send an error back.
				LOG.warn("Ignoring invalid Channel JID Visibility Value");
			}
		}
	}

	@Override
	public String getUID() {
		return id;
	}

	@Override
	public Element appendPayload(Element container) {
		container.add(form.getElement());
		
		return form.getElement();
	}
}
