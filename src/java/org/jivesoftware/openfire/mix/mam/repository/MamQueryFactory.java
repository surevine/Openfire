package org.jivesoftware.openfire.mix.mam.repository;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.Node;
import org.xmpp.packet.IQ;

public class MamQueryFactory implements QueryFactory {
	
	@SuppressWarnings("unchecked")
	public Query create(MixChannelArchiveRepository repository, IQ queryIQ) {

		List<Element> xNodes = queryIQ.getChildElement().elements("x");;
		
		if (xNodes.isEmpty()) {
			// Bare channel query
			return new BareChannelQuery(repository, queryIQ);
		} else {

			Element xNode = (Element) xNodes.get(0);
			List fieldNodes = xNode.elements("field");
			Iterator i = fieldNodes.iterator();
			while (i.hasNext()) {
				Node node = (Node) i.next();

				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element fieldNode = (Element) node;
					if (fieldNode.attribute("var").getText().equals("with")) {
						return new WithQuery(repository, queryIQ);
					} else if (fieldNode.attribute("var").getText().equals("start") || fieldNode.attribute("var").getText().equals("end")) {
						return new TimeBasedChannelQuery(repository, queryIQ);
					} else {
						return new BareChannelQuery(repository, queryIQ);
					}
				}
			}			
		}

		return null;
	}

}
