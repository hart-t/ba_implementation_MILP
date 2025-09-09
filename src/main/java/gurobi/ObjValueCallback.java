package gurobi;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjValueCallback extends GRBCallback {
    private Map<Double, Integer> targetFunctionValueCurve; // Changed from Integer to Double
    private List<Map<String, Object>> callbackValues;

    public ObjValueCallback() {
        this.callbackValues = new ArrayList<>();
    }

    // Add this method to set the target function value curve reference
    public void setTargetFunctionValueCurve(Map<Double, Integer> targetFunctionValueCurve) {
        this.targetFunctionValueCurve = targetFunctionValueCurve;
    }

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                // New incumbent solution found
                double objVal = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
                double runtime = getDoubleInfo(GRB.CB_RUNTIME);
                
                // Store with exact time (not rounded)
                if (targetFunctionValueCurve != null) {
                    targetFunctionValueCurve.put(runtime, (int) Math.round(objVal));
                }
                
                // Also store in callback values for detailed tracking
                Map<String, Object> entry = new HashMap<>();
                entry.put("time", runtime);
                entry.put("objValue", objVal);
                entry.put("type", "MIPSOL");
                callbackValues.add(entry);
                
                System.out.println("New incumbent found at time " + runtime + "s with objective value: " + objVal);
                
            } else if (where == GRB.CB_MIPNODE) {
                // New node in branch-and-bound tree (optional - for bound tracking)
                double objBound = getDoubleInfo(GRB.CB_MIPNODE_OBJBND);
                double runtime = getDoubleInfo(GRB.CB_RUNTIME);
                
                // Store bound information
                Map<String, Object> entry = new HashMap<>();
                entry.put("time", runtime);
                entry.put("objBound", objBound);
                entry.put("type", "MIPNODE");
                callbackValues.add(entry);
            }
        } catch (Exception e) {
            System.err.println("Error in callback: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getCallbackValues() {
        return callbackValues;
    }
}
