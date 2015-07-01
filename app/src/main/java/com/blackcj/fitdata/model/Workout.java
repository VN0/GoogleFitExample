package com.blackcj.fitdata.model;

import com.blackcj.fitdata.Utilities;

import java.io.Serializable;

/**
 * Created by chris.black on 5/1/15.
 */
public class Workout implements Comparable<Workout>, Serializable {

    public long _id;            // same as start
    public long duration = 0;   // length of activity
    public long start = 0;      // activity start time
    public int type;            // type of activity
    public int stepCount = 0;   // number of steps for activity

    @Override
    public int compareTo(Workout another) {
        int result = 0;
        Long obj1 = this.start;
        Long obj2 = another.start;

        // Summary is always first, walking is always second
        if(this.start == -1) {
            result = -1;
        } else if(another.start == -1) {
            result = 1;
        } else if (this.type == another.type) {
            result = obj1.compareTo(obj2);
        } else if(this.type == WorkoutTypes.STEP_COUNT.getValue()) {
            result = -1;
        } else if(another.type == WorkoutTypes.STEP_COUNT.getValue()) {
            result = 1;
        }else {
            result = obj1.compareTo(obj2);
        }
        return result;
    }

    @Override
    public String toString() {
        return "You went " + WorkoutTypes.getWorkOutTextById(type) +
                " on " + Utilities.getDateString(start) +
                " at " + Utilities.getTimeString(start) +
                " for " + WorkoutReport.getDurationBreakdown(duration) +
                " with " + stepCount + " steps";
    }
}
