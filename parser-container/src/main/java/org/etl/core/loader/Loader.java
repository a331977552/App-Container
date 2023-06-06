package org.etl.core.loader;

import org.etl.core.AppWrapper;

/**
 * A <b>Loader</b> represents a Java ClassLoader implementation that can
 * be used by a Container to load class files (within a repository associated
 * with the Loader) that are designed to be reloaded upon request, as well as
 * a mechanism to detect whether changes have occurred in the underlying
 * repository.
 * <p>
 * In order for a <code>Loader</code> implementation to successfully operate
 * with a <code>Context</code> implementation that implements reloading, it
 * must obey the following constraints:
 * <ul>
 * <li>Must implement <code>Lifecycle</code> so that the Context can indicate
 *     that a new class loader is required.
 * <li>The <code>start()</code> method must unconditionally create a new
 *     <code>ClassLoader</code> implementation.
 * <li>The <code>stop()</code> method must throw away its reference to the
 *     <code>ClassLoader</code> previously utilized, so that the class loader,
 *     all classes loaded by it, and all objects of those classes, can be
 *     garbage collected.
 * <li>Must allow a call to <code>stop()</code> to be followed by a call to
 *     <code>start()</code> on the same <code>Loader</code> instance.
 * <li>Based on a policy chosen by the implementation, must call the
 *     <code>Context.reload()</code> method on the owning <code>Context</code>
 *     when a change to one or more of the class files loaded by this class
 *     loader is detected.
 * </ul>
 *
 */
public interface Loader {
    void setAppWrapper(AppWrapper context);

    AppWrapper getAppWrapper();
    /**
     * @return the Java class loader to be used by this Container.
     */
    ClassLoader getClassLoader();

    void close();

    /**
     * Has the internal repository associated with this Loader been modified,
     * such that the loaded classes should be reloaded?
     *
     * @return <code>true</code> when the repository has been modified,
     *         <code>false</code> otherwise
     */
}
