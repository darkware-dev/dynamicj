/*==============================================================================
 =
 = Copyright 2017: darkware.org
 =
 =    Licensed under the Apache License, Version 2.0 (the "License");
 =    you may not use this file except in compliance with the License.
 =    You may obtain a copy of the License at
 =
 =        http://www.apache.org/licenses/LICENSE-2.0
 =
 =    Unless required by applicable law or agreed to in writing, software
 =    distributed under the License is distributed on an "AS IS" BASIS,
 =    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 =    See the License for the specific language governing permissions and
 =    limitations under the License.
 =
 =============================================================================*/

package org.darkware.lazy;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author jeff
 * @since 2016-06-05
 */
public class LazyLoadedSetTests
{
    private Set<String> loadedStrings;

    private Map<String, Set<String>> stringSets;

    public LazyLoadedSetTests()
    {
        super();

        this.stringSets = new HashMap<>();

        this.createSet("A", "A", "B", "C");
        this.createSet("D", "D", "E");
        this.createSet("F", "F", "G", "H", "I");
    }

    private void createSet(final String name, String ... items)
    {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, items);

        this.stringSets.put(name, set);
    }

    private void useSet(final String name)
    {
        this.loadedStrings = this.stringSets.get(name);
    }

    @Test
    public void checkCreation()
    {
        this.loadedStrings = new HashSet<>();
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());
    }

    @Test
    public void checkLoading()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());

        // Check that the loading does happen
        assertEquals(this.stringSets.get("A"), a.value());
        assertTrue(a.isLoaded());
    }

    @Test
    public void checkLoadingWithChangedBackend()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        // Check that the loading grabbed the initial value
        assertEquals(this.stringSets.get("A"), a.value());
        assertTrue(a.isLoaded());

        // Change the backend
        this.useSet("D");

        // Check that this doesn't change the loaded value
        assertNotEquals(this.stringSets.get("D"), a.value());

        // Expire the current value and check that the new value is loaded
        a.expire();
        assertEquals(this.stringSets.get("D"), a.value());
    }

    @Test
    public void checkExpiration()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        // Load the value
        Set<String> originalValue = a.value();

        // Check expiration
        assertFalse(a.isExpired());
        a.expire();
        assertTrue(a.isExpired());

        // Change the backend value
        this.useSet("D");

        // Renew the value
        a.renew();

        // Check if its not expired any longer
        assertFalse(a.isExpired());

        // Check that the value hasn't been reloaded
        assertEquals(originalValue, a.value());
    }

    @Test
    public void checkSetAccessor()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        assertThat(a.values()).containsExactlyElementsOf(this.stringSets.get("A"));
    }

    @Test
    public void checkIterator()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        Iterator<String> iter = a.iterator();

        assertThat(iter).containsExactlyElementsOf(this.stringSets.get("A"));
    }

    @Test
    public void checkStream()
    {
        this.useSet("A");
        LazyLoadedSet<String> a = new LazyLoadedSet<>(this::loadStrings);

        assertThat(a.stream()).containsExactlyElementsOf(this.stringSets.get("A"));
    }

    public Set<String> loadStrings()
    {
        return this.loadedStrings;
    }
}
