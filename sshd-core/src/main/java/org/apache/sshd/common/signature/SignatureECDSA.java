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
package org.apache.sshd.common.signature;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

/**
 * Signature algorithm for EC keys using ECDSA. There 
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class SignatureECDSA extends AbstractSignature {

    protected SignatureECDSA(String algo) {
        super(algo);
    }

    @Override
    public byte[] sign() throws Exception {
        byte[] sig = signature.sign();

        if ((sig[0] != 0x30) || (sig[1] != sig.length - 2) || (sig[2] != 0x02)) {
            throw new IOException("Invalid signature format");
        }

        int rLength = sig[3];
        if ((rLength + 6 > sig.length) || (sig[4 + rLength] != 0x02)) {
            throw new IOException("Invalid signature format");
        }

        int sLength = sig[5 + rLength];
        if (6 + rLength + sLength > sig.length) {
            throw new IOException("Invalid signature format");
        }

        byte[] rArray = new byte[rLength];
        byte[] sArray = new byte[sLength];

        System.arraycopy(sig, 4, rArray, 0, rLength);
        System.arraycopy(sig, 6 + rLength, sArray, 0, sLength);

        BigInteger r = new BigInteger(rArray);
        BigInteger s = new BigInteger(sArray);

        // Write the <r,s> to its own types writer.
        Buffer rsBuf = new ByteArrayBuffer();
        rsBuf.putMPInt(r);
        rsBuf.putMPInt(s);

        return rsBuf.getCompactData();
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        sig = extractSig(sig);

        Buffer rsBuf = new ByteArrayBuffer(sig);
        byte[] rArray = rsBuf.getMPIntAsBytes();
        byte[] sArray = rsBuf.getMPIntAsBytes();

        if (rsBuf.available() != 0) {
            throw new IOException("Signature had padding");
        }

        // ASN.1
        int frst = ((rArray[0] & 0x80) != 0 ? 1 : 0);
        int scnd = ((sArray[0] & 0x80) != 0 ? 1 : 0);

        int length = rArray.length + sArray.length + 6 + frst + scnd;
        byte[] tmp = new byte[length];
        tmp[0] = (byte) 0x30;
        tmp[1] = (byte) (rArray.length + sArray.length + 4);
        tmp[1] += frst;
        tmp[1] += scnd;
        tmp[2] = (byte) 0x02;
        tmp[3] = (byte) rArray.length;
        tmp[3] += frst;
        System.arraycopy(rArray, 0, tmp, 4 + frst, rArray.length);
        tmp[4 + tmp[3]] = (byte) 0x02;
        tmp[5 + tmp[3]] = (byte) sArray.length;
        tmp[5 + tmp[3]] += scnd;
        System.arraycopy(sArray, 0, tmp, 6 + tmp[3] + scnd, sArray.length);
        sig = tmp;

        return signature.verify(sig);
    }

}
