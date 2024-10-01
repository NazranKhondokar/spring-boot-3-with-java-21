package com.nazran.excel.controller;

import com.nazran.excel.service.ExcelUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/excel")
public class ExcelUploadController {

    @Autowired
    private ExcelUploadService excelUploadService;

    @RequestMapping(path = "/update", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public String updateExcel(@RequestPart("file") MultipartFile file,
                              @RequestParam(value = "sheetIndex", defaultValue = "0") int sheetIndex,
                              @RequestParam(value = "rowIndex", defaultValue = "0") int rowIndex,
                              @RequestParam(value = "cellIndex", defaultValue = "0") int cellIndex,
                              @RequestParam(value = "newValue") String newValue,
                              @RequestParam(value = "filePathToSave") String filePath) {

        String message = excelUploadService.updateExcelFile(file, sheetIndex, rowIndex, cellIndex, newValue, filePath);
        if (message.equals("SUCCESS")) return "File updated";
        else return message;
    }
}
