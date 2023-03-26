package com.zinhao.kikoeru;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.JSONObjectBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class Api {
    private static String HOST = "http://localhost:8888";
    private static final String TAG = "Api";
    public static final String REMOTE_HOST = "https://api.asmr.one";
    public static final String LOCAL_HOST = "http://localhost:8888";
    public static String authorization = "";
    public static String token = "";
    private static int subtitle = 1;
    private static int sort = 1;
    private static String order = "id";

    public static void init(@NonNull String tokenStr, @NonNull String host) {
        token = tokenStr;
        authorization = String.format("Bearer %s", tokenStr);
        if (host.startsWith("http")) {
            HOST = host;
        } else {
            HOST = String.format(Locale.US, "http://%s", host);
        }
        subtitle = (int) App.getInstance().getValue(App.CONFIG_ONLY_DISPLAY_LRC, 1);
        order = App.getInstance().getValue(App.CONFIG_ORDER, "id");
        sort = (int) App.getInstance().getValue(App.CONFIG_SORT, 0);
    }

    private static @NonNull String makeSort() {
        if (sort != 1) {
            return "desc";
        } else {
            return "asc";
        }
    }

    public static void setOrder(String order) {
        if (Api.order.equals(order)) {
            if (sort == 1) {
                sort = 0;
            } else {
                sort = 1;
            }
            App.getInstance().setValue(App.CONFIG_SORT, sort);
            return;
        }
        Api.order = order;
        App.getInstance().setValue(App.CONFIG_ORDER, order);
    }

    public static void setSubtitle(int subtitle) {
        Api.subtitle = subtitle;
    }

    public static void doGetWorks(int page, AsyncHttpClient.JSONObjectCallback callback) {
        //subtitle=1 带字幕
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/works?order=%s&sort=%s&page=%d&seed=35&subtitle=%d", order, makeSort(), page, subtitle)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static void doGetWorksByTag(int page, int tagId, AsyncHttpClient.JSONObjectCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/tags/%d/works?order=%s&sort=%s&page=%d&seed=21&subtitle=%d", tagId, order, makeSort(), page, subtitle)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static void doGetWorkByVa(int page, String vaId, AsyncHttpClient.JSONObjectCallback callback) {
//        http://localhost:8888/api/vas/2b5e7ab5-d994-5491-a53c-f1b6ae562d0e/works?order=price&sort=desc&page=1&seed=68
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/vas/%s/works?order=%s&sort=%s&page=%d&seed=21&subtitle=%d", vaId, order, makeSort(), page, subtitle)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static void doGetAllTags(AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + "/api/tags/"), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }

    public static void doGetAllVas(AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + "/api/vas/"), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }

    public static void doGetDocTree(int id, AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/tracks/%d", id)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }

    public static void doGetWork(String keyword, int page, AsyncHttpClient.JSONObjectCallback callback) {
//        http://localhost:8888/api/search/RJ381400?order=release&sort=desc&page=1&seed=18
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/search/%s?order=%s&sort=%s&page=%d&seed=18&subtitle=%d", keyword, order, makeSort(), page, subtitle)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static void checkLrc(String hash, AsyncHttpClient.JSONObjectCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/media/check-lrc/%s?token=%s", hash, token)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static void doGetMediaString(String hash, AsyncHttpClient.StringCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/media/stream/%s?token=%s", hash, token)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeString(request, callback);
    }

    public static void doGetToken(String userName, String password, String host, AsyncHttpClient.JSONObjectCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(host + "/api/auth/me"), "POST");
        JSONObject pwd = new JSONObject();
        try {
            pwd.put("name", userName);
            pwd.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.setBody(new JSONObjectBody(pwd));
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static final String FILTER_MARKED = "marked";
    public static final String FILTER_LISTENING = "listening";
    public static final String FILTER_LISTENED = "listened";
    public static final String FILTER_REPLAY = "replay";
    public static final String FILTER_POSTPONED = "postponed";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(value = {FILTER_MARKED, FILTER_LISTENING, FILTER_LISTENED, FILTER_REPLAY, FILTER_POSTPONED})
    public @interface Filter {
    }

    /**
     * GET
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=marked">...</a>      我的进度 - 想听
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=listening">...</a>   我的进度 - 在听
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=listened">...</a>    我的进度 - 听过
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=replay">...</a>      我的进度 - 重听
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1&filter=postponed">...</a>   我的进度 - 搁置
     * <a href="https://api.asmr.one/api/review?order=updated_at&sort=desc&page=1">...</a>                    我的评价
     */
    public static void doGetReview(@Filter String filter, int page, AsyncHttpClient.JSONObjectCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + String.format("/api/review?order=updated_at&sort=desc&page=%d&filter=%s", page, filter)), "GET");
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    /**
     * PUT
     * 标记在听 <a href="https://api.asmr.one/api/review?starOnly=false&progressOnly=true">...</a>
     * data:   {"user_name":"guest","work_id":380205,"progress":"listening"}
     * result: 200: {message: "更新进度成功"}
     */
    public static void doPutReview(long id, @Filter String progress, AsyncHttpClient.JSONObjectCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + "/api/review?starOnly=false&progressOnly=true"), "PUT");
        JSONObject jsonObject = new JSONObject();
        String userName = App.getInstance().currentUser().getName();
        try {
            jsonObject.put("user_name", userName);
            jsonObject.put("work_id", id);
            jsonObject.put("progress", progress);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.setBody(new JSONObjectBody(jsonObject));
        request.setTimeout(5000);
        request.addHeader("authorization", authorization);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, callback);
    }

    public static String formatGetUrl(String path, boolean useToken) {
        if (path.startsWith("http")) {
            if (useToken) {
                return String.format("%s?token=%s", path, token);
            } else {
                return path;
            }
        } else {
            if (useToken) {
                return String.format("%s%s?token=%s", HOST, path, token);
            } else {
                return String.format("%s%s", HOST, path);
            }
        }
    }
}
