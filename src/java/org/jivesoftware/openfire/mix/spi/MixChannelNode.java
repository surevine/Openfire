package org.jivesoftware.openfire.mix.spi;

public interface MixChannelNode {
	
	public NodeType getType();
	
	public enum NodeType {
	    presence("urn:xmpp:mix:nodes:presence"),
	    participants("urn:xmpp:mix:nodes:participants"),
	    subject("urn:xmpp:mix:nodes:subject"),
	    config("urn:xmpp:mix:nodes:config")
	    ;

	    private final String urn;

	    /**
	     * @param text
	     */
	    private NodeType(final String urn) {
	        this.urn = urn;
	    }


	    @Override
	    public String toString() {
	        return urn;
	    }
	}
}
