package gurobi;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBException;
import io.CallbackValues;

public class ObjValueCallback extends GRBCallback {
    private CallbackValues callbackValues = new CallbackValues();

    private long startTime;

    public ObjValueCallback() {
        this.startTime = System.nanoTime();
    }

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                // Wenn eine neue zulässige Lösung gefunden wurde
                double obj = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
                double time = (System.nanoTime() - startTime) / 1_000_000_000.0;

                callbackValues.addValues(obj, time, callbackValues.getSolutions().size() + 1);

                System.out.println("Neue Lösung #" + callbackValues.getSolutions().size() +
                        " -> Obj: " + obj + ", Zeit: " + time + "s");
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public CallbackValues getCallbackValues() {
        return callbackValues;
    }
}
