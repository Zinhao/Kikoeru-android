package com.zinhao.kikoeru;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zinhao.kikoeru.databinding.ActivityLocalHistoryBinding;
import com.zinhao.kikoeru.db.LocalWorkHistory;

import java.util.Comparator;
import java.util.List;

public class LastWatchActivity extends BaseActivity {
    private ActivityLocalHistoryBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityLocalHistoryBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        setTitle(R.string.local_history);

        App app = (App) getApplication();
        List<LocalWorkHistory> localWorkHistoryList = app.getLocalWorkHistoryList();
        localWorkHistoryList.sort(new Comparator<LocalWorkHistory>() {
            @Override
            public int compare(LocalWorkHistory o1, LocalWorkHistory o2) {
                return Long.compare(o2.getPosition(),o1.getPosition());
            }
        });
        viewBinding.mainRecycler.setAdapter(new SuperRecyclerAdapter<LocalWorkHistory>(localWorkHistoryList) {
            @Override
            public void bindData(SuperRecyclerAdapter.SuperVHolder  holder, int position) {
                LocalWorkHistory item = localWorkHistoryList.get(position);

                String coverUrl = Api.minCoverImageUrl(item.getRjNumber());
                holder.setImage(coverUrl,R.id.ivCover);

                holder.setText(""+item.getRjNumber() +item.getTitle(),R.id.tvTitle);

            }

            @Override
            public int setLayout(int viewType) {
                return R.layout.item_work_1;
            }

        });
        viewBinding.mainRecycler.setLayoutManager(new LinearLayoutManager(this));



    }
}
