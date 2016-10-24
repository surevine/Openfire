package com.reucon.openfire.plugin.archive.impl;

import com.reucon.openfire.plugin.archive.ArchivedMessageConsumer;
import com.reucon.openfire.plugin.archive.PersistenceManager;
import com.reucon.openfire.plugin.archive.model.ArchivedMessage;
import com.reucon.openfire.plugin.archive.model.Conversation;
import com.reucon.openfire.plugin.archive.model.Participant;
import com.reucon.openfire.plugin.archive.xep0059.XmppResultSet;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;
import org.eclipse.jdt.internal.compiler.apt.util.Archive;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.muc.NotAllowedException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.StringReader;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dwd on 25/07/16.
 */
public class MucMamPersistenceManager implements PersistenceManager {
    private static final String LOAD_HISTORY =
            "SELECT sender, nickname, logTime, subject, body, stanza FROM ofMucConversationLog " +
                    "WHERE logTime>? AND logTime <= ? AND roomID=? AND (nickname IS NOT NULL OR subject IS NOT NULL) ORDER BY logTime";
    @Override
    public boolean createMessage(ArchivedMessage message) {
        return false;
    }

    @Override
    public int processAllMessages(ArchivedMessageConsumer callback) {
        return 0;
    }

    @Override
    public boolean createConversation(Conversation conversation) {
        return false;
    }

    @Override
    public boolean updateConversationEnd(Conversation conversation) {
        return false;
    }

    @Override
    public boolean createParticipant(Participant participant, Long conversationId) {
        return false;
    }

    @Override
    public List<Conversation> findConversations(String[] participants, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public Collection<Conversation> findConversations(Date startDate, Date endDate, String owner, String with, XmppResultSet xmppResultSet) {
        return null;
    }

    @Override
    public Collection<ArchivedMessage> findMessages(Date startDate, Date endDate, String owner, String with, XmppResultSet xmppResultSet) {
        JID mucRoom = new JID(with);
        JID requestor = new JID(owner);
        MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
        MultiUserChatService service =  manager.getMultiUserChatService(mucRoom.getDomain());
        MUCRoom room = service.getChatRoom(mucRoom.getNode());
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        // If logging isn't enabled, do nothing.
        if (!room.isLogEnabled()) return null;
        // Check to see if the owner could join.
        if (room.getOutcasts().contains(requestor)) {
            return null;
        }
        if (room.isMembersOnly()) {
            if (!(room.getMembers().contains(requestor)
                    || room.getAdmins().contains(requestor)
                    || room.getOwners().contains(requestor))) {
                return null;
            }
        }
        List<ArchivedMessage>msgs = new LinkedList<>();
        int a = xmppResultSet.getCount()
        try {
            connection = DbConnectionManager.getConnection();
            pstmt = connection.prepareStatement(LOAD_HISTORY);
            pstmt.setString(1, StringUtils.dateToMillis(startDate));
            pstmt.setString(2, StringUtils.dateToMillis(endDate));
            pstmt.setLong(3, room.getID());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String senderJID = rs.getString(1);
                String nickname = rs.getString(2);
                Date sentDate = new Date(Long.parseLong(rs.getString(3).trim()));
                String subject = rs.getString(4);
                String body = rs.getString(5);
                String stanza = rs.getString(6);
                if (stanza == null) {
                    Message message = new Message();
                    message.setType(Message.Type.groupchat);
                    message.setSubject(subject);
                    message.setBody(body);
                    // Set the sender of the message
                    if (nickname != null && nickname.trim().length() > 0) {
                        JID roomJID = room.getRole().getRoleAddress();
                        // Recreate the sender address based on the nickname and room's JID
                        message.setFrom(new JID(roomJID.getNode(), roomJID.getDomain(), nickname, true));
                    }
                    else {
                        // Set the room as the sender of the message
                        message.setFrom(room.getRole().getRoleAddress());
                    }
                    stanza = message.toString();
                }
                ArchivedMessage archivedMessage = new ArchivedMessage(sentDate, ArchivedMessage.Direction.from, null, null);
                archivedMessage.setStanza(stanza);
                msgs.add(archivedMessage);
            }
        } catch (SQLException e) {
            // TODO ???
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, connection);
        }
        return msgs;
    }

    @Override
    public Collection<Conversation> getActiveConversations(int conversationTimeout) {
        return null;
    }

    @Override
    public List<Conversation> getConversations(Collection<Long> conversationIds) {
        return null;
    }

    @Override
    public Conversation getConversation(String ownerJid, String withJid, Date start) {
        return null;
    }

    @Override
    public Conversation getConversation(Long conversationId) {
        return null;
    }
}
