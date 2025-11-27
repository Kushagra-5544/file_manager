import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Desktop File Organizer Utility - Main Entry Point
 * 
 * A robust command-line utility that automatically organizes files in a source directory
 * into categorized subdirectories based on file extensions using concurrent processing.
 * 
 * @author Your Name
 * @version 1.0
 * @since JDK 17
 */
public class FileOrganizer {
    
    private static final String DEFAULT_CONFIG_FILE = "config.json";
    private static final String DEFAULT_SOURCE_DIR = "Downloads";
    
    /**
     * Main entry point for the File Organizer application.
     * 
     * Usage: java FileOrganizer [source-directory] [config-file]
     * 
     * @param args Command line arguments: [0] source directory (optional), [1] config file (optional)
     */
    public static void main(String[] args) {
        System.out.println("=== Desktop File Organizer Utility ===\n");
        
        String sourceDir = args.length > 0 ? args[0] : DEFAULT_SOURCE_DIR;
        String configFile = args.length > 1 ? args[1] : DEFAULT_CONFIG_FILE;
        
        try {
            // Load configuration
            Configuration config = new Configuration(configFile);
            System.out.println("Configuration loaded successfully.");
            System.out.println("Configured categories: " + config.getCategoryCount());
            
            // Initialize and execute directory scanner
            DirectoryScanner scanner = new DirectoryScanner(sourceDir, config);
            int filesProcessed = scanner.scan();
            
            System.out.println("\n=== Organization Complete ===");
            System.out.println("Total files processed: " + filesProcessed);
            
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load configuration: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

/**
 * Configuration management class that handles loading and accessing file organization rules.
 * 
 * Loads configuration from a JSON-like format file that maps file extensions to target directories.
 * Provides fallback categorization for unknown file types.
 */
class Configuration {
    
    private final Map<String, String> extensionToCategory;
    private final String defaultCategory;
    private final int threadPoolSize;
    
    /**
     * Constructs a Configuration object by loading rules from the specified file.
     * 
     * @param configFilePath Path to the configuration file
     * @throws IOException if the configuration file cannot be read or parsed
     */
    public Configuration(String configFilePath) throws IOException {
        this.extensionToCategory = new HashMap<>();
        this.defaultCategory = "Others";
        this.threadPoolSize = 4; // Default thread pool size
        
        loadConfiguration(configFilePath);
    }
    
    /**
     * Loads configuration from file. Supports simple JSON-like format.
     * Format: "extension": "CategoryName"
     * 
     * @param filePath Path to configuration file
     * @throws IOException if file cannot be read
     */
    private void loadConfiguration(String filePath) throws IOException {
        Path configPath = Paths.get(filePath);
        
        if (!Files.exists(configPath)) {
            System.out.println("Config file not found. Creating default configuration...");
            createDefaultConfiguration(configPath);
        }
        
        List<String> lines = Files.readAllLines(configPath);
        
        for (String line : lines) {
            line = line.trim();
            // Skip comments, empty lines, and structural JSON characters
            if (line.isEmpty() || line.startsWith("//") || line.equals("{") || line.equals("}")) {
                continue;
            }
            
            // Parse line: "extension": "Category"
            if (line.contains(":")) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String extension = parts[0].trim().replaceAll("[\"',]", "");
                    String category = parts[1].trim().replaceAll("[\"',]", "");
                    
                    if (!extension.isEmpty() && !category.isEmpty()) {
                        extensionToCategory.put(extension.toLowerCase(), category);
                    }
                }
            }
        }
        
        // Ensure at least some default mappings exist
        if (extensionToCategory.isEmpty()) {
            addDefaultMappings();
        }
    }
    
    /**
     * Creates a default configuration file with common file type mappings.
     * 
     * @param configPath Path where the configuration file should be created
     * @throws IOException if the file cannot be created
     */
    private void createDefaultConfiguration(Path configPath) throws IOException {
        List<String> defaultConfig = Arrays.asList(
            "{",
            "  // Document files",
            "  \"pdf\": \"Documents\",",
            "  \"doc\": \"Documents\",",
            "  \"docx\": \"Documents\",",
            "  \"txt\": \"Documents\",",
            "  \"odt\": \"Documents\",",
            "  \"rtf\": \"Documents\",",
            "",
            "  // Image files",
            "  \"jpg\": \"Images\",",
            "  \"jpeg\": \"Images\",",
            "  \"png\": \"Images\",",
            "  \"gif\": \"Images\",",
            "  \"bmp\": \"Images\",",
            "  \"svg\": \"Images\",",
            "  \"webp\": \"Images\",",
            "",
            "  // Video files",
            "  \"mp4\": \"Videos\",",
            "  \"avi\": \"Videos\",",
            "  \"mkv\": \"Videos\",",
            "  \"mov\": \"Videos\",",
            "  \"wmv\": \"Videos\",",
            "  \"flv\": \"Videos\",",
            "",
            "  // Audio files",
            "  \"mp3\": \"Audio\",",
            "  \"wav\": \"Audio\",",
            "  \"flac\": \"Audio\",",
            "  \"aac\": \"Audio\",",
            "  \"ogg\": \"Audio\",",
            "",
            "  // Archive files",
            "  \"zip\": \"Archives\",",
            "  \"rar\": \"Archives\",",
            "  \"7z\": \"Archives\",",
            "  \"tar\": \"Archives\",",
            "  \"gz\": \"Archives\",",
            "",
            "  // Code files",
            "  \"java\": \"Code\",",
            "  \"py\": \"Code\",",
            "  \"js\": \"Code\",",
            "  \"html\": \"Code\",",
            "  \"css\": \"Code\",",
            "  \"cpp\": \"Code\",",
            "  \"c\": \"Code\",",
            "",
            "  // Spreadsheet files",
            "  \"xls\": \"Spreadsheets\",",
            "  \"xlsx\": \"Spreadsheets\",",
            "  \"csv\": \"Spreadsheets\",",
            "",
            "  // Presentation files",
            "  \"ppt\": \"Presentations\",",
            "  \"pptx\": \"Presentations\"",
            "}"
        );
        
        Files.write(configPath, defaultConfig, StandardOpenOption.CREATE);
        addDefaultMappings();
    }
    
    /**
     * Adds default file extension mappings to the configuration.
     */
    private void addDefaultMappings() {
        extensionToCategory.put("pdf", "Documents");
        extensionToCategory.put("doc", "Documents");
        extensionToCategory.put("docx", "Documents");
        extensionToCategory.put("txt", "Documents");
        extensionToCategory.put("jpg", "Images");
        extensionToCategory.put("jpeg", "Images");
        extensionToCategory.put("png", "Images");
        extensionToCategory.put("mp4", "Videos");
        extensionToCategory.put("mp3", "Audio");
        extensionToCategory.put("zip", "Archives");
    }
    
    /**
     * Retrieves the target category for a given file extension.
     * 
     * @param extension File extension (without the dot)
     * @return Target category name, or default category if extension is unknown
     */
    public String getCategory(String extension) {
        return extensionToCategory.getOrDefault(extension.toLowerCase(), defaultCategory);
    }
    
    /**
     * Returns the configured thread pool size for concurrent processing.
     * 
     * @return Number of threads to use in the executor service
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    /**
     * Returns the number of configured file extension categories.
     * 
     * @return Count of configured mappings
     */
    public int getCategoryCount() {
        return extensionToCategory.size();
    }
}

/**
 * Runnable task that encapsulates the logic for organizing a single file.
 * 
 * Determines the file's extension, resolves the target directory, creates necessary
 * directories, and performs an atomic move operation.
 */
class OrganizerTask implements Runnable {
    
