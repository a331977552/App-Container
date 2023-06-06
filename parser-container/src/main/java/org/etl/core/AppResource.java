package org.etl.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 *Represents a file or directory within a web application.
 */
public interface AppResource {

    long getLastModified();

    boolean exists();

    boolean isDirectory();

    /**
     * @return {@link java.io.File#isFile()}.
     */
    boolean isFile();

    /**
     * @return {@link java.io.File#delete()}.
     */
    boolean delete();

    String getAppPath();

    /**
     * @return {@link java.io.File#getName()}.
     */
    String getName();

    /**
     * @return the binary content of this resource or {@code null} if it is not
     *         available in a byte[] because, for example, it is too big.
     */
    byte[] getContent();
    /**
     * Obtain an InputStream based on the contents of this resource.
     *
     * @return  An InputStream based on the contents of this resource or
     *          <code>null</code> if the resource does not exist or does not
     *          represent a file
     */
    InputStream getInputStream() throws IOException;

    public long getContentLength();

    public String getCanonicalPath();


    public long getCreation();

    public URL getURL();

    public Manifest getManifest();

    AppResourceRoot getAppResourceRoot();

    public Certificate[] getCertificates();
}
