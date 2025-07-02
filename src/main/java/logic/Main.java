package logic;

import java.util.ArrayList;
import java.util.List;

import interfaces.ModelInterface;
import models.FlowBasedContinuousTimeModel;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        Manager.runModels();

        ArrayList<ModelInterface> modelList = new ArrayList<ModelInterface>();

        /*
        modelList.add(new FlowBasedContinuousTimeModel());
        modelList.add(new DiscreteTimeModel());
        modelList.add(new OnOffEventBasedModel());

*/





    }
}