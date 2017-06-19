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

import org.darkware.jvmtools.JarBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author jeff@darkware.org
 * @since 2017-06-18
 */
public class ExampleBuilder
{
    public static void main(final String ... args)
    {
        Scanner input = new Scanner(System.in);
        System.out.printf("Enter the path to write the jar file to: ");
        String path = input.nextLine();
        Path output = Paths.get(path);

        JarBuilder builder = new JarBuilder();

        builder.addClass(Example.class);

        try
        {
            builder.write(Files.newOutputStream(output));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