    private final Path sourceFile;
    private final Path baseDirectory;
    private final Configuration config;
    
    /**
     * Constructs an organizer task for a specific file.
     * 
     * @param sourceFile The file to be organized
     * @param baseDirectory The base directory where categorized folders will be created
     * @param config Configuration containing extension-to-category mappings
     */
    public OrganizerTask(Path sourceFile, Path baseDirectory, Configuration config) {
        this.sourceFile = sourceFile;
        this.baseDirectory = baseDirectory;
        this.config = config;
    }
    
    /**
     * Executes the file organization task.
     * 
     * Process:
     * 1. Extract file extension
     * 2. Determine target category
     * 3. Create target directory if needed
     * 4. Perform atomic file move
     * 5. Handle any errors gracefully
     */
    @Override
    public void run() {
        try {
            // Extract file extension
            String fileName = sourceFile.getFileName().toString();
            String extension = getFileExtension(fileName);
            
            // Determine target category
            String category = config.getCategory(extension);
            
            // Create target directory path
            Path targetDir = baseDirectory.resolve(category);
            
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                System.out.println("Created directory: " + targetDir);
            }
            
            // Construct target file path
            Path targetFile = targetDir.resolve(fileName);
            
            // Handle file name conflicts
            targetFile = resolveNameConflict(targetFile);
            
            // Perform atomic move operation
            Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            
            System.out.println("Moved: " + fileName + " → " + category + "/");
            
        } catch (NoSuchFileException e) {
            System.err.println("ERROR: File not found: " + sourceFile);
        } catch (FileAlreadyExistsException e) {
            System.err.println("ERROR: Target file already exists: " + e.getMessage());
        } catch (AccessDeniedException e) {
            System.err.println("ERROR: Permission denied for: " + sourceFile);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to move file " + sourceFile + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error processing " + sourceFile + ": " + e.getMessage());
        }
    }
    
    /**
     * Extracts the file extension from a filename.
     * 
     * @param fileName Name of the file
     * @return File extension without the dot, or empty string if no extension
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * Resolves file name conflicts by appending a numeric suffix.
     * 
     * @param targetPath Original target path
     * @return Resolved path that doesn't conflict with existing files
     */
    private Path resolveNameConflict(Path targetPath) {
        if (!Files.exists(targetPath)) {
            return targetPath;
        }
        
        String fileName = targetPath.getFileName().toString();
        String nameWithoutExt;
        String extension;
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = fileName.substring(0, lastDot);
            extension = fileName.substring(lastDot);
        } else {
            nameWithoutExt = fileName;
            extension = "";
        }
        
        int counter = 1;
        Path newPath;
        do {
            String newFileName = nameWithoutExt + "_" + counter + extension;
            newPath = targetPath.getParent().resolve(newFileName);
            counter++;
        } while (Files.exists(newPath));
        
        return newPath;
    }
}

