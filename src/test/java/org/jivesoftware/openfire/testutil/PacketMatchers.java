package org.jivesoftware.openfire.testutil;

import org.dom4j.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

public class PacketMatchers {
	/**
	 * Matches if the given {@link Packet} is actually a {@link Message}
	 */
	@Factory
	public static <K extends Packet> Matcher<K> isMessage() {
		return new TypeSafeMatcher<K>() {
			@Override
			public void describeTo(Description arg0) {
				arg0.appendText("is a <message/>");
			}

			@Override
			protected boolean matchesSafely(K arg0) {
				return arg0 instanceof Message;
			}
		};
	}
	
	/**
	 * Runs the given {@link Matcher} on the root element of the packet.
	 */
	@Factory
	public static <K extends Packet> Matcher<K> element(final Matcher<Element> elementMatcher) {
		return new TypeSafeMatcher<K>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("element ");
				elementMatcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(K obj) {
				return elementMatcher.matches(obj.getElement());
			}
		};
	}
	
	@Factory
	public static <K extends Packet> Matcher<K> hasErrorCondition(final PacketError.Condition condition) {
		return new TypeSafeMatcher<K>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("packet has error condition ").appendText(condition.toString());
			}

			@Override
			protected boolean matchesSafely(K obj) {
				return obj.getError().getCondition().equals(condition);
			}
		};		
	}
}
