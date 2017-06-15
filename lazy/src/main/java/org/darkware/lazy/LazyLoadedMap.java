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

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link LazyLoadedMap} is a specialized {@link LazyLoader} which efficiently loads a {@link Map}.
 * <p>
 * <em>Note:</em> The {@link Map} returned by the attached {@link Supplier} should not be assumed to be the
 * {@link Map} used for internal storage in this object.
 *
 * @param <K> The object type for the {@link Map}'s key
 * @param <T> The object type for the {@link Map}'s value
 *
 * @author jeff
 * @since 2016-05-16
 */
public class LazyLoadedMap<K, T> extends LazyLoader<Map<K, T>> implements Iterable<T>
{
    /**
     * Create a new lazy loaded value handler. It will store a Set of the parameterized type. The value
     * will not be fetched until needed, and won't be fetched again until it expires. This particular value
     * does not automatically expire, but it can be made to manually expire.
     *
     * @param loader A {@link Supplier} reference which loads the map to be stored.
     */
    public LazyLoadedMap(final Supplier<Map<K, T>> loader)
    {
        this(loader, null);
    }

    /**
     * Create a new lazy loaded value handler. It will store a Set of the parameterized type. The value
     * will not be fetched until needed, and won't be fetched again until it expires.
     *
     * @param loader A {@link Supplier} reference which loads the map to be stored.
     * @param ttl The amount of time the value should be allowed to be stored before the value automatically
     * expires.
     */
    public LazyLoadedMap(final Supplier<Map<K, T>> loader, final Duration ttl)
    {
        super(loader, ttl);
    }

    @Override
    protected Map<K, T> prepopulate()
    {
        return new ConcurrentHashMap<>();
    }

    @Override
    public final void applyData(final Map<K, T> newData)
    {
        // Remove all items not in the current set
        this.data.entrySet().removeIf(e -> !newData.containsKey(e.getKey()));

        // Store all items in the set.
        newData.entrySet().forEach(e -> this.data.put(e.getKey(), e.getValue()));
    }

    /**
     * Fetch the value. If the value has not been fetched or if the value has expired, a new copy will be
     * retrieved.
     *
     * @return A value of the declared type. The value may be {@code null} or any assignable subtype.
     */
    public final Map<K, T> map()
    {
        return this.value();
    }

    @Override
    public final Iterator<T> iterator()
    {
        return this.map().values().iterator();
    }

    /**
     * Fetch a stream of the data map.
     *
     * @return A {@link Stream} object.
     */
    public Stream<Map.Entry<K,T>> stream()
    {
        return this.map().entrySet().stream();
    }
}
