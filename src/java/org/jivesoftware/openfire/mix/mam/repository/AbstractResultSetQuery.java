package org.jivesoftware.openfire.mix.mam.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.mix.mam.ArchivedMixChannelMessage;
import org.jivesoftware.openfire.mix.mam.MessageArchiveService;
import org.xmpp.packet.IQ;

public abstract class AbstractResultSetQuery implements ResultSetQuery {

	protected MixChannelArchiveRepository repository;
	
	protected String channelName;
	
	protected int limit = 0;
	
	private String[] paramKeys = {"FORM_TYPE", "start", "end", "with"};
	
	protected Map<String, String> params = new HashMap<>();

	@SuppressWarnings("unchecked")
	public AbstractResultSetQuery(MixChannelArchiveRepository repository, IQ query) {
		this.repository = repository;
		channelName = query.getTo().getNode();
		
		Element queryNode = query.getChildElement();

		if ((queryNode != null) && (queryNode.getQName().equals(QName.get("query", MessageArchiveService.MAM_NAMESPACE)))) {
			
			List<Element> x = queryNode.elements("x");

			if (!x.isEmpty() && x.size() == 1) {
				List<Element> fieldElems = x.get(0).elements("field");
				for (Element field : fieldElems) {
					String attributeName = field.attribute("var").getText();
					
					for (String paramKey : paramKeys) {
						if (paramKey.equals(attributeName)) {
							params.put(paramKey, field.element("value").getTextTrim());
						}
					}
				}
			}
			
			List<Element> sets = queryNode.elements("set");
			
			for (Element set : sets) {
				Element max = set.element("max");
				if (max != null) {
					limit = Integer.parseInt(max.getTextTrim());
				}
			}
		}
	}
	
	@Override
	public abstract List<ArchivedMixChannelMessage> execute();

}
