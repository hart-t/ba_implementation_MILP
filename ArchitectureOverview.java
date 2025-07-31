// New Clean Architecture Summary:

/*
FILEWRITER.JAVA (Simple I/O only):
- writeResults(directory, filename, results) - Main method to write results
- writeLinesToFile(file, lines) - Writes lines to file
- appendLinesToFile(file, lines) - Appends lines to file

RESULTFORMATTER.JAVA (Logic and Formatting):
- formatResults(results, file) - Main formatting method
- loadOptimalValues() - Loads benchmark optimal values
- loadExistingResults(file) - Loads existing data if file exists
- formatResultLine() - Formats individual result lines
- createHeader() - Creates file header
- Helper methods for parsing, formatting, etc.

BEHAVIOR:
1. NEW FILE + HEURISTIC RESULTS → Creates gurobi_computed_h_results.txt format
2. NEW FILE + NON-HEURISTIC RESULTS → Creates gurobi_computed_noh_results.txt format  
3. EXISTING FILE + NEW RESULTS → Merges to create gurobi_computed_both_results.txt format

EXAMPLE USAGE:
FileWriter writer = new FileWriter();
writer.writeResults("results", "my_results.txt", resultsList);

The system will:
- Create directory if needed
- Detect if file exists
- Format results appropriately (H, noH, or merged format)
- Calculate time differences when both H_Time and noH_Time are available
- Fill N/A spots with actual values
- Preserve existing data when merging
*/
