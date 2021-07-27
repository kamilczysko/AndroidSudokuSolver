package com.kamil.opcvtest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.kamil.opcvtest.solver.Solver;

import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;

import static android.widget.LinearLayout.SHOW_DIVIDER_MIDDLE;

public class SudokuActivity extends Activity {


    private TableLayout table;
    private List<List<Integer>> sudokuMap = Collections.emptyList();
    private Solver solver;
    private Button solveButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sudoku);

        this.solver = new Solver();

        Intent intent = getIntent();
        Parcelable sudoku = intent.getParcelableExtra("sudoku");
        sudokuMap = Parcels.unwrap(sudoku);
        table = findViewById(R.id.table);
        drawMapOnTable(sudokuMap);

        this.solveButton = findViewById(R.id.solveButton);
        this.solveButton.setOnClickListener(click -> {
            List<List<Integer>> result = solver.solve(sudokuMap);
            table.removeAllViews();
            drawMapOnTable(sudokuMap, result);
        });
    }

    private void drawMapOnTable(List<List<Integer>> sudokuMap) {
        for(List<Integer> row : sudokuMap) {
            TableRow tableRow = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
            tableRow.setLayoutParams(lp);
            for (Integer number : row) {
                TextView textView = new TextView(this);
                if (number == 0) {
                    textView.setText("*");
                } else {
                    textView.setText(number.toString());
                    textView.setTextColor(Color.RED);

                }
                textView.setTextSize(18);
                textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT,1.0f));
                tableRow.addView(textView);
                tableRow.setShowDividers(SHOW_DIVIDER_MIDDLE);
            }
            table.addView(tableRow);
        }
    }

    private void drawMapOnTable(List<List<Integer>> oldMap, List<List<Integer>> resolvedMap) {
        for(int i = 0; i < oldMap.size(); i++){
                TableRow tableRow = new TableRow(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                tableRow.setLayoutParams(lp);
            for(int j = 0; j < oldMap.size(); j++){
                TextView textView = new TextView(this);
                if(oldMap.get(i).get(j) != 0){
                    textView.setText(oldMap.get(i).get(j).toString());
                    textView.setTextColor(Color.RED);
                } else {
                    textView.setText(resolvedMap.get(i).get(j).toString());
                }
                textView.setTextSize(22);
                textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT,1.0f));
                tableRow.addView(textView);
                tableRow.setShowDividers(SHOW_DIVIDER_MIDDLE);
            }
            table.addView(tableRow);

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
