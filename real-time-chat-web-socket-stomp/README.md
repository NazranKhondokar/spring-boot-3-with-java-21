<div align="center">
  <h1>Realtime chat using web socket, STOMP</h1>
</div>

# Table of Contents

- [Install Java 17](#java-17-installation)
- [Install Git](#git-installation)
- [Configure Github](#clone-the-project-using-github-token)
- [Run & Check Swagger](#run-application-and-check-on-browser)
- [Docker Installation](#docker-installation)
- [PostgreSQL Installation](#postgresql-installation)
- [Run with `docker compose`](#run-application-using-docker-compose)

# Real-Time Chat System - Architecture & Implementation Guide

## System Overview

Real-time chat between customers and super_admins using WebSocket for bidirectional communication, with persistent storage and message delivery guarantees.

---

## Architecture Diagram

```
┌─────────────────┐                    ┌──────────────────┐
│   Next.js       │                    │   Spring Boot    │
│   Frontend      │◄──────WebSocket────►│   Backend        │
│   (Customer)    │                    │                  │
└─────────────────┘                    └──────────────────┘
        ▲                                        ▲
        │                                        │
        │ HTTP (REST)                            │
        │                                        │
        ▼                                        ▼
┌─────────────────┐                    ┌──────────────────┐
│   Next.js       │                    │  PostgreSQL      │
│   Admin Panel   │                    │  Database        │
│   (Super Admin) │                    │                  │
└─────────────────┘                    ├──────────────────┤
                                       │ Redis (Optional) │
                                       │ Message Queue    │
                                       └──────────────────┘
```

---

## Core Components

### 1. Database Schema

**Tables to Create:**

```sql
-- Conversations table
CREATE TABLE conversations (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES users(id),
    super_admin_id INT REFERENCES users(id),
    status ENUM ('OPEN', 'CLOSED', 'ASSIGNED') DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP
);

-- Messages table
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    conversation_id INT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id INT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    message_type ENUM ('TEXT', 'FILE', 'SYSTEM') DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Message attachments table
CREATE TABLE message_attachments (
    id SERIAL PRIMARY KEY,
    message_id INT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat status tracking (online/offline)
CREATE TABLE user_presence (
    user_id INT PRIMARY KEY REFERENCES users(id),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP,
    device_info VARCHAR(255)
);

-- Unread message count (for performance)
CREATE TABLE conversation_unread_count (
    conversation_id INT PRIMARY KEY REFERENCES conversations(id),
    user_id INT NOT NULL REFERENCES users(id),
    unread_count INT DEFAULT 0,
    UNIQUE(conversation_id, user_id)
);
```

---

## Backend Implementation Steps

### Step 1: Add WebSocket Dependencies

Update `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
implementation 'org.springframework.boot:spring-boot-starter-websocket-tomcat'
implementation 'org.springframework:spring-messaging'
implementation 'redis:redis-java:2.9.0' // For pub/sub
```

### Step 2: Create WebSocket Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint
        registry.addEndpoint("/api/v1/ws/chat")
                .setAllowedOrigins("http://localhost:3000", "https://consultinghub.xyz")
                .withSockJS();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use Redis for scalability (or simple in-memory broker for MVP)
        config.setApplicationDestinationPrefixes("/app")
              .enableSimpleBroker("/topic", "/queue");
    }
}
```

### Step 3: Create Entity Models

**Conversation Entity:**
- `id`, `customerId`, `superAdminId`
- `status` (OPEN, CLOSED, ASSIGNED)
- `createdAt`, `updatedAt`, `lastMessageAt`

**Message Entity:**
- `id`, `conversationId`, `senderId`
- `content`, `messageType` (TEXT, FILE, SYSTEM)
- `isRead`, `readAt`
- `createdAt`, `updatedAt`

**UserPresence Entity:**
- `userId`, `isOnline`, `lastSeen`

### Step 4: Create DTOs

```java
// Message DTO
@Data
public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private List<MessageAttachmentDto> attachments;
}

// Conversation DTO
@Data
public class ConversationDto {
    private Long id;
    private Long customerId;
    private Long superAdminId;
    private String status;
    private Long unreadCount;
    private MessageDto lastMessage;
    private LocalDateTime lastMessageAt;
}

// Incoming message from client
@Data
public class ChatMessageRequest {
    @NotBlank
    private String content;
    private MessageType messageType;
    private Long conversationId;
}

// Outgoing message to client
@Data
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private String status; // SENT, DELIVERED, READ
}
```

### Step 5: Create Repository Interfaces

```java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByCustomerIdAndSuperAdminId(Long customerId, Long adminId);
    List<Conversation> findByCustomerIdOrderByLastMessageAtDesc(Long customerId);
    List<Conversation> findBySuperAdminIdOrderByLastMessageAtDesc(Long adminId);
}

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
    Long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long userId);
}

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
    Optional<UserPresence> findByUserId(Long userId);
}
```

### Step 6: Implement Chat Service

**Core Responsibilities:**
- Create/retrieve conversations
- Save messages to database
- Manage read receipts
- Handle file uploads
- Track presence

### Step 7: WebSocket Message Handler

```java
@Component
@Slf4j
public class ChatMessageHandler {
    
