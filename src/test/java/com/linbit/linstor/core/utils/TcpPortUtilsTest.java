package com.linbit.linstor.core.utils;

import com.linbit.ValueOutOfRangeException;
import com.linbit.linstor.core.types.TcpPortNumber;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TcpPortUtilsTest
{
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    @Test
    public void freePortIsAvailable() throws IOException
    {
        int port = findFreePort();
        assertTrue(TcpPortUtils.isTcpPortAvailable(LOOPBACK, port));
    }

    @Test
    public void portInUseIsNotAvailable() throws IOException
    {
        try (ServerSocket blocker = new ServerSocket())
        {
            blocker.bind(new InetSocketAddress(LOOPBACK, 0));
            int port = blocker.getLocalPort();
            assertFalse(TcpPortUtils.isTcpPortAvailable(LOOPBACK, port));
        }
    }

    @Test
    public void portInUseOnGivenIpIsReportedBlocked() throws IOException, ValueOutOfRangeException
    {
        try (ServerSocket blocker = new ServerSocket())
        {
            blocker.bind(new InetSocketAddress(LOOPBACK, 0));
            int port = blocker.getLocalPort();

            List<Integer> blockedPorts = TcpPortUtils.getBlockedPortsAsIntList(
                null,
                Collections.singletonList(LOOPBACK),
                Collections.singletonList(new TcpPortNumber(port))
            );
            assertEquals(Collections.singletonList(port), blockedPorts);
        }
    }

    @Test
    public void wildcardIsCheckedWhenNoIpsGiven() throws IOException, ValueOutOfRangeException
    {
        try (ServerSocket blocker = new ServerSocket())
        {
            blocker.bind(new InetSocketAddress((InetAddress) null, 0));
            int port = blocker.getLocalPort();

            List<Integer> blockedPorts = TcpPortUtils.getBlockedPortsAsIntList(
                null,
                null,
                Collections.singletonList(new TcpPortNumber(port))
            );
            assertEquals(Collections.singletonList(port), blockedPorts);
        }
    }

    @Test
    public void freePortIsNotReportedBlocked() throws IOException, ValueOutOfRangeException
    {
        int port = findFreePort();
        List<Integer> blockedPorts = TcpPortUtils.getBlockedPortsAsIntList(
            null,
            Collections.singletonList(LOOPBACK),
            Collections.singletonList(new TcpPortNumber(port))
        );
        assertTrue(blockedPorts.isEmpty());
    }

    @Test
    public void loopbackIsLocallyAssigned()
    {
        assertTrue(TcpPortUtils.isIpAddressLocallyAssigned(LOOPBACK));
    }

    private int findFreePort() throws IOException
    {
        int port;
        try (ServerSocket srvSocket = new ServerSocket())
        {
            srvSocket.setReuseAddress(true);
            srvSocket.bind(new InetSocketAddress(LOOPBACK, 0));
            port = srvSocket.getLocalPort();
        }
        return port;
    }
}
