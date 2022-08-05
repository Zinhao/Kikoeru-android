package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.kikoeru.databinding.ActivityUserSwitchBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class UserSwitchActivity extends BaseActivity {
    private ActivityUserSwitchBinding binding;
    private List<User> users;
    private UserAdapter adapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem signOutMenu = menu.add(0,0,0, "添加账号");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 0){
            startActivity(new Intent(this,LoginAccountActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserSwitchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        App app = (App) getApplication();
        users = app.getAllUsers();
        if(users.size() == 0){
            binding.button5.setVisibility(View.VISIBLE);
        }else {
            binding.button5.setVisibility(View.GONE);
        }
        binding.button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(),LoginAccountActivity.class));
                finish();
            }
        });
        adapter = new UserAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(UserSwitchActivity.this));
    }

    public void switchUser(User user){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认切换？");
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Api.init(user.getToken(),user.getHost());
                stopService(new Intent(UserSwitchActivity.this,AudioService.class));
                App.getInstance().setValue(App.CONFIG_USER_DATABASE_ID,user.getId());
                App.getInstance().setCurrentUserId(user.getId());
                startActivity(new Intent(UserSwitchActivity.this,LauncherActivity.class));
                finish();
            }
        });
        builder.create().show();
    }

    private User refreshUser;
    private AsyncHttpClient.JSONObjectCallback refreshTokenCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!=null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse.code() == 200){
                if(jsonObject.has("token")){
                    try {
                        if(refreshUser == null)
                            return;
                        String newToken = jsonObject.getString("token");
                        if(refreshUser.getToken().equals(Api.token)){
                            // 需要更新当前账号token
                            Api.init(newToken,refreshUser.getHost());
                        }
                        refreshUser.setToken(newToken);
                        App.getInstance().updateUser(refreshUser);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UserSwitchActivity.this,"refresh token success!",Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        alertException(jsonException);
                    }
                }
            }else{
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    if(jsonObject.has("error")){
                        stringBuilder.append(jsonObject.getString("error"));
                    }else if(jsonObject.has("errors")){
                        JSONArray errors = jsonObject.getJSONArray("errors");
                        for (int i = 0; i < errors.length(); i++) {
                            JSONObject error = errors.getJSONObject(i);
                            String errorValue = error.getString("msg");
                            stringBuilder.append(errorValue);
                        }
                    }
                }catch (JSONException e1){
                    alertException(e);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(UserSwitchActivity.this,String.format("%d:%s",asyncHttpResponse.code(),stringBuilder.toString()),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    private class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_and_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            User user = users.get(position);
            if(holder instanceof UserViewHolder){
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchUser(user);
                    }
                });
                if(Api.token.equals(user.getToken())){
                    ((UserViewHolder) holder).tvName.setText(user.getName()+"(当前)");
                }else {
                    ((UserViewHolder) holder).tvName.setText(user.getName());
                }

                ((UserViewHolder) holder).tvServer.setText(user.getHost());
                ((UserViewHolder) holder).ibDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        notifyItemRemoved(position);
                        App app = (App) getApplication();
                        app.deleteUser(user);
                        notifyItemRangeChanged(position,users.size()-position);
                        if(user.getId().equals(app.getCurrentUserId())){
                            if(app.getAllUsers().size()!=0){
                                User firstUser = app.getAllUsers().get(0);
                                app.setCurrentUserId(firstUser.getId());
                                Api.init(firstUser.getToken(),firstUser.getHost());
                            }else{
                                binding.button5.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
                ((UserViewHolder) holder).ibRefresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshUser = user;
                        Api.doGetToken(user.getName(),user.getPassword(),user.getHost(),refreshTokenCallback);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    private static class UserViewHolder extends RecyclerView.ViewHolder{
        private TextView tvName;
        private TextView tvServer;
        private ImageButton ibDelete;
        private ImageButton ibRefresh;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvServer = itemView.findViewById(R.id.tvServer);
            ibDelete = itemView.findViewById(R.id.imageButton3);
            ibRefresh= itemView.findViewById(R.id.imageButton4);

        }
    }
}