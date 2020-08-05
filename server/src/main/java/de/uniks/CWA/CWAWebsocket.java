package de.uniks.CWA;

import de.uniks.Main;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.Arrays;

@WebSocket
public class CWAWebsocket {

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        session.getRemote().sendString("Usage:\nrsin:keyNr");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {

    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {

        if (!message.matches("[0-9]{7}:[0-9]+")) {
            session.getRemote().sendString("Wrong Input!");
        } else {
            String[] split = message.split(":");
            try {
                session.getRemote().sendString(Arrays.toString(Main.getInfections().get(Integer.parseInt(split[0])).get(Integer.parseInt(split[1]))));
            } catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
                session.getRemote().sendString("Not found");
            }
        }
    }
}
