package com.zinhao.kikoeru

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetToken
import com.zinhao.kikoeru.Api.init
import com.zinhao.kikoeru.databinding.ActivityUserSwitchBinding
import com.zinhao.kikoeru.db.User
import org.json.JSONException
import org.json.JSONObject

class UserSwitchActivity : BaseActivity() {
    private lateinit var binding: ActivityUserSwitchBinding
    private var users: MutableList<User>? = null
    private var adapter: UserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSwitchBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        setSafeArea(binding.root)
        val app = getApplication() as App
        users = app.getAllUsers()
        binding!!.button5.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                startActivity(Intent(v.getContext(), LoginAccountActivity::class.java))
                finish()
            }
        })
        adapter = UserAdapter()
        binding!!.recyclerView.setAdapter(adapter)
        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this@UserSwitchActivity))
    }

    fun switchUser(user: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("确认切换？")
        builder.setPositiveButton(R.string.confirm, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
                init(user.getToken(), user.getHost())
                stopService(Intent(this@UserSwitchActivity, AudioService::class.java))
                App.getInstance().setValue(App.CONFIG_USER_DATABASE_ID, user.getId())
                App.getInstance().setCurrentUserId(user.getId())
                startActivity(Intent(this@UserSwitchActivity, LauncherActivity::class.java))
                finish()
            }
        })
        builder.create().show()
    }

    private var refreshUser: User? = null
    private val refreshTokenCallback: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse, jsonObject: JSONObject) {
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse.code() == 200) {
                if (jsonObject.has("token")) {
                    try {
                        if (refreshUser == null) return
                        val newToken = jsonObject.getString("token")
                        if (refreshUser!!.getToken() == Api.token) {
                            // 需要更新当前账号token
                            init(newToken, refreshUser!!.getHost())
                        }
                        refreshUser!!.setToken(newToken)
                        App.getInstance().updateUser(refreshUser)
                        runOnUiThread(object : Runnable {
                            override fun run() {
                                Toast.makeText(this@UserSwitchActivity, "refresh token success!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        })
                    } catch (jsonException: JSONException) {
                        jsonException.printStackTrace()
                        alertException(jsonException)
                    }
                }
            } else {
                val stringBuilder = StringBuilder()
                try {
                    if (jsonObject.has("error")) {
                        stringBuilder.append(jsonObject.getString("error"))
                    } else if (jsonObject.has("errors")) {
                        val errors = jsonObject.getJSONArray("errors")
                        for (i in 0..<errors.length()) {
                            val error = errors.getJSONObject(i)
                            val errorValue = error.getString("msg")
                            stringBuilder.append(errorValue)
                        }
                    }
                } catch (e1: JSONException) {
                    alertException(e1)
                    return
                }
                runOnUiThread(object : Runnable {
                    override fun run() {
                        Toast.makeText(
                            this@UserSwitchActivity,
                            String.format("%d:%s", asyncHttpResponse.code(), stringBuilder.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    private inner class UserAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return UserViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_and_user, parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
            val user = users!!.get(position)
            if (holder is UserViewHolder) {
                holder.itemView.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        switchUser(user)
                    }
                })
                if (Api.token == user.getToken()) {
                    holder.tvName.setText(user.getName() + "(当前)")
                } else {
                    holder.tvName.setText(user.getName())
                }

                holder.tvServer.setText(user.getHost())
                holder.ibDelete.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        notifyItemRemoved(position)
                        val app = getApplication() as App
                        app.deleteUser(user)
                        notifyItemRangeChanged(position, users!!.size - position)
                        if (user.getId() == app.getCurrentUserId()) {
                            if (app.getAllUsers().size != 0) {
                                val firstUser = app.getAllUsers().get(0)
                                app.setCurrentUserId(firstUser.getId())
                                init(firstUser.getToken(), firstUser.getHost())
                            } else {
                                binding!!.button5.setVisibility(View.VISIBLE)
                            }
                        }
                    }
                })
                holder.ibRefresh.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        refreshUser = user
                        doGetToken(user.getName(), user.getPassword(), user.getHost(), refreshTokenCallback)
                    }
                })
            }
        }

        override fun getItemCount(): Int {
            return users!!.size
        }
    }

    private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView
        val tvServer: TextView
        val ibDelete: ImageButton
        val ibRefresh: ImageButton

        init {
            tvName = itemView.findViewById<TextView>(R.id.tvName)
            tvServer = itemView.findViewById<TextView>(R.id.tvServer)
            ibDelete = itemView.findViewById<ImageButton>(R.id.imageButton3)
            ibRefresh = itemView.findViewById<ImageButton>(R.id.imageButton4)
        }
    }
}