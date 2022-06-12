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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zinhao.kikoeru.databinding.ActivityUserSwitchBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserSwitchActivity extends BaseActivity {
    private ActivityUserSwitchBinding binding;
    private JSONArray users;
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
        try {
            users = app.getUsersJSONObject().getJSONArray("users");
            adapter = new UserAdapter();
            binding.recyclerView.setAdapter(adapter);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(UserSwitchActivity.this));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void switchUser(JSONObject user){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认切换？");
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                try {
                    String host = user.getString(JSONConst.User.HOST);
                    String token = user.getString(JSONConst.User.TOKEN);
                    Api.init(token,host);
                    stopService(new Intent(UserSwitchActivity.this,AudioService.class));
                    App.getInstance().setValue(App.CONFIG_HOST,host);
                    App.getInstance().setValue(App.CONFIG_TOKEN,token);
                    startActivity(new Intent(UserSwitchActivity.this,LauncherActivity.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                finish();
            }
        });
        builder.create().show();
    }

    private class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_and_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            try {
                JSONObject user = users.getJSONObject(position);
                if(holder instanceof UserViewHolder){
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            switchUser(user);
                        }
                    });
                    if(Api.token.equals(user.getString(JSONConst.User.TOKEN))){
                        ((UserViewHolder) holder).tvName.setText(user.getString(JSONConst.User.NAME)+"(当前)");
                    }else {
                        ((UserViewHolder) holder).tvName.setText(user.getString(JSONConst.User.NAME));
                    }

                    ((UserViewHolder) holder).tvServer.setText(user.getString(JSONConst.User.HOST));
                    ((UserViewHolder) holder).ibDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            notifyItemRemoved(position);
                            users.remove(position);
                            notifyItemRangeChanged(position,users.length()-position);
                            App app = (App) getApplication();
                            LocalFileCache.getInstance().saveUsers(v.getContext(),app.getUsersJSONObject());
                        }
                    });
                }
            } catch (JSONException e) {
            }
        }

        @Override
        public int getItemCount() {
            return users.length();
        }
    }

    private static class UserViewHolder extends RecyclerView.ViewHolder{
        private TextView tvName;
        private TextView tvServer;
        private ImageButton ibDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvServer = itemView.findViewById(R.id.tvServer);
            ibDelete = itemView.findViewById(R.id.imageButton3);
        }
    }
}