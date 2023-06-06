/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.etl.core.resources;


import lombok.extern.slf4j.Slf4j;
import org.etl.core.util.URLEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
public class JarArchiveResource extends AbstractResource {

    private final AbstractArchiveResourceSet archiveResourceSet;
    private final String baseUrl;
    private final JarEntry resource;
    private final String name;
    private boolean certReady;
    private Certificate[] certificates;



    protected JarArchiveResource(AbstractArchiveResourceSet archiveResourceSet, String appPath, String baseUrl,
                                 JarEntry jarEntry) {
        super(archiveResourceSet.getRoot(), appPath);
        this.archiveResourceSet = archiveResourceSet;
        this.baseUrl = baseUrl;
        this.resource = jarEntry;
        name = jarEntry.getName();
    }

    protected AbstractArchiveResourceSet getArchiveResourceSet() {
        return archiveResourceSet;
    }

    protected final String getBase() {
        return archiveResourceSet.getBase();
    }

    protected final String getBaseUrl() {
        return baseUrl;
    }

    protected final JarEntry getResource() {
        return resource;
    }

    @Override
    public long getLastModified() {
        return resource.getTime();
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return resource.isDirectory();
    }

    @Override
    public boolean isFile() {
        return !resource.isDirectory();
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getContentLength() {
        if (isDirectory()) {
            return -1;
        }
        return resource.getSize();
    }

    @Override
    public String getCanonicalPath() {
        return null;
    }


    @Override
    public long getCreation() {
        return resource.getTime();
    }



    @Override
    public final byte[] getContent() {
        long len = getContentLength();

        if (len > Integer.MAX_VALUE) {
            // Can't create an array that big
            throw new ArrayIndexOutOfBoundsException("content too large :"+getAppPath()+" : "+ len);
        }

        if (len < 0) {
            // Content is not applicable here (e.g. is a directory)
            return null;
        }

        int size = (int) len;
        byte[] result = new byte[size];

        int pos = 0;
        try (JarInputStreamWrapper jisw = getJarInputStreamWrapper()) {
            if (jisw == null) {
                // An error occurred, don't return corrupted content
                return null;
            }
            while (pos < size) {
                int n = jisw.read(result, pos, size - pos);
                if (n < 0) {
                    break;
                }
                pos += n;
            }
            certificates = jisw.getCertificates();
            certReady = true;

            // Once the stream has been read, read the certs
        } catch (IOException ioe) {
            // Don't return corrupted content
            return null;
        }

        return result;
    }




    @Override
    public Certificate[] getCertificates() {
        if (!certReady)
            throw new IllegalStateException();

        return certificates;
    }

    @Override
    public Manifest getManifest() {
        return archiveResourceSet.getManifest();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getJarInputStreamWrapper();
    }

    public URL getURL() {
        String url = baseUrl + URLEncoder.DEFAULT.encode(resource.getName(), StandardCharsets.UTF_8);
        try {
            return new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            log.error("failed to getURL{}",resource.getName(),e);
            return null;
        }
    }

    /**
     * This wrapper assumes that the InputStream was created from a JarFile obtained from a call to
     * getArchiveResourceSet().openJarFile(). If this is not the case then the usage counting in
     * AbstractArchiveResourceSet will break and the JarFile may be unexpectedly closed.
     */
    protected class JarInputStreamWrapper extends InputStream {

        private final JarEntry jarEntry;
        private final InputStream is;
        private final AtomicBoolean closed = new AtomicBoolean(false);


        public JarInputStreamWrapper(JarEntry jarEntry, InputStream is) {
            this.jarEntry = jarEntry;
            this.is = is;
        }


        @Override
        public int read() throws IOException {
            return is.read();
        }


        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }


        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);
        }


        @Override
        public long skip(long n) throws IOException {
            return is.skip(n);
        }


        @Override
        public int available() throws IOException {
            return is.available();
        }


        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                // Must only call this once else the usage counting will break
                archiveResourceSet.closeJarFile();
            }
            is.close();
        }


        @Override
        public synchronized void mark(int readlimit) {
            is.mark(readlimit);
        }


        @Override
        public synchronized void reset() throws IOException {
            is.reset();
        }


        @Override
        public boolean markSupported() {
            return is.markSupported();
        }

        public Certificate[] getCertificates() {
            return jarEntry.getCertificates();
        }
    }

    protected JarInputStreamWrapper getJarInputStreamWrapper() {
        JarFile jarFile = null;
        try {
            jarFile = getArchiveResourceSet().openJarFile();
            // Need to create a new JarEntry so the certificates can be read
            JarEntry jarEntry = jarFile.getJarEntry(getResource().getName());
            InputStream is = jarFile.getInputStream(jarEntry);
            return new JarInputStreamWrapper(jarEntry, is);
        } catch (IOException e) {
            log.error("get jar inputstream failed,{},{}",getResource().getName(), getBaseUrl(),e);
            if (jarFile != null) {
                getArchiveResourceSet().closeJarFile();
            }
            return null;
        }
    }
}
