package heuristics.geneticAlgorithm;

import java.util.List;

public final class Project {
    int J;                      // number of non-dummy activities
    int K;                      // number of renewable resources
    int[] p;                    // duration[1..J]
    int[][] r;                  // r[j][k], resource demand
    int[] R;                    // availability[k]
    List<Integer>[] P;          // immediate predecessors for each j
    List<Integer>[] S;          // immediate successors for each j
    int horizon;                // upper bound time horizon
    
    // Preprocessing cache
    private ProjectPreprocessor preprocessor;

    public Project(int J, int K, int[] p, int[][] r, int[] R, List<Integer>[] P, List<Integer>[] S, int horizon) {
        this.J = J;
        this.K = K;
        this.p = p;
        this.r = r;
        this.R = R;
        this.P = P;
        this.S = S;
        this.horizon = horizon;
        this.preprocessor = null; // Lazy initialization
    }
    
    /**
     * Get or create preprocessor (lazy initialization)
     */
    public ProjectPreprocessor getPreprocessor() {
        if (preprocessor == null) {
            preprocessor = new ProjectPreprocessor(this);
        }
        return preprocessor;
    }
    
    /**
     * Force recomputation of preprocessor (use after modifying project)
     */
    public void invalidatePreprocessor() {
        this.preprocessor = null;
    }
}
