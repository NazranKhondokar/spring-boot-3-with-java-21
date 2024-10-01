package com.nazran.excel.service;

import com.nazran.excel.util.CustomMultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadService.class);

    public String updateExcelFile(MultipartFile file, int sheetIndex, int rowIndex, int cellIndex, String newValue, String filePath) {
        // Convert MultipartFile to InputStream
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // Get the sheet, row, and cell
            Sheet sheet = workbook.getSheetAt(sheetIndex);

            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);  // Create the row if it doesn't exist
            }
            Cell cell = row.getCell(cellIndex);
            if (cell == null) {
                cell = row.createCell(cellIndex);  // Create the cell if it doesn't exist
            }

            // Update the cell value
            cell.setCellValue(newValue);

            // Write the updated workbook to a byte array
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                workbook.close();

                // Save the updated file to the specified location
                Path path = Paths.get(filePath);
                Files.write(path, outputStream.toByteArray(), StandardOpenOption.CREATE);

                // Convert the byte array back to a MultipartFile (if needed)
                new CustomMultipartFile(outputStream.toByteArray(), file.getOriginalFilename(), file.getContentType());
                logger.info("File updated");
                return "SUCCESS";
            } catch (Exception e) {
                logger.error(e.getMessage());
                return e.getMessage();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
}
