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

package org.darkware.dmod;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author jeff@darkware.org
 * @since 2017-06-15
 */
public class DynamicClassLoaderTests
{
    @Test
    public void classNameForJarPath_simple()
    {
        assertThat(DynamicClassLoader.classNameForJarPath("org/darkware/foo/Sample.class")).isEqualTo("org.darkware.foo.Sample");
    }

    @Test
    public void classNameForJarPath_noPackage()
    {
        assertThat(DynamicClassLoader.classNameForJarPath("Sample.class")).isEqualTo("Sample");
    }

    @Test
    public void isAssignableFrom()
    {
        Class<?> parentClass = CharSequence.class;
        Class<?> childClass = String.class;

        assertThat(parentClass.isAssignableFrom(childClass)).isTrue();
    }
}
