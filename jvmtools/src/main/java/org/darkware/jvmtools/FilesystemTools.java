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

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author jeff@darkware.org
 * @since 2017-06-12
 */
public class FilesystemTools
{

    /**
     * Copy data to an output {@link Channel}.
     *
     * @param input The {@link InputStream} source of the data to copy.
     * @param output The {@link WritableByteChannel} to copy data to.
     * @return The number of bytes to the output channel.
     * @throws IOException
     */
    public static long channelCopy(final InputStream input, final WritableByteChannel output) throws IOException
    {
        return FilesystemTools.channelCopy(Channels.newChannel(input), output);
    }

    /**
     * Copy data to an output {@link Channel}.
     *
     * @param input The {@link ReadableByteChannel} source of the data to copy.
     * @param output The {@link WritableByteChannel} to copy data to.
     * @return The number of bytes to the output channel.
     * @throws IOException
     */
    public static long channelCopy(final ReadableByteChannel input, final WritableByteChannel output) throws IOException
    {
        long bytesWritten = 0;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (input.read(buffer) != -1) {
            buffer.flip();
            bytesWritten += output.write(buffer);
            buffer.compact();
        }

        // Write any remaining bytes
        buffer.flip();
        while (buffer.hasRemaining())
        {
            bytesWritten += output.write(buffer);
        }

        output.close();

        return bytesWritten;
    }

    /**
     * Create a concrete copy of a classpath resource on the local filesystem.
     *
     * @param resource The resource to copy, as a {@link URL}.
     * @param outputPath The {@link Path} to write the resource data to.
     * @throws IOException If there is an error while copying the data.
     * @return The number of bytes to the output path.
     */
    public static long createConcreteResource(final URL resource, final Path outputPath) throws IOException
    {
        return FilesystemTools.channelCopy(resource.openStream(), Files.newByteChannel(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    }

    /**
     * Create a concrete copy of a classpath resource on the local filesystem.
     *
     * @param resourcePath The resource to copy, as a {@link String} path to a local resource.
     * @param outputPath The {@link Path} to write the resource data to.
     * @return The number of bytes to the output path.
     * @throws IOException If there is an error while copying the data.
     */
    public static long createConcreteResource(final String resourcePath, final Path outputPath) throws IOException
    {
        return FilesystemTools.createConcreteResource(Resources.getResource(resourcePath), outputPath);
    }

}
