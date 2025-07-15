package com.dest.pm_java.ui.home;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dest.pm_java.databinding.FragmentHomeBinding;
import com.dest.pm_java.models.Project;
import com.dest.pm_java.utilities.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> projectNames = new ArrayList<>();
    private TokenManager tokenManager;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenManager = new TokenManager(requireContext());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        listView = binding.lsProject;
        View root = binding.getRoot();

        // Inisialisasi adapter dengan context yang benar
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, projectNames);
        listView.setAdapter(adapter);

        // Jalankan AsyncTask untuk mengambil data
        new ProjectTask().execute();

        return root;
    }

    private class ProjectTask extends AsyncTask<Void, Void, List<Project>> {
        @Override
        protected List<Project> doInBackground(Void... voids) {
            try {
                URL url = new URL("http://10.0.2.2:3000/api/v1/project");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                String token = tokenManager.getToken();

                if (token != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray data = json.getJSONArray("data");
                    List<Project> list = new ArrayList<>();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject parsingJSON = data.getJSONObject(i);
                        Project pro = new Project(parsingJSON.optString("name"), parsingJSON.optString("description"));
                        list.add(pro);
                    }

                    return list;
                } else {
                    // Handle error response
                    System.err.println("HTTP Error: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                e.printStackTrace(); // Log error untuk debugging
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Project> projects) {
            super.onPostExecute(projects);

            if (projects != null && !projects.isEmpty()) {
                projectNames.clear();

                for (Project project : projects) {
                    projectNames.add(project.getName());
                }

                adapter.notifyDataSetChanged();
            } else {
                projectNames.clear();
                projectNames.add("No projects found");
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}