package com.smart.cryptosingleactivityapp;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView recView;
    public final String CRYPTO_URL_PATH = "https://files.coinmarketcap.com/static/img/coins/128x128/%s.png";
    public final String ENDPOINT_FETCH_CRYPTO_DATA = "https://api.coinmarketcap.com/v1/ticker/?limit=100";
    private RequestQueue mQueue;
    private final ObjectMapper mObjMapper = new ObjectMapper();
    private String DATA_FILE_NAME = "crypto.data";
    private MyCryptoAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        fetchData();
    }
    private void bindViews() {
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        recView = this.findViewById(R.id.recView);
        mAdapter = new MyCryptoAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setOrientation(LinearLayoutManager.VERTICAL);
        recView.setLayoutManager(lm);
        recView.setAdapter(mAdapter);
        setSupportActionBar(toolbar);
    }

    private void fetchData() {
        Log.d(TAG, "fetchData() called" + "fetchData");
        if(mQueue == null){
            mQueue = Volley.newRequestQueue(this);
            final JsonArrayRequest jsonObjReq = new JsonArrayRequest(ENDPOINT_FETCH_CRYPTO_DATA,
                    response -> {
                        writeDataToInternalStorage(response);
                        //Log.d(TAG, "fetchData() called" + response);
                        ArrayList<CryptoCoinEntity> data = parseJSON(response.toString());
                        Log.d(TAG, "fetchData() called" + data);
                        new EntityToModelMapperTask().execute(data);
                    },
                    error ->{
                        try {
                            JSONArray data =  readDataFromStorage();
                            ArrayList<CryptoCoinEntity> entities = parseJSON(data.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });

            mQueue.add(jsonObjReq);
        }
    }

    public ArrayList<CryptoCoinEntity> parseJSON(String jsonStr){
        ArrayList<CryptoCoinEntity> data = null;

        try{
            data = mObjMapper.readValue(jsonStr, new TypeReference<ArrayList<CryptoCoinEntity>>() {
            });
            }catch (Exception e){
        }

        return data;
    }

    private void writeDataToInternalStorage(JSONArray data){
        FileOutputStream fos = null;

        try{
            fos = openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        try{
            fos.write(data.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONArray readDataFromStorage() throws JSONException{
        FileInputStream fis = null;
        try{
            fis = openFileInput(DATA_FILE_NAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return new JSONArray(sb.toString());
    }

    private class EntityToModelMapperTask extends AsyncTask<List<CryptoCoinEntity>,Void,List<CoinModel>>{
        @Override
        protected List<CoinModel> doInBackground(List<CryptoCoinEntity>... data) {
            final ArrayList<CoinModel> listData = new ArrayList<>();
            CryptoCoinEntity entity;
            for(int i = 0 ; i < data[0].size(); i++){
                entity = data[0].get(i);
                listData.add(new CoinModel(entity.getName() , entity.getSymbol() , String.format(CRYPTO_URL_PATH , entity.getId()) , entity.getPriceUsd() , entity.getPercentChange24h()));
            }
            return listData;
        }

        @Override
        protected void onPostExecute(List<CoinModel> data) {
            Log.d(TAG, "onPostExecute() called with: data = [" + data + "]");
            mAdapter.setItems(data);
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(data);
        }
    }


    private class CoinModel {
        public final String name;
        public final String symbol;
        public final String imageUrl;
        public final String priceUsd;
        public final String volume24H;

        public CoinModel(String name, String symbol, String imageUrl, String priceUsd, String volume24H) {
            this.name = name;
            this.symbol = symbol;
            this.imageUrl = imageUrl;
            this.priceUsd = priceUsd;
            this.volume24H = volume24H;
        }
    }

    private static class MyCryptoAdapter extends RecyclerView.Adapter<MyCryptoAdapter.CoinViewHolder>{
        List<CoinModel> mItems = new ArrayList<>();
        public final String STR_TEMPLATE_NAME = "%s\t\t\t\t\t\t%s";
        public final String STR_TEMPLATE_PRICE = "%s$\t\t\t\t\t\t24H Volume:\t\t\t%s$";

        @Override
        public CoinViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Log.d(TAG, "onBindViewHolder() called with: holder = " + "onCreateViewHolder");
            return new CoinViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,parent,false));
        }

        @Override
        public void onBindViewHolder(CoinViewHolder holder, int position) {
            final CoinModel model = mItems.get(position);
            //Log.d(TAG, "onBindViewHolder() called with: holder = " + model.name);
            holder.tvNameAndSymbol.setText(String.format(STR_TEMPLATE_NAME,model.name , model.symbol));
            holder.tvPriceAndVolume.setText(String.format(STR_TEMPLATE_PRICE,model.priceUsd , model.volume24H));
            Glide.with(holder.ivIcon).load(model.imageUrl).into(holder.ivIcon);
        }

        @Override
        public int getItemCount() {
            //Log.d(TAG, "setItems()" + mItems.size());
            return mItems.size();
        }

        public void setItems(List<CoinModel> data){
            //Log.d(TAG, "setItems()" + data.size());
            this.mItems.clear();
            this.mItems.addAll(data);
        }

        class CoinViewHolder extends RecyclerView.ViewHolder{

            TextView tvNameAndSymbol;
            TextView tvPriceAndVolume;
            ImageView ivIcon;

            public CoinViewHolder(View itemView) {
                super(itemView);
                tvNameAndSymbol = itemView.findViewById(R.id.tvNameAndSymbol);
                tvPriceAndVolume = itemView.findViewById(R.id.tvPriceAndVolume);
                ivIcon = itemView.findViewById(R.id.ivIcon);
            }
        }
    }

}
