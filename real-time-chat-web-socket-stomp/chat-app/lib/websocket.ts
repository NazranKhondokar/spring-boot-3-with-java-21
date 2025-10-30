import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8000/ws/chat';

export class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, any> = new Map();
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private firebaseUserId: string | null = null;

  connect(firebaseToken: string, firebaseUserId: string, onConnect?: () => void, onError?: (error: any) => void) {
    if (this.client?.active) {
      console.log('WebSocket already connected');
      return;
    }

    this.firebaseUserId = firebaseUserId;

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${firebaseToken}`,
      },
      debug: (str) => console.log('STOMP Debug:', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('✅ WebSocket connected');
        onConnect?.();
      },
      onStompError: (frame) => {
        console.error('❌ STOMP error:', frame);
        onError?.(frame);
      },
      onWebSocketError: (event) => {
        console.error('❌ WebSocket error:', event);
        onError?.(event);
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        if (this.heartbeatInterval) {
          clearInterval(this.heartbeatInterval);
          this.heartbeatInterval = null;
        }
      }
    });

    this.client.activate();
  }

  disconnect() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }

    if (this.client?.active) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      console.log('WebSocket disconnected');
    }
  }

  // Subscribe to conversation messages
  subscribeToConversation(conversationId: number, callback: (message: any) => void) {
    if (!this.client?.active) {
      console.error('WebSocket not connected');
      return;
    }

    const destination = `/topic/conversation/${conversationId}`;

    // Unsubscribe if already subscribed
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        console.log('📨 Received message:', data);
        callback(data);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log(`✅ Subscribed to: ${destination}`);
  }

  // Subscribe to typing indicators
  subscribeToTyping(conversationId: number, callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = `/topic/conversation/${conversationId}/typing`;

    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing typing indicator:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
  }

  // Subscribe to read receipts
  subscribeToReadReceipts(conversationId: number, callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = `/topic/conversation/${conversationId}/read`;

    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing read receipt:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
  }

  // Subscribe to presence updates
  subscribeToPresence(callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = '/topic/presence';

    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing presence update:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
  }

  // ✅ UPDATED: Send message with Firebase UID in path
  sendMessage(conversationId: number, content: string, messageType = 'TEXT') {
    if (!this.client?.active || !this.firebaseUserId) {
      console.error('WebSocket not connected or no Firebase UID');
      throw new Error('WebSocket not connected or no Firebase UID');
    }

    console.log('📤 Sending message:', { conversationId, content });

    this.client.publish({
      destination: `/app/chat/send/${this.firebaseUserId}`,
      body: JSON.stringify({
        conversationId,
        content,
        messageType,
      }),
    });
  }

  // ✅ UPDATED: Send typing with Firebase UID in path
  sendTyping(conversationId: number, isTyping: boolean) {
    if (!this.client?.active || !this.firebaseUserId) {
      console.log('⚠️ Cannot send typing - WebSocket not connected or no Firebase UID');
      return;
    }

    this.client.publish({
      destination: `/app/chat/typing/${this.firebaseUserId}`,
      body: JSON.stringify({
        conversationId,
        isTyping,
      }),
    });
  }

  // ✅ UPDATED: Mark as read with Firebase UID in path
  markAsRead(conversationId: number, messageId?: number) {
    if (!this.client?.active || !this.firebaseUserId) {
      console.log('⚠️ Cannot mark as read - WebSocket not connected or no Firebase UID');
      return;
    }

    this.client.publish({
      destination: `/app/chat/read/${this.firebaseUserId}`,
      body: JSON.stringify({
        conversationId,
        messageId: messageId || null,
      }),
    });
  }

  // ✅ UPDATED: Join conversation with Firebase UID in path
  joinConversation(conversationId: number) {
    if (!this.client?.active || !this.firebaseUserId) {
      console.log('⚠️ Cannot join conversation - WebSocket not connected or no Firebase UID');
      return;
    }

    this.client.publish({
      destination: `/app/chat/join/${this.firebaseUserId}`,
      body: JSON.stringify(conversationId),
    });
  }

  // ✅ UPDATED: Leave conversation with Firebase UID in path
  leaveConversation(conversationId: number) {
    if (!this.client?.active || !this.firebaseUserId) {
      console.log('⚠️ Cannot leave conversation - WebSocket not connected or no Firebase UID');
      return;
    }

    this.client.publish({
      destination: `/app/chat/leave/${this.firebaseUserId}`,
      body: JSON.stringify(conversationId),
    });
  }

  // ✅ ALREADY CORRECT: Update presence with Firebase UID in path
  updatePresence(isOnline: boolean, deviceInfo = 'web') {
    if (!this.client?.active || !this.firebaseUserId) {
      console.log('⚠️ Cannot update presence - WebSocket not connected or no Firebase UID');
      return;
    }

    const destination = isOnline
      ? `/app/presence/online/${this.firebaseUserId}`
      : `/app/presence/offline/${this.firebaseUserId}`;

    console.log(`📡 Updating presence: ${isOnline ? 'ONLINE' : 'OFFLINE'} - ${destination}`);

    this.client.publish({
      destination,
      body: JSON.stringify({ deviceInfo }),
    });
  }

  // ✅ ALREADY CORRECT: Heartbeat with Firebase UID in path
  startHeartbeat(intervalMs = 30000) {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    this.heartbeatInterval = setInterval(() => {
      if (this.client?.active && this.firebaseUserId) {
        console.log('💓 Sending heartbeat');
        this.client.publish({
          destination: `/app/presence/heartbeat/${this.firebaseUserId}`,
          body: JSON.stringify({ deviceInfo: 'web' }),
        });
      }
    }, intervalMs);
  }

  // Check if connected
  isConnected(): boolean {
    return this.client?.active || false;
  }
}

export const wsService = new WebSocketService();