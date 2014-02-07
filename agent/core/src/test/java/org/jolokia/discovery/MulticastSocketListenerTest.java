package org.jolokia.discovery;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.URL;
import java.util.List;

import org.jolokia.Version;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.util.StdoutLogHandler;
import org.json.simple.JSONObject;
import org.testng.annotations.*;

import static org.jolokia.discovery.AbstractDiscoveryMessage.MessageType.QUERY;
import static org.jolokia.discovery.MulticastUtil.*;
import static org.testng.Assert.*;

/**
 * @author roland
 * @since 27.01.14
 */

public class MulticastSocketListenerTest {

    public static final String JOLOKIA_URL = "http://localhost:8080/jolokia";
    URL url;
    private MulticastSocket socket;
    private MulticastSocketListener listener;

    @BeforeClass
    public void startSocketListener() throws IOException {
        socket = newMulticastSocket();
        url = new URL(JOLOKIA_URL);
        final AgentDetails details = new AgentDetails();
        details.updateAgentParameters(JOLOKIA_URL, 100, false);
        details.setServerInfo("jolokia", "jolokia-test", "1.0");

        listener = new MulticastSocketListener(socket,
                                               getAgentDetailsHolder(details),
                                               new AllowAllRestrictor(),
                                               new StdoutLogHandler());
        Thread thread = new Thread(listener);
        thread.start();
    }


    private AgentDetailsHolder getAgentDetailsHolder(final AgentDetails pDetails) {
        return new AgentDetailsHolder() {
            public AgentDetails getAgentDetails() {
                return pDetails;
            }
        };
    }

    @AfterClass
    public void stopSocketListener() {
        listener.stop();
        socket.close();
    }

    @Test
    public void simple() throws IOException {

        DiscoveryOutgoingMessage out =
                new DiscoveryOutgoingMessage.Builder(QUERY)
                .build();
        List<DiscoveryIncomingMessage> discovered = sendQueryAndCollectAnswers(out,500);
        for (DiscoveryIncomingMessage in : discovered) {
            assertFalse(in.isQuery());
            AgentDetails agentDetails = in.getAgentDetails();
            JSONObject details = agentDetails.toJSONObject();
            if (details.get("server_vendor") != null && details.get("server_vendor").equals("jolokia")) {
                assertEquals(details.get("url"), JOLOKIA_URL);
                assertEquals(details.get("version"), Version.getAgentVersion());
                return;
            }
        }
        fail("No message found");
    }
}