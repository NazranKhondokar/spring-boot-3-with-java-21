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
      // ✅ FIXED: Pass Firebase UID to WebSocket
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
            // Update user online status in conversations
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
          // Optionally show error to user
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
      setMessages(msgs.reverse()); // Reverse to show oldest first

      // Join conversation
      wsService.joinConversation(conversationId);

      // Subscribe to new messages
      wsService.subscribeToConversation(conversationId, (newMessage) => {
        console.log('📨 New message received:', newMessage);
        setMessages((prev) => [...prev, newMessage]);

        // Update conversation last message
        setConversations((prev) =>
          prev.map((conv) =>
            conv.id === conversationId
              ? { ...conv, lastMessage: newMessage, lastMessageAt: newMessage.createdAt }
              : conv
          )
        );
      });

      // Subscribe to typing indicators
      wsService.subscribeToTyping(conversationId, (data) => {
        // Don't show own typing indicator
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

      // Subscribe to read receipts
      wsService.subscribeToReadReceipts(conversationId, (data) => {
        console.log('Read receipt:', data);
        // Mark messages as read in UI
        if (data.userId !== userData?.id) {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id <= data.messageId ? { ...msg, readAt: data.timestamp } : msg
            )
          );
        }
      });

      // Mark as read via WebSocket
      wsService.markAsRead(conversationId);

      // Also mark via HTTP for persistence
      await apiService.markAsRead(conversationId);
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const handleSelectConversation = (conversation: any) => {
    // Leave previous conversation
    if (selectedConversation) {
      wsService.leaveConversation(selectedConversation.id);
    }

    setSelectedConversation(conversation);
    setMessages([]);
    setTypingUsers(new Set());
    loadMessages(conversation.id);
  };

  // ✅ FIXED: Send button handler
  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!messageInput.trim() || !selectedConversation || sending) {
      console.log('⚠️ Cannot send - empty message or no conversation selected or already sending');
      return;
    }

    const tempMessage = messageInput.trim();
    setMessageInput('');
    setSending(true);

    // Stop typing indicator
    if (isTyping) {
      wsService.sendTyping(selectedConversation.id, false);
      setIsTyping(false);
    }

    try {
      console.log('📤 Attempting to send message:', tempMessage);

      // Send via WebSocket for real-time delivery
      if (wsService.isConnected()) {
        console.log('✅ Sending via WebSocket');
        wsService.sendMessage(selectedConversation.id, tempMessage);

        // Note: The message will be added to the UI when we receive it back
        // from the WebSocket subscription in subscribeToConversation()
      } else {
        // Fallback to HTTP if WebSocket disconnected
        console.log('⚠️ WebSocket not connected, falling back to HTTP');
        const response = await apiService.sendMessage(
          selectedConversation.id,
          tempMessage
        );
        // Add to messages manually since WebSocket didn't broadcast
        setMessages((prev) => [...prev, response.data]);

        // Update conversation last message
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
      // Restore message input on error
      setMessageInput(tempMessage);
      alert('Failed to send message. Please try again.');
    } finally {
      setSending(false);
    }
  };

  const handleTyping = () => {
    if (!selectedConversation) return;

    // Send typing indicator
    if (!isTyping) {
      wsService.sendTyping(selectedConversation.id, true);
      setIsTyping(true);
    }

    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // Set timeout to stop typing indicator
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

      // Add new conversation to list
      setConversations((prev) => [response.data, ...prev]);

      // Select the new conversation
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
        <div className="p-4 border-b">
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-xl font-bold">Conversations</h2>
            <button
              onClick={handleLogout}
              className="text-sm text-red-600 hover:text-red-800"
            >
              Logout
            </button>
          </div>
          <button
            onClick={() => setShowNewConversation(true)}
            className="w-full bg-blue-500 text-white py-2 px-4 rounded-lg hover:bg-blue-600 transition"
          >
            + New Conversation
          </button>
        </div>

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
            <div className="bg-white p-4 border-b">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="font-semibold text-lg">
                    {selectedConversation.customer?.firstName}{' '}
                    {selectedConversation.customer?.lastName}
                  </h3>
                  <p className="text-sm text-gray-500">
                    {selectedConversation.status}
                  </p>
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
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.map((msg) => {
                const isOwnMessage = msg.senderId === userData?.id;

                return (
                  <div
                    key={msg.id}
                    className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                        isOwnMessage
                          ? 'bg-blue-500 text-white'
                          : 'bg-gray-200 text-gray-800'
                      }`}
                    >
                      {!isOwnMessage && (
                        <p className="text-sm font-semibold mb-1">
                          {msg.senderName}
                        </p>
                      )}
                      <p>{msg.content}</p>
                      <p className="text-xs mt-1 opacity-75">
                        {new Date(msg.createdAt).toLocaleTimeString()}
                        {msg.readAt && isOwnMessage && ' • Read'}
                      </p>
                    </div>
                  </div>
                );
              })}

              {/* Typing Indicator */}
              {typingUsers.size > 0 && (
                <div className="flex justify-start">
                  <div className="bg-gray-200 text-gray-800 px-4 py-2 rounded-lg">
                    <div className="flex space-x-1">
                      <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce"></div>
                      <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                      <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
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
                  className="flex-1 border rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  disabled={sending}
                />
                <button
                  type="submit"
                  disabled={!messageInput.trim() || sending}
                  className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {sending ? 'Sending...' : 'Send'}
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center text-gray-500">
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
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
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