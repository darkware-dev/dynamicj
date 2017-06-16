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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link DynamicClassLoader} is a specialized child ClassLoader which resolves classes from a
 * single Jar file and provides extra information to an attached {@link DynamicModule} instance.
 * This {@link ClassLoader} implements a child-first resolution model that enhances the separation
 * of class definition contexts between the rest of the JVM and classes loaded via this ClassLoader.
 *
 * @author jeff@darkware.org
 * @since 2017-05-31
 */
class DynamicClassLoader extends ClassLoader
{
    /**
     * Convert a resource path into the canonical class name expected for that resource path.
     *
     * @param jarPath The path to convert.
     * @return The expected class name, as a {@link String}.
     */
    public static String classNameForJarPath(final String jarPath)
    {
        int endIndex = (jarPath.endsWith(".class")) ? jarPath.length() - 6 : jarPath.length();
        return jarPath.substring(0, endIndex).replace('/', '.');
    }

    private final Logger log = LoggerFactory.getLogger("DynamicClassLoader");
    private final Path jarFile;
    private final Map<String, JarEntry> jarIndex;
    private final ClassLoader parent;

    /**
     * Create a new {@link DynamicClassLoader} which loads classes from a given Jar file.
     *
     * @param jarFile A {@link Path} to a Jar file.
     */
    public DynamicClassLoader(final Path jarFile)
    {
        super(Thread.currentThread().getContextClassLoader());

        this.parent = Thread.currentThread().getContextClassLoader();
        this.jarFile = jarFile;
        this.jarIndex = Maps.newConcurrentMap();

        this.rebuildIndex();
    }

    @Override
    protected void finalize() throws Throwable
    {
        this.jarIndex.clear();
    }

    /**
     * Rebuild the index of classes available to load. This should be done after a Jar file change is
     * detected and before the next attempt to resolve classes from the Jar file.
     */
    protected void rebuildIndex() {
        synchronized (this.jarIndex) {
            this.log.debug("Rebuilding class index.");
            try (JarFile jar = new JarFile(this.jarFile.toFile()))
            {
                this.jarIndex.clear();

                jar.stream()
                   //.peek(je -> this.log.info("Plugin:{} :: Class => {}", jarFile.getFileName(), PluginClassLoader.classNameForJarPath(je.getName())))
                   .filter(je -> je.getName().endsWith(".class"))
                   .forEach(je -> this.jarIndex.put(DynamicClassLoader.classNameForJarPath(je.getName()), je));
            }
            catch (IOException e)
            {
                this.log.error("Error while indexing plugin jar file.", e);
            }
        }
    }

    /**
     * Check to see if the given class is included in the currently loaded Jar file.
     *
     * @param className The fully qualified class name.
     * @return {@code true} if the class can be loaded from the current Jar file version, {@code false} if
     * it was not found in the Jar file.
     */
    public boolean hasClass(final String className)
    {
        return this.jarIndex.containsKey(className);
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException
    {
        this.log.info("Attempting to load class: {}", name);
        if (this.jarIndex.containsKey(name))
        {
            try
            {
                JarEntry classEntry = this.jarIndex.get(name);

                this.log.info("Reading definition of class {} from jar file.", name);

                return this.readEntry(classEntry);
            }
            catch (IOException e)
            {
                this.log.error("Error while loading class from plugin jar file.", e);
            }
        }

        this.log.info("Delegating class loading of {} to the parent ClassLoader", name);
        return this.parent.loadClass(name);
    }

    /**
     * Read a class entry from the Jar file.
     *
     * @param entry The {@link JarEntry} referencing the class data to load.
     * @return The loaded and defined {@link Class} based on the data referenced by the entry.
     * @throws IOException If there are problems reading the Jar file.
     */
    protected Class<?> readEntry(final JarEntry entry) throws IOException
    {
        final byte[] readbuffer = new byte[4096];

        String className = DynamicClassLoader.classNameForJarPath(entry.getName());
        final JarFile jar = new JarFile(this.jarFile.toFile());
        try (InputStream jarData = jar.getInputStream(entry))
        {
            // Read the entry data
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            int bytesRead = 0;
            while (bytesRead >= 0)
            {
                bytesRead = jarData.read(readbuffer);
                if (bytesRead > 0)
                {
                    data.write(readbuffer, 0, bytesRead);
                }
            }

            data.flush();

            final byte[] classData = data.toByteArray();

            return this.defineClass(className, classData, 0, classData.length);
        }
        finally
        {
            jar.close();
        }
    }
}
