package com.wasem.mysteriousquestions.Views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.wasem.mysteriousquestions.AppSharedPreferences;
import com.wasem.mysteriousquestions.DataBase.Listeners.InsertListener;
import com.wasem.mysteriousquestions.DataBase.Listeners.SelectLevelListener;
import com.wasem.mysteriousquestions.DataBase.Models.Level;
import com.wasem.mysteriousquestions.Adapters.LevelAdapter;
import com.wasem.mysteriousquestions.DataBase.Models.Question;
import com.wasem.mysteriousquestions.DataBase.PlayerViewModel;
import com.wasem.mysteriousquestions.databinding.ActivityLevelsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LevelsActivity extends AppCompatActivity {
    ActivityLevelsBinding binding;
    LevelAdapter adapter;
    PlayerViewModel viewModel;
    List<Level> getLevels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLevelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvTotalPoints.setText(String.valueOf(AppSharedPreferences.getInstance(getApplicationContext()).getScore()));

        viewModel = new ViewModelProvider(this).get(PlayerViewModel.class);

        viewModel.getAllLevels().observe(this, new Observer<List<Level>>() {
            @Override
            public void onChanged(List<Level> levels) {
                if (levels.size() > 0) {
                    Log.d("LEVEL-listSize", "onChanged: " + levels.size());
                    getLevels = levels;
                    Log.d("newLEVEL-listSize", "onChanged: " + getLevels.size());
                } else {
                    readJson();
                }

                adapter = new LevelAdapter(getLevels, getApplicationContext(), new SelectLevelListener() {
                    @Override
                    public void onSelectedLevelListener(Level level) {
                        Log.d("LEVEL-SELECTED-NUMBER", "onSelectedLevelListener: " + level.level_no);
                        viewModel.getAllLevelQuestions(level.level_no).observe(LevelsActivity.this, new Observer<List<Question>>() {
                            @Override
                            public void onChanged(List<Question> questions) {
                                Log.d("LEVEL-QUESTIONS-SIZE", "onChanged: " + questions.size());
                                Intent intent = new Intent(getApplicationContext(), GameActivity.class);
                                Gson gson = new Gson();
                                String myJson = gson.toJson(questions);
                                intent.putExtra("currentLevelQuestions", myJson);
                                startActivity(intent);
                            }
                        });

                    }
                });
                initializeAdapter();
            }
        });


    }

    public void initializeAdapter() {
        binding.rv.setAdapter(adapter);
        binding.rv.setHasFixedSize(true);
        binding.rv.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = this.getAssets().open("puzzleGameData.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void readJson() {
        try {
            JSONArray jsonArray = new JSONArray(loadJSONFromAsset());

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int levelNo = jsonObject.getInt("level_no");
                int unlockPoints = jsonObject.getInt("unlock_points");

                Level level = new Level(levelNo, unlockPoints);

                viewModel.insertLevel(level, new InsertListener() {
                    @Override
                    public void onInsertListener(Long itemId) {
                        Log.d("INSERT-LEVELS-LISTENER", "onInsertListener: " + itemId);

                        try {
                            JSONArray array = jsonObject.getJSONArray("questions");

                            for (int j = 0; j < array.length(); j++) {

                                JSONObject jsonObject1 = array.getJSONObject(j);

                                int id = jsonObject1.getInt("id");
                                String title = jsonObject1.getString("title");
                                String answer1 = jsonObject1.getString("answer_1");
                                String answer2 = jsonObject1.getString("answer_2");
                                String answer3 = jsonObject1.getString("answer_3");
                                String answer4 = jsonObject1.getString("answer_4");
                                String true_answer = jsonObject1.getString("true_answer");
                                int points = jsonObject1.getInt("points");
                                int duration = jsonObject1.getInt("duration");
                                String hint = jsonObject1.getString("hint");
                                int pattern_id = jsonObject1.getInt("pattern_id");

                                Question question = new Question(levelNo, title, answer1, answer2, answer3, answer4, true_answer, points, duration, pattern_id, hint);

                                viewModel.insertQuestion(question, new InsertListener() {
                                    @Override
                                    public void onInsertListener(Long itemId) {
                                        Log.d("INSERT-LISTENERS", "onInsertListener: " + itemId);
                                    }
                                });

                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}