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

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link LazyLoadedSet} is a specialized {@link LazyLoader} which efficiently loads a {@link Set}.
 * <p>
 * <em>Note:</em> The {@link Set} returned by the attached {@link Supplier} should not be assumed to be the
 * {@link Set} used for internal storage in this object.
 *
 * @param <T> The object type stored in the {@link Set}.
 *
 * @author jeff
 * @since 2016-05-16
 */
public class LazyLoadedSet<T> extends LazyLoader<Set<T>> implements Iterable<T>
{
    /**
     * Create a new lazy loaded value handler. It will store a Set of the parameterized type. The value
     * will not be fetched until needed, and won't be fetched again until it expires. This particular value
     * does not automatically expire, but it can be made to manually expire.
     *
     * @param loader A {@link Supplier} reference which loads the map to be stored.
     */
    public LazyLoadedSet(final Supplier<Set<T>> loader)
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
    public LazyLoadedSet(final Supplier<Set<T>> loader, final Duration ttl)
    {
        super(loader, ttl);
    }

    @Override
    protected Set<T> prepopulate()
    {
        return Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public final void applyData(final Set<T> items)
    {
        this.data.removeIf(i -> !items.contains(i));
        items.stream().filter(i -> !this.data.add(i)).forEach(this.data::add);
    }

    /**
     * Fetch the value. If the value has not been fetched or if the value has expired, a new copy will be
     * retrieved.
     *
     * @return A value of the declared type. The value may be {@code null} or any assignable subtype.
     */
    public final Set<T> values()
    {
        return this.value();
    }

    @Override
    public final Iterator<T> iterator()
    {
        return this.values().iterator();
    }

    /**
     * Fetch a stream of the data map.
     *
     * @return A {@link Stream} object.
     */
    public Stream<T> stream()
    {
        return this.values().stream();
    }
}
