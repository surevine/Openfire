/*
 * Copyright (C) 2026 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the admin console's message bundles (openfire_i18n*.properties). A key defined twice in a bundle is a
 * mistake that nothing else reports: the later definition silently replaces the earlier one, so a page shows another
 * message than the one it asks for.
 *
 * @author Matthew Vivian
 */
public class I18nBundleKeysTest
{
    private static final String BASE_BUNDLE = "openfire_i18n.properties";

    /**
     * Verifies that no message bundle defines a key more than once.
     */
    @Test
    public void testNoBundleDefinesAKeyTwice() throws Exception
    {
        // Setup test fixture.
        final URL base = I18nBundleKeysTest.class.getClassLoader().getResource(BASE_BUNDLE);
        assertNotNull(base, "the base message bundle must be on the classpath");

        // Execute system under test.
        final Map<String, List<String>> duplicatesByBundle = new LinkedHashMap<>();
        final int bundles = forEachBundle(base.toURI(), (name, content) -> {
            final List<String> duplicates = duplicateKeys(content);
            if (!duplicates.isEmpty()) {
                duplicatesByBundle.put(name, duplicates);
            }
        });

        // Verify result.
        assertTrue(bundles > 1, "the base bundle and its translations must be found, but " + bundles + " bundle(s) were");
        assertEquals(Map.of(), duplicatesByBundle, "no bundle may define a key twice");
    }

    /**
     * Verifies that the check finds a key defined twice, including one whose first definition spans several lines,
     * and does not mistake continuation lines or comments for keys.
     */
    @Test
    public void testDuplicateKeysAreFound()
    {
        // Setup test fixture.
        final String content = String.join("\n",
            "# a.b=a comment",
            "a.b=first \\",
            "    a.c=part of a.b",
            "a.c=second",
            "a.d:third",
            "a.b = again",
            "");

        // Execute system under test.
        final List<String> result = duplicateKeys(content);

        // Verify result.
        assertEquals(List.of("a.b"), result, "only the key defined twice must be reported");
        assertFalse(duplicateKeys("x=1\ny=2\n").contains("x"), "distinct keys must not be reported");
    }

    private interface BundleVisitor
    {
        void visit(String name, String content) throws IOException;
    }

    /**
     * Visits every openfire_i18n*.properties beside the base bundle, whether that is in a directory or a jar.
     *
     * @return the number of bundles visited.
     */
    private static int forEachBundle(final URI base, final BundleVisitor visitor) throws IOException
    {
        if ("jar".equals(base.getScheme())) {
            try (final FileSystem jar = FileSystems.newFileSystem(base, Map.of())) {
                return visitBundles(jar.getPath("/"), visitor);
            }
        }
        return visitBundles(Path.of(base).getParent(), visitor);
    }

    private static int visitBundles(final Path directory, final BundleVisitor visitor) throws IOException
    {
        int count = 0;
        try (final Stream<Path> files = Files.list(directory)) {
            for (final Path file : files.filter(f -> {
                final String name = f.getFileName().toString();
                return name.startsWith("openfire_i18n") && name.endsWith(".properties");
            }).toList()) {
                visitor.visit(file.getFileName().toString(), Files.readString(file, StandardCharsets.UTF_8));
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the keys that a properties file defines more than once, following the format that
     * {@link java.util.Properties#load(java.io.Reader)} reads: comments start with '#' or '!', a line ending in an odd
     * number of backslashes continues on the next line, and a key ends at an unescaped '=', ':' or whitespace.
     */
    static List<String> duplicateKeys(final String content)
    {
        final String[] lines = content.split("\n", -1);
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].stripLeading();
            final StringBuilder logical = new StringBuilder(line);
            while (endsWithContinuation(logical) && i + 1 < lines.length) {
                logical.setLength(logical.length() - 1);
                logical.append(lines[++i].stripLeading());
            }
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                continue;
            }
            counts.merge(key(logical.toString()), 1, Integer::sum);
        }
        final List<String> duplicates = new ArrayList<>();
        counts.forEach((key, count) -> {
            if (count > 1) {
                duplicates.add(key);
            }
        });
        return duplicates;
    }

    private static boolean endsWithContinuation(final CharSequence line)
    {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static String key(final String logicalLine)
    {
        final StringBuilder key = new StringBuilder();
        for (int i = 0; i < logicalLine.length(); i++) {
            final char c = logicalLine.charAt(i);
            if (c == '\\' && i + 1 < logicalLine.length()) {
                key.append(c).append(logicalLine.charAt(++i));
            } else if (c == '=' || c == ':' || Character.isWhitespace(c)) {
                break;
            } else {
                key.append(c);
            }
        }
        return key.toString();
    }
}
