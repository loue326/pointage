package com.example.gestionpointage;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.EmployeeAdapter;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.User;
import com.example.gestionpointage.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Liste des employés + recherche.
 * Un clic ouvre le détail (pointages et pauses de l'employé).
 */
public class AdminUsersActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private EmployeeAdapter adapter;
    private final List<User> allUsers = new ArrayList<>();
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        db = new DatabaseHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.employeesList);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new ArrayList<>(), db);
        list.setAdapter(adapter);

        adapter.setOnUserClick(user -> {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.putExtra("userId", user.id);
            startActivity(intent);
        });

        EditText search = findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString().trim().toLowerCase(Locale.getDefault());
                filter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        allUsers.clear();
        allUsers.addAll(db.getUsersByRole("employee"));
        filter();
    }

    private void filter() {

        List<User> result = new ArrayList<>();

        for (User user : allUsers) {
            if (query.isEmpty()
                    || user.name.toLowerCase(Locale.getDefault()).contains(query)
                    || (user.job != null && user.job.toLowerCase(Locale.getDefault()).contains(query))
                    || user.email.toLowerCase(Locale.getDefault()).contains(query)) {
                result.add(user);
            }
        }

        adapter.update(result);

        TextView count = findViewById(R.id.employeesCount);
        TextView empty = findViewById(R.id.emptyEmployees);

        count.setText(result.size() + " employé" + (result.size() > 1 ? "s" : ""));
        empty.setVisibility(result.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
