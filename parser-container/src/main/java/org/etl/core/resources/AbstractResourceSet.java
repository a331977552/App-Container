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


import org.etl.core.AppResourceRoot;
import org.etl.core.AppResourceSet;

import java.util.jar.Manifest;

public abstract class AbstractResourceSet implements AppResourceSet {

    private AppResourceRoot root;
    private String base;
    private String internalPath = "";
    private Manifest manifest;
    private String webAppMount;

    protected final void checkPath(String path) {
        if (path == null || path.length() == 0 || path.charAt(0) != '/') {
            throw new IllegalArgumentException("illegal path");
        }
    }

    @Override
    public final void setRoot(AppResourceRoot root) {
        this.root = root;
    }

    protected final AppResourceRoot getRoot() {
        return root;
    }


    protected final String getInternalPath() {
        return internalPath;
    }

    public final void setInternalPath(String internalPath) {
        checkPath(internalPath);
        // Optimise internal processing
        if (internalPath.equals("/")) {
            this.internalPath = "";
        } else {
            this.internalPath = internalPath;
        }
    }

    public final void setWebAppMount(String webAppMount) {
        checkPath(webAppMount);
        // Optimise internal processing
        if (webAppMount.equals("/")) {
            this.webAppMount = "";
        } else {
            this.webAppMount = webAppMount;
        }
    }

    protected final String getWebAppMount() {
        return webAppMount;
    }

    public final void setBase(String base) {
        this.base = base;
    }

    protected final String getBase() {
        return base;
    }


    protected final void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    protected final Manifest getManifest() {
        return manifest;
    }


    // -------------------------------------------------------- Lifecycle methods

}
