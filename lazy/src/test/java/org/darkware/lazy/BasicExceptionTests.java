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

import org.assertj.core.util.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * @author jeff@darkware.org
 * @since 2017-01-18
 */
@RunWith(Parameterized.class)
public class BasicExceptionTests
{
    @Parameterized.Parameters
    public static Set<Class<? extends Exception>> getTargets()
    {
        return Sets.newLinkedHashSet(
                LazyLoadingException.class,
                LazyGenerationException.class
        );
    }

    private Class<? extends Exception> target;

    public BasicExceptionTests(final Class<? extends Exception> target)
    {
        super();

        this.target = target;
    }

    @Test
    public void constructor_message() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        String message = "Explanation";
        Constructor<? extends Exception> constructor = this.target.getConstructor(String.class);

        Exception e = constructor.newInstance(message);

        assertThat(e.getMessage()).isEqualTo(message);
    }

    @Test
    public void constructor_messageAndCause() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        String message = "Explanation";
        Throwable cause = new IllegalArgumentException("Stuff happened.");

        Constructor<? extends Exception> constructor = this.target.getConstructor(String.class, Throwable.class);
        Exception e = constructor.newInstance(message, cause);

        assertThat(e.getMessage()).isEqualTo(message);
        assertThat(e.getCause()).isEqualTo(cause);
    }

}
