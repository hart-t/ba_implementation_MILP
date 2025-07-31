// Expected improved output after the fixes:

/*
===================================================================================================================================================================================
Instance Set            : j30
Type                    : sm

===================================================================================================================================================================================
Paramter Instance Model	no-H-Makespan H-Makespan  UB  LB  MIP-Gap Optimal-Makespan no-H-Time H-Time time-diff Heuristic-Makespan Stopped Error Heuristics
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
1       1         FLOW      43            43          43  43  0.00    43               5.09      2.97   -2.12     46                 false   false SSGS-GRPW, SSGS-MLST
                  DISC      43            43          43  43  0.00    43               0.80      0.25   -0.55     46                 false   false SSGS-GRPW, SSGS-MLST
                  ONOFF     56            46          46  10  0.78    43               15.30     15.01  -0.29     46                 true    false SSGS-GRPW, SSGS-MLST
                  FLOW      43            43          43  43  0.00    43               5.09                      46                 false   false SSGS-GRPW, SSGS-MLST
                  DISC      43            43          43  43  0.00    43               0.80                      46                 false   false SSGS-GRPW, SSGS-MLST
                  ONOFF     56            46          56  7   0.88    43               15.30                     46                 true    false SSGS-GRPW, SSGS-MLST
*/

// Key improvements in this version:
// 1. Proper time-diff calculation: H-Time - no-H-Time
//    - FLOW: 2.97 - 5.09 = -2.12
//    - DISC: 0.25 - 0.80 = -0.55  
//    - ONOFF: 15.01 - 15.30 = -0.29
//
// 2. Better column alignment with consistent spacing
// 3. Improved parsing of existing data when merging
// 4. Consistent integer formatting for makespan values
// 5. Better detection of time values vs other numeric values
