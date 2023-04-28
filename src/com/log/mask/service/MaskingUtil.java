package com.log.mask.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;  
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.log.mask.model.Field2OutputVO;



public class MaskingUtil {
	
	private static final String PRODUCT_NAMES_FILE = "resources\\Pro-Sub.xlsx";
	private static final String FIELDS_NAMES_GROUP_1 = "resources\\fields\\Fields_group1.txt";
	private static final String FIELDS_NAMES_GROUP_2 = "resources\\fields\\Fields_group2.txt";
	private static final String FIELDS_NAMES_GROUP_3 = "resources\\fields\\Fields_group3.txt";

	private static final String MASK_VALUE = " XXXXXX ";
	
	public static Scanner logFileScanner = null;
	public static Scanner fileNameScanner = null;
	public static Scanner group1AttributeNameScanner = null;
	public static Scanner group2AttributeNameScanner = null;
	public static Scanner group3AttributeNameScanner = null;
	
	public static FileWriter fileWriter = null;
	public static PrintWriter printWriter = null;

	public static void main(String[] args) throws IOException {
		
	try {

		File dir = new File("resources\\");
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {
		     if (child.getName().startsWith("Group1_")) {
		    	String fileName = child.getName();
		 		fileWriter = new FileWriter("resources\\masked_files\\"+fileName+"_generated.txt");
				printWriter = new PrintWriter(fileWriter);

				//Mask Sensitive data elements of files provided
				scanAndMaskSensitiveText(fileName, printWriter);
				
		    	fileWriter.close();
		     }
		    }
		  }
			  
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally {
			logFileScanner.close();
			group1AttributeNameScanner.close();
			group2AttributeNameScanner.close();
			group3AttributeNameScanner.close();
		}
	}

	private static void scanAndMaskSensitiveText(String fileName, PrintWriter printWriter) throws FileNotFoundException, IOException {
		logFileScanner = new Scanner(new File("resources\\"+fileName));

		System.out.println("Masking Started for ######## "+ fileName);
		
		boolean clearLeftoverText = false;
		
		while (logFileScanner.hasNextLine()) {
			String logline = logFileScanner.nextLine();
			
			if(clearLeftoverText) {
				if(logline.indexOf("<") > 0) {
					String sensitiveText = logline.substring(0, logline.indexOf("<"));
					logline = logline.replace(sensitiveText, " XX ");
					clearLeftoverText = false;
				}
			}

			//Masking attributes from Fields_group1
			logline = maskDataElementsForFields_1List(logline);
			
			//Masking attributes from Fields_group2
			logline = maskDataElementsForFields_2List(logline);
			
			//Masking products values provided in Pro-Sub.xlsx
			logline = maskPredefindProducts(logline);
			
			//Masking attributes from Fields_group3
			Field2OutputVO field2OutputVO = maskDataElementsForFields_3List(logline);
			
			//Write line to the file
			printWriter.println(field2OutputVO.getLogLine());
			
			if(field2OutputVO.isPartialTextMask()) {
				clearLeftoverText = true;
				continue;
			}
		}
		System.out.println("Masking Completed ######## "+ fileName);
		System.out.println("Generated File ######## "+ fileName+"_generated.txt under resources folder");
		
	}


	private static String maskDataElementsForFields_1List(String logline) throws FileNotFoundException {
		
		List<String> group1AttribueNames = new ArrayList<>();
		group1AttributeNameScanner = new Scanner(new File(FIELDS_NAMES_GROUP_1));
		while (group1AttributeNameScanner.hasNextLine()) {
			String fieldName = group1AttributeNameScanner.nextLine();
			group1AttribueNames.add(fieldName.trim());
			
		}
		
		// Masking logs for Fields_group1
		for(String field : group1AttribueNames) { 
			if(logline.contains(field)) {
				int fieldEndIndex = 0;
				String sensitiveText = "0";
				int fieldStartIndex = logline.indexOf(field) + field.length();
				
				String textAfterFieldName = logline.substring(fieldStartIndex);
				if(textAfterFieldName.indexOf("n,") > 0) {
					fieldEndIndex = fieldStartIndex + textAfterFieldName.indexOf("n,") - 3;
					sensitiveText = logline.substring(fieldStartIndex, fieldEndIndex);
				}else {
					sensitiveText = logline.substring(fieldStartIndex);
				}
				
				logline = logline.replace(sensitiveText, MASK_VALUE);

			}
		}
		
		return logline;
	}
	
