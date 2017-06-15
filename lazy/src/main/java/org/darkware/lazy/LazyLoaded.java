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
import java.util.function.Supplier;

/**
 * This is a simple wrapper object which acts as deferral mechanism for loading an object value. Upon
 * creation, only the wrapper is allocated. No underlying object is loaded. Only once the value is requested
 * will it be loaded. Once loaded, the value will be retained until it expires, at which point a new value
 * will be fetched. The method of fetching or deriving the value is left for child classes or instances to
 * implement.
 * <p>
 * There are two slightly different modes this wrapper can operate. Ultimately, both abide by the
 * same rules.
 *
 * @param <T> The type of object stored in the field.
 *
 * @author jeff
 * @since 2016-05-15
 */
public class LazyLoaded<T> extends LazyLoader<T>
{
    /**
     * Create a new lazy loaded value handler. It will store an object of the parameterized type. The value
     * will not be fetched until needed, and won't be fetched again until it expires. This particular value
     * does not automatically expire, but it can be made to manually expire.
     *
     * @param loader A {@link Supplier} which returns the value the field should store at any time.
     */
    public LazyLoaded(final Supplier<T> loader)
    {
        this(loader, null);
    }

    /**
     * Create a new lazy loaded value handler. It will store an object of the parameterized type. The value
     * will not be fetched until needed, and won't be fetched again until it expires.
     *
     * @param loader A {@link Supplier} which returns the value the field should store at any time.
     * @param ttl The amount of time the value should be allowed to be stored before the value automatically
     * expires.
     */
    public LazyLoaded(final Supplier<T> loader, final Duration ttl)
    {
        super(loader, ttl);
    }
}
