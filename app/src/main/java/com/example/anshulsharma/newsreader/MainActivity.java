package com.example.anshulsharma.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String>titlesNews=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ArrayList<String>titleIds=new ArrayList<>();
    String[] ids=new String[441];
    ListView newsTitle;
    SQLiteDatabase articlesDb;
    ArrayList <String> content = new ArrayList<>();

    public class DownloadIds extends AsyncTask<String,Void ,String> {


        @Override
        protected String doInBackground(String... urls) {

            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url = new URL(urls[0]);
                urlConnection=(HttpURLConnection)url.openConnection();
                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                int data=reader.read();
                while (data!=-1){
                    char current=(char)data;
                    result+=current;
                    data=reader.read();
                }

                ids=result.split("\\s*,\\s");

                articlesDb.execSQL("DELETE FROM articles");

                for (int i = 1; i < 20; i++) {

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + ids[i]+ ".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1) {
                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                        String articleTitle = jsonObject.getString("title");

                        String articleURL = jsonObject.getString("url");

                        titlesNews.add(articleTitle);

                        String sql = "INSERT INTO articles (urls) VALUES (?)";

                        SQLiteStatement statement = articlesDb.compileStatement(sql);

                        statement.bindString(1, articleURL);

                        statement.execute();

                    }

                }

            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }

    }

    public void updateListView() {
        Cursor c = articlesDb.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("urls");

        if (c.moveToFirst()) {
            content.clear();

            do {
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsTitle=findViewById(R.id.newsTitle);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titlesNews);
        newsTitle.setAdapter(arrayAdapter);

        newsTitle.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(position));

                startActivity(intent);
            }
        });



        articlesDb = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDb.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, urls VARCHAR)");

        updateListView();

        DownloadIds task=new DownloadIds();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
