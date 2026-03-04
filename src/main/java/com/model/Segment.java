package com.model;

import com.google.gson.annotations.SerializedName;

public class Segment {

    public long id;
    public String name;
    @SerializedName("activity_type")
    public String activityType;
    public float distance;        // meters
    @SerializedName("average_grade")
    public float averageGrade;    // %
    @SerializedName("maximum_grade")
    public float maximumGrade;    // %
    @SerializedName("elevation_high")
    public float elevationHigh;   // meters
    @SerializedName("elevation_low")
    public float elevationLow;    // meters
    @SerializedName("effort_count")
    public int effortCount;
    @SerializedName("athlete_count")
    public int athleteCount;
    @SerializedName("star_count")
    public int starCount;
    public String city;
    public String state;
    public String country;

    @Override
    public String toString() {
        return String.format(
            "Segment [id=%d, name='%s', type=%s, distance=%.0fm, grade=%.1f%%, efforts=%d, athletes=%d]",
            id, name, activityType, distance, averageGrade, effortCount, athleteCount
        );
    }
}