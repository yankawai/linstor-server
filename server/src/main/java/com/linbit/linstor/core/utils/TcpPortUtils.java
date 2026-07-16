package com.linbit.linstor.core.utils;

import com.linbit.linstor.annotation.Nullable;
import com.linbit.linstor.core.types.TcpPortNumber;
import com.linbit.linstor.logging.ErrorReporter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TcpPortUtils
{
    private TcpPortUtils()
    {
    }

    /**
     * Returns a possibly empty list of ports that could not be bound on at least one of the given IPs as int.
     * {@code null} or an empty collection as {@code ipAddrsRef} will check on 0.0.0.0 (wildcard) instead.
     */
    public static List<Integer> getBlockedPortsAsIntList(
        @Nullable ErrorReporter errorReporterRef,
        @Nullable Collection<InetAddress> ipAddrsRef,
        @Nullable Collection<TcpPortNumber> portsRef
    )
    {
        List<Integer> ret = new ArrayList<>();
        if (portsRef != null)
        {
            Collection<InetAddress> ipAddrs = ipAddrsRef == null || ipAddrsRef.isEmpty() ?
                Collections.singletonList(null) :
                ipAddrsRef;
            for (TcpPortNumber port : portsRef)
            {
                for (InetAddress ipAddr : ipAddrs)
                {
                    IOException bindExc = tryTcpBind(ipAddr, port.value);
                    if (bindExc != null)
                    {
                        if (errorReporterRef != null)
                        {
                            errorReporterRef.logDebug(
                                "TCP port %d cannot be bound on %s: %s",
                                port.value,
                                ipAddr == null ? "0.0.0.0" : ipAddr.getHostAddress(),
                                bindExc.getMessage()
                            );
                        }
                        ret.add(port.value);
                        break;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns true/false whether or not the given TCP port is available on the given IP. {@code null} as an IP will
     * be interpreted as 0.0.0.0
     */
    public static boolean isTcpPortAvailable(@Nullable InetAddress ipAddr, TcpPortNumber port)
    {
        return isTcpPortAvailable(ipAddr, port.value);
    }

    /**
     * Returns true/false whether or not the given TCP port is available on the given IP. {@code null} as an IP will
     * be interpreted as 0.0.0.0
     */
    public static boolean isTcpPortAvailable(@Nullable InetAddress ipAddr, int port)
    {
        return tryTcpBind(ipAddr, port) == null;
    }

    /**
     * Tries to briefly bind a TCP server socket to the given IP + port. Returns {@code null} if the bind succeeded,
     * the causing {@link IOException} otherwise. {@code null} as an IP will be interpreted as 0.0.0.0.
     * <p>
     * SO_REUSEADDR is enabled before binding so that sockets in TIME_WAIT state do not cause a port to be considered
     * blocked. Note that on Windows the JDK binds in exclusive mode (SO_EXCLUSIVEADDRUSE) and only emulates
     * SO_REUSEADDR, which means that ports with TIME_WAIT remnants are still reported as blocked there.
     */
    public static @Nullable IOException tryTcpBind(@Nullable InetAddress ipAddr, int port)
    {
        IOException ret = null;
        try (ServerSocket ss = new ServerSocket())
        {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(ipAddr, port));
        }
        catch (IOException exc)
        {
            ret = exc;
        }
        return ret;
    }

    /**
     * Returns true if the given IP address is currently assigned to one of the local network interfaces. Binding an
     * address that is not assigned locally fails regardless of the chosen port, i.e. such a failure must not be
     * interpreted as "port blocked".
     */
    public static boolean isIpAddressLocallyAssigned(InetAddress ipAddr)
    {
        boolean assigned = false;
        try
        {
            assigned = NetworkInterface.getByInetAddress(ipAddr) != null;
        }
        catch (SocketException ignored)
        {
        }
        return assigned;
    }
}
