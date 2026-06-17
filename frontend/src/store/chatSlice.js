import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  user: JSON.parse(localStorage.getItem('cs_user')) || null,
  accessToken: localStorage.getItem('cs_token') || null,
  refreshToken: localStorage.getItem('cs_refresh') || null,
  activeConversation: null, // { id, name, isGroup }
  conversations: [], // list of user / group threads
  messages: {}, // mapping of threadId -> list of messages
  onlineUsers: {}, // mapping of userId -> onlineStatus
  typingStates: {}, // mapping of threadId -> list of typing userIds
  notifications: [],
  deviceSessions: [],
  blockedUsers: [],
  featureFlags: {
    ENABLE_REACTIONS: true,
    ENABLE_PINS: true,
    ENABLE_ANALYTICS: true,
  }
};

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    setAuth: (state, action) => {
      state.user = action.payload.user;
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;

      localStorage.setItem('cs_user', JSON.stringify(action.payload.user));
      localStorage.setItem('cs_token', action.payload.accessToken);
      localStorage.setItem('cs_refresh', action.payload.refreshToken);
    },
    clearAuth: (state) => {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.activeConversation = null;
      state.conversations = [];
      state.messages = {};
      state.onlineUsers = {};
      state.typingStates = {};
      state.notifications = [];
      state.deviceSessions = [];
      state.blockedUsers = [];

      localStorage.removeItem('cs_user');
      localStorage.removeItem('cs_token');
      localStorage.removeItem('cs_refresh');
    },
    setActiveConversation: (state, action) => {
      state.activeConversation = action.payload;
    },
    setConversations: (state, action) => {
      state.conversations = action.payload;
    },
    addConversation: (state, action) => {
      const exists = state.conversations.find(c => c.id === action.payload.id);
      if (!exists) {
        state.conversations.push(action.payload);
      }
    },
    setMessages: (state, action) => {
      const { conversationId, messages } = action.payload;
      state.messages[conversationId] = messages;
    },
    addMessage: (state, action) => {
      const { conversationId, message } = action.payload;
      if (!state.messages[conversationId]) {
        state.messages[conversationId] = [];
      }
      // Idempotency check in Redux state
      const exists = state.messages[conversationId].find(m => m.id === message.id || m.clientMessageId === message.clientMessageId);
      if (!exists) {
        state.messages[conversationId].push(message);
        // Sort chronologically by sequence number
        state.messages[conversationId].sort((a, b) => a.sequenceNumber - b.sequenceNumber);
      }
    },
    updateMessage: (state, action) => {
      const { conversationId, message } = action.payload;
      if (state.messages[conversationId]) {
        const index = state.messages[conversationId].findIndex(m => m.id === message.id);
        if (index !== -1) {
          state.messages[conversationId][index] = message;
        }
      }
    },
    setOnlineUsers: (state, action) => {
      state.onlineUsers = action.payload; // map of userId -> status
    },
    updateOnlineStatus: (state, action) => {
      const { userId, status } = action.payload;
      state.onlineUsers[userId] = status;
    },
    setTypingState: (state, action) => {
      const { conversationId, userId, isTyping } = action.payload;
      if (!state.typingStates[conversationId]) {
        state.typingStates[conversationId] = [];
      }
      if (isTyping) {
        if (!state.typingStates[conversationId].includes(userId)) {
          state.typingStates[conversationId].push(userId);
        }
      } else {
        state.typingStates[conversationId] = state.typingStates[conversationId].filter(id => id !== userId);
      }
    },
    setNotifications: (state, action) => {
      state.notifications = action.payload;
    },
    addNotification: (state, action) => {
      state.notifications.unshift(action.payload);
    },
    setBlockedUsers: (state, action) => {
      state.blockedUsers = action.payload;
    },
    setDeviceSessions: (state, action) => {
      state.deviceSessions = action.payload;
    }
  }
});

export const {
  setAuth,
  clearAuth,
  setActiveConversation,
  setConversations,
  addConversation,
  setMessages,
  addMessage,
  updateMessage,
  setOnlineUsers,
  updateOnlineStatus,
  setTypingState,
  setNotifications,
  addNotification,
  setBlockedUsers,
  setDeviceSessions
} = chatSlice.actions;

export default chatSlice.reducer;
