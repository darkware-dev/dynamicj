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

/**
 * A {@link LazyGenerationException} is an exception tossed while generating the lazily loaded value for a
 * {@link LazyLoader}.
 *
 * @author jeff@darkware.org
 * @since 2017-01-18
 */
public class LazyGenerationException extends RuntimeException
{
    /**
     * Create a new {@link LazyGenerationException} with a given message. This represents an error that occurs internal
     * to the {@link LazyLoader} logic, without an associated upstream error.
     *
     * @param message A message describing the problem.
     */
    public LazyGenerationException(final String message)
    {
        super(message);
    }

    /**
     * Create a new {@link LazyGenerationException} with the given message and upstream cause.
     *
     * @param message A message describing the problem.
     * @param cause The upstream cause for the problem.
     */
    public LazyGenerationException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
