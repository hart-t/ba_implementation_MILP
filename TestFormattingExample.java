// Example of expected output format after fixes:

/*
===================================================================================================================================================================================
Instance Set            : j30
Type                    : sm

===================================================================================================================================================================================
Paramter Instance Model	no-H-Makespan H-Makespan  UB  LB  MIP-Gap Optimal-Makespan no-H-Time H-Time time-diff Heuristic-Makespan Stopped Error Heuristics
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
1       1         FLOW 	43            43          43  43  0.00    43               4.79      2.83   -1.96     46                 false   false SSGS-GRPW, SSGS-MLST
          DISC 	        43            43          43  43  0.00    43               0.78      0.26   -0.52     46                 false   false SSGS-GRPW, SSGS-MLST
          ONOFF	        46            56          56  10  0.78    43               15.05     15     -0.05     46                 true    false SSGS-GRPW, SSGS-MLST
          FLOW 	43                      43          43  43  0.00    43               4.79                                        false   false
          DISC 	43                      43          43  43  0.00    43               0.78                                        false   false
          ONOFF	56                      56          56  7   0.88    43               15.05                                       true    false
*/

// Key fixes made:
// 1. Fixed tab-based parsing in loadExistingResults() and mergeWithExistingFile()
// 2. Proper handling of continuation lines (empty parameter/instance for subsequent models)
// 3. Better alignment and spacing in format strings
// 4. Correct extraction of existing values when merging
// 5. Proper calculation of time-diff when both H-Time and no-H-Time are available
