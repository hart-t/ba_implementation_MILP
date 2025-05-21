package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {
    public static void main(String[] args) {
        // define max resource capacity
        Map<ResourceType, Integer> caps = new HashMap<>();
        caps.put(ResourceType.R1, 5);
        caps.put(ResourceType.R2, 3);
        caps.put(ResourceType.R3, 7);

        // add activity(activityId, activityDuration, activityRequirement(resourceType, amount))
        List<Activity> activities = new ArrayList<>();
        Map<ResourceType, Integer> req1 = Map.of(ResourceType.R1, 2);
        activities.add(new Activity(1, 4, req1));
        activities.add(new Activity(2, 1, Map.of(ResourceType.R1, 2)));
        activities.add(new Activity(2, 2, Map.of(ResourceType.R2, 2)));

        RCPSPProblem problem = new RCPSPProblem(activities, caps);


        /**
         * SSGSSolver solver = new SSGSSolver(problem);

        //Schedule schedule = solver.solve();

        // output results
        for (Activity a : activities) {
            System.out.printf("Activity %d: start at %d\n", a.getId(), schedule.getStartTime(a.getId()));
        }*/
    }
}
