package com.zinhao.kikoeru;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import com.zinhao.kikoeru.databinding.ActivityMoreBinding;
import org.jetbrains.annotations.NotNull;

public class MoreActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    private View itemOnlyLoadLrc;
    private CheckBox cbOnlyLrcWork;

    private View vLicense;

    private View itemSaveExternal;
    private CheckBox cbSaveExternal;

    private View vAbout;

    private View itemDebug;
    private CheckBox cbDebug;
    private ActivityMoreBinding viewBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMoreBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        setSupportActionBar(viewBinding.toolbar);
        setSafeArea(viewBinding.appBarLayout, new InsetReady() {
            @Override
            public void onInsetReady(@NotNull Insets insets) {
                viewBinding.content.setPadding(insets.left, 0, insets.right, insets.bottom);
            }
        });
        itemOnlyLoadLrc = viewBinding.relativeLayout;
        itemOnlyLoadLrc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbOnlyLrcWork.toggle();
            }
        });
        cbOnlyLrcWork = viewBinding.checkBox;
        long onlyLrcFlag = App.getInstance().getValue(App.CONFIG_ONLY_DISPLAY_LRC, 1);
        cbOnlyLrcWork.setChecked(onlyLrcFlag == 1);
        cbOnlyLrcWork.setOnCheckedChangeListener(this);

        itemSaveExternal = findViewById(R.id.saveExternal);
        itemSaveExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbSaveExternal.toggle();
            }
        });
        cbSaveExternal = findViewById(R.id.checkBox3);
        cbSaveExternal.setChecked(App.getInstance().isSaveExternal());
        cbSaveExternal.setOnCheckedChangeListener(this);

        vLicense = findViewById(R.id.license);
        vLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MoreActivity.this, LicenseActivity.class));
            }
        });

        vAbout = findViewById(R.id.about);
        vAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, AboutActivity.class));
            }
        });

        itemDebug = viewBinding.debug;
        itemDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbDebug.toggle();
            }
        });
        cbDebug = findViewById(R.id.checkBox1);
        cbDebug.setChecked(App.getInstance().isAppDebug());
        cbDebug.setOnCheckedChangeListener(this);


        viewBinding.rlHomeTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewBinding.cbHomeTab.toggle();
            }
        });
        viewBinding.cbHomeTab.setChecked(App.getInstance().isUseNewLayout());
        viewBinding.cbHomeTab.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == cbOnlyLrcWork) {
            long value = b ? 1 : 0;
            App.getInstance().setValue(App.CONFIG_ONLY_DISPLAY_LRC, value);
            Api.setSubtitle((int) value);
        }

        if (compoundButton == cbDebug) {
            App.getInstance().setAppDebug(b);
        }

        if( compoundButton == viewBinding.cbHomeTab ){
            App.getInstance().setUseNewLayout(b);
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
            return;
        }

        if (cbSaveExternal == compoundButton) {
            if (b) {
                boolean result = requestReadWriteExternalPermission(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (!Environment.isExternalStorageManager()) {
                                if (cbSaveExternal.isChecked()) {
                                    cbSaveExternal.toggle();
                                }
                                return;
                            }
                        } else {
                            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                                if (cbSaveExternal.isChecked()) {
                                    cbSaveExternal.toggle();
                                }
                                return;
                            }
                        }
                        App.getInstance().setSaveExternal(true);
                        if (!cbSaveExternal.isChecked()) {
                            cbSaveExternal.toggle();
                        }
                    }
                });
                if (result) {
                    App.getInstance().setSaveExternal(true);
                }
            } else {
                App.getInstance().setSaveExternal(false);
            }
        }
    }
}