/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.zuul.message;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Wraps a ListMultimap and ensures all keys are lower-case as http headers are
 * case insensitive.
 *
 * User: michaels@netflix.com
 * Date: 2/20/15
 * Time: 3:13 PM
 */
public class Headers implements Cloneable
{
    private final ListMultimap<HeaderName, String> delegate;
    private final boolean immutable;

    public Headers()
    {
        delegate = ArrayListMultimap.create();
        immutable = false;
    }

    private Headers(ListMultimap<HeaderName, String> delegate)
    {
        this.delegate = delegate;
        immutable = ImmutableListMultimap.class.isAssignableFrom(delegate.getClass());
    }

    /**
     * Get the first value found for this key even if there are multiple. If none, then
     * return null.
     *
     * @param name
     * @return
     */
    public String getFirst(String name)
    {
        HeaderName hn = new HeaderName(name);
        List<String> values = delegate.get(hn);
        if (values != null) {
            if (values.size() > 0) {
                return values.get(0);
            }
        }
        return null;
    }

    /**
     * Get the first value found for this key even if there are multiple. If none, then
     * return the specified defaultValue.
     *
     * @param name
     * @return
     */
    public String getFirst(String name, String defaultValue)
    {
        String value = getFirst(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public List<String> get(String name)
    {
        HeaderName hn = new HeaderName(name);
        return delegate.get(hn);
    }

    /**
     * Replace any/all entries with this key, with this single entry.
     *
     * If value is null, then not added, but any existing header of same name is removed.
     *
     * @param name
     * @param value
     */
    public void set(String name, String value)
    {
        HeaderName hn = new HeaderName(name);
        delegate.removeAll(hn);
        if (value != null) {
            delegate.put(hn, value);
        }
    }

    public boolean setIfAbsent(String name, String value)
    {
        boolean did = false;
        if (! contains(name)) {
            set(name, value);
            did = true;
        }
        return did;
    }

    public void add(String name, String value)
    {
        HeaderName hn = new HeaderName(name);
        delegate.put(hn, value);
    }

    public void putAll(Headers headers)
    {
        delegate.putAll(headers.delegate);
    }

    public List<String> remove(String name)
    {
        HeaderName hn = new HeaderName(name);
        return delegate.removeAll(hn);
    }

    public Collection<Header> entries()
    {
        return delegate.entries()
                .stream()
                .map(entry -> new Header(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Set<String> keySet()
    {
        return delegate.keySet().stream()
                .map(headerName -> headerName.getName())
                .collect(Collectors.toSet());
    }

    public boolean contains(String name)
    {
        return delegate.containsKey(new HeaderName(name));
    }

    public boolean contains(String name, String value)
    {
        HeaderName hn = new HeaderName(name);
        return delegate.containsEntry(hn, value);
    }

    public int size()
    {
        return delegate.size();
    }

    @Override
    public Headers clone()
    {
        Headers copy = new Headers();
        copy.delegate.putAll(this.delegate);
        return copy;
    }

    public Headers immutableCopy()
    {
        return new Headers(ImmutableListMultimap.copyOf(delegate));
    }

    public boolean isImmutable()
    {
        return immutable;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (! (obj instanceof Headers))
            return false;

        Headers h2 = (Headers) obj;
        return Iterables.elementsEqual(delegate.entries(), h2.delegate.entries());
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }


    @RunWith(MockitoJUnitRunner.class)
    public static class UnitTest
    {
        @Test
        public void testCaseInsensitiveKeys_Set()
        {
            Headers headers = new Headers();
            headers.set("Content-Length", "5");
            headers.set("content-length", "10");

            assertEquals("10", headers.getFirst("Content-Length"));
            assertEquals("10", headers.getFirst("content-length"));
            assertEquals(1, headers.get("content-length").size());
        }

        @Test
        public void testCaseInsensitiveKeys_Add()
        {
            Headers headers = new Headers();
            headers.add("Content-Length", "5");
            headers.add("content-length", "10");

            List<String> values = headers.get("content-length");
            assertTrue(values.contains("10"));
            assertTrue(values.contains("5"));
            assertEquals(2, values.size());
        }

        @Test
        public void testCaseInsensitiveKeys_SetIfAbsent()
        {
            Headers headers = new Headers();
            headers.set("Content-Length", "5");
            headers.setIfAbsent("content-length", "10");

            List<String> values = headers.get("content-length");
            assertEquals(1, values.size());
            assertEquals("5", values.get(0));
        }

        @Test
        public void testCaseInsensitiveKeys_PutAll()
        {
            Headers headers = new Headers();
            headers.add("Content-Length", "5");
            headers.add("content-length", "10");

            Headers headers2 = new Headers();
            headers2.putAll(headers);

            List<String> values = headers2.get("content-length");
            assertTrue(values.contains("10"));
            assertTrue(values.contains("5"));
            assertEquals(2, values.size());
        }
    }
}