    @MessageMapping("/chat/send")
    @SendToUser("/queue/messages")
    public ChatMessageResponse handleMessage(
            @Payload ChatMessageRequest message,
            Principal principal) {
        
        // 1. Authenticate sender
        // 2. Validate conversation ownership
        // 3. Save message to database
        // 4. Broadcast to recipient via STOMP
        // 5. Update unread count
        // 6. Return response
    }
    
    @MessageMapping("/chat/typing")
    public void handleTypingIndicator(
            @Payload TypingIndicator indicator,
            Principal principal) {
        // Broadcast typing status without persisting
    }
    
    @MessageMapping("/chat/read")
    public void handleReadReceipt(
            @Payload ReadReceiptDto receipt,
            Principal principal) {
        // Mark messages as read
        // Broadcast read status
    }
}
```

### Step 8: Implement Chat Controller (REST Endpoints)

```java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDto>> getConversations() {
        // Get conversations for current user
    }
    
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Page<MessageDto>> getConversationMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page) {
        // Get paginated messages for conversation
    }
    
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDto> createConversation(
            @RequestBody CreateConversationRequest request) {
        // Create new conversation with super_admin
    }
    
    @PutMapping("/conversations/{id}/close")
    public ResponseEntity<Void> closeConversation(@PathVariable Long id) {
        // Close conversation
    }
}
```

---

## Performance Optimization Strategies

### 1. Message Pagination
- Load last 20-50 messages initially
- Implement "load more" functionality
- Index `conversation_id` and `created_at` in messages table

### 2. Caching Layer
```yaml
# Redis configuration
spring:
  redis:
    host: localhost
    port: 6379
```

- Cache recent conversations (TTL: 5 minutes)
- Cache unread message counts
- Cache user presence

### 3. Database Indexing
```sql
CREATE INDEX idx_messages_conversation_created 
ON messages(conversation_id, created_at DESC);

CREATE INDEX idx_conversations_customer 
ON conversations(customer_id, last_message_at DESC);

CREATE INDEX idx_messages_read_status 
ON messages(conversation_id, is_read);
```

### 4. Connection Management
- Use Hikari connection pool (already configured)
- Set maximum WebSocket connections limit
- Implement heartbeat/ping-pong to detect stale connections

### 5. Message Queue (Optional - For Scalability)
```yaml
# For multiple server instances
spring:
  rabbitmq:
    host: localhost
    port: 5672
