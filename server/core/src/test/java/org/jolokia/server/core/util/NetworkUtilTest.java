package org.jolokia.server.core.util;

import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.config.StaticConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author roland
 * @since 07.02.14
 */
public class NetworkUtilTest {

    @BeforeClass
    public void setup() {
        // To speed up the tests on Mac
        if (System.getProperty("os.name").startsWith("Mac")) {
            NetworkUtil.MAC_SKIP_NIFS.addAll(List.of("awdl", "llw", "utun"));
        }
    }

    @AfterClass
    public void tearDown() {
        NetworkUtil.MAC_SKIP_NIFS.clear();
    }

    @Test
    public void dump() {
        try {
            System.out.println(NetworkUtil.dumpLocalNetworkInfo());
        } catch (Exception exp) {
            System.out.println(exp.getMessage());
        }
    }

    @Test
    public void dumpBestMatch() {
        NetworkUtil.getBestMatchAddresses().forEach((name, addresses) ->
            System.out.printf("%s: IP4 = %s, IP6 = %s%n",
                name,
                addresses.getIa4().map(Inet4Address::getHostAddress).orElse("<null>"),
                addresses.getIa6().map(Inet6Address::getHostAddress).orElse("<null>")));
    }

    @Test
    public void findLocalIP4Address() throws SocketException {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        System.out.println("IFs");
        boolean found = false;
        while (ifs.hasMoreElements()) {
            NetworkInterface intf = ifs.nextElement();
            System.out.println(intf + " is loopback: " + intf.isLoopback());
            found = found || (!intf.isLoopback() && intf.supportsMulticast() && intf.isUp());
        }
        InetAddress addr = NetworkUtil.findLocalAddressViaNetworkInterface(Inet4Address.class);
        System.out.println("Address found via NIF: " + addr);
        assertEquals(addr != null, found);
        if (addr != null) {
            assertTrue(addr instanceof Inet4Address);
        }
    }

    @Test
    public void ip6Support() {
        boolean preferIP4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");
        boolean preferIP6Addresses = Boolean.getBoolean("java.net.preferIPv6Addresses");
        assertEquals(NetworkUtil.isIPv6Supported(), !preferIP4Stack);
    }

    @Test
    public void replaceExpression() {
        NetworkInterface nic = NetworkUtil.getBestMatchNetworkInterface();
        Map<String, InetAddresses> map = NetworkUtil.getBestMatchAddresses();
        assertNotNull(nic);
        String host = map.get(nic.getName()).getIa4().map(Inet4Address::getHostName).orElse(null);
        String ip = map.get(nic.getName()).getIa4().map(Inet4Address::getHostAddress).orElse(null);
        System.getProperties().setProperty("test.prop", "testy");
        System.getProperties().setProperty("test2:prop", "testx");
        String[] testData = {
                "${host}",host,
                "bla ${host} blub","bla " + host + " blub",
                "${ip}${host}",ip + host,
                "${ ip     }",ip,
                "|${prop:test.prop}|","|testy|",
                "${prop:test2:prop}","testx"
        };
        // Skip the following test on Mac as it can be extremely slow
        if (System.getProperty("os.name").startsWith("Mac")) {
            return;
        }
        StaticConfiguration config = new StaticConfiguration(ConfigKey.ALLOW_DNS_REVERSE_LOOKUP, "true");
        for (int i = 0; i < testData.length; i += 2) {
            assertEquals(config.resolve(testData[i]), testData[i + 1], "Checking " + testData[i]);
        }
    }

    @Test
    public void replaceExpressionWithEnv() {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            StaticConfiguration config = new StaticConfiguration();
            String path = config.resolve("Hello ${env:PATH} World");
            assertTrue(path.contains("bin"));
            assertTrue(path.startsWith("Hello"));
            assertTrue(path.endsWith("World"));
        }
    }
}
