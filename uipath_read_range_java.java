import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Custom DataTable class that mimics UiPath's System.Data.DataTable behavior
 * Supports column-based access and maintains row/column structure
 */
class DataTable {
    private List<String> columnNames;
    private List<Map<String, Object>> rows;
    private Map<String, Integer> columnIndexMap;
    
    public DataTable() {
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.columnIndexMap = new HashMap<>();
    }
    
    public DataTable(List<String> columnNames) {
        this();
        setColumnNames(columnNames);
    }
    
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = new ArrayList<>(columnNames);
        this.columnIndexMap.clear();
        for (int i = 0; i < columnNames.size(); i++) {
            this.columnIndexMap.put(columnNames.get(i), i);
        }
    }
    
    public void addRow(Map<String, Object> rowData) {
        rows.add(new HashMap<>(rowData));
    }
    
    public void addRow(List<Object> rowValues) {
        if (rowValues.size() != columnNames.size()) {
            throw new IllegalArgumentException("Row values count doesn't match column count");
        }
        
        Map<String, Object> rowData = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            rowData.put(columnNames.get(i), rowValues.get(i));
        }
        addRow(rowData);
    }
    
    public Object getValue(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Row index out of bounds");
        }
        return rows.get(rowIndex).get(columnName);
    }
    
    public Object getValue(int rowIndex, int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnNames.size()) {
            throw new IndexOutOfBoundsException("Column index out of bounds");
        }
        return getValue(rowIndex, columnNames.get(columnIndex));
    }
    
    public Map<String, Object> getRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Row index out of bounds");
        }
        return new HashMap<>(rows.get(rowIndex));
    }
    
    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }
    
    public int getRowCount() {
        return rows.size();
    }
    
    public int getColumnCount() {
        return columnNames.size();
    }
    
    public boolean hasColumn(String columnName) {
        return columnIndexMap.containsKey(columnName);
    }
    
    // UiPath-like iteration support
    public Iterator<Map<String, Object>> iterator() {
        return rows.iterator();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // Header row
        sb.append("| ");
        for (String column : columnNames) {
            sb.append(String.format("%-15s | ", column));
        }
        sb.append("\n");
        
        // Separator
        sb.append("|");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("-".repeat(17)).append("|");
        }
        sb.append("\n");
        
        // Data rows
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (String column : columnNames) {
                Object value = row.get(column);
                String displayValue = (value != null) ? value.toString() : "";
                sb.append(String.format("%-15s | ", displayValue));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}

/**
 * Excel Reader that replicates UiPath's Read Range activity behavior
 * Features:
 * - Reads only visible data (skips hidden rows and columns)
 * - Uses first row as headers
 * - Returns data in DataTable structure
 * - Handles both .xlsx and .xls files
 */
class ExcelReadRange {
    
    /**
     * Main method to read Excel range similar to UiPath's Read Range activity
     * @param filePath Path to Excel file
     * @param sheetName Name of the sheet to read (null for first sheet)
     * @param startRange Starting cell (e.g., "A1") - null for auto-detect
     * @param endRange Ending cell (e.g., "Z100") - null for auto-detect
     * @return DataTable with the read data
     */
    public static DataTable readRange(String filePath, String sheetName, 
                                    String startRange, String endRange) throws IOException {
        
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = null;
        
        try {
            // Determine workbook type based on file extension
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Use .xlsx or .xls");
            }
            
            // Get the specified sheet or first sheet
            Sheet sheet = (sheetName != null) ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + sheetName);
            }
            
            return readSheetData(sheet, startRange, endRange);
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
            fis.close();
        }
    }
    
    /**
     * Overloaded method for simpler usage (read entire sheet)
     */
    public static DataTable readRange(String filePath) throws IOException {
        return readRange(filePath, null, null, null);
    }
    
    /**
     * Overloaded method to specify sheet name only
     */
    public static DataTable readRange(String filePath, String sheetName) throws IOException {
        return readRange(filePath, sheetName, null, null);
    }
    
    private static DataTable readSheetData(Sheet sheet, String startRange, String endRange) {
        // Find the data boundaries (similar to UiPath's auto-detection)
        int startRow = 0;
        int endRow = sheet.getLastRowNum();
        int startCol = 0;
        int endCol = 0;
        
        // Calculate end column by finding the maximum column used
        for (Row row : sheet) {
            if (row != null && !isRowHidden(row)) {
                endCol = Math.max(endCol, row.getLastCellNum() - 1);
            }
        }
        
        // Override with specific ranges if provided
        if (startRange != null) {
            // Parse start range (simplified - in production, use proper cell reference parsing)
            // This is a basic implementation
        }
        if (endRange != null) {
            // Parse end range
        }
        
        // Read header row (first visible row)
        List<String> headers = new ArrayList<>();
        Row headerRow = null;
        
        // Find first visible row for headers
        for (int i = startRow; i <= endRow; i++) {
            Row row = sheet.getRow(i);
            if (row != null && !isRowHidden(row)) {
                headerRow = row;
                startRow = i + 1; // Data starts from next row
                break;
            }
        }
        
        if (headerRow == null) {
            throw new RuntimeException("No visible rows found in the specified range");
        }
        
        // Extract headers from first visible row, skipping hidden columns
        for (int col = startCol; col <= endCol; col++) {
            if (!isColumnHidden(sheet, col)) {
                Cell cell = headerRow.getCell(col);
                String header = getCellValueAsString(cell);
                if (header.trim().isEmpty()) {
                    header = "Column" + (headers.size() + 1); // Default column name
                }
                headers.add(header);
            }
        }
        
        // Create DataTable with headers
        DataTable dataTable = new DataTable(headers);
        
        // Read data rows, skipping hidden rows and columns
        for (int rowNum = startRow; rowNum <= endRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null || isRowHidden(row)) {
                continue; // Skip hidden or null rows
            }
            
            List<Object> rowData = new ArrayList<>();
            int headerIndex = 0;
            
            for (int col = startCol; col <= endCol; col++) {
                if (!isColumnHidden(sheet, col)) {
                    Cell cell = row.getCell(col);
                    Object value = getCellValue(cell);
                    rowData.add(value);
                    headerIndex++;
                }
            }
            
            // Only add rows that have some data
            boolean hasData = rowData.stream().anyMatch(val -> val != null && !val.toString().trim().isEmpty());
            if (hasData) {
                dataTable.addRow(rowData);
            }
        }
        
        return dataTable;
    }
    
    /**
     * Check if a row is hidden (UiPath skips hidden rows)
     */
    private static boolean isRowHidden(Row row) {
        return row.getZeroHeight() || 
               (row.getSheet().getRowBreakPosition(row.getRowNum()) != -1 && 
                row.getHeightInPoints() == 0);
    }
    
    /**
     * Check if a column is hidden (UiPath skips hidden columns)
     */
    private static boolean isColumnHidden(Sheet sheet, int columnIndex) {
        return sheet.isColumnHidden(columnIndex);
    }
    
    /**
     * Extract cell value as Object (preserves data types like UiPath)
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Return as integer if it's a whole number
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    } else {
                        return numericValue;
                    }
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                // Evaluate formula and return result
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }
    
    /**
     * Extract cell value as String (for headers)
     */
    private static String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return (value != null) ? value.toString() : "";
    }
}

