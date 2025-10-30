import axios, { AxiosInstance } from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor to attach token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('firebaseToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Add response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Token expired or invalid
          localStorage.removeItem('firebaseToken');
          window.location.href = '/';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth
  async login(idToken: string, deviceInfo: any) {
    const response = await this.api.post('/api/v1/auth/login', {
      idToken,
      deviceInfo: deviceInfo || { deviceType: 'WEB' },
    });
    return response.data;
  }

  async logout() {
    const response = await this.api.post('/api/v1/auth/logout');
    return response.data;
  }

  // ✅ FIXED: Conversations - backend expects only initialMessage
  async createConversation(initialMessage: string) {
    const response = await this.api.post('/api/v1/chat/conversations', {
      initialMessage,
    });
    return response.data;
  }

  async getConversations(page = 0, size = 20) {
    const response = await this.api.get('/api/v1/chat/conversations', {
      params: { page, size },
    });
    return response.data;
  }

  async getConversation(conversationId: number) {
    const response = await this.api.get(`/api/v1/chat/conversations/${conversationId}`);
    return response.data;
  }

  async getUnassignedConversations(page = 0, size = 20) {
    const response = await this.api.get('/api/v1/chat/conversations/unassigned', {
      params: { page, size },
    });
    return response.data;
  }

  async assignConversation(conversationId: number, superAdminId: number) {
    const response = await this.api.put('/api/v1/chat/conversations/assign', {
      conversationId,
      superAdminId,
    });
    return response.data;
  }

  async closeConversation(conversationId: number) {
    const response = await this.api.put(`/api/v1/chat/conversations/${conversationId}/close`);
    return response.data;
  }

  // Messages - Keep HTTP for fallback, but prefer WebSocket
  async sendMessage(conversationId: number, content: string, messageType = 'TEXT') {
    const response = await this.api.post('/api/v1/chat/messages', {
      conversationId,
      content,
      messageType,
    });
    return response.data;
  }

  async getMessages(conversationId: number, page = 0, size = 50) {
    const response = await this.api.get(`/api/v1/chat/conversations/${conversationId}/messages`, {
      params: { page, size },
    });
    return response.data;
  }

  async markAsRead(conversationId: number, messageId?: number) {
    const response = await this.api.put('/api/v1/chat/messages/read', {
      conversationId,
      messageId: messageId || null,
    });
    return response.data;
  }

  async sendAttachment(conversationId: number, file: File, caption?: string) {
    const formData = new FormData();
    formData.append('conversationId', conversationId.toString());
    formData.append('file', file);
    if (caption) formData.append('caption', caption);

    const response = await this.api.post('/api/v1/chat/messages/attachment', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  }

  // Stats
  async getTotalUnread() {
    const response = await this.api.get('/api/v1/chat/unread/total');
    return response.data;
  }

  async getConversationUnread(conversationId: number) {
    const response = await this.api.get(`/api/v1/chat/conversations/${conversationId}/unread`);
    return response.data;
  }

  async getChatStats() {
    const response = await this.api.get('/api/v1/chat/stats');
    return response.data;
  }

  // Search
  async searchConversations(searchTerm: string, page = 0, size = 20) {
    const response = await this.api.get('/api/v1/chat/conversations/search', {
      params: { searchTerm, page, size },
    });
    return response.data;
  }
}

export const apiService = new ApiService();