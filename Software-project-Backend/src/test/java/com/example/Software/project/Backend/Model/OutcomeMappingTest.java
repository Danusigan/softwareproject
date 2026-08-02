package com.example.Software.project.Backend.Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutcomeMapping Tests")
class OutcomeMappingTest {

    @Test
    @DisplayName("getWeightDescription does not throw when weight is null (e.g. before it is set)")
    void getWeightDescription_nullWeight_returnsUnknownInsteadOfThrowing() {
        OutcomeMapping mapping = new OutcomeMapping();

        assertEquals("Unknown", mapping.getWeightDescription());
    }

    @Test
    @DisplayName("getWeightDescription maps each defined weight to its label")
    void getWeightDescription_mapsKnownWeights() {
        OutcomeMapping mapping = new OutcomeMapping();

        mapping.setWeight(0);
        assertEquals("No Correlation", mapping.getWeightDescription());
        mapping.setWeight(1);
        assertEquals("Low Correlation", mapping.getWeightDescription());
        mapping.setWeight(2);
        assertEquals("Medium Correlation", mapping.getWeightDescription());
        mapping.setWeight(3);
        assertEquals("High Correlation", mapping.getWeightDescription());
    }
}
