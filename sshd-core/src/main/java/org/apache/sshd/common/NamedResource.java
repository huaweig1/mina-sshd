/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sshd.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sshd.common.util.GenericUtils;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public interface NamedResource {
    /**
     * @return The resource name
     */
    String getName();
    
    /**
     * Compares 2 {@link NamedResource}s according to their {@link #getName()}
     * value case <U>insensitive</U>
     */
    Comparator<NamedResource> BY_NAME_COMPARATOR=new Comparator<NamedResource>() {
            @Override
            public int compare(NamedResource r1, NamedResource r2) {
                String  n1=r1.getName(), n2=r2.getName();
                return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
            }
        };

    /**
     * Returns the value of {@link #getName()} - or {@code null} if argument is {@code null}
     */
    Transformer<NamedResource,String> NAME_EXTRACTOR=new Transformer<NamedResource,String>() {
            @Override
            public String transform(NamedResource input) {
                if (input == null) {
                    return null;
                } else {
                    return input.getName();
                }
            }
        };

    /**
     * Utility class to help using {@link NamedResource}s
     */
    public static final class Utils {
        /**
         * @param resources The named resources
         * @return A {@link List} of all the factories names - in same order
         * as they appear in the input collection
         */
        public static List<String> getNameList(Collection<? extends NamedResource> resources) {
            if (GenericUtils.isEmpty(resources)) {
                return Collections.emptyList();
            }

            List<String> names = new ArrayList<>(resources.size());
            for (NamedResource r : resources) {
                names.add(r.getName());
            }

            return names;
        }
        
        /**
         * @param resources list of available resources
         * @return A comma separated list of factory names
         */
        public static String getNames(Collection<? extends NamedResource> resources) {
            return GenericUtils.join(getNameList(resources), ',');
        }
    }
}