```

Use RabbitMQ or Kafka for:
- Reliable message delivery
- Scaling across multiple servers
- Decoupling message processing

### 6. Read Receipt Optimization
- Batch read receipt updates
- Use async processing for marking messages as read
- Don't persist every intermediate state

---

## Security Considerations

### 1. Authentication
- Validate Firebase token at WebSocket handshake
- Verify user ownership of conversations
- Use Principal from Spring Security

### 2. Authorization
- Customers can only chat with super_admins
- Customers can only see their own conversations
- Super_admins can see all assigned conversations

### 3. Message Validation
- Sanitize message content (XSS prevention)
- Validate file uploads (size, type)
- Rate limit messages per user (e.g., 10 msgs/second)

### 4. Data Privacy
- Encrypt sensitive data at rest (optional)
- Use HTTPS/WSS for all connections
- GDPR compliance: allow message deletion

---

## Implementation Sequence

1. **Database setup** - Create tables and indices
2. **Entity models** - Define JPA entities and DTOs
3. **Repositories** - Implement data access layer
4. **Core service** - ChatService with business logic
5. **WebSocket config** - Enable STOMP messaging
6. **Message handler** - Implement chat message routing
7. **REST controller** - History and metadata endpoints
8. **Presence management** - Track online/offline status
9. **Error handling** - Global exception handler for chat
10. **Testing** - Unit and integration tests
11. **Performance tuning** - Add caching and optimization
12. **Monitoring** - Logging and metrics

---

## Key File Structure

```
src/main/java/com/nexalinx/nexadoc/
├── chat/
│   ├── config/
│   │   └── WebSocketConfig.java
│   ├── controller/
│   │   └── ChatController.java
│   ├── entity/
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   └── UserPresence.java
│   ├── dto/
│   │   ├── ChatMessageRequest.java
│   │   ├── ChatMessageResponse.java
│   │   └── ConversationDto.java
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   ├── MessageRepository.java
│   │   └── UserPresenceRepository.java
│   ├── service/
│   │   ├── ChatService.java
│   │   └── MessageService.java
│   ├── handler/
│   │   └── ChatMessageHandler.java
│   └── exception/
│       └── ChatException.java
```

# Java 17 Installation
To install Java 17 on Ubuntu 24.04, you can follow these steps:

### 1. **Update the Package Index**
Open a terminal and run:
```bash
sudo apt update
```

### 2. **Install Java 17 from Ubuntu's Default Repository**
Ubuntu 24.04 includes OpenJDK in its default repositories. To install it, run:
```bash
sudo apt install openjdk-17-jdk -y
```

### 3. **Verify the Installation**
Check the installed Java version:
```bash
java -version
```
You should see output indicating that Java 17 is installed, like this:
```
openjdk version "17.x.x" ...
```

# Git Installation
To install Git on Ubuntu 24.04, follow these steps:

### **1. Install Git**
Install Git using the following command:
```bash
sudo apt install git -y
```

### **2. Verify the Installation**
Check if Git is installed and its version:
```bash
git --version
```
You should see output similar to:
```
git version 2.x.x
```

# Clone the project using github token

### Step 1: Create Token
1. Go to [Github Token](https://github.com/settings/tokens)
2. Create a token and copy that.

### Step 2: Clone the project

### Step 3: Configure `git`
```bash
  git config --global user.email <email>
  git config --global user.name <username>
```

### Step 4: Set remote with previous token
```bash
git remote set-url origin https://<username>:<token>@github.com/bduswork/bitcoinapps_backend.git
```

# Run application and check on browser

### Step 1: Run application

### Step 2: Check on browser
1. Go to [Open API docs for documentation](http://localhost:8082/swagger-ui/index.html#/)
2. If change the port then 
```bash
http://localhost:<port>/swagger-ui/index.html#/
```

# Docker Installation

### **Step 1: Install Required Dependencies**
Docker requires some dependencies to be installed:
```bash
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
```

### **Step 2: Add Docker's Official GPG Key**
Add Docker’s official GPG key for package verification:
```bash
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
```

### **Step 3: Add Docker’s APT Repository**
Add the Docker repository to your system:
```bash
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

### **Step 4: Update the Package List**
Refresh the package list to include Docker's repository:
```bash
sudo apt update
```

