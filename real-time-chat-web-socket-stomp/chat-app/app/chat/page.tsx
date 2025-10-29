'use client';

import { useEffect, useState } from 'react';
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
  const [typingUsers, setTypingUsers] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (!firebaseToken) {
      router.push('/');
      return;
    }

    initializeChat();

    return () => {
      wsService.disconnect();
    };
  }, [firebaseToken]);

  const initializeChat = async () => {
    try {
      // Connect WebSocket
      wsService.connect(
        firebaseToken!,
        () => {
          console.log('✅ WebSocket connected');
          wsService.startHeartbeat();
          wsService.updatePresence(true);

          // Subscribe to presence updates
          wsService.subscribeToPresence((data) => {
            console.log('Presence update:', data);
          });
        },
        (error) => console.error('WebSocket error:', error)
      );

      // Load conversations
      await loadConversations();
      setLoading(false);
    } catch (error) {
      console.error('Failed to initialize chat:', error);
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
      setMessages(response.data.content || []);

      // Subscribe to conversation
      wsService.subscribeToConversation(conversationId, (newMessage) => {
        console.log('New message received:', newMessage);
        setMessages((prev) => [newMessage, ...prev]);
      });

      wsService.subscribeToTyping(conversationId, (data) => {
        if (data.isTyping) {
          setTypingUsers((prev) => new Set(prev).add(data.userId));
        } else {
          setTypingUsers((prev) => {
            const updated = new Set(prev);
            updated.delete(data.userId);
            return updated;
          });
        }
      });

      // Mark as read
      await apiService.markAsRead(conversationId);
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const handleSelectConversation = (conversation: any) => {
    setSelectedConversation(conversation);
    loadMessages(conversation.id);
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!messageInput.trim() || !selectedConversation) return;

    try {
      const response = await apiService.sendMessage(
        selectedConversation.id,
        messageInput
      );

      setMessages((prev) => [response.data, ...prev]);
      setMessageInput('');
    } catch (error) {
      console.error('Failed to send message:', error);
    }
  };

  const handleTyping = () => {
    if (selectedConversation && userData) {
      wsService.sendTyping(selectedConversation.id, userData.id, true);

      setTimeout(() => {
        wsService.sendTyping(selectedConversation.id, userData.id, false);
      }, 3000);
    }
  };

  const handleLogout = async () => {
    try {
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
      <div className="min-h-screen flex items-center justify-center">
        <p>Loading chat...</p>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar - Conversations */}
      <div className="w-80 bg-white border-r flex flex-col">
        <div className="p-4 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold">Conversations</h2>
          <button
            onClick={handleLogout}
            className="text-sm text-red-600 hover:text-red-800"
          >
            Logout
          </button>
        </div>

        <div className="overflow-y-auto flex-1">
          {conversations.length === 0 ? (
            <p className="text-center text-gray-500 mt-4">No conversations yet</p>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => handleSelectConversation(conv)}
                className={`p-4 border-b cursor-pointer hover:bg-gray-50 ${
                  selectedConversation?.id === conv.id ? 'bg-blue-50' : ''
                }`}
              >
                <div className="flex justify-between">
                  <p className="font-semibold">
                    {conv.customer?.firstName} {conv.customer?.lastName}
                  </p>
                  {conv.unreadCount > 0 && (
                    <span className="bg-red-500 text-white text-xs rounded-full px-2 py-1">
                      {conv.unreadCount}
                    </span>
                  )}
                </div>
                <p className="text-sm text-gray-600 truncate">
                  {conv.lastMessage?.content}
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  {new Date(conv.lastMessageAt).toLocaleString()}
                </p>
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
              <h3 className="font-semibold text-lg">
                {selectedConversation.customer?.firstName}{' '}
                {selectedConversation.customer?.lastName}
              </h3>
              <p className="text-sm text-gray-500">
                {selectedConversation.status}
              </p>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 flex flex-col-reverse">
              {typingUsers.size > 0 && (
                <div className="text-sm text-gray-500 italic">
                  User is typing...
                </div>
              )}

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
                      <p className="text-sm font-semibold mb-1">
                        {msg.senderName}
                      </p>
                      <p>{msg.content}</p>
                      <p className="text-xs mt-1 opacity-75">
                        {new Date(msg.createdAt).toLocaleTimeString()}
                      </p>
                    </div>
                  </div>
                );
              })}
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
                  placeholder="Type a message..."
                  className="flex-1 border rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  type="submit"
                  className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition"
                >
                  Send
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center text-gray-500">
            Select a conversation to start chatting
          </div>
        )}
      </div>
    </div>
  );
}