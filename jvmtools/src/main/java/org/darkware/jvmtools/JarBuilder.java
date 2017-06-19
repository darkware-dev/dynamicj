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

package org.darkware.jvmtools;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author jeff@darkware.org
 * @since 2017-05-31
 */
public class JarBuilder
{
    private final Manifest manifest;
    private final Map<String, ByteBuffer> entries;

    public JarBuilder()
    {
        super();

        this.manifest = new Manifest();
        this.manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        this.entries = Maps.newHashMap();
    }

    public void addClass(final Class<?> classInstance, final String className)
    {
        try
        {
            String srcPath = classInstance.getName().replace('.', '/') + ".class";
            ByteBuffer classData = ByteBuffer.wrap(
                    ByteStreams.toByteArray(classInstance.getClassLoader().getResourceAsStream(srcPath)));

            String destPath = className.replace('.', '/') + ".class";
            this.entries.put(destPath, classData);
        }
        catch (IOException e)
        {

        }
    }

    public void addClass(final Class<?> classInstance)
    {
        this.addClass(classInstance, classInstance.getName());
    }

    public void write(final OutputStream stream) throws IOException
    {
        JarOutputStream jar = new JarOutputStream(stream, this.manifest);

        this.entries.entrySet().stream()
                .forEach(e -> {
                    try
                    {
                        JarEntry entry = new JarEntry(e.getKey());
                        jar.putNextEntry(entry);
                        jar.write(e.getValue().array());
                        jar.closeEntry();
                    }
                    catch (IOException e2)
                    {
                        //TODO: Do something
                    }
                });

        jar.close();
        stream.close();
    }
}
