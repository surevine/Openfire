package org.jivesoftware.openfire.testutil;

import org.dom4j.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ElementMatchers {
	@Factory
	public static <K extends Element> Matcher<K> hasTextChild(final String childName, final Matcher<String> textValueMatcher) {
		return new TypeSafeMatcher<K>() {

			@Override
			public void describeTo(Description arg0) {
				arg0.appendText("has child " + childName + " with text value " + textValueMatcher);
			}

			@Override
			protected boolean matchesSafely(K arg0) {
				return (arg0.element(childName) != null)
						&& (textValueMatcher.matches(arg0.element(childName).getText()));
			}
			
		};
	}
	
	@Factory
	public static <K extends Element> Matcher<K> hasNoChild(final String childName) {
		return new TypeSafeMatcher<K>() {

			@Override
			public void describeTo(Description arg0) {
				arg0.appendText("doesn't have child " + childName);
			}

			@Override
			protected boolean matchesSafely(K arg0) {
				return (arg0.element(childName) == null);
			}
		};
	}

}
