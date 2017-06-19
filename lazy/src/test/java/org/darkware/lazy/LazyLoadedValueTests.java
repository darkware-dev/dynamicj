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

import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author jeff
 * @since 2016-06-05
 */
public class LazyLoadedValueTests
{
    private String loadedString;

    @Before
    public void reset()
    {
        this.loadedString = "Testing Canary";
    }

    @Test
    public void checkCreation()
    {
        LazyLoaded<String> a = new LazyLoaded<>(this::loadString);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());
    }

    @Test
    public void checkCreationWithTTL() throws InterruptedException
    {
        LazyLoaded<String> a = new LazyLoaded<>(this::loadString, Duration.ofMillis(300));

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());

        // Load the value and implicitly set expiration
        a.value();
        assertFalse(a.isExpired());

        // Trigger the expiration by pushing it into the past.
        a.updateExpiration(LocalDateTime.now().minus(5, ChronoUnit.SECONDS));

        // Check that the value is expired now
        assertTrue(a.isExpired());
    }

    @Test
    public void checkLoading()
    {
        LazyLoaded<String> a = new LazyLoaded<>(this::loadString);

        // Check that the loading hasn't happened automatically
        assertFalse(a.isLoaded());

        // Check that the loading does happen
        assertEquals(this.loadedString, a.value());
        assertTrue(a.isLoaded());
    }

    @Test
    public void checkLoading_default()
    {
        final String value = "VALUE";
        final AtomicInteger callCount = new AtomicInteger(0);

        LazyLoaded<String> a = new LazyLoaded<>(() -> {
            callCount.incrementAndGet();
            return value;
        });

        assertThat(callCount.get()).isEqualTo(0);
        a.load();
        assertThat(callCount.get()).isEqualTo(1);
        a.load();
    }

    @Test
    public void checkLoadingWithChangedBackend()
    {
        LazyLoaded<String> a = new LazyLoaded<>(this::loadString);

        // Check that the loading grabbed the initial value
        assertEquals(this.loadedString, a.value());
        assertTrue(a.isLoaded());

        // Change the backend
        this.loadedString = "TestB";

        // Check that this doesn't change the loaded value
        assertNotEquals(this.loadedString, a.value());

        // Expire the current value and check that the new value is loaded
        a.expire();
        assertEquals(this.loadedString, a.value());
    }

    @Test
    public void checkExpiration()
    {
        LazyLoaded<String> a = new LazyLoaded<>(this::loadString);

        // Load the value
        String originalValue = a.value();

        // Check expiration
        assertFalse(a.isExpired());
        a.expire();
        assertTrue(a.isExpired());

        // Change the backend value
        this.loadedString = "BackendChange";

        // Renew the value
        a.renew();

        // Check if its not expired any longer
        assertFalse(a.isExpired());

        // Check that the value hasn't been reloaded
        assertEquals(originalValue, a.value());
    }

    @Test
    public void checkUnloading()
    {
        final Integer trackedValue = new Integer(42);
        WeakReference<Integer> valueReference = new WeakReference<>(trackedValue);
        LazyLoaded<Integer> a = new LazyLoaded<>(() -> trackedValue);

        assertThat(valueReference.get()).isNotNull();
        assertThat(a.value()).isEqualTo(trackedValue);

        a.unload();

        assertThat(a.isLoaded()).isFalse();

        assertThat(a.data).isNotEqualTo(trackedValue);
    }

    @Test
    public void synchronization_value()
    {
        LazyLoaded<String> example = new LazyLoaded<>(() -> {
           throw new RuntimeException("Exception");
        });

        assertThatThrownBy(() -> { example.value(); }).isInstanceOf(LazyGenerationException.class);
    }

    @Test
    public void synchronization_expire()
    {
        LazyLoader<String> example = new LazyLoader<String>(() -> { return new String("Foo"); }, Duration.of(1, ChronoUnit.HOURS))
        {
            protected void onExpiration()
            {
                throw new RuntimeException("Sync Error Test");
            }
        };

        assertThatThrownBy(() -> { example.expire(); }).isInstanceOf(RuntimeException.class);
    }

    public String loadString()
    {
        return this.loadedString;
    }
}
