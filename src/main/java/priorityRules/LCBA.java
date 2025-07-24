package priorityRules;

import io.JobDataInstance;
import utility.DAGLongestPath;

import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Local Constraint Based Analysis (LCBA) - Ozdamar and Ulusoy 1994
 * This implementation focuses on the core precedence analysis and feasibility checks
 * for doubly constrained project scheduling problems
 */
public class LCBA implements PriorityRuleInterface {
    
    private static class ConflictPair {
        int activityI, activityJ;
        boolean isPermanent;
        
        ConflictPair(int i, int j, boolean permanent) {
            this.activityI = i;
            this.activityJ = j;
            this.isPermanent = permanent;
        }
    }
    
    private static class ActivityState {
        int earliestStart;
        int latestFinish;
        int duration;
        boolean isScheduled;
        boolean isInProgress;
        
        ActivityState(int es, int lf, int dur) {
            this.earliestStart = es;
            this.latestFinish = lf;
            this.duration = dur;
            this.isScheduled = false;
            this.isInProgress = false;
        }
    }
    
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return (activity1, activity2) -> {
            // Apply LCBA logic to determine priority
            double priority1 = calculateLCBAPriority(activity1, data);
            double priority2 = calculateLCBAPriority(activity2, data);
            
            // Higher priority value means higher precedence
            return Double.compare(priority2, priority1);
        };
    }
    
    private double calculateLCBAPriority(int activityId, JobDataInstance data) {
        // Initialize activity states
        Map<Integer, ActivityState> activityStates = initializeActivityStates(data);
        
        // Find schedulable activities (SET)
        Set<Integer> schedulableSet = findSchedulableActivities(data, activityStates);
        
        if (!schedulableSet.contains(activityId)) {
            return Double.NEGATIVE_INFINITY; // Not schedulable
        }
        
        // Identify conflict pairs
        List<ConflictPair> conflictPairs = identifyConflictPairs(schedulableSet, data);
        
        // Apply feasibility checks
        double projectDurationIncrease = applyFeasibilityChecks(conflictPairs, activityStates, data);
        
        // Apply essential conditions and calculate priority
        return applyEssentialConditions(activityId, schedulableSet, conflictPairs, activityStates, data);
    }
    
    private Map<Integer, ActivityState> initializeActivityStates(JobDataInstance data) {
        Map<Integer, ActivityState> states = new HashMap<>();
        
        for (int i = 0; i < data.numberJob; i++) {
            // Calculate earliest start and latest finish times
            int es = calculateEarliestStart(i, data);
            int lf = calculateLatestFinish(i, data);
            int duration = data.jobDuration.get(i);
            states.put(i, new ActivityState(es, lf, duration));
        }
        
        return states;
    }
    
    private int calculateEarliestStart(int activityId, JobDataInstance data) {
        // Simple forward pass calculation
        int maxPredFinish = 0;
        List<List<Integer>> precedences = data.jobPrecedences;
        
        for (int pred = 0; pred < data.numberJob; pred++) {
            if (precedences.get(pred).contains(activityId)) {
                int predFinish = calculateEarliestStart(pred, data) + data.jobDuration.get(pred);
                maxPredFinish = Math.max(maxPredFinish, predFinish);
            }
        }
        
        return maxPredFinish;
    }
    
    private int calculateLatestFinish(int activityId, JobDataInstance data) {
        // Simple backward pass calculation - simplified for this implementation
        DAGLongestPath longestPath = new DAGLongestPath();
        return longestPath.computeLongestPath(data.jobPrecedences, data.jobDuration) 
               - calculateRemainingWork(activityId, data);
    }
    
    private int calculateRemainingWork(int activityId, JobDataInstance data) {
        // Calculate remaining work from this activity to project end
        int maxSuccPath = 0;
        List<List<Integer>> precedences = data.jobPrecedences;
        
        if (precedences.get(activityId) != null) {
            for (int succ : precedences.get(activityId)) {
                int succPath = data.jobDuration.get(succ) + calculateRemainingWork(succ, data);
                maxSuccPath = Math.max(maxSuccPath, succPath);
            }
        }
        
        return maxSuccPath;
    }
    
    private Set<Integer> findSchedulableActivities(JobDataInstance data, Map<Integer, ActivityState> states) {
        Set<Integer> schedulable = new HashSet<>();
        List<List<Integer>> precedences = data.jobPrecedences;
        
        for (int i = 0; i < data.numberJob; i++) {
            if (states.get(i).isScheduled) continue;
            
            boolean canSchedule = true;
            // Check if all predecessors are completed
            for (int pred = 0; pred < data.numberJob; pred++) {
                if (precedences.get(pred).contains(i) && !states.get(pred).isScheduled) {
                    canSchedule = false;
                    break;
                }
            }
            
            if (canSchedule) {
                schedulable.add(i);
            }
        }
        
        return schedulable;
    }
    
    private List<ConflictPair> identifyConflictPairs(Set<Integer> schedulableSet, JobDataInstance data) {
        List<ConflictPair> conflicts = new ArrayList<>();
        List<Integer> activities = new ArrayList<>(schedulableSet);
        
        for (int i = 0; i < activities.size(); i++) {
            for (int j = i + 1; j < activities.size(); j++) {
                int actI = activities.get(i);
                int actJ = activities.get(j);
                
                // Check resource conflicts
                boolean hasConflict = checkResourceConflict(actI, actJ, data);
                if (hasConflict) {
                    boolean isPermanent = checkPermanentConflict(actI, actJ, data);
                    conflicts.add(new ConflictPair(actI, actJ, isPermanent));
                }
            }
        }
        
        return conflicts;
    }
    
    private boolean checkResourceConflict(int actI, int actJ, JobDataInstance data) {
        // Check if activities compete for the same limited resources
        List<Integer> resourceReqI = data.jobResource.get(actI);
        List<Integer> resourceReqJ = data.jobResource.get(actJ);
        List<Integer> resourceCapacity = data.resourceCapacity;

        for (int r = 0; r < resourceCapacity.size(); r++) {
            if (resourceReqI.get(r) + resourceReqJ.get(r) > resourceCapacity.get(r)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkPermanentConflict(int actI, int actJ, JobDataInstance data) {
        // Simplified: permanent conflict if resource requirements exceed capacity for all time
        return checkResourceConflict(actI, actJ, data);
    }
    
    private double applyFeasibilityChecks(List<ConflictPair> conflicts, 
                                        Map<Integer, ActivityState> states, 
                                        JobDataInstance data) {
        double maxIncrease = 0.0;
        
        for (ConflictPair conflict : conflicts) {
            double increase;
            
            if (conflict.isPermanent) {
                // Apply Formula F1: permanent conflict feasibility check
                increase = permanentConflictFeasibilityCheck(conflict, states, data);
            } else {
                // Apply Formula F3: temporary conflict feasibility check
                increase = temporaryConflictFeasibilityCheck(conflict, states, data);
            }
            
            maxIncrease = Math.max(maxIncrease, increase);
        }
        
        // Apply groupwise feasibility check if needed (Formula F2)
        if (conflicts.size() > 1 && allPairsInPermanentConflict(conflicts)) {
            double groupIncrease = groupwiseFeasibilityCheck(conflicts, states, data);
            maxIncrease = Math.max(maxIncrease, groupIncrease);
        }
        
        return maxIncrease;
    }
    
    private double permanentConflictFeasibilityCheck(ConflictPair conflict, 
                                                   Map<Integer, ActivityState> states, 
                                                   JobDataInstance data) {
        // Formula F1: α = max(0, t_now - max{lft_k} + Σp_km)
        ActivityState stateI = states.get(conflict.activityI);
        ActivityState stateJ = states.get(conflict.activityJ);
        
        int tNow = getCurrentTime(data); // Simplified as 0 for this implementation
        int maxLft = Math.max(stateI.latestFinish, stateJ.latestFinish);
        int sumDurations = stateI.duration + stateJ.duration;
        
        return Math.max(0, tNow - maxLft + sumDurations);
    }
    
    private double temporaryConflictFeasibilityCheck(ConflictPair conflict, 
                                                   Map<Integer, ActivityState> states, 
                                                   JobDataInstance data) {
        // Formula F3: More complex calculation for temporary conflicts
        ActivityState stateI = states.get(conflict.activityI);
        ActivityState stateJ = states.get(conflict.activityJ);
        
        int tNow = getCurrentTime(data);
        int conflictResolutionTime = calculateConflictResolutionTime(conflict, states, data);
        
        double option1 = Math.max(0, conflictResolutionTime + stateI.duration - stateJ.latestFinish);
        double option2 = Math.max(0, stateI.duration + tNow - stateJ.latestFinish);
        
        return Math.min(option1, option2);
    }
    
    private double groupwiseFeasibilityCheck(List<ConflictPair> conflicts, 
                                           Map<Integer, ActivityState> states, 
                                           JobDataInstance data) {
        // Formula F2: Groupwise feasibility for permanent conflicts
        Set<Integer> allActivities = new HashSet<>();
        for (ConflictPair conflict : conflicts) {
            allActivities.add(conflict.activityI);
            allActivities.add(conflict.activityJ);
        }
        
        int tNow = getCurrentTime(data);
        int minLft = allActivities.stream()
                .mapToInt(act -> states.get(act).latestFinish)
                .min().orElse(0);
        int sumDurations = allActivities.stream()
                .mapToInt(act -> states.get(act).duration)
                .sum();
        
        return Math.max(0, tNow - minLft + sumDurations);
    }
    
    private boolean allPairsInPermanentConflict(List<ConflictPair> conflicts) {
        return conflicts.stream().allMatch(c -> c.isPermanent);
    }
    
    private int calculateConflictResolutionTime(ConflictPair conflict, 
                                              Map<Integer, ActivityState> states, 
                                              JobDataInstance data) {
        // Simplified calculation of when resources become available
        return getCurrentTime(data) + 1; // Placeholder implementation
    }
    
    private double applyEssentialConditions(int activityId, 
                                          Set<Integer> schedulableSet, 
                                          List<ConflictPair> conflicts, 
                                          Map<Integer, ActivityState> states, 
                                          JobDataInstance data) {
        // Apply essential conditions to determine precedence relationships
        double priority = 0.0;
        
        // Base priority on urgency (latest finish time)
        ActivityState state = states.get(activityId);
        priority += 1000.0 / (state.latestFinish + 1); // Higher priority for more urgent activities
        
        // Adjust priority based on conflicts
        for (ConflictPair conflict : conflicts) {
            if (conflict.activityI == activityId || conflict.activityJ == activityId) {
                // Higher priority if this activity can resolve conflicts
                priority += conflict.isPermanent ? 100.0 : 50.0;
            }
        }
        
        // Add critical path consideration
        priority += calculateCriticalPathPriority(activityId, data);
        
        return priority;
    }
    
    private double calculateCriticalPathPriority(int activityId, JobDataInstance data) {
        // Higher priority for activities on or near critical path
        ActivityState state = calculateEarliestStart(activityId, data) == 0 ? null : null;
        int slack = calculateLatestFinish(activityId, data) - 
                   (calculateEarliestStart(activityId, data) + data.jobDuration.get(activityId));
        
        return slack == 0 ? 200.0 : Math.max(0, 100.0 / (slack + 1));
    }
    
    private int getCurrentTime(JobDataInstance data) {
        // Simplified - in real implementation this would be the current scheduling time
        return 0;
    }
}