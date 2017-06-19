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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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
        this(jarFile, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Create a new {@link DynamicClassLoader} which loads classes from a given Jar file.
     *
     * @param jarFile A {@link Path} to a Jar file.
     * @param classLoader The parent {@link ClassLoader} to use for resolving classes outside
     * the provided Jar file.
     */
    public DynamicClassLoader(final Path jarFile, final ClassLoader classLoader)
    {
        super(Thread.currentThread().getContextClassLoader());

        this.parent = classLoader;
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

    /**
     * Find all the classes in the Jar file which extend or implement the supplied {@link Class}.
     *
     * @param parentClass The parent {@link Class} to scan for.
     * @return A {@link Set} of {@link Class}es wh ich extend or implement the parent class.
     */
    public Set<Class<?>> findSubClasses(final Class<?> parentClass) {
        return this.jarIndex.values().stream()
                            .map(e -> this.getConcreteClass(e.getName()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(c -> parentClass.isAssignableFrom(c))
                            .collect(Collectors.toSet());
    }

    /**
     * Fetch a concrete {@link Class} object for the given class name. This is primarily useful in converting
     * the possibility of a {@link ClassNotFoundException} into an empty {@link Optional}. This will delegate
     * to the parent {@link ClassLoader} where needed.
     *
     * @param className The name of the class to fetch.
     * @return An {@link Optional} containing the {@link Class}, if one can be fetched.
     */
    protected Optional<Class<?>> getConcreteClass(final String className) {
        try
        {
            return Optional.ofNullable(this.findClass(className));
        }
        catch (ClassNotFoundException e)
        {
            return Optional.empty();
        }
    }

    /**
     * Load {@link Class} object from the attached Jar file which is a unique subclass of the
     * given class.
     * <p>
     * <em>Performance Note:</em> In order to check for class ancestry, each class must be loaded. This
     * will be slow
     *
     * @param parentClass The parent class to compare with when scanning Jar contents.
     * @return A {@link Class} object which extends or implements the supplied {@link Class}.
     * @throws ClassNotFoundException If no matching classes were found in the Jar file.
     * @throws IllegalArgumentException If multiple matching classes were found.
     */
    public Class<?> loadClass(final Class<?> parentClass) throws ClassNotFoundException
    {
        Set<Class<?>> matches = this.findSubClasses(parentClass);

        if (matches.size() < 1)
        {
            throw new ClassNotFoundException("Found no classes extending " + parentClass.getName());
        }
        else if (matches.size() > 1)
        {
            throw new IllegalArgumentException("Multiple class matches extending " + parentClass.getName());
        }
        else
        {
            return matches.iterator().next();
        }
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
