/*==============================================================================
 =
 = Copyright 2017: Jeff Sharpe
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * @author jeff
 * @since 2016-06-05
 */
public class LazyLoadedMapTests
{
    private Map<Integer, String> loadedMap;

    private Map<String, Map<Integer, String>> stringMaps;

    public LazyLoadedMapTests()
    {
        super();

        this.stringMaps = new HashMap<>();

        this.createMap("A", "A", "B", "C");
        this.createMap("D", "D", "E");
        this.createMap("F", "F", "G", "H", "I");
    }

    private void createMap(final String name, String ... items)
    {
        Map<Integer, String> map = new HashMap<>();
        int id = 0;
        for (String item : items) map.put(id++, item);

        this.stringMaps.put(name, map);
    }

    private void useMap(final String name)
    {
        this.loadedMap = this.stringMaps.get(name);
    }

    @Test
    public void checkCreation()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());
    }

    @Test
    public void checkLoading()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());

        // Check that the loading does happen
        assertEquals(this.stringMaps.get("A"), a.value());
        assertTrue(a.isLoaded());
    }

    @Test
    public void checkLoadingWithChangedBackend()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        // Check that the loading grabbed the initial value
        assertEquals(this.stringMaps.get("A"), a.value());
        assertTrue(a.isLoaded());

        // Change the backend
        this.useMap("D");

        // Check that this doesn't change the loaded value
        assertNotEquals(this.stringMaps.get("D"), a.value());

        // Expire the current value and check that the new value is loaded
        a.expire();
        assertEquals(this.stringMaps.get("D"), a.value());
    }

    @Test
    public void checkExpiration()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        // Load the value
        Map<Integer, String> originalValue = a.value();

        // Check expiration
        assertFalse(a.isExpired());
        a.expire();
        assertTrue(a.isExpired());

        // Change the backend value
        this.useMap("D");

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
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        assertThat(a.map()).containsAllEntriesOf(this.stringMaps.get("A"));
    }

    @Test
    public void checkIterator()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        Iterator<String> iter = a.iterator();

        assertThat(iter).containsExactlyElementsOf(this.stringMaps.get("A").values());
    }

    @Test
    public void checkStream()
    {
        this.useMap("A");
        LazyLoadedMap<Integer, String> a = new LazyLoadedMap<>(this::loadMap);

        assertThat(a.stream()).containsExactlyElementsOf(this.stringMaps.get("A").entrySet());
    }

    public Map<Integer, String> loadMap()
    {
        return this.loadedMap;
    }
}
