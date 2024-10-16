# Upload Large File Using Chunk Compression Thread

### Reference Documentation
Update Excel file specific cell and export the file:
Run the project and `import` `curl` on `Postman`

```curl
curl -X POST "http://localhost:8080/upload/sftp" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/file.csv"
```
