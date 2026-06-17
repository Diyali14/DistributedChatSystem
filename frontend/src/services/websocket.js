import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { addMessage, updateOnlineStatus, setTypingState, addNotification } from '../store/chatSlice';

let chatClient = null;
let groupClient = null;
let presenceClient = null;

let chatRetryTime = 1000;
let groupRetryTime = 1000;
let presenceRetryTime = 1000;

export function connectAllWebSockets(token, dispatch) {
  disconnectAllWebSockets();

  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  let wsProtocol, wsHost, httpProtocol;

  if (baseUrl) {
    const url = new URL(baseUrl);
    wsProtocol = url.protocol === 'https:' ? 'wss' : 'ws';
    wsHost = url.host;
    httpProtocol = url.protocol;
  } else {
    wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    wsHost = window.location.host;
    httpProtocol = window.location.protocol;
  }

  // 1. CHAT SERVICE CONNECTION
  chatClient = new Client({
    brokerURL: `${wsProtocol}://${wsHost}/ws/chat?token=${token}`,
    connectHeaders: { token },
    onConnect: () => {
      console.log('[WS Chat] Connected');
      chatRetryTime = 1000; // Reset retry interval on success

      // Subscribe to private message queue
      chatClient.subscribe('/user/queue/messages', (message) => {
        const payload = JSON.parse(message.body);
        console.log('[WS Chat] Message received: ', payload);
        
        // Dispatch to Redux store
        dispatch(addMessage({
          conversationId: payload.senderId, // Index by sender
          message: payload
        }));
      });

      // Subscribe to private errors queue
      chatClient.subscribe('/user/queue/errors', (err) => {
        console.error('[WS Chat] Error message: ', err.body);
      });
    },
    onDisconnect: () => {
      console.warn('[WS Chat] Disconnected');
    },
    onStompError: (frame) => {
      console.error('[WS Chat] Broker error: ', frame.headers['message']);
    }
  });

  // Fallback to SockJS if WebSockets are blocked
  chatClient.webSocketFactory = () => new SockJS(`${httpProtocol}//${wsHost}/ws/chat?token=${token}`);
  chatClient.activate();

  // 2. GROUP SERVICE CONNECTION
  groupClient = new Client({
    brokerURL: `${wsProtocol}://${wsHost}/ws/group?token=${token}`,
    connectHeaders: { token },
    onConnect: () => {
      console.log('[WS Group] Connected');
      groupRetryTime = 1000;

      // Subscription to groups occurs dynamically in UI when selecting a group
      // But we can define a wildcard or general broadcast listener here if needed
    },
    onDisconnect: () => {
      console.warn('[WS Group] Disconnected');
    }
  });
  groupClient.webSocketFactory = () => new SockJS(`${httpProtocol}//${wsHost}/ws/group?token=${token}`);
  groupClient.activate();

  // 3. PRESENCE SERVICE CONNECTION
  presenceClient = new Client({
    brokerURL: `${wsProtocol}://${wsHost}/ws/presence?token=${token}`,
    connectHeaders: { token },
    onConnect: () => {
      console.log('[WS Presence] Connected');
      presenceRetryTime = 1000;

      // Subscribe to global presence events
      presenceClient.subscribe('/topic/presence', (message) => {
        const payload = JSON.parse(message.body);
        dispatch(updateOnlineStatus({
          userId: payload.userId,
          status: payload.status
        }));
      });
    },
    onDisconnect: () => {
      console.warn('[WS Presence] Disconnected');
    }
  });
  presenceClient.webSocketFactory = () => new SockJS(`${httpProtocol}//${wsHost}/ws/presence?token=${token}`);
  presenceClient.activate();
}

export function subscribeToGroup(groupId, dispatch) {
  if (groupClient && groupClient.connected) {
    console.log('[WS Group] Subscribing to group topic: ', groupId);
    return groupClient.subscribe(`/topic/group/${groupId}`, (message) => {
      const payload = JSON.parse(message.body);
      dispatch(addMessage({
        conversationId: groupId,
        message: payload
      }));
    });
  }
  return null;
}

export function subscribeToTyping(conversationId, dispatch) {
  if (presenceClient && presenceClient.connected) {
    console.log('[WS Presence] Subscribing to typing topic: ', conversationId);
    return presenceClient.subscribe(`/topic/typing/${conversationId}`, (message) => {
      const payload = JSON.parse(message.body);
      dispatch(setTypingState({
        conversationId: payload.conversationId,
        userId: payload.userId,
        isTyping: payload.isTyping
      }));
    });
  }
  return null;
}

export function publishPrivateMessage(receiverId, content, clientMsgId, type = 'TEXT', replyToId = null) {
  if (chatClient && chatClient.connected) {
    chatClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        clientMessageId: clientMsgId,
        receiverId,
        message: content,
        type,
        replyToId
      })
    });
    return true;
  }
  return false;
}

export function publishGroupMessage(groupId, content, clientMsgId, type = 'TEXT', replyToId = null) {
  if (groupClient && groupClient.connected) {
    groupClient.publish({
      destination: '/app/group.send',
      body: JSON.stringify({
        clientMessageId: clientMsgId,
        groupId,
        message: content,
        type,
        replyToId
      })
    });
    return true;
  }
  return false;
}

export function publishTypingState(conversationId, isTyping) {
  if (presenceClient && presenceClient.connected) {
    presenceClient.publish({
      destination: '/app/typing',
      body: JSON.stringify({
        conversationId,
        isTyping
      })
    });
  }
}

export function publishPresenceStatus(status) {
  if (presenceClient && presenceClient.connected) {
    presenceClient.publish({
      destination: '/app/presence.status',
      body: JSON.stringify({ status })
    });
  }
}

export function disconnectAllWebSockets() {
  if (chatClient) chatClient.deactivate();
  if (groupClient) groupClient.deactivate();
  if (presenceClient) presenceClient.deactivate();

  chatClient = null;
  groupClient = null;
  presenceClient = null;
}
