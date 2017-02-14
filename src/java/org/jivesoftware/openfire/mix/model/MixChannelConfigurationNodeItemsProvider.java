package org.jivesoftware.openfire.mix.model;

import java.util.Arrays;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.MixPersistenceException;
import org.jivesoftware.openfire.mix.MixPersistenceManager;
import org.jivesoftware.openfire.mix.exception.MixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;

public class MixChannelConfigurationNodeItemsProvider implements MixChannelNodeItemsProvider<MixChannelConfigurationNodeItem> {
	private static final Logger LOG = LoggerFactory.getLogger(MixChannelConfigurationNodeItemsProvider.class);
	
	private MixChannel mixChannel;
	
	private MixChannelConfigurationNodeItem currentItem;

	private MixPersistenceManager persistenceManager;
	
	public MixChannelConfigurationNodeItemsProvider(MixChannel mixChannel, MixPersistenceManager persistenceManager) {
		this.mixChannel = mixChannel;
		this.persistenceManager = persistenceManager;
		
		currentItem = new MixChannelConfigurationNodeItem(mixChannel);
	}

	@Override
	public List<MixChannelConfigurationNodeItem> getItems() {
		return Arrays.asList(new MixChannelConfigurationNodeItem(mixChannel));
	}

	@Override
	public void addItemsListener(
			MixChannelNodeItemsProvider.ItemsListener<MixChannelConfigurationNodeItem> listener) {
	}

	@Override
	public MixChannelConfigurationNodeItem getItem(String itemId) {
		if(itemId.equals(currentItem.getUID())) {
			return currentItem;
		}
		
		return null;
	}

	@Override
	public MixChannelConfigurationNodeItem receiveItem(Element itemElement) throws MixException {
		String incomingId = itemElement.attributeValue("id");
		
		if((incomingId != null) && (incomingId != "")) {
			// TODO Return this error to the user
			LOG.error("A configuration item should have no (or blank) 'id' attribute");
			return null;
		}
		
		DataForm form = new DataForm(itemElement.element(QName.get(DataForm.ELEMENT_NAME, DataForm.NAMESPACE)));
		
		if(form != null) {
			MixChannelConfigurationNodeItem updateItem = new MixChannelConfigurationNodeItem(mixChannel, form);
			
			updateItem.applyConfigurationToChannel();
			
			try {
				persistenceManager.save(mixChannel);
				
				// Reload the item from the channel (with the full form)
				currentItem = new MixChannelConfigurationNodeItem(mixChannel);
			} catch (MixPersistenceException e) {
				// TODO Return this error to the user
				LOG.error("An error occurred while updating channel configuration", e);				
				return null;
			}
		}
		
		return currentItem;
	}

}
