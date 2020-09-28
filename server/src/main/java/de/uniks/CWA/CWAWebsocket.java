package de.uniks.CWA;

import de.uniks.SQLite.SQLite;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class CWAWebsocket {

    static final Map<Session, Connection> sessionMap = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        Connection connection;
        try {
            connection = SQLite.openDatabase();
        } catch (SQLiteException e) {
            session.getRemote().sendString("Failed to open database: " + e.getMessage());
            session.close();
            return;
        }

        sessionMap.put(session, connection);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        try {
            SQLite.closeDatabase(sessionMap.get(session));
        } catch (SQLException e) {
            System.out.println("Failed to close database: " + e.getMessage());
        }
        sessionMap.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        if (!message.matches("[0-9]{7}:[0-9]+")) {
            session.getRemote().sendString("Wrong Input!");
        } else {
            String[] split = message.split(":");
            try {
                session.getRemote().sendString(Arrays.toString(
                        SQLite.getKeyData(Integer.parseInt(split[0]), Integer.parseInt(split[1]), sessionMap.get(session))));
            } catch (NumberFormatException | SQLException e) {
                System.out.println(e.getMessage());
                session.getRemote().sendString("Not found");
            }
        }
    }
}
