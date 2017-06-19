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

import java.io.Console;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

/**
 * @author jeff@darkware.org
 * @since 2017-05-31
 */
public class DynamicModuleDemo
{
    private static final Logger log = LoggerFactory.getLogger("Demo");

    public static void main(final String ... args)
    {
        Scanner input = new Scanner(System.in);
        System.out.printf("Enter a directory to scan for Jar files: ");
        final String userpath = input.nextLine();

        final Path scanDir = DynamicModuleDemo.resolvePath(userpath);
        final Path moduleJar = DynamicModuleDemo.scanForJarFile(scanDir);

        // Announce the selected file
        log.info("Creating a DynamicModule for file: {}", moduleJar);
        DynamicModule mod = new DynamicModule(moduleJar);

        // Set up a handy alias
        mod.createAlias("org.darkware.dmod.example.Example", "Example");

        final AtomicBoolean run = new AtomicBoolean(true);
        while (run.get())
        {
            Calculator instance = mod.getInstance(Calculator.class, "Example");

            log.info("Module calculation [{}] :: 42,16 = {} ({})",
                     instance.getVersion(),
                     instance.calculate(42, 16),
                     instance.getDescription());

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

    private static Path resolvePath(final String userpath)
    {
        if (userpath.isEmpty())
        {
            try
            {
                Path tempPath = Files.createTempDirectory("DModDemo");
                ShutdownHandler.getCurrent().doom(tempPath);
                return tempPath;
            }
            catch (IOException e)
            {
                LoggerFactory.getLogger("Demo").error("Failed to create temporary file.", e);
                throw new RuntimeException("Failed to create temporary file.");
            }
        }
        else
        {
            Path realPath = Paths.get(userpath);

            if (!Files.exists(realPath))
            {
                LoggerFactory.getLogger("Demo").error("User-supplied path does not exist.");
                throw new RuntimeException("User-supplied path does not exist.");
            }
            else if (!Files.isDirectory(realPath))
            {
                LoggerFactory.getLogger("Demo").error("User-supplied path is not a directory.");
                throw new RuntimeException("User-supplied path is not a directory.");
            }
            else
            {
                return realPath;
            }
        }
    }

    private static Path scanForJarFile(final Path scanDir)
    {
        DynamicModuleDemo.log.info("Looking for module jar in: {}", scanDir);

        Path moduleJar = null;
        // Scan until you find a module
        while(moduleJar == null)
        {
            try (DirectoryStream<Path> dirContents = Files.newDirectoryStream(scanDir))
            {
                Optional<Path> potentialJar = StreamSupport.stream(dirContents.spliterator(), false)
                                                           .filter(Files::isRegularFile)
                                                           .filter(file -> file.getFileName().toString().endsWith(".jar"))
                                                           .findFirst();

                if (potentialJar.isPresent())
                {
                    return potentialJar.get();
                }

                Thread.sleep(1000);
            }
            catch (InterruptedException ie)
            {
                DynamicModuleDemo.log.warn("Directory backoff interrupted for some reason.");
            }
            catch (IOException dioe)
            {
                DynamicModuleDemo.log.warn("Failed to read target directory.", dioe);
            }
        }

        throw new IllegalStateException("Failed to find a module Jar file.");
    }
}