### **Step 5: Install Docker Engine**
Now, install Docker:
```bash
sudo apt install -y docker-ce docker-ce-cli containerd.io
```

### **Step 6: Verify Docker Installation**
Check if Docker is installed correctly:
```bash
docker --version
```

You can also test it by running the `hello-world` container:
```bash
sudo docker run hello-world
```

### **Step 7: Enable and Start Docker**
Ensure Docker starts on system boot:
```bash
sudo systemctl enable docker
sudo systemctl start docker
```

# PostgreSQL installation

### **Step 1: Pull the PostgreSQL Docker Image**
Download the latest PostgreSQL image from Docker Hub:
```bash
docker pull postgres
```

### **Step 2: Run a PostgreSQL Container**
Run the container with a specified name, username, and password:
```bash
sudo docker run --name postgres_container -e POSTGRES_USER=root -e POSTGRES_PASSWORD=password -e POSTGRES_DB=db -p 5432:5432 -d postgres
```

#### Explanation:
- `--name postgres_container`: Sets the container name.
- `-e POSTGRES_USER=root`: Creates a user named `root`.
- `-e POSTGRES_PASSWORD=password`: Sets the password for the user.
- `-e POSTGRES_DB=db`: Creates a database named `db`.
- `-p 5432:5432`: Maps the container's PostgreSQL port `5432` to the host's port `5432`.
- `-d`: Runs the container in detached mode.

### **Step 3: Verify the Container is Running**
Check if the PostgreSQL container is running:
```bash
sudo docker ps
```

You should see your `postgres_container` container listed.

### **Step 4: Access PostgreSQL**
You can access PostgreSQL in two ways:

#### 1. **Using `psql` in the Container**
Enter the running container:
```bash
docker exec -it postgres_container psql -U root -d password
```

#### 2. **Using a Database Client on the Host**
Use a PostgreSQL client (e.g., `psql`, DBeaver, or pgAdmin) to connect. Use the following credentials:
- **Host**: `localhost`
- **Port**: `5432`
- **Username**: `root`
- **Password**: `password`
- **Database**: `db`


### **Step 5: Stop and Remove the Container**
To stop the container:
```bash
sudo docker stop postgres_container
```

To remove the container:
```bash
sudo docker rm postgres_container
```

To remove the image (optional):
```bash
sudo docker rmi postgres
```

# Run application using docker compose

### **Step 1: Verify Prerequisites**
1. **Docker Compose**: Ensure Docker Compose are installed on your machine:
   ```bash
   sudo docker compose --version
   ```

2. **PostgreSQL Setup in Docker**: Since the `database` service in your `docker-compose.yml` is already configured, you don't need a separate PostgreSQL instance running on your host. Make sure it's not conflicting with the port `5432`.


### **Step 2: Build and Start the Docker Compose Services**
Navigate to the directory where your `docker-compose.yml` is located.

1. **Build the Services**:
   ```bash
   sudo docker compose build
   ```

2. **Start the Services**:
   ```bash
   sudo docker compose up -d
   ```
    - `-d` runs the containers in detached mode.

3. **Verify the Services are Running**:
   ```bash
   sudo docker compose ps
   ```
   This will list the running containers with their status.

### **Step 3: Verify the Backend and Database**
1. **Access PostgreSQL**:
   Connect to the database container to confirm it is running:
   ```bash
   docker exec -it postgres_container psql -U root -d db
   ```
   You can run a test query, such as:
   ```sql
   \dt
   ```
   (This lists tables in the database.)

### **Step 4: Test Backend Accessibility**
1. Open your browser or use a tool like `curl` or Postman to access the backend at:
   ```bash
   http://localhost:8082/swagger-ui/index.html#/
   ```
   Ensure the backend responds as expected.

2. **Environment Variables**:
   Confirm the backend application is correctly using the provided `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to connect to the `database` service.

### **Step 5: Stop the Services**
To stop and remove the containers when you're done:
```bash
sudo docker compose down
```