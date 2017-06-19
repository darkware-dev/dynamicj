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

/**
 * @author jeff@darkware.org
 * @since 2017-06-16
 */
public class DynamicModuleException extends RuntimeException
{
    /**
     * Create a new {@link DynamicModuleException}.
     *
     * @param message An explanation of the error which occurred.
     */
    public DynamicModuleException(final String message)
    {
        super(message);
    }

    /**
     * Create a new {@link DynamicModuleException}.
     *
     * @param message An explanation of the situation where the error was encountered.
     * @param cause The error which was encountered.
     */
    public DynamicModuleException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
