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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")
            .withZone(ZoneId.systemDefault());

    private final Logger log = LoggerFactory.getLogger("DynamicModule");
    private final Path moduleJarFile;
    private final Map<String, Object> instances;
    private final Map<String, String> aliases;
    private final Lock mutationLock;

    private DynamicClassLoader classLoader;

    private FileTime jarFileModified;
    private long jarFileSize;
    private String version;

    /**
     * Create a new {@link DynamicModule} backed by the supplied
     * @param moduleJarFile
     */
    public DynamicModule(final Path moduleJarFile)
    {
        super();

        this.moduleJarFile = moduleJarFile;
        this.mutationLock = new ReentrantLock();
        this.instances = Maps.newConcurrentMap();
        this.aliases = Maps.newConcurrentMap();

        this.load();
    }

    /**
     * Load metadata about the backing Jar file.  This is used to detect differences in the file which
     * may warrant reloading the file.
     */
    protected final void loadJarFileMeta()
    {
        try
        {
            this.jarFileModified = Files.getLastModifiedTime(this.moduleJarFile);
            this.jarFileSize = Files.size(this.moduleJarFile);
            this.version = String.format("v%s_%08d",
                                         DynamicModule.dateFormatter.format(this.jarFileModified.toInstant()),
                                         this.jarFileSize);
        }
        catch (IOException e)
        {
            this.jarFileModified = null;
            this.jarFileSize = -1;
        }
    }

    /**
     * Fetch a symbolic version identifier for the currently loaded Jar file data.
     * <p>
     * While this can be used for comparison to detect file changes, the actual checks are performed on
     * the raw sources of data used to generate the version name. The primary purpose of this field is to
     * provide a version identifier that is easily sortable and human-digestible.
     *
     * @return A symbolic version for the currently loaded module data.
     */
    public final String getVersion()
    {
        return this.version;
    }

    /**
     * Check to see if the backing Jar file appears to have changed.
     *
     * @return {@code true} if the file appears to have changed, otherwise {@code false}.
     */
    public boolean fileChanged()
    {
        this.mutationLock.lock();
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
        finally
        {
            this.mutationLock.unlock();
        }

        return false;
    }

    /**
     * Load the Jar file and clear the cache of Class definitions and instances. Any defined
     * aliases will be retained.
     */
    protected void load()
    {
        try
        {
            this.mutationLock.lock();

            this.log.debug("Reloading DynamicModule: {}", this.moduleJarFile);
            this.instances.clear();
            this.classLoader = new DynamicClassLoader(this.moduleJarFile);
            this.loadJarFileMeta();
            this.log.debug("Loaded module version: {}.", this.getVersion());
        }
        finally
        {
            this.mutationLock.unlock();
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
        this.mutationLock.lock();
        try
        {
            if (this.fileChanged())
            {
                this.load();
            }
            return this.classLoader.loadClass(this.resolveAlias(classNameOrAlias).orElse(classNameOrAlias));
        }
        finally
        {
            this.mutationLock.unlock();
        }
    }

    /**
     * Check to see if the given class is available through this {@link DynamicModule}. This will check for
     * class availability without raising a {@link ClassCastException}.
     * <p>
     * <em>Note:</em> This only checks for availability strictly within the classes sourced from the
     * {@link DynamicModule}. The module's integrated {@link ClassLoader} is capable of resolving classes
     * outside the attached Jar, but this method won't check for any of those classes. This is designed
     * specifically to check if a given class is supported by the module, not the bootstrap or system
     * {@link ClassLoader}s.
     * <p>
     * <em>Another Note:</em> The result of this method should not be taken as a contract to guarantee that
     * a given class can be instantiated from the module. Instantiation requires extra checks which are not taken
     * into account here.
     *
     * @param classNameOrAlias A class name or alias to a class name.
     * @return {@code true} if the given class can be loaded from the {@link DynamicModule}, {@code false} if the
     * class is not recognized by the module, or if the first available definition comes from an ancestor
     * {@link ClassLoader}.
     */
    public boolean hasClass(final String classNameOrAlias) {
        this.mutationLock.lock();
        try
        {
            if (this.fileChanged())
            {
                this.load();
            }
            return this.classLoader.hasClass(this.resolveAlias(classNameOrAlias).orElse(classNameOrAlias));
        }
        finally
        {
            this.mutationLock.unlock();
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
            Constructor con = this.getModuleClass(className).getConstructor(new Class<?>[] {});
            return this.createInstance(con);
        }
        catch (ClassNotFoundException e)
        {
            this.log.error("Module class not found.", e);
            throw new DynamicModuleException("Module instance class could not be found.", e);
        }
        catch (NoSuchMethodException e)
        {
            this.log.error("Module class does not support default construction.", e);
            throw new DynamicModuleException("Module class does not support default construction.", e);
        }
    }

    /**
     * Create a new instance of the declared class with the supplied arguments.
     *
     * @param constructor The {@link Constructor} to use when creating the object.
     * @param args The arguments to use when creating the instance.
     * @return The created instance.
     */
    protected Object createInstance(final Constructor constructor, Object ... args)
    {
        try
        {
            return constructor.newInstance(args);
        }
        catch (InstantiationException e)
        {
            this.log.error("Failed to create module instance.", e);
            throw new DynamicModuleException("Failed to create module class instance.", e);
        }
        catch (IllegalAccessException e)
        {
            this.log.error("Unable to create module instance.", e);
            throw new DynamicModuleException("Not allowed to create module class instance.", e);
        }
        catch (InvocationTargetException e)
        {
            this.log.error("Error while creating module instance.", e);
            throw new DynamicModuleException("Error while creating module instance.", e);
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
    public <T> T getInstance(final Class<T> instanceClass, final String classNameOrAlias)
    {
        try
        {
            if (this.fileChanged())
            {
                this.load();
            }

            this.mutationLock.lock();
            final String className = this.resolveAlias(classNameOrAlias).orElse(classNameOrAlias);
            this.instances.computeIfAbsent(className, this::createInstance);

            Object instance = this.instances.get(className);
            if (!instanceClass.isInstance(instance))
            {
                throw new ClassCastException("Cannot cast instance of " + instance.getClass() + " to type " + instanceClass.getName());
            }
            return (T) instance;
        }
        finally
        {
            this.mutationLock.unlock();
        }
    }

    //TODO Class searching
}
