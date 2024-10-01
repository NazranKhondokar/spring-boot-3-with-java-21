# Excel upload using `Apache Poi`

### Reference Documentation
Update Excel file specific cell and export the file:
Run the project and `import` `curl` on `Postman`

```curl
curl -X POST http://localhost:8080/api/v1/excel/update \
  -F "file=@path_to_your_excel_file.xlsx" \
  -F "sheetIndex=0" \
  -F "rowIndex=0" \
  -F "cellIndex=0" \
  -F "newValue=Updated Value"
  -F "filePathToSave=/home/nazran/Desktop/updatedExcel.xlsx"
```

