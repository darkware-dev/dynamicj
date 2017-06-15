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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link DynamicModule} represents an interface to one or more classes that are loaded from a
 * Jar file which may be changed at any moment. The {@link DynamicModule} ensures that each time
 * an object instance is fetched via its API, the returned object (or lack thereof) matches the
 * {@link Class} instance defined in the attached file.
 *
 * @author jeff@darkware.org
 * @since 2017-05-31
 */
public class DynamicModule
{
    private final Logger log = LoggerFactory.getLogger("DynamicModule");
    private final Path moduleJarFile;
    private final Map<String, Object> instances;
    private final Map<String, String> aliases;

    private DynamicClassLoader classLoader;

    private FileTime jarFileModified;
    private long jarFileSize;

    /**
     * Create a new {@link DynamicModule} backed by the supplied
     * @param moduleJarFile
     */
    public DynamicModule(final Path moduleJarFile)
    {
        super();

        this.moduleJarFile = moduleJarFile;
        this.instances = Maps.newConcurrentMap();
        this.aliases = Maps.newConcurrentMap();

        this.loadJarFileMeta();

        this.classLoader = new DynamicClassLoader(moduleJarFile);
    }

    /**
     * Load metadata about the backing Jar file.  This is used to detect differences in the file which
     * may warrant reloading the file.
     */
    protected void loadJarFileMeta()
    {
        try
        {
            this.jarFileModified = Files.getLastModifiedTime(this.moduleJarFile);
            this.jarFileSize = Files.size(this.moduleJarFile);
        }
        catch (IOException e)
        {
            this.jarFileModified = null;
            this.jarFileSize = -1;
        }
    }

    /**
     * Check to see if the backing Jar file appears to have changed.
     *
     * @return {@code true} if the file appears to have changed, otherwise {@code false}.
     */
    public boolean fileChanged()
    {
        synchronized (this.moduleJarFile)
        {
            try
            {
                if (Files.exists(this.moduleJarFile))
                {
                    if (this.jarFileSize != Files.size(this.moduleJarFile)
                        || !this.jarFileModified.equals(Files.getLastModifiedTime(this.moduleJarFile)))
                    {
                        this.log.debug("Jar file change detected.");
                        return true;
                    }
                }
                else
                {
                    this.log.debug("File no longer exists.");
                    return true;
                }
            }
            catch (IOException e)
            {
                this.log.warn("Error while checking plugin jar file for changes.", e);
            }
        }

        return false;
    }

    /**
     * Reload the Jar file and clear the cache of Class definitions and instances. Any defined
     * aliases will be retained.
     */
    protected void reload()
    {
        synchronized (this.moduleJarFile)
        {
            this.log.debug("Reloading DynamicModule @ {}.", this.moduleJarFile);
            this.instances.clear();
            this.classLoader = new DynamicClassLoader(this.moduleJarFile);
            this.loadJarFileMeta();
        }
    }

    /**
     * Fetch the {@link Class} for the given name, according to the attached Jar file.
     * <p>
     * <em>Important Note:</em> This will resolve {@link Class} instances declared by the module's
     * parent {@link ClassLoader}, including the System and Bootstrap ClassLoaders. That is, there is
     * no guarantee that the returned class will be one defined in the attached Jar file, but every
     * attempt will be made to use the classes from the Jar file before consulting other ClassLoaders.
     *
     * @param classNameOrAlias The class name or a previously declared alias for a class.
     * @return A {@link Class} instance matching the supplied name or alias.
     * @throws ClassNotFoundException If no matching class could be found.
     */
    public Class<?> getModuleClass(final String classNameOrAlias) throws ClassNotFoundException
    {
        synchronized (this.moduleJarFile)
        {
            if (this.fileChanged())
            {
                this.reload();
            }
            return this.classLoader.loadClass(classNameOrAlias);
        }
    }

    /**
     * Create a new alias for the given class name. After creation, the alias can be used instead
     * of the full class name in calls to {@link #getInstance(Class, String)} and {@link #getModuleClass(String)}.
     *
     * @param className The fully qualified {@link Class} name.
     * @param alias A convenient alias for the class name.
     */
    public void createAlias(final String className, final String alias)
    {
        this.aliases.put(alias, className);
    }

    /**
     * Attempt to resolve the given string against the list of registered aliases.
     *
     * @param alias An alias to resolve.
     * @return An {@link Optional} containing a resolved alias, if one was registered.
     */
    public Optional<String> resolveAlias(final String alias)
    {
        return Optional.ofNullable(this.aliases.get(alias));
    }

    /**
     * Create a new instance of the declared class using the default constructor.
     *
     * @param className The name of the class to get an instance for.
     * @return The created instance or {@code null} if the class could not be created.
     */
    protected Object createInstance(final String className)
    {
        try
        {
            this.log.info("Creating new dynamic instance for {}", className);
            return this.getModuleClass(className).newInstance();
        }
        catch (InstantiationException e)
        {
            this.log.error("Failed to create module instance.", e);
            return null;
        }
        catch (IllegalAccessException e)
        {
            this.log.error("Unable to create module instance.", e);
            return null;
        }
        catch (ClassNotFoundException e)
        {
            this.log.error("Module class not found.", e);
            return null;
        }
    }

    /**
     * Fetch an instance with the given class name or alias as an object of the supplied type.
     *
     * @param instanceClass The type of object to try to return. If the retrieved object cannot be
     * cast to this type, the object won't be returned.
     * @param classNameOrAlias The class name or alias to a class name of the module entry to create.
     * @param <T> A type of object to return.
     * @return An {@link Optional} containing the instance of the requested class, or nothing if the
     * class was not found or could not be created.
     */
    public <T> Optional<T> getInstance(final Class<T> instanceClass, final String classNameOrAlias)
    {
        synchronized (this.moduleJarFile)
        {
            if (this.fileChanged())
            {
                this.reload();
            }

            final String className = this.resolveAlias(classNameOrAlias).orElse(classNameOrAlias);
            this.instances.computeIfAbsent(className, this::createInstance);

            try
            {
                return Optional.ofNullable((T) this.instances.get(className));
            }
            catch (ClassCastException cce)
            {
                this.log.error("Requested incompatible type ({}) for retrieval. Actual type is {}",
                               instanceClass.getSimpleName(), this.instances.get(className).getClass().getSimpleName());
                return Optional.empty();
            }
        }
    }
}