/**
 * Example usage and demonstration
 */
public class UiPathReadRangeExample {
    
    public static void main(String[] args) {
        try {
            // Example 1: Read entire sheet
            System.out.println("=== Example 1: Reading entire Excel sheet ===");
            DataTable dt1 = ExcelReadRange.readRange("sample_data.xlsx");
            System.out.println(dt1);
            
            // Example 2: UiPath-like data access
            System.out.println("\n=== Example 2: UiPath-like data access ===");
            demonstrateUiPathLikeAccess(dt1);
            
            // Example 3: Read specific sheet
            System.out.println("\n=== Example 3: Reading specific sheet ===");
            DataTable dt2 = ExcelReadRange.readRange("sample_data.xlsx", "Sheet2");
            System.out.println("Rows read: " + dt2.getRowCount());
            System.out.println("Columns: " + dt2.getColumnNames());
            
        } catch (IOException e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate UiPath-like data access patterns
     */
    private static void demonstrateUiPathLikeAccess(DataTable dt) {
        System.out.println("Total rows: " + dt.getRowCount());
        System.out.println("Total columns: " + dt.getColumnCount());
        System.out.println("Column names: " + dt.getColumnNames());
        
        // Access data by column name (like UiPath)
        if (dt.getRowCount() > 0) {
            System.out.println("\n--- Accessing data by column name ---");
            for (String columnName : dt.getColumnNames()) {
                Object value = dt.getValue(0, columnName);
                System.out.println(columnName + ": " + value);
            }
        }
        
        // Iterate through all rows (like UiPath's For Each Row)
        System.out.println("\n--- For Each Row iteration (UiPath style) ---");
        int rowIndex = 0;
        for (Map<String, Object> row : dt) {
            System.out.println("Row " + rowIndex + ":");
            for (Map.Entry<String, Object> cell : row.entrySet()) {
                System.out.println("  " + cell.getKey() + ": " + cell.getValue());
            }
            rowIndex++;
            if (rowIndex >= 3) break; // Limit output for demo
        }
        
        // Access specific cell (like UiPath's dt.Rows(index)(columnName))
        if (dt.getRowCount() > 0 && dt.getColumnCount() > 0) {
            String firstColumn = dt.getColumnNames().get(0);
            Object cellValue = dt.getValue(0, firstColumn);
            System.out.println("\nFirst cell value: " + cellValue);
        }
    }
    
    /**
     * Create sample Excel file for testing (run this first)
     */
    public static void createSampleExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Age");
        headerRow.createCell(2).setCellValue("City");
        headerRow.createCell(3).setCellValue("Salary");
        
        // Create data rows
        Object[][] data = {
            {"John Doe", 30, "New York", 75000.50},
            {"Jane Smith", 25, "Los Angeles", 68000.75},
            {"Bob Johnson", 35, "Chicago", 82000.00},
            {"Alice Brown", 28, "Houston", 71500.25}
        };
        
        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                if (data[i][j] instanceof String) {
                    cell.setCellValue((String) data[i][j]);
                } else if (data[i][j] instanceof Integer) {
                    cell.setCellValue((Integer) data[i][j]);
                } else if (data[i][j] instanceof Double) {
                    cell.setCellValue((Double) data[i][j]);
                }
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Save file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream("sample_data.xlsx")) {
            workbook.write(fos);
        }
        
        workbook.close();
        System.out.println("Sample Excel file created: sample_data.xlsx");
    }
}