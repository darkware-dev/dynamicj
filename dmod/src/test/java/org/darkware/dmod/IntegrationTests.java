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

import org.darkware.jvmtools.FilesystemTools;
import org.darkware.jvmtools.ShutdownHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jeff@darkware.org
 * @since 2017-06-12
 */
public class IntegrationTests
{
    private final static Logger log = LoggerFactory.getLogger("Unit-Test");

    @Test
    public void integration_happy() throws Exception
    {
        final Path jarTemp = Files.createTempFile("jpi-", ".jar");
        ShutdownHandler.getCurrent().doom(jarTemp);

        this.log.info("Using jar file: {}", jarTemp);

        // Move in version 001.
        FilesystemTools.createConcreteResource("test-module-001.jar", jarTemp);

        DynamicModule manager = new DynamicModule(jarTemp);

        Callable<String> o1 = (Callable<String>) manager.getClass().newInstance();

        // Expect the version 001 output
        String output1 = o1.call();
        assertThat(output1).isEqualToIgnoringCase("test-module:version-001");

        // We don't expect that the file has changed yet/
        assertThat(manager.fileChanged()).isFalse();

        // Replace with version 002
        long bytes2 = FilesystemTools.createConcreteResource("test-module-002.jar", jarTemp);
        IntegrationTests.log.info("Wrote {} bytes to {}", bytes2, jarTemp);
        IntegrationTests.log.info("Jar size is now {}", Files.size(jarTemp));

        // We should see a changed file now
        assertThat(manager.fileChanged()).isTrue();

        // Have the manager reload the file
        Callable<String> o2 = (Callable<String>)manager.getClass().newInstance();

        String output2 = o2.call();
        assertThat(output2).isEqualToIgnoringCase("test-module:version-002");
    }
}
