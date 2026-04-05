package com.project.plaque.plaque_calculator.service;

import com.project.plaque.plaque_calculator.model.FD;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FDServiceTest {

    private final FDService fdService = new FDService();

    @Test
    void computeClosure_resolvesTransitiveDependency() {
        // This test checks that closure can follow dependencies step by step
        List<FD> fds = List.of(
                new FD(Set.of("A"), Set.of("B")),
                new FD(Set.of("B"), Set.of("C"))
        );

        Set<String> closure = fdService.computeClosure(Set.of("A"), fds);

        // If A->B and B->C, closure of A must include C
        assertThat(closure).containsExactlyInAnyOrder("A", "B", "C");
    }
}


