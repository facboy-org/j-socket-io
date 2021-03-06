package org.facboy.engineio;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;

import org.facboy.engineio.EngineIo.HandshakeSender;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.Parameter;
import org.facboy.engineio.protocol.StringPacket;
import org.facboy.engineio.protocol.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

/**
 * This implements the websocket transport.
 *
 * @author Christopher Ng
 */
public class EngineIoEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(EngineIoEndpoint.class);

    private final EngineIo engineIo;
    private final ObjectMapper objectMapper;

    @Inject
    public EngineIoEndpoint(EngineIo engineIo, ObjectMapper objectMapper) {
        this.engineIo = engineIo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        session.addMessageHandler(new Whole<String>() {
            @Override
            public void onMessage(String message) {
                // TODO implement this!
                throw new UnsupportedOperationException();
            }
        });

        Map<String, List<String>> requestParams = session.getRequestParameterMap();

        String sid = getFirstRequestParameter(requestParams, Parameter.SESSION_ID);
        String transport = getFirstRequestParameter(requestParams, Parameter.TRANSPORT);
        if (sid == null) {
            Transport transportObj = Transport.valueOf(transport);
            engineIo.handshake(transportObj.getHandshakeSession(), new HandshakeSender() {
                @Override
                public void sendHandshake(EngineIo.HandshakeResponse handshakeResponse) {
                    try {
                        final Writer sendWriter = session.getBasicRemote().getSendWriter();
                        try {
                            new StringPacket(Type.OPEN,
                                    objectMapper.writeValueAsString(handshakeResponse)).write(sendWriter);
                        } finally {
                            try {
                                sendWriter.close();
                            } catch (Exception e) {
                                logger.error("Error closing sendStream:", e);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private String getFirstRequestParameter(Map<String, List<String>> requestParameterMap, String parameter) {
        List<String> values = requestParameterMap.get(parameter);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
