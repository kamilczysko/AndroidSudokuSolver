package com.kamil.opcvtest;

import com.kamil.opcvtest.solver.Solver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SolverTest {

    private Solver solver;

    @Before
    public void init(){
        solver = new Solver();
    }

    @Test
    public void solveTest() {
        List<List<Integer>> input = Arrays.asList(
                Arrays.asList(0, 0, 0, 8, 0, 0, 0, 0, 0),
                Arrays.asList(7, 8, 9, 0, 1, 0, 0, 0, 6),
                Arrays.asList(0, 0, 0, 0, 0, 6, 1, 0, 0),
                Arrays.asList(0, 0, 7, 0, 0, 0, 0, 5, 0),
                Arrays.asList(5, 0, 8, 7, 0, 9, 3, 0, 4),
                Arrays.asList(0, 4, 0, 0, 0, 0, 2, 0, 0),
                Arrays.asList(0, 0, 3, 2, 0, 0, 0, 0, 0),
                Arrays.asList(8, 0, 0, 0, 7, 0, 4, 3, 9),
                Arrays.asList(0, 0, 0, 0, 0, 1, 0, 0, 0)
        );

        List<List<Integer>> result = solver.solve(input);

        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 6, 5, 8, 4, 7, 9, 2, 3),
                Arrays.asList(7, 8, 9, 3, 1, 2, 5, 4, 6),
                Arrays.asList(4, 3, 2, 5, 9, 6, 1, 7, 8),
                Arrays.asList(2, 9, 7, 4, 6, 3, 8, 5, 1),
                Arrays.asList(5, 1, 8, 7, 2, 9, 3, 6, 4),
                Arrays.asList(3, 4, 6, 1, 5, 8, 2, 9, 7),
                Arrays.asList(9, 7, 3, 2, 8, 4, 6, 1, 5),
                Arrays.asList(8, 2, 1, 6, 7, 5, 4, 3, 9),
                Arrays.asList(6, 5, 4, 9, 3, 1, 7, 8, 2)
        );

        Assert.assertEquals(result, expected);
    }

    @Test
    public void tesLastChar(){
        String test = "677778";
        String substring = test.substring(test.length()-1);
        Assert.assertEquals(substring, "8");
    }
}