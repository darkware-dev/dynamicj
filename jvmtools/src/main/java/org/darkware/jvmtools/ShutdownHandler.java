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

package org.darkware.jvmtools;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jeff@darkware.org
 * @since 2017-06-09
 */
public class ShutdownHandler extends Thread
{
    private static final Logger log = LoggerFactory.getLogger("ShutdownHandler");
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static ShutdownHandler globalHandler;

    /**
     * Fetch the global instance of the {@link ShutdownHandler}.
     *
     * @return
     */
    public static ShutdownHandler getCurrent()
    {
        synchronized (ShutdownHandler.initialized)
        {
            if (ShutdownHandler.initialized.compareAndSet(false, true))
            {
                ShutdownHandler.globalHandler = new ShutdownHandler();
            }
        }

        return ShutdownHandler.globalHandler;
    }

    private final Runtime runtime;

    private final Set<Path> doomedPaths;

    public ShutdownHandler()
    {
        super();

        this.runtime = Runtime.getRuntime();

        this.doomedPaths = Sets.newConcurrentHashSet();

        this.runtime.addShutdownHook(this);
    }

    /**
     * Doom a file to be deleted when the JVM shuts down.
     *
     * @param file A {@link Path} representing the file to delete.
     */
    public void doom(final Path file)
    {
        this.doomedPaths.add(file);
    }

    @Override
    public void run()
    {
        // Delete all doomed files
        this.doomedPaths.stream()
                        .filter(Files::exists)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try
                            {
                                Files.deleteIfExists(path);
                            }
                            catch (IOException e)
                            {
                                ShutdownHandler.log.warn("Error while deleting doomed file: {}", path, e);
                            }
                        });

        // Delete all doomed directories
        this.doomedPaths.stream()
                        .filter(Files::exists)
                        .filter(Files::isDirectory)
                        .forEach(dir -> {
                            try
                            {
                                Files.walk(dir)
                                     .sorted(Comparator.reverseOrder())
                                     .map(Path::toFile)
                                     .forEach(File::delete);
                            }
                            catch (IOException e)
                            {
                                ShutdownHandler.log.warn("Error while deleting doomed directory: {}", dir, e);
                            }
                        });

    }
}
