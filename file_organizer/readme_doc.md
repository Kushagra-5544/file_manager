# Desktop File Organizer Utility

## Overview
A robust, configurable command-line utility in Java that automatically organizes files in a source directory into categorized subdirectories based on file extensions. Built with modern Java NIO.2 API and concurrent processing capabilities.

## Technical Specifications

### System Requirements
- **Java Version**: JDK 17 or higher
- **API**: Java NIO.2 (`java.nio.file`)
- **Concurrency**: `java.util.concurrent.ExecutorService`
- **Dependencies**: None (Pure Java, no external libraries)

### Architecture Overview
The application follows a clean Object-Oriented Design with three core components:

1. **Configuration Class**
   - Loads and manages file organization rules from `config.json`
   - Maps file extensions to target directory categories
   - Provides fallback category ("Others") for unknown file types
   - Auto-generates default configuration if file is missing

2. **OrganizerTask Class (Runnable)**
   - Encapsulates logic for processing individual files
   - Extracts file extensions and determines target directories
   - Creates target directories atomically if they don't exist
   - Performs atomic file move operations using `StandardCopyOption.ATOMIC_MOVE`
   - Handles file name conflicts with automatic renaming

3. **DirectoryScanner Class**
   - Iterates through the source directory
   - Filters out directories, hidden files, and system files
   - Delegates file processing to a fixed-size thread pool
   - Manages concurrent execution using `ExecutorService`

## Setup Instructions

### 1. Compilation
```bash
javac FileOrganizer.java
```

### 2. Configuration Setup
Create a `config.json` file in the same directory as the compiled class, or allow the program to auto-generate one on first run. The configuration file maps file extensions to folder names:

```json
{
  "pdf": "Documents",
  "jpg": "Images",
  "mp4": "Videos",
  "mp3": "Audio"
}
```

### 3. Prepare Source Directory
Create a directory with files to organize (or use an existing one like Downloads):
```bash
mkdir TestDirectory
# Add some test files
touch TestDirectory/document.pdf
touch TestDirectory/photo.jpg
touch TestDirectory/video.mp4
```

## Usage

### Basic Usage
```bash
java FileOrganizer
```
This uses default values:
- Source Directory: `Downloads`
- Config File: `config.json`

### Specify Custom Directory
```bash
java FileOrganizer /path/to/directory
```

### Specify Both Directory and Config File
```bash
java FileOrganizer /path/to/directory custom-config.json
```

## Example Execution

### Before Organization
```
Downloads/
├── report.pdf
├── photo.jpg
├── presentation.pptx
├── song.mp3
└── data.csv
```

### Running the Organizer
```bash
java FileOrganizer Downloads
```

### Output
```
=== Desktop File Organizer Utility ===

Configuration loaded successfully.
Configured categories: 45
Scanning directory: Downloads
Thread pool size: 4

Created directory: Downloads/Documents
Created directory: Downloads/Images
Created directory: Downloads/Presentations
Created directory: Downloads/Audio
Created directory: Downloads/Spreadsheets
Moved: report.pdf → Documents/
Moved: photo.jpg → Images/
Moved: presentation.pptx → Presentations/
Moved: song.mp3 → Audio/
Moved: data.csv → Spreadsheets/

=== Organization Complete ===
Total files processed: 5
```

### After Organization
```
Downloads/
├── Documents/
│   └── report.pdf
├── Images/
│   └── photo.jpg
├── Presentations/
│   └── presentation.pptx
├── Audio/
│   └── song.mp3
└── Spreadsheets/
    └── data.csv
```

## Key Features

### 1. Atomic Operations
All file moves use `StandardCopyOption.ATOMIC_MOVE` to ensure data integrity and prevent partial moves or corruption.

### 2. Concurrent Processing
Files are processed in parallel using a fixed thread pool (default: 4 threads), significantly improving performance for large directories.

### 3. Robust Error Handling
- Handles file not found errors
- Manages permission denied scenarios
- Deals with file name conflicts automatically
- Graceful degradation on errors with detailed logging

### 4. File Name Conflict Resolution
If a file with the same name exists in the target directory, the utility automatically appends a numeric suffix:
```
document.pdf → document_1.pdf
document.pdf → document_2.pdf
```

