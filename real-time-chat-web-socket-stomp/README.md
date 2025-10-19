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