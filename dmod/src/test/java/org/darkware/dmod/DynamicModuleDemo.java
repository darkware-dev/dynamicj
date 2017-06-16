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

package org.darkware.dmod;

import org.darkware.jvmtools.ShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

/**
 * @author jeff@darkware.org
 * @since 2017-05-31
 */
public class DynamicModuleDemo
{
    public static void main(final String ... args)
    {
        final Logger log = LoggerFactory.getLogger("Demo");

        try
        {
            // Create a temp directory
            Path tempDir = Files.createTempDirectory("DModDemo");
            ShutdownHandler.getCurrent().doom(tempDir);
            log.info("Looking for module jar in: {}", tempDir);

            Path moduleJar = null;
            // Scan until you find a module
            while(moduleJar == null)
            {
                try (DirectoryStream<Path> dirContents = Files.newDirectoryStream(tempDir))
                {
                    Optional<Path> potentialJar = StreamSupport.stream(dirContents.spliterator(), false)
                                                               .filter(Files::isRegularFile)
                                                               .filter(file -> file.getFileName().toString().endsWith(".jar"))
                                                               .findFirst();

                    // If we found one, note it and short circuit to let the condition pull us
                    // out of the loop. A break would work too, but... conditional exit just
                    // feels less dirty.
                    if (potentialJar.isPresent())
                    {
                        moduleJar = potentialJar.get();
                        continue;
                    }

                    Thread.sleep(1000);
                }
                catch (InterruptedException ie)
                {
                    log.warn("Directory backoff interrupted for some reason.");
                }
                catch (IOException dioe)
                {
                    log.warn("Failed to read target directory.", dioe);
                }
            }

            // Announce the selected file
            log.info("Creating a DynamicModule for file: {}", moduleJar);
            DynamicModule mod = new DynamicModule(moduleJar);

            // Set up a handy alias
            mod.createAlias("org.darkware.dmod.example.Example", "Example");

            final AtomicBoolean run = new AtomicBoolean(true);
            while (run.get())
            {
                Calculator instance = mod.getInstance(Calculator.class, "Example");

                log.info("Module calculation: 42={}, 1337={} ({})",
                         instance.calculate(42), instance.calculate(1337),
                         instance.getVersion());

                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    log.warn("Something interrupted the calculation delay.");
                }
            }
        }
        catch (IOException ioe) {
            log.error("Failed to create temporary directory.", ioe);
        }
    }
}
