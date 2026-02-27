package org.jolokia.server.core.util.jmx;

import java.lang.management.ManagementFactory;

import javax.management.*;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author roland
 * @since 07.03.13
 */
public class JmxUtilTest implements NotificationListener {


    private int counter = 0;

    @Test
    public void newObjectName() throws MalformedObjectNameException {
        ObjectName name = JmxUtil.newObjectName("java.lang:type=blub");
        assertEquals(name, new ObjectName("java.lang:type=blub"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*Invalid.*sogehtsnicht.*")
    public void invalidObjectName() {
        JmxUtil.newObjectName("bla:blub:name=sogehtsnicht");
    }

    @Test
    public void unknowListenerDeregistrationShouldBeSilentlyIgnored() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        JmxUtil.removeMBeanRegistrationListener(server,this);

    }

    public void handleNotification(Notification notification, Object handback) {
        System.out.println("got notification: " + notification);
        counter++;
    }

    public static class Bla implements BlaMBean {}
    public interface BlaMBean {}
}
