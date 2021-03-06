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

package org.apache.sshd.common.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.sshd.common.Digest;
import org.apache.sshd.common.Mac;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.NamedFactoriesListParseResult;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;

/**
 * Provides easy access to the currently implemented macs
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public enum BuiltinMacs implements MacFactory {
    hmacmd5(Constants.HMAC_MD5) {
        @Override
        public Mac create() {
            return new BaseMac("HmacMD5", 16, 16);
        }
    },
    hmacmd596(Constants.HMAC_MD5_96) {
        @Override
        public Mac create() {
            return new BaseMac("HmacMD5", 12, 16);
        }
    },
    hmacsha1(Constants.HMAC_SHA1) {
        @Override
        public Mac create() {
            return new BaseMac("HmacSHA1", 20, 20);
        }
    },
    hmacsha196(Constants.HMAC_SHA1_96) {
        @Override
        public Mac create() {
            return new BaseMac("HmacSHA1", 12, 20);
        }
    },
    hmacsha256(Constants.HMAC_SHA2_256) {
        @Override
        public Mac create() {
            return new BaseMac("HmacSHA256", 32, 32);
        }
    },
    hmacsha512(Constants.HMAC_SHA2_512) {
        @Override
        public Mac create() {
            return new BaseMac("HmacSHA1", 64, 64);
        }
    };

    private final String factoryName;

    @Override
    public final String getName() {
        return factoryName;
    }

    @Override
    public final boolean isSupported() {
        return true;
    }

    @Override
    public final String toString() {
        return getName();
    }
    
    BuiltinMacs(String facName) {
        factoryName = facName;
    }

    public static final Set<BuiltinMacs> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(BuiltinMacs.class));
    private static final Map<String,MacFactory>   extensions =
            new TreeMap<String,MacFactory>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Registered a {@link NamedFactory} to be available besides the built-in
     * ones when parsing configuration
     * @param extension The factory to register
     * @throws IllegalArgumentException if factory instance is {@code null},
     * or overrides a built-in one or overrides another registered factory
     * with the same name (case <U>insensitive</U>).
     */
    public static final void registerExtension(MacFactory extension) {
        String  name=ValidateUtils.checkNotNull(extension, "No extension provided", GenericUtils.EMPTY_OBJECT_ARRAY).getName();
        ValidateUtils.checkTrue(fromFactoryName(name) == null, "Extension overrides built-in: %s", name);

        synchronized(extensions) {
            ValidateUtils.checkTrue(!extensions.containsKey(name), "Extension overrides existinh: %s", name);
            extensions.put(name, extension);
        }
    }

    /**
     * @return A {@link SortedSet} of the currently registered extensions, sorted
     * according to the factory name (case <U>insensitive</U>)
     */
    public static final SortedSet<MacFactory> getRegisteredExtensions() {
        // TODO for JDK-8 return Collections.emptySortedSet()
        synchronized(extensions) {
            return GenericUtils.asSortedSet(NamedResource.BY_NAME_COMPARATOR, extensions.values());
        }
    }

    /**
     * Unregisters specified extension
     * @param name The factory name - ignored if {@code null}/empty
     * @return The registered extension - {@code null} if not found
     */
    public static final MacFactory unregisterExtension(String name) {
        if (GenericUtils.isEmpty(name)) {
            return null;
        }
        
        synchronized(extensions) {
            return extensions.remove(name);
        }
    }

    /**
     * @param s The {@link Enum}'s name - ignored if {@code null}/empty
     * @return The matching {@link org.apache.sshd.common.mac.BuiltinMacs} whose {@link Enum#name()} matches
     * (case <U>insensitive</U>) the provided argument - {@code null} if no match
     */
    public static BuiltinMacs fromString(String s) {
        if (GenericUtils.isEmpty(s)) {
            return null;
        }

        for (BuiltinMacs c : VALUES) {
            if (s.equalsIgnoreCase(c.name())) {
                return c;
            }
        }

        return null;
    }

    /**
     * @param factory The {@link org.apache.sshd.common.NamedFactory} for the Mac - ignored if {@code null}
     * @return The matching {@link org.apache.sshd.common.mac.BuiltinMacs} whose factory name matches
     * (case <U>insensitive</U>) the digest factory name
     * @see #fromFactoryName(String)
     */
    public static BuiltinMacs fromFactory(NamedFactory<Digest> factory) {
        if (factory == null) {
            return null;
        } else {
            return fromFactoryName(factory.getName());
        }
    }

    /**
     * @param n The factory name - ignored if {@code null}/empty
     * @return The matching {@link org.apache.sshd.common.mac.BuiltinMacs} whose factory name matches
     * (case <U>insensitive</U>) the provided name - {@code null} if no match
     */
    public static BuiltinMacs fromFactoryName(String n) {
        if (GenericUtils.isEmpty(n)) {
            return null;
        }

        for (BuiltinMacs c : VALUES) {
            if (n.equalsIgnoreCase(c.getName())) {
                return c;
            }
        }

        return null;
    }

    /**
     * @param macs A comma-separated list of MACs' names - ignored
     * if {@code null}/empty
     * @return A {@link ParseResult} containing the successfully parsed
     * factories and the unknown ones. <B>Note:</B> it is up to caller to
     * ensure that the lists do not contain duplicates
     */
    public static final ParseResult parseMacsList(String macs) {
        return parseMacsList(GenericUtils.split(macs, ','));
    }

    public static final ParseResult parseMacsList(String ... macs) {
        return parseMacsList(GenericUtils.isEmpty((Object[]) macs) ? Collections.<String>emptyList() : Arrays.asList(macs));
    }

    public static final ParseResult parseMacsList(Collection<String> macs) {
        if (GenericUtils.isEmpty(macs)) {
            return ParseResult.EMPTY;
        }
        
        List<MacFactory> factories=new ArrayList<MacFactory>(macs.size());
        List<String>            unknown=Collections.<String>emptyList();
        for (String name : macs) {
            MacFactory   m=resolveFactory(name);
            if (m != null) {
                factories.add(m);
            } else {
                // replace the (unmodifiable) empty list with a real one
                if (unknown.isEmpty()) {
                    unknown = new ArrayList<String>();
                }
                unknown.add(name);
            }
        }
        
        return new ParseResult(factories, unknown);
    }

    /**
     * @param name The factory name
     * @return The factory or {@code null} if it is neither a built-in one
     * or a registered extension 
     */
    public static final MacFactory resolveFactory(String name) {
        if (GenericUtils.isEmpty(name)) {
            return null;
        }

        MacFactory  m=fromFactoryName(name);
        if (m != null) {
            return m;
        }
        
        synchronized(extensions) {
            return extensions.get(name);
        }
    }

    public static final class ParseResult extends NamedFactoriesListParseResult<Mac,MacFactory> {
        public static final ParseResult EMPTY=new ParseResult(Collections.<MacFactory>emptyList(), Collections.<String>emptyList());
        
        public ParseResult(List<MacFactory> parsed, List<String> unsupported) {
            super(parsed, unsupported);
        }
    }

    public static final class Constants {
        public static final String HMAC_MD5 = "hmac-md5";
        public static final String HMAC_MD5_96 = "hmac-md5-96";
        public static final String HMAC_SHA1 = "hmac-sha1";
        public static final String HMAC_SHA1_96 = "hmac-sha1-96";
        public static final String HMAC_SHA2_256 = "hmac-sha2-256";
        public static final String HMAC_SHA2_512 = "hmac-sha2-512";
    }
}
