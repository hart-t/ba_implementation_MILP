package heuristics.geneticAlgorithm;

public final class DecoderState {
    int T;                // time-horizon used by arrays
    int[][] avail;        // resource availability avail[k][t] for t=0..T-1; initialize to R[k]
    int[] start;          // start time for j (size J+2, include 0 and J+1= sink)
    int[] finish;         // start[j] + p[j]
    
    public DecoderState(Project project) {
        this.T = project.horizon;
        this.avail = new int[project.K][T];
        this.start = new int[project.J + 2];
        this.finish = new int[project.J + 2];
        
        // Initialize resource availability
        for (int k = 0; k < project.K; k++) {
            for (int t = 0; t < T; t++) {
                avail[k][t] = project.R[k];
            }
        }
        
        // Initialize dummy start job (job 0)
        start[0] = 0;
        finish[0] = project.p[0]; // Usually 0 for dummy start
    }
    
    /**
     * Reset state for new decoding
     */
    public void reset(Project project) {
        // Reset resource availability
        for (int k = 0; k < project.K; k++) {
            for (int t = 0; t < T; t++) {
                avail[k][t] = project.R[k];
            }
        }
        
        // Reset start/finish times
        for (int j = 0; j <= project.J + 1; j++) {
            start[j] = 0;
            finish[j] = 0;
        }
        
        // Initialize dummy start job
        start[0] = 0;
        finish[0] = project.p[0];
    }
}
