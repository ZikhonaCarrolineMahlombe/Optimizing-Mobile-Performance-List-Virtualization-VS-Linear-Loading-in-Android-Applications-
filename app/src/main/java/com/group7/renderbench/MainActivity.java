package com.group7.renderbench;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RadioButton radioScroll = findViewById(R.id.radioScroll);
        RadioButton radioRecycler = findViewById(R.id.radioRecycler);
        Spinner spinner = findViewById(R.id.spinnerSize);

        String[] labels = new String[TestConfig.DATASET_SIZES.length];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = NumberFormat.getIntegerInstance().format(TestConfig.DATASET_SIZES[i]) + " rows";
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //To run the test button
        findViewById(R.id.btnRun).setOnClickListener(v -> {
            if (!radioScroll.isChecked() && !radioRecycler.isChecked()) {
                Toast.makeText(this, "Pick ScrollView or RecyclerView first", Toast.LENGTH_SHORT).show();
                return;
            }
            String engine = radioRecycler.isChecked()
                    ? TestConfig.ENGINE_RECYCLERVIEW
                    : TestConfig.ENGINE_SCROLLVIEW;
            int size = TestConfig.DATASET_SIZES[spinner.getSelectedItemPosition()];
            startActivity(TestActivity.newIntent(this, engine, size, 1, TestConfig.ITERATIONS));
        });

//Jumps to text results, needs to be connected once text screen available
        findViewById(R.id.btnResults).setOnClickListener(v ->
                Toast.makeText(this, "Results screen coming soon", Toast.LENGTH_SHORT).show());
    }
}