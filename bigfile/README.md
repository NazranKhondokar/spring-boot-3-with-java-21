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
