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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This is a base implementation of a lazy loading helper class. This class is mostly useless by itself,
 * but it simplifies and normalizes the creation of child classes for lazy loading particular types of
 * data.
 *
 * @param <T> The type of object which will be loaded.
 *
 * @author jeff
 * @since 2016-05-16
 */
public abstract class LazyLoader<T>
{
    /** The internal data storage for the item */
    protected T data;
    private Supplier<T> loader;
    private final Duration ttl;
    private LocalDateTime expiration;

    /**
     * Create a new {@link LazyLoader}. The loader will be called (possibly repeatedly) to populate the value
     * of this field.
     * <p>
     * There is no deterministic timeline for when the supplier lambda will be called. The generator code should be
     * written and structured so that it can be invoked at any time during the lifetime of the loader.
     *
     * @param loader A {@link Supplier} to invoke to generate a value.
     * @param ttl The amount of time the value should be held before it is regenerated.
     */
    public LazyLoader(final Supplier<T> loader, final Duration ttl)
    {
        super();

        this.ttl = ttl;
        this.expiration = null;
        this.loader = loader;

        this.data = this.prepopulate();
    }

    /**
     * Prepopulate the data upon creation and data release. This is often used to initialized stored objects that will
     * have specialized {@link #applyData(Object)} methods.
     *
     * @return The data to pre-populate.
     */
    protected T prepopulate()
    {
        return null;
    }

    /**
     * Unload the currently stored data and mark it as expired. This will force it to be re-fetched the next time the
     * value is accessed. This is similar to {@link #expire()}, but actually removes the reference to stored data,
     * possibly allowing it to be garbage-collected. <em>Note:</em> Using this instead of {@link #expire()} can decrease
     * the efficiency composed items like {@link LazyLoadedSet}s and {@link LazyLoadedMap}s
     */
    public final void unload()
    {
        synchronized (this)
        {
            this.data = prepopulate();
            this.expire();
        }
    }

    /**
     * Load the data for this item, if needed. This will only load data if the data is not currently loaded
     * or if it has expired.
     */
    public final void load()
    {
        this.load(false);
    }

    /**
     * Load the data for this item.
     *
     * @param forceful Force the data to reload, ignoring and resetting the expiration time, if used.
     */
    public final void load(final boolean forceful)
    {
        synchronized (this)
        {
            if (forceful || this.isExpired())
            {
                try
                {
                    this.applyData(loader.get());
                    this.renew();
                }
                catch (Exception e)
                {
                    this.reportLoadError(e);
                    throw new LazyGenerationException("There was an error loading the value.", e);
                }
            }
        }
    }

    /**
     * Set the internal data to match the data loaded. In very simple implementations, this just sets the data to the
     * object supplied. Other implementations, particularly those with {@link Set}s or {@link Map}s may choose to do
     * more complex merge operations to maintain smoother concurrency behavior.
     *
     * @param newData The data to store in this object.
     */
    protected void applyData(final T newData)
    {
        this.data = newData;
    }

    /**
     * Fetch the value. If the value has not been fetched or if the value has expired, a new copy will be
     * retrieved.
     *
     * @return A value of the declared type. The value may be {@code null} or any assignable subtype.
     */
    public final T value()
    {
        synchronized (this)
        {
            this.load(false);
            return this.data;
        }
    }

    /**
     * Force the expiration of the value. Following this call, the next call to retrieve the data will
     * trigger a fresh fetch of the data.
     */
    public final void expire()
    {
        synchronized (this)
        {
            this.expiration = null;
            this.onExpiration();
        }
    }

    /**
     * Force an update to the expiration time. This is primarily useful for manufacturing testing scenarios.
     * It shouldn't be used for normal behavior unless you really want to be responsible for the ramifications.
     *
     * @param newExpiration The {@link LocalDateTime} of the new expiration time.
     */
    protected final void updateExpiration(final LocalDateTime newExpiration)
    {
        if (this.expiration != null)
        {
            this.expiration = newExpiration;
        }
    }

    /**
     * Check if the data has been loaded for this item.
     * <p>
     * <em>Note:</em> Data that is loaded may still be expired. This method simply checks to see if some data has been
     * loaded.
     *
     * @return {@code true} if the data is loaded, {@code false} if it is not.
     */
    public final boolean isLoaded()
    {
        return this.expiration != null;
    }

    /**
     * Checks if the lazy loaded data is expired or not. Data that has never been loaded is considered to
     * be expired.
     *
     * @return {@code true} if the data is expired or never loaded, otherwise {@code false}.
     */
    public final boolean isExpired()
    {
        return this.expiration == null || this.expiration.isBefore(LocalDateTime.now());
    }

    /**
     * Extend the expiration another generation.
     */
    protected void renew()
    {
        if (ttl == null)
            this.expiration = LocalDateTime.MAX;
        else
            this.expiration = LocalDateTime.now().plus(this.ttl);
    }

    /**
     * Report any errors encountered while trying to fetch the backend value.
     *
     * @param t The {@link Throwable} which was caught during value loading.
     */
    protected void reportLoadError(final Throwable t)
    {
        // Do nothing by default.
    }

    /**
     * This method is called whenever a {@link LazyLoader}
     */
    protected void onExpiration()
    {
        // Do nothing by default
    }
}
