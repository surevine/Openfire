package org.jivesoftware.openfire.fdp;

import org.dom4j.Element;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.PubSubService;
import org.jivesoftware.openfire.pubsub.PublishedItem;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

/**
 * Represents a XEP-0346 Form Submission.
 * @author jonnyheavey
 */
public class FormSubmission {

	public static final String FDP_SUBMITTED_PREFIX = "fdp/submitted/";
	public static final String FDP_TEMPLATE_PREFIX = "fdp/template/";

	private PubSubService service;
	private Node node;
	private DataForm submittedForm;

	public FormSubmission(PubSubService service, Node node, DataForm submittedForm) {
		this.service = service;
		this.node = node;
		this.submittedForm = submittedForm;
	}

	/**
	 * Determines whether form submission is valid
	 * @return true if form submitted was valid (according to template)
	 */
	public boolean isValid() {

		DataForm formTemplate = getFormTemplate();
		if(formTemplate == null) {
			return false;
		}

		for(FormField templateField : formTemplate.getFields()) {

			FormField submittedField = submittedForm.getField(templateField.getVariable());
			if(submittedField == null) {
        		return false;
        	}
        	if(submittedField.getType() != templateField.getType()) {
        		return false;
        	}
 			if(submittedField.getFirstValue() == null) {
 				return false;
 			}

		}

        return true;
	}

	/**
	 * Retrieves corresponding template for submitted form.
	 * @return form template or null if none was found.
	 */
	private DataForm getFormTemplate() {
		DataForm templateForm = null;

    	String templateNodeID = node.getNodeID().replace(FDP_SUBMITTED_PREFIX, FDP_TEMPLATE_PREFIX);
    	Node templateNode = service.getNode(templateNodeID);

    	if(templateNode != null) {
	    	PublishedItem template = templateNode.getLastPublishedItem();
	    	if(template != null) {
	    		Element templatePayload = template.getPayload();
	    		templateForm = new DataForm(templatePayload);
	    	}
    	}

    	return templateForm;
	}

    /**
     * Determines whether node is a XEP-0346 Submission node.
     * @param node node to check
     * @return
     */
	public static boolean isFDPSubmissionNode(Node node) {
		return node.getNodeID().startsWith(FDP_SUBMITTED_PREFIX);
	}

}
