package io;

import com.gurobi.gurobi.GRBModel;

import enums.ModelType;
import interfaces.ModelSolutionInterface;

public class OptimizedSolution implements ModelSolutionInterface {
    private ModelType modelType;
    private GRBModel model;

    public OptimizedSolution(GRBModel model, ModelType modelType) {
        this.model = model;
        this.modelType = modelType;
    }

    @Override
    public GRBModel getModel() {
        return model;
    }

    @Override
    public ModelType getModelType() {
        return modelType;
    }
}