	private static String maskDataElementsForFields_2List(String logline) throws FileNotFoundException { //TODO
		
		List<String> group1AttribueNames = new ArrayList<>();
		group2AttributeNameScanner = new Scanner(new File(FIELDS_NAMES_GROUP_2));
		while (group2AttributeNameScanner.hasNextLine()) {
			String fieldName = group2AttributeNameScanner.nextLine();
			group1AttribueNames.add(fieldName.trim());
			
		}
		
		// Masking logs for Fields_group1
		for(String field : group1AttribueNames) { 
			if(logline.contains(field)) {
				int fieldEndIndex = 0;
				String sensitiveText = "0";
				int fieldStartIndex = logline.indexOf(field) + field.length();

				String textAfterFieldName = logline.substring(fieldStartIndex);
				if(textAfterFieldName.indexOf("<br>") > 0) {
					fieldEndIndex = fieldStartIndex + textAfterFieldName.indexOf("<br>");
					sensitiveText = logline.substring(fieldStartIndex, fieldEndIndex);
				}else {
					sensitiveText = logline.substring(fieldStartIndex);
				}

				
				logline = logline.replace(sensitiveText, MASK_VALUE);

			}
		}
		
		return logline;
	 }
	
	
	private static Field2OutputVO maskDataElementsForFields_3List(String logline) throws FileNotFoundException {
		Field2OutputVO field2OutputVO = new Field2OutputVO();
		
		List<String> group1AttribueNames = new ArrayList<>();
		group3AttributeNameScanner = new Scanner(new File(FIELDS_NAMES_GROUP_3));
		while (group3AttributeNameScanner.hasNextLine()) {
			String fieldName = group3AttributeNameScanner.nextLine();
			group1AttribueNames.add(fieldName.trim());
			
		}
		
		boolean parialTextPending = false;
		// Masking logs for Fields_group1
		for(String field : group1AttribueNames) { 
			String sensitiveText = "";
			if(logline.contains(field)) {
				int fieldStartIndex = logline.indexOf(field) + field.length();

				String textAfterFieldName = logline.substring(fieldStartIndex);
				int fieldEndIndex = 0;
				
				if(textAfterFieldName.indexOf("<") > 0) {
					fieldEndIndex = fieldStartIndex + textAfterFieldName.indexOf("<");
					sensitiveText = logline.substring(fieldStartIndex, fieldEndIndex);
					logline = logline.replace(sensitiveText, MASK_VALUE);
					field2OutputVO.setLogLine(logline);
				}else {
					sensitiveText = logline.substring(fieldStartIndex);
					logline = logline.replace(sensitiveText, MASK_VALUE);
					parialTextPending = true;
					field2OutputVO.setLogLine(logline);
					field2OutputVO.setPartialTextMask(parialTextPending);
					return field2OutputVO;
				}

			}else {
				field2OutputVO.setLogLine(logline);
			}
		}
		
		return field2OutputVO;
	}
	


	private static String maskPredefindProducts(String logline) throws FileNotFoundException, IOException {
		List<String> productNames = maskSubProducts();
		//Masking Configured product names
		 for (String productName : productNames) {
			 String sensitiveText = productName;

			 if(logline.indexOf(sensitiveText) > 0)
				 logline = logline.replace(sensitiveText, MASK_VALUE);
		}
		return logline;
	}
	
	private static List<String> maskSubProducts() throws FileNotFoundException, IOException {

		List<String> productNames = new ArrayList<>();
		
		String excelFilePath = PRODUCT_NAMES_FILE;
		FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
		Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = firstSheet.iterator();
         
        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            Iterator<Cell> cellIterator = nextRow.cellIterator();
             
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                 
                switch (cell.getCellType()) {
                    case STRING:
                        productNames.add(cell.getStringCellValue());
                        break;

                }
            }
        }
        
        workbook.close();
        inputStream.close();
        
        return productNames;
		
	}
	
}


