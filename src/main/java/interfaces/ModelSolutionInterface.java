package interfaces;

import com.gurobi.gurobi.GRBModel;

import enums.ModelType;

public interface ModelSolutionInterface {
    public GRBModel getModel();

    public ModelType getModelType();
}
