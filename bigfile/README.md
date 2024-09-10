# Getting Started

### Encounter version issue
```bash
java.lang.UnsupportedClassVersionError: com/nazran/bigfile/BigfileApplication has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0
```
* Explanation:
  The error `java.lang.UnsupportedClassVersionError` indicates that our application (BigfileApplication) has been compiled using a newer version of Java (in this case, Java 21 with class file version 65.0), but we are attempting to run it on an older Java Runtime Environment (JRE) that supports only up to class file version 61.0, which corresponds to Java 17.


* Solution: If we are running the application from IntelliJ IDEA, make sure to
  - Set Project SDK to JDK 21 in `File` > `Project Structure` > **Project Settings**.
  - Set the JDK version in the Run/Debug Configurations to JDK 21.

### Use Streaming Approach
* Instead of reading the entire file into memory, stream the file in chunks. This prevents loading the whole file into memory and avoids `OutOfMemoryError`.
* Use `MultipartFile.getInputStream()` to process the file stream as it's uploaded.

### Increase Maximum Upload File Size
* By default, Spring Boot limits the file size for uploads.
* We need to increase the limit in the `application.properties` or `application.yml`
```properties
spring.servlet.multipart.max-file-size=600MB
spring.servlet.multipart.max-request-size=1200MB
```

### Limit Concurrent Uploads
* Control the number of concurrent uploads to prevent overloading the server by configuring the thread pool:
```properties
# Enable asynchronous processing, Limit the number of concurrent uploads
spring.task.execution.pool.max-size=8
```