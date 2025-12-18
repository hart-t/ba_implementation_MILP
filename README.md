# RCPSP MILP Solver with Warmstart Strategies

A Java-based solver for the Resource-Constrained Project Scheduling Problem (RCPSP) that combines 5 different MILP formulations with heuristic warmstart strategies to accelerate solution finding.

## Models

- **DT** - Discrete Time (time-indexed)
- **FCT** - Flow-Based Continuous Time
- **OOE** - On-Off Event Based
- **IEE** - Interval Event Based  
- **SEQ** - Sequencing

## Warmstart Strategies

- **STD** - No warmstart (baseline)
- **VS** - Variable Start values (Gurobi MIP start)
- **VH** - Variable Hints (soft constraints)

## Heuristics

**SSGS** with 8 priority rules: SPT, GRPW, MRU, RSM, MTS, MLST, MLFT, MJS  
**GA** - Genetic Algorithm (Hartmann)

## Run Experiments

java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
      utility.TestAllModelInstanceCombinations > TerminalOutput.txt 2>&1

## Requirements

- Java 23
- Gurobi 12.0.2 (with valid license)
- Maven 3.x

---

## Program Structure:

ba_implementation_MILP/
├── src/main/java/
│   ├── enums/              # Enumerations for models, heuristics, warmstart strategies
│   ├── heuristics/         # Heuristic algorithms (SSGS, Genetic Algorithm)
│   ├── interfaces/         # Core interfaces
│   ├── io/                 # Data structures and file I/O
│   ├── logic/              # Main solver logic and integrated approach
│   ├── models/             # MILP formulations (5 different models)
│   ├── modelSolutions/     # Solution representations for each model
│   ├── priorityRules/      # Activity priority rules
│   ├── samplingTypes/      # Sampling strategies for heuristics
│   ├── solutionBuilder/    # Warmstart solution builders
│   └── utility/            # Helper utilities
├── benchmarkSets/          # PSPLIB benchmark instances (.sm files)
├── auswertung_csv_file/    # Analysis scripts and CSV output
└── pom.xml                 # Maven project configuration

## Running the experiments

### Testing a single instance

To test a single instance, use the `main` method in the `logic` package.  
In the corresponding class, you need to adjust the paths to the instance file and to the output files.  
Configuration details are documented directly in the class.

### Testing multiple instances

To run multiple instances and all model–strategy combinations, use the  
`TestAllModelInstanceCombinations` method in the `test` package.  
Here as well, the paths in the corresponding class must be adapted to the local environment.  
The necessary configuration steps are documented in the code.

### Gurobi license

Running the models requires a valid Gurobi license.  
The license file can be referenced via the `GRB_LICENSE_FILE` environment variable, for example:

    export GRB_LICENSE_FILE=/path/to/gurobi.lic

### Running with many instances

When using the `Result` class, large numbers of instances can lead to increased memory consumption/memory leaks. (I didnt know about CSV files back there :/ )
It is therefore recommended to start the program with the following command:

    java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
      utility.TestAllModelInstanceCombinations > TerminalOutput.txt 2>&1

The complete console output is written to `TerminalOutput.txt`.  
This output can then be processed with a separate parser to extract the desired metrics into a CSV file.

### CSV Files

There are two csv files in the csv_files folder, containing all j30 intance runs and a few of the j120

**Bachelor Thesis** | KIT 2025 | Tobias Hart