import React, { useState, useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { 
  MessageSquare, Users, Settings, LogOut, Search, Send, Paperclip, Mic, Smile, 
  MoreVertical, Calendar, Archive, ShieldAlert, Trash, Plus, Download, Play, 
  Check, CheckCheck, Lock, Monitor, AlertCircle, X, ShieldCheck
} from 'lucide-react';
import { apiFetch } from '../services/api';
import { 
  clearAuth, setActiveConversation, setConversations, setMessages, addMessage, 
  updateOnlineStatus, setTypingState, setBlockedUsers, updateMessage
} from '../store/chatSlice';
import { 
  connectAllWebSockets, disconnectAllWebSockets, publishPrivateMessage, 
  publishGroupMessage, publishTypingState, publishPresenceStatus, 
  subscribeToGroup, subscribeToTyping 
} from '../services/websocket';

export default function Dashboard({ onNavigate }) {
  const dispatch = useDispatch();
  const authState = useSelector(state => state.chat);
  const { user, accessToken, activeConversation, conversations, messages, onlineUsers, typingStates, blockedUsers } = authState;

  // UI state variables
  const [sidebarTab, setSidebarTab] = useState('chats'); // chats, groups, settings, admin
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [messageInput, setMessageInput] = useState('');
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);
  
  // Advanced Messaging states
  const [replyToMessage, setReplyToMessage] = useState(null);
  const [scheduleTime, setScheduleTime] = useState('');
  const [showScheduler, setShowScheduler] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  
  // Settings & Admin states
  const [deviceSessions, setDeviceSessions] = useState([]);
  const [adminReports, setAdminReports] = useState([]);
  const [adminAuditLogs, setAdminAuditLogs] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  
  // Refs
  const messageEndRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const groupSubscriptionRef = useRef(null);
  const typingSubscriptionRef = useRef(null);

  // Initialize WebSockets on Login
  useEffect(() => {
    if (accessToken) {
      connectAllWebSockets(accessToken, dispatch);
      loadConversations();
      loadBlockedUsers();
    }
    return () => {
      disconnectAllWebSockets();
    };
  }, [accessToken]);

  // Load message history on active thread change
  useEffect(() => {
    if (activeConversation) {
      loadMessageHistory(activeConversation.id, activeConversation.isGroup);
      
      // Dynamic WS subscriptions for group / typing states
      if (groupSubscriptionRef.current) groupSubscriptionRef.current.unsubscribe();
      if (typingSubscriptionRef.current) typingSubscriptionRef.current.unsubscribe();

      if (activeConversation.isGroup) {
        groupSubscriptionRef.current = subscribeToGroup(activeConversation.id, dispatch);
      }
      typingSubscriptionRef.current = subscribeToTyping(activeConversation.id, dispatch);

      // Load draft message
      loadDraft(activeConversation.id, activeConversation.isGroup);
    }
  }, [activeConversation]);

  // Auto scroll to bottom
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeConversation]);

  // Load user threads
  const loadConversations = async () => {
    try {
      // Mock thread list: in production, we query historical chats and groups
      // For demo, search for all users as conversations
      const response = await apiFetch('/api/users/search?query=');
      if (response.ok) {
        const users = await response.json();
        // Filter out self
        const threads = users
          .filter(u => u.id !== user.id)
          .map(u => ({ id: u.id, name: u.username, isGroup: false, avatarUrl: u.avatarUrl, bio: u.bio }));
        dispatch(setConversations(threads));
      }
    } catch (err) {
      console.error('Failed to load conversations', err);
    }
  };

  // Load message history
  const loadMessageHistory = async (id, isGroup) => {
    const url = isGroup 
      ? `/api/groups/${id}/messages?page=0&size=50`
      : `/api/chats?recipientId=${id}&page=0&size=50`;
    try {
      const response = await apiFetch(url);
      if (response.ok) {
        const history = await response.json();
        // Backend returns oldest-first or newest-first. We display oldest-first, sorted in slice.
        dispatch(setMessages({ conversationId: id, messages: history }));
      }
    } catch (err) {
      console.error('Failed to load message history', err);
    }
  };

  // Handle typing triggers
  const handleTyping = (e) => {
    setMessageInput(e.target.value);

    if (activeConversation) {
      // Autosave draft in backend
      saveDraft(activeConversation.id, activeConversation.isGroup, e.target.value);

      publishTypingState(activeConversation.id, true);

      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = setTimeout(() => {
        publishTypingState(activeConversation.id, false);
      }, 2000);
    }
  };

  // Autosave Draft
  const saveDraft = async (recipientId, isGroup, content) => {
    try {
      await apiFetch('/api/chats/draft', {
        method: 'POST',
        body: JSON.stringify({ recipientId, isGroup, content }),
      });
    } catch (e) {
      console.error('Autosave draft failed', e);
    }
  };

  const loadDraft = async (recipientId, isGroup) => {
    try {
      const res = await apiFetch(`/api/chats/draft?recipientId=${recipientId}&isGroup=${isGroup}`);
      if (res.ok) {
        const draft = await res.text();
        setMessageInput(draft);
      }
    } catch (e) {
      console.error('Load draft failed', e);
    }
  };

  // Send Message
  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!messageInput.trim() || !activeConversation) return;

    const clientMsgId = 'MSG_' + UUID();

    if (showScheduler && scheduleTime) {
      // Message Scheduling (Send Later)
      scheduleMessage(clientMsgId);
      return;
    }

    if (activeConversation.isGroup) {
      publishGroupMessage(
        activeConversation.id, 
        messageInput, 
        clientMsgId, 
        'TEXT', 
        replyToMessage?.id
      );
    } else {
      publishPrivateMessage(
        activeConversation.id, 
        messageInput, 
        clientMsgId, 
        'TEXT', 
        replyToMessage?.id
      );
      
      // Optimitic local render
      dispatch(addMessage({
        conversationId: activeConversation.id,
        message: {
          id: clientMsgId,
          clientMessageId: clientMsgId,
          senderId: user.id,
          receiverId: activeConversation.id,
          message: messageInput,
          type: 'TEXT',
          sequenceNumber: 9999, // fallback before lock sequence assignment
          replyToId: replyToMessage?.id,
          createdAt: new Date().toISOString()
        }
      }));
    }

    setMessageInput('');
    setReplyToMessage(null);
  };

  // Schedule Message
  const scheduleMessage = async (clientMsgId) => {
    try {
      const response = await apiFetch('/api/chats/schedule', {
        method: 'POST',
        body: JSON.stringify({
          recipientId: activeConversation.id,
          isGroup: activeConversation.isGroup,
          message: messageInput,
          scheduledTime: scheduleTime,
        }),
      });

      if (response.ok) {
        alert('Message scheduled successfully for ' + new Date(scheduleTime).toLocaleString());
        setMessageInput('');
        setShowScheduler(false);
        setScheduleTime('');
      } else {
        alert('Failed to schedule message');
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Handle file uploads
  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file || !activeConversation) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      const uploadRes = await fetch('/api/media/upload', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${accessToken}` },
        body: formData
      });

      if (uploadRes.ok) {
        const result = await uploadRes.json();
        const clientMsgId = 'MSG_' + UUID();

        // Send the uploaded attachment link as a message
        if (activeConversation.isGroup) {
          publishGroupMessage(activeConversation.id, result.url, clientMsgId, 'ATTACHMENT');
        } else {
          publishPrivateMessage(activeConversation.id, result.url, clientMsgId, 'ATTACHMENT');
        }
      } else {
        alert('File upload failed (exceeds size limit or invalid type)');
      }
    } catch (err) {
      console.error('File upload error', err);
    }
  };

  // Voice note recorder
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      const chunks = [];

      mediaRecorder.ondataavailable = (e) => chunks.push(e.data);
      mediaRecorder.onstop = async () => {
        const blob = new Blob(chunks, { type: 'audio/webm' });
        const file = new File([blob], 'voice_note.webm', { type: 'audio/webm' });
        const formData = new FormData();
        formData.append('file', file);

        const uploadRes = await fetch('/api/media/upload', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${accessToken}` },
          body: formData
        });

        if (uploadRes.ok) {
          const result = await uploadRes.json();
          const clientMsgId = 'MSG_' + UUID();
          publishPrivateMessage(activeConversation.id, result.url, clientMsgId, 'AUDIO');
        }
      };

      mediaRecorder.start();
      setIsRecording(true);
    } catch (e) {
      console.error('Audio recording failed', e);
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
    }
  };

  // Block User
  const handleBlockUser = async () => {
    try {
      await apiFetch(`/api/chats/block/${activeConversation.id}`, { method: 'POST' });
      loadBlockedUsers();
      alert('User blocked');
    } catch (e) {
      console.error(e);
    }
  };

  const loadBlockedUsers = async () => {
    try {
      const res = await apiFetch('/api/chats/blocked');
      if (res.ok) {
        const list = await res.json();
        dispatch(setBlockedUsers(list));
      }
    } catch (e) {
      console.error(e);
    }
  };

  // Export Chat History
  const handleExportChat = async () => {
    try {
      const res = await apiFetch(`/api/chats/export?recipientId=${activeConversation.id}`);
      if (res.ok) {
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `chat_${activeConversation.name}.txt`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      }
    } catch (e) {
      console.error(e);
    }
  };

  // Report User
  const handleReportUser = async () => {
    const reason = prompt('Reason for reporting this user:');
    if (!reason) return;
    try {
      await apiFetch('/api/chats/report', {
        method: 'POST',
        body: JSON.stringify({ reportedUserId: activeConversation.id, reason }),
      });
      alert('User reported successfully.');
    } catch (e) {
      console.error(e);
    }
  };

  // Load device sessions
  const loadDeviceSessions = async () => {
    try {
      const res = await apiFetch('/api/auth/sessions');
      if (res.ok) {
        const data = await res.json();
        setDeviceSessions(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleRevokeSession = async (sid) => {
    try {
      await apiFetch(`/api/auth/sessions/${sid}`, { method: 'DELETE' });
      loadDeviceSessions();
    } catch (e) {
      console.error(e);
    }
  };

  // Load Admin reports & audits
  const loadAdminData = async () => {
    try {
      const reportsRes = await apiFetch('/api/audit/logs'); // Audit logs
      if (reportsRes.ok) {
        const logs = await reportsRes.json();
        setAdminAuditLogs(logs);
      }
      const statsRes = await apiFetch('/api/analytics/stats'); // Real-time analytics
      if (statsRes.ok) {
        const stats = await statsRes.json();
        setAnalytics(stats);
      }
    } catch (e) {
      console.error(e);
    }
  };

  // Global user search (Postgres ILIKE full-text fallback)
  const handleUserSearch = async (val) => {
    setSearchQuery(val);
    if (!val.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      const res = await apiFetch(`/api/users/search?query=${val}`);
      if (res.ok) {
        const data = await res.json();
        setSearchResults(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const startNewConversation = (u) => {
    const thread = { id: u.id, name: u.username, isGroup: false, avatarUrl: u.avatarUrl };
    // Add to local list if not there
    loadConversations();
    dispatch(setActiveConversation(thread));
    setSearchQuery('');
    setSearchResults([]);
  };

  // Create Group
  const [newGroupName, setNewGroupName] = useState('');
  const [newGroupDesc, setNewGroupDesc] = useState('');
  const handleCreateGroup = async (e) => {
    e.preventDefault();
    try {
      const res = await apiFetch('/api/groups', {
        method: 'POST',
        body: JSON.stringify({ name: newGroupName, description: newGroupDesc }),
      });
      if (res.ok) {
        const group = await res.json();
        const thread = { id: group.id, name: group.name, isGroup: true };
        loadConversations();
        dispatch(setActiveConversation(thread));
        setShowCreateGroupModal(false);
        setNewGroupName('');
        setNewGroupDesc('');
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleLogout = async () => {
    await apiFetch('/api/auth/logout', { method: 'POST' });
    dispatch(clearAuth());
    onNavigate('login');
  };

  // Helper UUID generator
  function UUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  // Format timestamp helper
  const formatTime = (isoString) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="dashboard-container">
      {/* Sidebar 1: Far Left Icon Bar */}
      <div className="icon-sidebar">
        <div className="sidebar-logo">CSX</div>
        
        <div className="sidebar-menu">
          <button 
            className={`menu-icon-btn ${sidebarTab === 'chats' ? 'active' : ''}`}
            onClick={() => setSidebarTab('chats')}
            title="Private Chats"
          >
            <MessageSquare size={20} />
          </button>
          
          <button 
            className={`menu-icon-btn ${sidebarTab === 'groups' ? 'active' : ''}`}
            onClick={() => { setSidebarTab('groups'); loadConversations(); }}
            title="Groups"
          >
            <Users size={20} />
          </button>
          
          <button 
            className={`menu-icon-btn ${sidebarTab === 'settings' ? 'active' : ''}`}
            onClick={() => { setSidebarTab('settings'); loadDeviceSessions(); }}
            title="Device Sessions & Settings"
          >
            <Settings size={20} />
          </button>

          {user?.username === 'admin' && (
            <button 
              className={`menu-icon-btn ${sidebarTab === 'admin' ? 'active' : ''}`}
              onClick={() => { setSidebarTab('admin'); loadAdminData(); }}
              title="Admin Moderation"
            >
              <ShieldCheck size={20} />
            </button>
          )}
        </div>

        <button className="menu-logout-btn" onClick={handleLogout} title="Log Out">
          <LogOut size={20} />
        </button>
      </div>

      {/* Sidebar 2: Thread List & Action hub */}
      <div className="list-sidebar">
        {/* Search */}
        <div className="search-bar-container">
          <div className="search-input-wrapper">
            <Search size={16} className="search-icon" />
            <input 
              type="text" 
              placeholder="Search users globally..." 
              value={searchQuery}
              onChange={e => handleUserSearch(e.target.value)}
            />
          </div>
          <button 
            className="create-group-btn-icon" 
            onClick={() => setShowCreateGroupModal(true)} 
            title="Create Group"
          >
            <Plus size={16} />
          </button>
        </div>

        {/* Global Search Results overlay */}
        {searchResults.length > 0 && (
          <div className="search-results-overlay">
            {searchResults.map(u => (
              <div 
                key={u.id} 
                className="search-result-item"
                onClick={() => startNewConversation(u)}
              >
                <div className="user-avatar-placeholder">
                  {u.username.substring(0, 2).toUpperCase()}
                </div>
                <div className="user-details">
                  <div className="user-name">{u.username}</div>
                  <div className="user-bio">{u.bio || 'Available'}</div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Dynamic Lists */}
        {sidebarTab === 'chats' && (
          <div className="threads-list">
            <h2 className="list-title">Conversations</h2>
            {conversations.filter(c => !c.isGroup).map(c => (
              <div 
                key={c.id} 
                className={`thread-item ${activeConversation?.id === c.id ? 'active' : ''}`}
                onClick={() => dispatch(setActiveConversation(c))}
              >
                <div className="user-avatar-placeholder">
                  {c.name.substring(0, 2).toUpperCase()}
                  <span className={`status-badge ${onlineUsers[c.id] === 'ONLINE' ? 'online' : 'offline'}`} />
                </div>
                <div className="thread-info">
                  <div className="thread-name-row">
                    <span className="thread-name">{c.name}</span>
                  </div>
                  <div className="thread-preview">
                    {typingStates[c.id]?.length > 0 ? (
                      <span className="typing-text">typing...</span>
                    ) : (
                      <span>{c.bio || 'Tap to chat'}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {sidebarTab === 'groups' && (
          <div className="threads-list">
            <div className="list-title-row">
              <h2 className="list-title">Active Groups</h2>
            </div>
            {conversations.filter(c => c.isGroup).map(c => (
              <div 
                key={c.id} 
                className={`thread-item ${activeConversation?.id === c.id ? 'active' : ''}`}
                onClick={() => dispatch(setActiveConversation(c))}
              >
                <div className="user-avatar-placeholder group">
                  GP
                </div>
                <div className="thread-info">
                  <span className="thread-name">{c.name}</span>
                  <span className="thread-preview">{c.description || 'No description'}</span>
                </div>
              </div>
            ))}
          </div>
        )}

        {sidebarTab === 'settings' && (
          <div className="settings-panel">
            <h2 className="list-title">Settings</h2>
            
            <div className="user-settings-summary">
              <div className="settings-avatar">
                {user?.username.substring(0, 2).toUpperCase()}
              </div>
              <div className="settings-user-info">
                <h3>{user?.username}</h3>
                <p>{user?.email}</p>
              </div>
            </div>

            <div className="sessions-list-container">
              <h4 className="section-title">Active Device Sessions</h4>
              {deviceSessions.map(s => (
                <div key={s.id} className="device-session-item">
                  <div className="device-details">
                    <div className="device-title-row">
                      <Monitor size={14} />
                      <span className="device-name">{s.deviceName}</span>
                    </div>
                    <span className="device-ip">{s.ipAddress} • {s.location}</span>
                  </div>
                  <button 
                    className="session-revoke-btn" 
                    onClick={() => handleRevokeSession(s.id)}
                    title="Revoke session"
                  >
                    Revoke
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {sidebarTab === 'admin' && (
          <div className="settings-panel">
            <h2 className="list-title">Moderation Center</h2>
            
            {analytics && (
              <div className="analytics-summary">
                <h4 className="section-title">System Metrics</h4>
                <div className="metrics-grid">
                  <div className="metric-box">
                    <span className="metric-num">{analytics.totalMessages}</span>
                    <span className="metric-label">Messages</span>
                  </div>
                  <div className="metric-box">
                    <span className="metric-num">{analytics.totalGroups}</span>
                    <span className="metric-label">Groups</span>
                  </div>
                  <div className="metric-box">
                    <span className="metric-num">{analytics.totalLogins}</span>
                    <span className="metric-label">Logins</span>
                  </div>
                  <div className="metric-box">
                    <span className="metric-num">{analytics.messagesPerMinute}</span>
                    <span className="metric-label">MPM</span>
                  </div>
                </div>
              </div>
            )}

            <div className="sessions-list-container">
              <h4 className="section-title">Security & Audit Trails</h4>
              <div className="audit-logs-list">
                {adminAuditLogs.slice(0, 10).map(log => (
                  <div key={log.id} className="audit-log-item">
                    <div className="audit-header">
                      <span className="audit-action">{log.action}</span>
                      <span className="audit-time">{formatTime(log.createdAt)}</span>
                    </div>
                    <p className="audit-details">{log.details || 'No details'}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Center Panel: Messaging View */}
      <div className="chat-center-panel">
        {activeConversation ? (
          <>
            {/* Chat Header */}
            <div className="chat-header">
              <div className="header-info">
                <h3>{activeConversation.name}</h3>
                {!activeConversation.isGroup && (
                  <span className="status-text">
                    {onlineUsers[activeConversation.id] === 'ONLINE' ? 'Online' : 'Offline'}
                  </span>
                )}
              </div>
              <div className="header-actions">
                <button onClick={handleExportChat} title="Export Chat"><Download size={18} /></button>
                <button onClick={handleBlockUser} title="Block User" className={blockedUsers.includes(activeConversation.id) ? 'blocked' : ''}><Lock size={18} /></button>
                <button onClick={handleReportUser} title="Report User"><ShieldAlert size={18} /></button>
              </div>
            </div>

            {/* Messages Area */}
            <div className="messages-viewport">
              {(messages[activeConversation.id] || []).map((msg) => (
                <div 
                  key={msg.id} 
                  className={`message-bubble-wrapper ${msg.senderId === user.id ? 'sent' : 'received'}`}
                >
                  <div className="message-bubble">
                    {msg.replyToId && (
                      <div className="reply-quote-preview">
                        Quote message: {msg.replyToId.substring(0, 8)}...
                      </div>
                    )}

                    {msg.isDeleted ? (
                      <span className="deleted-message-text">This message was deleted.</span>
                    ) : (
                      <>
                        {msg.type === 'TEXT' && <p>{msg.message}</p>}
                        {msg.type === 'ATTACHMENT' && (
                          <div className="attachment-box">
                            <Paperclip size={16} />
                            <a href={msg.message} target="_blank" rel="noopener noreferrer" className="attachment-link">
                              Attachment File ({msg.message.substring(msg.message.lastIndexOf('/') + 1)})
                            </a>
                          </div>
                        )}
                        {msg.type === 'AUDIO' && (
                          <div className="audio-box">
                            <Play size={16} />
                            <audio src={msg.message} controls className="audio-player" />
                          </div>
                        )}
                      </>
                    )}

                    <div className="message-footer-row">
                      <span className="msg-time">{formatTime(msg.createdAt)}</span>
                      {msg.senderId === user.id && (
                        <span className="receipt-check">
                          <CheckCheck size={14} className="delivered" />
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="message-hover-actions">
                    <button onClick={() => setReplyToMessage(msg)} title="Reply">Reply</button>
                    {msg.senderId === user.id && !msg.isDeleted && (
                      <button onClick={() => chatService.deleteMessage(msg.id, user.id)} title="Delete">Delete</button>
                    )}
                  </div>
                </div>
              ))}
              <div ref={messageEndRef} />
            </div>

            {/* Reply bar indicator */}
            {replyToMessage && (
              <div className="reply-indicator-bar">
                <span>Replying to: <strong>{replyToMessage.message}</strong></span>
                <button onClick={() => setReplyToMessage(null)}><X size={14} /></button>
              </div>
            )}

            {/* Input Bar */}
            <form onSubmit={handleSendMessage} className="chat-input-bar">
              <div className="input-attachments">
                <label className="attachment-upload-label">
                  <Paperclip size={18} />
                  <input type="file" onChange={handleFileUpload} style={{ display: 'none' }} />
                </label>
                
                <button 
                  type="button" 
                  className={`audio-rec-btn ${isRecording ? 'recording' : ''}`}
                  onMouseDown={startRecording}
                  onMouseUp={stopRecording}
                  title="Hold to record voice note"
                >
                  <Mic size={18} />
                </button>
              </div>

              <div className="main-text-input-wrapper">
                <input 
                  type="text" 
                  placeholder={isRecording ? 'Recording voice note...' : 'Type a message...'}
                  value={messageInput}
                  onChange={handleTyping}
                  disabled={isRecording}
                />
              </div>

              <div className="input-send-actions">
                <button 
                  type="button" 
                  className={`schedule-trigger-btn ${showScheduler ? 'active' : ''}`}
                  onClick={() => setShowScheduler(!showScheduler)}
                  title="Schedule send"
                >
                  <Calendar size={18} />
                </button>
                <button type="submit" className="send-btn"><Send size={18} /></button>
              </div>
            </form>

            {/* Scheduler Panel overlay */}
            {showScheduler && (
              <div className="scheduler-popover">
                <label>Deliver at:</label>
                <input 
                  type="datetime-local" 
                  value={scheduleTime} 
                  onChange={e => setScheduleTime(e.target.value)} 
                />
                <button type="button" onClick={() => setShowScheduler(false)} className="scheduler-close">
                  Confirm
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="empty-chat-viewport">
            <MessageSquare size={48} className="empty-chat-icon" />
            <h2>Select a thread or search a user to begin messaging</h2>
            <p>Your connection is secured with end-to-end AES-128 databases encryption.</p>
          </div>
        )}
      </div>

      {/* Modal: Create Group */}
      {showCreateGroupModal && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Create Group Chat</h3>
              <button onClick={() => setShowCreateGroupModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateGroup} className="modal-form">
              <div className="form-group">
                <label>Group Name</label>
                <input 
                  type="text" 
                  required 
                  placeholder="e.g. Work Squad" 
                  value={newGroupName} 
                  onChange={e => setNewGroupName(e.target.value)} 
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea 
                  placeholder="Describe your group..." 
                  value={newGroupDesc} 
                  onChange={e => setNewGroupDesc(e.target.value)} 
                />
              </div>
              <button type="submit" className="btn-primary">Create Group</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