### 5. Smart File Filtering
- Only processes regular files (ignores directories)
- Skips hidden files (files starting with `.`)
- Skips system files
- Handles files without extensions (moved to "Others" category)

### 6. Automatic Configuration Generation
If `config.json` is not found, the program creates a comprehensive default configuration with common file types pre-configured.

## Configuration Details

### Supported File Categories (Default)
| Category | Extensions |
|----------|------------|
| Documents | pdf, doc, docx, txt, odt, rtf |
| Images | jpg, jpeg, png, gif, bmp, svg, webp, ico |
| Videos | mp4, avi, mkv, mov, wmv, flv, webm |
| Audio | mp3, wav, flac, aac, ogg, m4a, wma |
| Archives | zip, rar, 7z, tar, gz, bz2 |
| Code | java, py, js, html, css, cpp, c, h, php, rb, go, rs |
| Spreadsheets | xls, xlsx, csv, ods |
| Presentations | ppt, pptx, odp |
| Executables | exe, msi, dmg, deb, rpm |
| eBooks | epub, mobi, azw |
| Databases | db, sqlite, sql |
| Others | All unrecognized extensions |

### Customizing Configuration
Edit `config.json` to add your own mappings:
```json
{
  "your-extension": "YourCustomFolder",
  "log": "Logs",
  "md": "Documentation"
}
```

## Technical Implementation Details

### Java NIO.2 API Usage
```java
// Listing directory contents
Stream<Path> paths = Files.list(sourceDirectory)

// Creating directories
Files.createDirectories(targetDir)

// Atomic file move
Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)

// File attribute checks
Files.isRegularFile(path)
Files.isHidden(path)
Files.exists(path)
```

### Concurrency Model
```java
ExecutorService executorService = Executors.newFixedThreadPool(4);
executorService.submit(new OrganizerTask(file, directory, config));
executorService.shutdown();
executorService.awaitTermination(60, TimeUnit.SECONDS);
```

## Error Handling Examples

### File Not Found
```
ERROR: File not found: /path/to/missing-file.txt
```

### Permission Denied
```
ERROR: Permission denied for: /path/to/restricted-file.pdf
```

### File Already Exists
```
ERROR: Target file already exists: /path/to/duplicate.jpg
```

## Performance Considerations

### Thread Pool Size
The default thread pool size is 4. For directories with many files, this can be increased by modifying the `threadPoolSize` field in the `Configuration` class.

### Memory Usage
The application uses streaming (`Files.list`) rather than loading all files into memory, making it efficient even for very large directories.

### Atomic Operations
Using `ATOMIC_MOVE` ensures that either a file is fully moved or not moved at all, preventing partial transfers or file corruption.

## Limitations and Considerations

1. **Same Filesystem**: Atomic moves only work within the same filesystem. Cross-filesystem moves will fall back to copy-and-delete.

2. **File Locks**: Cannot move files that are currently open or locked by other applications.

3. **Large Files**: Moving very large files is still instantaneous as it's a metadata operation (on the same filesystem).

4. **Thread Pool Timeout**: Tasks have a 60-second timeout. Adjust if dealing with network drives or slow storage.

## Troubleshooting

### Issue: "Source directory does not exist"
**Solution**: Verify the path is correct and the directory exists.

### Issue: Files not being moved
**Solution**: Check file permissions and ensure the application has read/write access.

### Issue: Configuration not loading
**Solution**: Ensure `config.json` is in the same directory as the compiled `.class` file or specify the full path.

### Issue: "Permission denied" errors
**Solution**: Run with appropriate permissions or change the target directory to one you have write access to.

## Code Quality

- **Comprehensive Javadoc**: All public methods and classes documented
- **Clean Code Principles**: Single Responsibility, clear naming conventions
- **Error Handling**: Try-catch blocks for all I/O operations
- **Modern Java**: Uses JDK 17+ features and best practices
- **No External Dependencies**: Pure Java implementation

## License
This project is provided as-is for educational and personal use.

## Author
Your Name

## Version
1.0 - Initial Release