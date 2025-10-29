import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8000/ws/chat';

export class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, any> = new Map();

  connect(firebaseToken: string, onConnect?: () => void, onError?: (error: any) => void) {
    if (this.client?.active) {
      console.log('WebSocket already connected');
      return;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${firebaseToken}`,
      },
      debug: (str) => console.log('STOMP Debug:', str),
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
    });

    this.client.activate();
  }

  disconnect() {
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
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
    console.log(`Subscribed to: ${destination}`);
  }

  // Subscribe to typing indicators
  subscribeToTyping(conversationId: number, callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = `/topic/conversation/${conversationId}/typing`;
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
  }

  // Subscribe to read receipts
  subscribeToReadReceipts(conversationId: number, callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = `/topic/conversation/${conversationId}/read`;
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
  }

  // Subscribe to presence updates
  subscribeToPresence(callback: (data: any) => void) {
    if (!this.client?.active) return;

    const destination = '/topic/presence';
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
  }

  // Send message
  sendMessage(conversationId: number, content: string, messageType = 'TEXT') {
    if (!this.client?.active) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: '/app/chat/send',
      body: JSON.stringify({
        conversationId,
        content,
        messageType,
      }),
    });
  }

  // Send typing indicator
  sendTyping(conversationId: number, userId: number, isTyping: boolean) {
    if (!this.client?.active) return;

    this.client.publish({
      destination: '/app/chat/typing',
      body: JSON.stringify({
        conversationId,
        userId,
        isTyping,
      }),
    });
  }

  // Mark as read
  markAsRead(conversationId: number, messageId?: number) {
    if (!this.client?.active) return;

    this.client.publish({
      destination: '/app/chat/read',
      body: JSON.stringify({
        conversationId,
        messageId: messageId || null,
      }),
    });
  }

  // Update presence
  updatePresence(isOnline: boolean, deviceInfo = 'web') {
    if (!this.client?.active) return;

    const destination = isOnline ? '/app/presence/online' : '/app/presence/offline';
    this.client.publish({
      destination,
      body: JSON.stringify({ deviceInfo }),
    });
  }

  // Heartbeat
  startHeartbeat(intervalMs = 30000) {
    setInterval(() => {
      if (this.client?.active) {
        this.client.publish({
          destination: '/app/presence/heartbeat',
          body: JSON.stringify({ deviceInfo: 'web' }),
        });
      }
    }, intervalMs);
  }
}

export const wsService = new WebSocketService();