/**
 * Directory scanner that iterates through source directory and delegates file
 * organization tasks to a thread pool for concurrent processing.
 */
class DirectoryScanner {
    
    private final Path sourceDirectory;
    private final Configuration config;
    private final ExecutorService executorService;
    
    /**
     * Constructs a DirectoryScanner for the specified source directory.
     * 
     * @param sourceDirPath Path to the directory to be scanned
     * @param config Configuration containing organization rules
     * @throws IOException if the source directory is invalid
     */
    public DirectoryScanner(String sourceDirPath, Configuration config) throws IOException {
        this.sourceDirectory = Paths.get(sourceDirPath);
        this.config = config;
        
        // Validate source directory
        if (!Files.exists(sourceDirectory)) {
            throw new IOException("Source directory does not exist: " + sourceDirPath);
        }
        
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException("Source path is not a directory: " + sourceDirPath);
        }
        
        // Initialize thread pool
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }
    
    /**
     * Scans the source directory and organizes all eligible files.
     * 
     * Files are processed concurrently using a thread pool. The method blocks
     * until all files have been processed.
     * 
     * @return Number of files submitted for processing
     * @throws IOException if directory scanning fails
     */
    public int scan() throws IOException {
        System.out.println("Scanning directory: " + sourceDirectory);
        System.out.println("Thread pool size: " + config.getThreadPoolSize() + "\n");
        
        int fileCount = 0;
        
        try (Stream<Path> paths = Files.list(sourceDirectory)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                // Filter: only process regular files
                if (Files.isRegularFile(path) && !isHiddenOrSystem(path)) {
                    // Submit task to executor service
                    executorService.submit(new OrganizerTask(path, sourceDirectory, config));
                    fileCount++;
                }
            }
        }
        
        // Shutdown executor and wait for completion
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                System.err.println("WARNING: Some tasks did not complete within timeout period");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IOException("Directory scanning interrupted", e);
        }
        
        return fileCount;
    }
    
    /**
     * Checks if a file is hidden or a system file.
     * 
     * @param path File path to check
     * @return true if the file should be skipped, false otherwise
     */
    private boolean isHiddenOrSystem(Path path) {
        try {
            // Check if file is hidden
            if (Files.isHidden(path)) {
                return true;
            }
            
            // Check if filename starts with dot (hidden on Unix-like systems)
            String fileName = path.getFileName().toString();
            if (fileName.startsWith(".")) {
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            // If we can't determine, err on the side of caution and skip
            return true;
        }
    }
}