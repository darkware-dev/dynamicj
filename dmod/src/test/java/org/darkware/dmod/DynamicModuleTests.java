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

import org.darkware.dmod.sample.SampleAntiSocialClass;
import org.darkware.dmod.sample.SampleCallable;
import org.darkware.dmod.sample.SampleInstantiationKiller;
import org.darkware.dmod.sample.SampleSet;
import org.darkware.dmod.sample.SampleThread;
import org.darkware.dmod.sample.SampleUncheckedInstantiationKiller;
import org.darkware.jvmtools.JarBuilder;
import org.darkware.jvmtools.ShutdownHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author jeff@darkware.org
 * @since 2017-06-15
 */
public class DynamicModuleTests
{
    private static Logger log = LoggerFactory.getLogger(DynamicModuleTests.class.getSimpleName());
    private static Path tempJar;

    @BeforeClass
    public static void initialize() throws IOException
    {
        DynamicModuleTests.tempJar = Files.createTempFile("DynamicModuleTest-", ".jar");
        DynamicModuleTests.log.debug("Using temp Jar file: {}", DynamicModuleTests.tempJar);
        ShutdownHandler.getCurrent().doom(tempJar);


    }

    private DynamicModule module;

    @Before
    public void setup() throws IOException
    {
        final JarBuilder builder = new JarBuilder();
        builder.addClass(SampleCallable.class);
        builder.addClass(SampleThread.class);
        builder.addClass(SampleInstantiationKiller.class);
        builder.addClass(SampleUncheckedInstantiationKiller.class);
        builder.addClass(SampleAntiSocialClass.class);

        builder.write(Files.newOutputStream(DynamicModuleTests.tempJar));

        this.module = new DynamicModule(DynamicModuleTests.tempJar);
    }

    @Test
    public void construct()
    {
        assertThat(this.module).isNotNull();
        assertThat(this.module.fileChanged()).isFalse();
    }

    @Test
    public void classAvailability() throws ClassNotFoundException
    {
        assertThat(this.module.getModuleClass(SampleCallable.class.getName())).isNotNull();
    }

    @Test
    public void instance()
    {
        assertThat(this.module.getInstance(Callable.class, SampleCallable.class.getName())).isNotNull();
    }

    @Test
    public void instance_badCast()
    {
        assertThatThrownBy(() ->
                           {
                               Collection<String> instance = this.module.getInstance(Collection.class, SampleCallable.class.getName());
                           }).isInstanceOf(ClassCastException.class);
    }

    @Test
    public void instance_antiSocial()
    {
        assertThatThrownBy(() ->
                           {
                               Object instance = this.module.getInstance(Object.class,SampleAntiSocialClass.class.getName());
                           }).isInstanceOf(DynamicModuleException.class);
    }

    @Test
    public void instance_instantiationKiller()
    {
        assertThatThrownBy(() ->
                           {
                               Collection<String> instance = this.module.getInstance(Collection.class, SampleInstantiationKiller.class.getName());
                           }).isInstanceOf(DynamicModuleException.class)
                             .hasCauseInstanceOf(InstantiationException.class);
    }

    @Test
    public void instance_uncheckedInstantiationKiller()
    {
        assertThatThrownBy(() ->
                           {
                               Collection<String> instance = this.module.getInstance(Collection.class, SampleUncheckedInstantiationKiller.class.getName());
                           }).isInstanceOf(DynamicModuleException.class)
                             .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void instance_noClass()
    {
        assertThatThrownBy(() ->
                           {
                               Collection<String> instance = this.module.getInstance(Collection.class, SampleCallable.class.getPackage().getName() + ".NonexistentClass");
                           }).isInstanceOf(DynamicModuleException.class)
                             .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void alias_simple()
    {
        Class<?> testClass = SampleCallable.class;

        assertThat(this.module.resolveAlias("foo")).isEmpty();
        this.module.createAlias(testClass.getName(), "foo");
        assertThat(this.module.resolveAlias("foo")).isNotEmpty().contains(testClass.getName());

        Callable<String> sample = this.module.getInstance(Callable.class, "foo");
    }

    @Test
    public void reload() throws IOException, ClassNotFoundException
    {
        Class<?> targetClass = SampleSet.class;
        assertThat(this.module.hasClass(targetClass.getName())).isFalse();

        // Capture the version
        final String originalVersion = this.module.getVersion();

        // Now create a new Jar with that class...
        final JarBuilder builder = new JarBuilder();
        builder.addClass(SampleCallable.class);
        builder.addClass(SampleThread.class);
        builder.addClass(SampleInstantiationKiller.class);
        builder.addClass(SampleUncheckedInstantiationKiller.class);
        builder.addClass(SampleAntiSocialClass.class);
        builder.addClass(SampleSet.class);

        builder.write(Files.newOutputStream(DynamicModuleTests.tempJar));

        // Now show that the file change is detected
        assertThat(this.module.fileChanged()).isTrue();

        // Now check for the class again
        this.module.hasClass(targetClass.getName());

        // Confirm that the version is different
        assertThat(this.module.getVersion()).isNotEqualTo(originalVersion);
    }

    @Test
    public void reload_viaInstance() throws IOException, ClassNotFoundException
    {
        Class<?> targetClass = SampleSet.class;
        assertThat(this.module.hasClass(targetClass.getName())).isFalse();

        // Now create a new Jar with that class...
        final JarBuilder builder = new JarBuilder();
        builder.addClass(SampleCallable.class);
        builder.addClass(SampleThread.class);
        builder.addClass(SampleUncheckedInstantiationKiller.class);
        builder.addClass(SampleAntiSocialClass.class);
        builder.addClass(SampleSet.class);

        builder.write(Files.newOutputStream(DynamicModuleTests.tempJar));

        // Now show that the file change is detected
        assertThat(this.module.fileChanged()).isTrue();

        Object instance = this.module.getInstance(Set.class, targetClass.getName());
    }

    @Test
    public void reload_viaClassLookup() throws IOException, ClassNotFoundException
    {
        Class<?> targetClass = SampleSet.class;
        assertThat(this.module.hasClass(targetClass.getName())).isFalse();

        // Now create a new Jar with that class...
        final JarBuilder builder = new JarBuilder();
        builder.addClass(SampleCallable.class);
        builder.addClass(SampleThread.class);
        builder.addClass(SampleUncheckedInstantiationKiller.class);
        builder.addClass(SampleAntiSocialClass.class);
        builder.addClass(SampleSet.class);

        builder.write(Files.newOutputStream(DynamicModuleTests.tempJar));

        // Now show that the file change is detected
        assertThat(this.module.fileChanged()).isTrue();

        Class<?> instanceClass = this.module.getModuleClass(targetClass.getName());
        assertThat(instanceClass.getName()).isEqualTo(targetClass.getName());
    }


}
