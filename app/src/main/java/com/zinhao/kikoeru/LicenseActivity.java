package com.zinhao.kikoeru;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class LicenseActivity extends BaseActivity {
    private ListView listView;
    private List<OpenSourceProject> projectList;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        listView = findViewById(R.id.listview);

        projectList = new ArrayList<>();
        projectList.add(new OpenSourceProject("Glide", getString(R.string.project_glide_license)));
        projectList.add(new OpenSourceProject("ExoPlayer", getString(R.string.project_exo_player_license)));
        projectList.add(new OpenSourceProject("AndroidAsync", getString(R.string.project_android_async_license)));
        projectList.add(new OpenSourceProject("SubsamplingScaleImageView", getString(R.string.project_subsampling_scale_image_view_license)));

        listView.setAdapter(new ArrayAdapter<>(this, R.layout.item_open_source_project, projectList));
        alertDialog = new AlertDialog.Builder(this).create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                alertDialog.setTitle(projectList.get(position).getName());
                alertDialog.setMessage(projectList.get(position).getLicenseUrl());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.view), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(projectList.get(position).getLicenseUrl());
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });
                alertDialog.show();
            }
        });
    }

    static class OpenSourceProject {
        String name;
        String licenseUrl;

        public OpenSourceProject(String name, String licenseUrl) {
            this.name = name;
            this.licenseUrl = licenseUrl;
        }

        public String getName() {
            return name;
        }

        public String getLicenseUrl() {
            return licenseUrl;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}