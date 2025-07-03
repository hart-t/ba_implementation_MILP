package io;

import com.gurobi.gurobi.GRBModel;
import interfaces.ModelSolutionInterface;

public class OptimizedSolution implements ModelSolutionInterface {
    private GRBModel model;

    public OptimizedSolution(GRBModel model) {
        this.model = model;
    }

    @Override
    public GRBModel getModel() {
        return model;
    }   
}
