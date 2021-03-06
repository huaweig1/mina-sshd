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
package org.apache.sshd;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Arrays;

import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientConnectionService;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.deprecated.ClientUserAuthServiceOld;
import org.apache.sshd.deprecated.UserAuthKeyboardInteractive;
import org.apache.sshd.deprecated.UserAuthPassword;
import org.apache.sshd.deprecated.UserAuthPublicKey;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;
import org.apache.sshd.util.BaseTestSupport;
import org.apache.sshd.util.BogusPasswordAuthenticator;
import org.apache.sshd.util.BogusPublickeyAuthenticator;
import org.apache.sshd.util.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuthenticationTest extends BaseTestSupport {

    private static final String WELCOME = "Welcome to SSHD";

    private SshServer sshd;
    private int port;

    @Before
    public void setUp() throws Exception {
        sshd = SshServer.setUpDefaultServer();
        sshd.setKeyPairProvider(Utils.createTestHostKeyProvider());
        sshd.setPasswordAuthenticator(new BogusPasswordAuthenticator());
        sshd.setPublickeyAuthenticator(new BogusPublickeyAuthenticator());
        sshd.getProperties().put(ServerFactoryManager.WELCOME_BANNER, WELCOME);
        sshd.getProperties().put(ServerFactoryManager.AUTH_METHODS, "publickey,password publickey,keyboard-interactive");
        sshd.setSessionFactory(new SessionFactory() {
            @Override
            protected AbstractSession doCreateSession(IoSession ioSession) throws Exception {
                return new TestSession(server, ioSession);
            }
        });
        sshd.start();
        port = sshd.getPort();
    }

    @After
    public void tearDown() throws Exception {
        if (sshd != null) {
            sshd.stop(true);
        }
    }

    @Test
    public void testChangeUser() throws Exception {
        try(SshClient client = SshClient.setUpDefaultClient()) {
            client.setServiceFactories(Arrays.asList(
                    new ClientUserAuthServiceOld.Factory(),
                    new ClientConnectionService.Factory()
            ));

            client.start();
                
            try(ClientSession s = client.connect(null, "localhost", port).await().getSession()) {
                s.waitFor(ClientSession.CLOSED | ClientSession.WAIT_AUTH, 0);
        
                assertFalse(authPassword(s, "user1", "the-password").await().isSuccess());
                assertFalse(authPassword(s, "user2", "the-password").await().isSuccess());
        
                // Note that WAIT_AUTH flag should be false, but since the internal
                // authentication future is not updated, it's still returned
                assertEquals(ClientSession.CLOSED | ClientSession.WAIT_AUTH, s.waitFor(ClientSession.CLOSED, 1000));
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testAuthPasswordOnly() throws Exception {
        try(SshClient client = SshClient.setUpDefaultClient()) {
            client.setServiceFactories(Arrays.asList(
                    new ClientUserAuthServiceOld.Factory(),
                    new ClientConnectionService.Factory()
            ));
            client.start();
            
            try(ClientSession s = client.connect(null, "localhost", port).await().getSession()) {
                s.waitFor(ClientSession.CLOSED | ClientSession.WAIT_AUTH, 0);
        
                assertFalse(authPassword(s, "smx", "smx").await().isSuccess());
        
                s.close(true);
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testAuthKeyPassword() throws Exception {
        try(SshClient client = SshClient.setUpDefaultClient()) {
            client.setServiceFactories(Arrays.asList(
                    new ClientUserAuthServiceOld.Factory(),
                    new ClientConnectionService.Factory()
            ));
            client.start();
            
            try(ClientSession s = client.connect(null, "localhost", port).await().getSession()) {
                s.waitFor(ClientSession.CLOSED | ClientSession.WAIT_AUTH, 0);
        
                KeyPair pair = Utils.createTestHostKeyProvider().loadKey(KeyPairProvider.SSH_RSA);
                assertFalse(authPublicKey(s, "smx", pair).await().isSuccess());
        
                assertTrue(authPassword(s, "smx", "smx").await().isSuccess());
                s.close(true);
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testAuthKeyInteractive() throws Exception {
        try(SshClient client = SshClient.setUpDefaultClient()) {
            client.setServiceFactories(Arrays.asList(
                    new ClientUserAuthServiceOld.Factory(),
                    new ClientConnectionService.Factory()
            ));
            client.start();
            
            try(ClientSession s = client.connect(null, "localhost", port).await().getSession()) {
                s.waitFor(ClientSession.CLOSED | ClientSession.WAIT_AUTH, 0);
        
                KeyPair pair = Utils.createTestHostKeyProvider().loadKey(KeyPairProvider.SSH_RSA);
                assertFalse(authPublicKey(s, "smx", pair).await().isSuccess());
        
                assertTrue(authInteractive(s, "smx", "smx").await().isSuccess());
        
                s.close(true);
            } finally {
                client.stop();
            }
        }
    }

    private AuthFuture authPassword(ClientSession s, String user, String pswd) throws IOException {
        ((ClientSessionImpl) s).setUsername(user);
        return s.getService(ClientUserAuthServiceOld.class)
                .auth(new UserAuthPassword((ClientSessionImpl) s, "ssh-connection", pswd));
    }

    private AuthFuture authInteractive(ClientSession s, String user, String pswd) throws IOException {
        ((ClientSessionImpl) s).setUsername(user);
        return s.getService(ClientUserAuthServiceOld.class)
                .auth(new UserAuthKeyboardInteractive((ClientSessionImpl) s, "ssh-connection", pswd));
    }

    private AuthFuture authPublicKey(ClientSession s, String user, KeyPair pair) throws IOException {
        ((ClientSessionImpl) s).setUsername(user);
        return s.getService(ClientUserAuthServiceOld.class)
                .auth(new UserAuthPublicKey((ClientSessionImpl) s, "ssh-connection", pair));
    }

    public static class TestSession extends ServerSession {
        public TestSession(ServerFactoryManager server, IoSession ioSession) throws Exception {
            super(server, ioSession);
        }
        @Override
        public void handleMessage(Buffer buffer) throws Exception {
            super.handleMessage(buffer);
        }
    }
}
