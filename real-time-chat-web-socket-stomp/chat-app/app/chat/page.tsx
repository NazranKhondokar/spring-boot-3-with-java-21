'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';
import { apiService } from '@/lib/api';
import { wsService } from '@/lib/websocket';
import { logout as firebaseLogout } from '@/lib/firebase';

export default function ChatPage() {
  const router = useRouter();
  const { user, firebaseToken, userData, logout: storeLogout } = useAuthStore();

  const [conversations, setConversations] = useState<any[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<any>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [typingUsers, setTypingUsers] = useState<Set<number>>(new Set());
  const [isTyping, setIsTyping] = useState(false);
  const [showNewConversation, setShowNewConversation] = useState(false);
  const [newConvMessage, setNewConvMessage] = useState('');

  const typingTimeoutRef = useRef<NodeJS.Timeout>();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!firebaseToken || !user) {
      router.push('/');
      return;
    }

    initializeChat();

    return () => {
      if (selectedConversation) {
        wsService.leaveConversation(selectedConversation.id);
      }
      wsService.disconnect();
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, [firebaseToken, user]);

  const initializeChat = async () => {
    try {
      const firebaseUserId = user?.uid;
      if (!firebaseUserId) {
        console.error('No Firebase UID available');
        return;
      }

      // Connect WebSocket
      wsService.connect(
        firebaseToken!,
        firebaseUserId,
        () => {
          console.log('✅ WebSocket connected');
          wsService.startHeartbeat();
          wsService.updatePresence(true);

          // Subscribe to presence updates
          wsService.subscribeToPresence((data) => {
            console.log('Presence update:', data);
            setConversations((prev) =>
              prev.map((conv) => {
                if (conv.customer?.id === data.userId || conv.admin?.id === data.userId) {
                  return {
                    ...conv,
                    participantOnline: data.isOnline,
                  };
                }
                return conv;
              })
            );
          });
        },
        (error) => {
          console.error('WebSocket error:', error);
        }
      );

      // Load conversations
      await loadConversations();
      setLoading(false);
    } catch (error) {
      console.error('Failed to initialize chat:', error);
      setLoading(false);
    }
  };

  const loadConversations = async () => {
    try {
      const response = await apiService.getConversations();
      setConversations(response.data.content || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }
  };

  const loadMessages = async (conversationId: number) => {
    try {
      const response = await apiService.getMessages(conversationId);
      const msgs = response.data.content || [];
      setMessages(msgs.reverse());

      wsService.joinConversation(conversationId);

      wsService.subscribeToConversation(conversationId, (newMessage) => {
        console.log('📨 New message received:', newMessage);
        setMessages((prev) => [...prev, newMessage]);

        setConversations((prev) =>
          prev.map((conv) =>
            conv.id === conversationId
              ? { ...conv, lastMessage: newMessage, lastMessageAt: newMessage.createdAt }
              : conv
          )
        );
      });

      wsService.subscribeToTyping(conversationId, (data) => {
        if (data.userId !== userData?.id) {
          if (data.isTyping) {
            setTypingUsers((prev) => new Set(prev).add(data.userId));
          } else {
            setTypingUsers((prev) => {
              const updated = new Set(prev);
              updated.delete(data.userId);
              return updated;
            });
          }
        }
      });

      wsService.subscribeToReadReceipts(conversationId, (data) => {
        console.log('Read receipt:', data);
        if (data.userId !== userData?.id) {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id <= data.messageId ? { ...msg, readAt: data.timestamp } : msg
            )
          );
        }
      });

      wsService.markAsRead(conversationId);
      await apiService.markAsRead(conversationId);
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const handleSelectConversation = (conversation: any) => {
    if (selectedConversation) {
      wsService.leaveConversation(selectedConversation.id);
    }

    setSelectedConversation(conversation);
    setMessages([]);
    setTypingUsers(new Set());
    loadMessages(conversation.id);
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!messageInput.trim() || !selectedConversation || sending) {
      return;
    }

    const tempMessage = messageInput.trim();
    setMessageInput('');
    setSending(true);

    if (isTyping) {
      wsService.sendTyping(selectedConversation.id, false);
      setIsTyping(false);
    }

    try {
      if (wsService.isConnected()) {
        wsService.sendMessage(selectedConversation.id, tempMessage);
      } else {
        const response = await apiService.sendMessage(
          selectedConversation.id,
          tempMessage
        );
        setMessages((prev) => [...prev, response.data]);

        setConversations((prev) =>
          prev.map((conv) =>
            conv.id === selectedConversation.id
              ? { ...conv, lastMessage: response.data, lastMessageAt: response.data.createdAt }
              : conv
          )
        );
      }
    } catch (error) {
      console.error('❌ Failed to send message:', error);
      setMessageInput(tempMessage);
      alert('Failed to send message. Please try again.');
    } finally {
      setSending(false);
    }
  };

  const handleTyping = () => {
    if (!selectedConversation) return;

    if (!isTyping) {
      wsService.sendTyping(selectedConversation.id, true);
      setIsTyping(true);
    }

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      wsService.sendTyping(selectedConversation.id, false);
      setIsTyping(false);
    }, 3000);
  };

  const handleCreateConversation = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newConvMessage.trim()) return;

    try {
      const response = await apiService.createConversation(newConvMessage);

      setConversations((prev) => [response.data, ...prev]);

      setShowNewConversation(false);
      setNewConvMessage('');
      handleSelectConversation(response.data);
    } catch (error) {
      console.error('Failed to create conversation:', error);
      alert('Failed to create conversation. Please try again.');
    }
  };

  const handleLogout = async () => {
    try {
      if (selectedConversation) {
        wsService.leaveConversation(selectedConversation.id);
      }
      wsService.updatePresence(false);
      wsService.disconnect();
      await apiService.logout();
      await firebaseLogout();
      storeLogout();
      router.push('/');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  // Get user initials for avatar
  const getUserInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  // Get current user's full name
  const currentUserName = userData?.firstName && userData?.lastName
    ? `${userData.firstName} ${userData.lastName}`
    : user?.email || 'User';

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading chat...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar - Conversations */}
      <div className="w-80 bg-white border-r flex flex-col">
        {/* User Profile Section */}
        <div className="p-4 border-b bg-gradient-to-r from-blue-500 to-blue-600">
          <div className="flex items-center space-x-3 mb-3">
            {/* Avatar */}
            <div className="w-12 h-12 rounded-full bg-white text-blue-600 flex items-center justify-center font-bold text-lg shadow-md">
              {getUserInitials(currentUserName)}
            </div>

            {/* User Info */}
            <div className="flex-1 text-white">
              <p className="font-semibold text-sm">{currentUserName}</p>
              <p className="text-xs opacity-90 truncate">{user?.uid}</p>
            </div>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              className="text-white hover:bg-white hover:bg-opacity-20 p-2 rounded-lg transition"
              title="Logout"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </button>
          </div>

          {/* New Conversation Button */}
          <button
            onClick={() => setShowNewConversation(true)}
            className="w-full bg-white text-blue-600 py-2 px-4 rounded-lg hover:bg-opacity-90 transition font-medium text-sm shadow-md"
          >
            + New Conversation
          </button>
        </div>

        {/* Conversations Header */}
        <div className="px-4 py-3 border-b bg-gray-50">
          <h2 className="text-lg font-bold text-gray-800">Messages</h2>
        </div>

        {/* Conversations List */}
        <div className="overflow-y-auto flex-1">
          {conversations.length === 0 ? (
            <div className="text-center text-gray-500 mt-8 px-4">
              <p className="mb-2">No conversations yet</p>
              <p className="text-sm">Click "New Conversation" to start chatting</p>
            </div>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => handleSelectConversation(conv)}
                className={`p-4 border-b cursor-pointer hover:bg-gray-50 transition ${
                  selectedConversation?.id === conv.id ? 'bg-blue-50 border-l-4 border-l-blue-500' : ''
                }`}
              >
                <div className="flex justify-between items-start mb-1">
                  <div className="flex items-center">
                    <p className="font-semibold">
                      {conv.customer?.firstName} {conv.customer?.lastName}
                    </p>
                    {conv.participantOnline && (
                      <span className="ml-2 w-2 h-2 bg-green-500 rounded-full"></span>
                    )}
                  </div>
                  {conv.unreadCount > 0 && (
                    <span className="bg-red-500 text-white text-xs rounded-full px-2 py-1">
                      {conv.unreadCount}
                    </span>
                  )}
                </div>
                <p className="text-sm text-gray-600 truncate">
                  {conv.lastMessage?.content || 'No messages yet'}
                </p>
                <div className="flex justify-between items-center mt-1">
                  <p className="text-xs text-gray-400">
                    {conv.lastMessageAt
                      ? new Date(conv.lastMessageAt).toLocaleString()
                      : ''}
                  </p>
                  <span
                    className={`text-xs px-2 py-1 rounded ${
                      conv.status === 'OPEN'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {conv.status}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {selectedConversation ? (
          <>
            {/* Chat Header */}
            <div className="bg-white p-4 border-b shadow-sm">
              <div className="flex justify-between items-center">
                <div className="flex items-center space-x-3">
                  {/* Participant Avatar */}
                  <div className="w-10 h-10 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 text-white flex items-center justify-center font-bold">
                    {getUserInitials(`${selectedConversation.customer?.firstName} ${selectedConversation.customer?.lastName}`)}
                  </div>

                  <div>
                    <h3 className="font-semibold text-lg">
                      {selectedConversation.customer?.firstName}{' '}
                      {selectedConversation.customer?.lastName}
                    </h3>
                    <p className="text-sm text-gray-500">
                      {selectedConversation.participantOnline ? (
                        <span className="text-green-600 flex items-center">
                          <span className="w-2 h-2 bg-green-500 rounded-full mr-1"></span>
                          Online
                        </span>
                      ) : (
                        <span>{selectedConversation.status}</span>
                      )}
                    </p>
                  </div>
                </div>
                {selectedConversation.status === 'OPEN' && (
                  <button
                    onClick={() => apiService.closeConversation(selectedConversation.id)}
                    className="text-sm text-gray-600 hover:text-gray-800 border px-3 py-1 rounded"
                  >
                    Close Conversation
                  </button>
                )}
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50">
              {messages.map((msg) => {
                const isOwnMessage = msg.senderId === userData?.id;

                return (
                  <div
                    key={msg.id}
                    className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'}`}
                  >
                    <div className={`flex items-end space-x-2 max-w-xs lg:max-w-md ${isOwnMessage ? 'flex-row-reverse space-x-reverse' : ''}`}>
                      {/* Avatar for other user */}
                      {!isOwnMessage && (
                        <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 text-white flex items-center justify-center font-bold text-xs flex-shrink-0">
                          {getUserInitials(msg.senderName || 'U')}
                        </div>
                      )}

                      {/* Message Bubble */}
                      <div
                        className={`px-4 py-2 rounded-2xl shadow-sm ${
                          isOwnMessage
                            ? 'bg-blue-500 text-white rounded-br-none'
                            : 'bg-white text-gray-800 rounded-bl-none'
                        }`}
                      >
                        {!isOwnMessage && (
                          <p className="text-xs font-semibold mb-1 opacity-75">
                            {msg.senderName}
                          </p>
                        )}
                        <p className="break-words">{msg.content}</p>
                        <p className={`text-xs mt-1 ${isOwnMessage ? 'text-blue-100' : 'text-gray-500'}`}>
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          {msg.readAt && isOwnMessage && (
                            <span className="ml-1">
                              <svg className="w-4 h-4 inline" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"/>
                                <path d="M12.707 5.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414-1.414l4-4a1 1 0 011.414 0z"/>
                              </svg>
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}

              {/* Typing Indicator */}
              {typingUsers.size > 0 && (
                <div className="flex justify-start">
                  <div className="flex items-end space-x-2">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 text-white flex items-center justify-center font-bold text-xs">
                      ...
                    </div>
                    <div className="bg-white text-gray-800 px-4 py-3 rounded-2xl rounded-bl-none shadow-sm">
                      <div className="flex space-x-1">
                        <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce"></div>
                        <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                        <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Message Input */}
            <form onSubmit={handleSendMessage} className="bg-white p-4 border-t">
              <div className="flex space-x-2">
                <input
                  type="text"
                  value={messageInput}
                  onChange={(e) => {
                    setMessageInput(e.target.value);
                    handleTyping();
                  }}
                  onKeyPress={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage(e);
                    }
                  }}
                  placeholder="Type a message..."
                  className="flex-1 border rounded-full px-6 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  disabled={sending}
                />
                <button
                  type="submit"
                  disabled={!messageInput.trim() || sending}
                  className="bg-blue-500 text-white px-6 py-3 rounded-full hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-md"
                >
                  {sending ? (
                    <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                  ) : (
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/>
                    </svg>
                  )}
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center text-gray-500 bg-gray-50">
            <div className="text-center">
              <svg
                className="w-24 h-24 mx-auto mb-4 text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                />
              </svg>
              <p className="text-lg">Select a conversation to start chatting</p>
            </div>
          </div>
        )}
      </div>

      {/* New Conversation Modal */}
      {showNewConversation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4 shadow-2xl">
            <h3 className="text-xl font-bold mb-4">Start New Conversation</h3>
            <form onSubmit={handleCreateConversation}>
              <textarea
                value={newConvMessage}
                onChange={(e) => setNewConvMessage(e.target.value)}
                placeholder="Type your first message..."
                className="w-full border rounded-lg px-4 py-2 h-32 focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
                autoFocus
              />
              <div className="flex space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowNewConversation(false);
                    setNewConvMessage('');
                  }}
                  className="flex-1 border border-gray-300 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-50 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!newConvMessage.trim()}
                  className="flex-1 bg-blue-500 text-white py-2 px-4 rounded-lg hover:bg-blue-600 transition disabled:opacity-50"
                >
                  Start Chat
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}