package com.sebastian_eggers.MediApp.Activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.sebastian_eggers.MediApp.Helper.DrugDBHelper;
import com.sebastian_eggers.MediApp.Adapter.TimeAdapter;
import com.sebastian_eggers.MediApp.Models.Drug;
import com.sebastian_eggers.MediApp.Enum.DrugForm;
import com.sebastian_eggers.MediApp.R;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

public class EditActivity extends AddActivity {

    private final Context context = this;
    private Drug drug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.edit_title);

        drug = getDrug();
        initializeTimeValues(drug);
        initializeCheckValues(drug);
        initializeRadioValues(drug);
        initializeEditTexts(drug);
    }

    /**
     * Returns the Drug Model from the id passed to the bundle
     *
     * @return Drug Model
     */
    private Drug getDrug() {
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        long id = bundle.getLong("itemId");
        DrugDBHelper dbHelper = new DrugDBHelper(this);
        return dbHelper.getDrug(id);
    }

    /**
     * Initialize time values of the time list based on the given Drug Model
     *
     * @param drug Drug Model
     */
    private void initializeTimeValues(Drug drug) {
        times.addAll(drug.getIntake());
        ListView timesListView = findViewById(R.id.list_view_times);
        TimeAdapter arrayAdapter = (TimeAdapter) timesListView.getAdapter();
        arrayAdapter.notifyDataSetChanged();
        setListViewHeight(timesListView);
    }

    /**
     * Initialize weekday values of the checkboxes based on the given Drug Model
     *
     * @param drug Drug Model
     */
    private void initializeCheckValues(Drug drug) {
        RadioGroup drugIntervalRadioGroup = findViewById(R.id.radio_group_drug_interval);
        for(int i = 0; i < drugIntervalRadioGroup.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) drugIntervalRadioGroup.getChildAt(i);
            String checkBoxValue = checkBox.getText().toString().toUpperCase();
            for(DayOfWeek dayOfWeek: drug.getDays()) {
                if(dayOfWeek == DayOfWeek.valueOf(translateDayOfWeekGermanEnglish(checkBoxValue)))
                    checkBox.setChecked(true);
            }
        }
    }

    /**
     * Initialize DrugForm values of the DrugForm radio buttons based on the given Drug Model
     *
     * @param drug Drug Model
     */
    private void initializeRadioValues(Drug drug) {
        RadioGroup drugFormRadioGroup = findViewById(R.id.radio_group_drug_form);
        for(int i = 0; i < drugFormRadioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) drugFormRadioGroup.getChildAt(i);
            String form = getName(drug.getForm());
            if(radioButton.getText().equals(form)) {
                radioButton.setChecked(true);
            }
        }
    }

    /**
     * Initialize the edit text fields by the values of the Drug Model
     * @param drug  Drug Model
     */
    private void initializeEditTexts(Drug drug) {
        ((EditText) findViewById(R.id.edit_drug_name)).setText(drug.getName());
        ((EditText) findViewById(R.id.edit_drug_description)).setText(drug.getDescription());
        ((EditText) findViewById(R.id.edit_drug_dose_per_intake)).setText(Integer.toString(drug.getDosePerIntake()));
        ((EditText) findViewById(R.id.edit_drug_dose_unit)).setText(drug.getDoseUnit());
        ((Button) findViewById(R.id.button_add)).setText(R.string.edit_title);

        LocalDate date = drug.getDateOfLastIntake();
        if (date != null) {
            int month = date.getMonthValue();
            int dayOfMonth = date.getDayOfMonth();
            String dateString = date.getYear() + "-" + (month > 9 ? month : "0" + month) + "-" + (dayOfMonth > 9 ? dayOfMonth : "0" + dayOfMonth);
            ((EditText) findViewById(R.id.edit_drug_date_of_last_intake)).setText(dateString);
        }

    }

    @Override
    protected void initializeAddButtonEventListener() {
        Button addDrugButton = findViewById(R.id.button_add);
        addDrugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String drugName = ((EditText) findViewById(R.id.edit_drug_name)).getText().toString();
                String drugDescription = ((EditText) findViewById(R.id.edit_drug_description)).getText().toString();
                ArrayList<DayOfWeek> weekDays = getWeekDaysFromActivity();

                String sDosePerIntake = ((EditText) findViewById(R.id.edit_drug_dose_per_intake)).getText().toString();
                final int drugDosePerIntake = (sDosePerIntake.length() > 0) ? Integer.parseInt(sDosePerIntake) : 0;

                String drugDoseUnit = ((EditText) findViewById(R.id.edit_drug_dose_unit)).getText().toString();
                DrugForm drugForm = getDrugFormFromActivity();

                if(drugName.length() > 0 && weekDays.size() > 0 && times.size() > 0 && drugDosePerIntake > 0 && drugForm != null) {
                    Drug drug = new Drug(drugName, times, weekDays, drugDosePerIntake, drugForm, drugDescription, drugDoseUnit);
                    drug.scheduleNotification(context);
                    long id = Objects.requireNonNull(getIntent().getExtras()).getLong("itemId");
                    drug.setId(id);

                    String date = ((EditText) findViewById(R.id.edit_drug_date_of_last_intake)).getText().toString();
                    if(date.length() > 0) {
                        drug.setDateOfLastIntake(LocalDate.parse(date));
                        scheduleNotificationCancelWorker(drug, context);
                    }

                    DrugDBHelper dbHelper = new DrugDBHelper(context);
                    dbHelper.updateDrug(drug);

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(mainActivity);
                        }
                    });
                    alert.setTitle(R.string.edit_title);
                    alert.setMessage(R.string.edit_successful);
                    alert.show();
                }
                else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle(R.string.add_title);
                    alert.setMessage(R.string.add_unsuccessful);
                    alert.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(drugName.length() <= 0) {
                                findViewById(R.id.edit_drug_name).requestFocus();
                            }
                            else if(drugDosePerIntake <= 0) {
                                findViewById(R.id.edit_drug_dose_per_intake).requestFocus();
                            }
                        }
                    });
                    alert.show();
                }
            }
        });
    }

    @Override
    protected void initializeDatePicker() {
        final TextView dateTextView = findViewById(R.id.edit_drug_date_of_last_intake);
        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDate date = drug.getDateOfLastIntake();
                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = year + "-" + (month > 9 ? (month + 1) : "0" + (month + 1)) + "-" + (dayOfMonth > 9 ? dayOfMonth : "0" + dayOfMonth);
                        dateTextView.setText(date);
                    }
                }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

                datePickerDialog.show();
            }
        });
    }
}
