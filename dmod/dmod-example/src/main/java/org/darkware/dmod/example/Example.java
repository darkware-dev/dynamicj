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

package org.darkware.dmod.example;

import org.darkware.dmod.Calculator;

/**
 * @author jeff@darkware.org
 * @since 2017-06-13
 */
public class Example implements Calculator
{
    @Override
    public String getVersion()
    {
        return "v0.3";
    }

    @Override
    public String getDescription()
    {
        return "((base + modify) x 3333) % 99";
    }

    @Override
    public int calculate(final int base, final int modify)
    {
        return ((base + modify) * 3333) % 99;
    }
}
