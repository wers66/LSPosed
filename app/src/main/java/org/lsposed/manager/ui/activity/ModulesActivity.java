/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lsposed.manager.Constants;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.AppHelper;
import org.lsposed.manager.ui.activity.base.ListActivity;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.ModuleUtil;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class ModulesActivity extends ListActivity implements ModuleUtil.ModuleListener {

    private PackageManager pm;
    private ModuleUtil moduleUtil;
    private ModuleAdapter adapter = null;
    private String selectedPackageName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        moduleUtil = ModuleUtil.getInstance();
        pm = getPackageManager();
        moduleUtil.addListener(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_modules, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        moduleUtil.removeListener(this);
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, ModuleUtil.InstalledModule module) {
        adapter.refresh();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(selectedPackageName);
        if (module == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            String packageName = module.packageName;
            if (packageName == null) {
                return false;
            }
            Intent intent = AppHelper.getSettingsIntent(packageName, pm);
            if (intent != null) {
                startActivity(intent);
            } else {
                Snackbar.make(binding.snackbar, R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
            }
            return true;
        } else if (itemId == R.id.menu_app_store) {
            Uri uri = Uri.parse("market://details?id=" + module.packageName);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (itemId == R.id.menu_app_info) {
            startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", module.packageName, null)));
            return true;
        } else if (itemId == R.id.menu_uninstall) {
            startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", module.packageName, null)));
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected BaseAdapter<?> createAdapter() {
        return adapter = new ModuleAdapter();
    }

    private class ModuleAdapter extends BaseAdapter<ModuleAdapter.ViewHolder> {
        private List<ModuleUtil.InstalledModule> fullList, showList;

        ModuleAdapter() {
            fullList = showList = Collections.emptyList();
            refresh();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_module, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ModuleUtil.InstalledModule item = showList.get(position);
            holder.root.setAlpha(moduleUtil.isModuleEnabled(item.packageName) ? 1.0f : .5f);
            holder.appName.setText(item.getAppName());
            GlideApp.with(holder.appIcon)
                    .load(item.getPackageInfo())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            holder.appIcon.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (!item.getDescription().isEmpty()) {
                sb.append(item.getDescription());
            } else {
                sb.append(getString(R.string.module_empty_description));
            }

            int installedXposedVersion = Constants.getXposedApiVersion();
            String warningText = null;
            if (item.minVersion == 0) {
                warningText = getString(R.string.no_min_version_specified);
            } else if (installedXposedVersion > 0 && item.minVersion > installedXposedVersion) {
                warningText = String.format(getString(R.string.warning_xposed_min_version), item.minVersion);
            } else if (item.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                warningText = String.format(getString(R.string.warning_min_version_too_low), item.minVersion, ModuleUtil.MIN_MODULE_VERSION);
            } else if (item.isInstalledOnExternalStorage()) {
                warningText = getString(R.string.warning_installed_on_external_storage);
            }
            if (warningText != null) {
                sb.append("\n");
                sb.append(warningText);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(ModulesActivity.this, R.color.material_red_500));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sb.setSpan(typefaceSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    sb.setSpan(styleSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                sb.setSpan(foregroundColorSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            holder.appDescription.setText(sb);

            holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                getMenuInflater().inflate(R.menu.context_menu_modules, menu);
                Intent intent = AppHelper.getSettingsIntent(item.packageName, pm);
                if (intent == null) {
                    menu.removeItem(R.id.menu_launch);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ModulesActivity.this, AppListActivity.class);
                intent.putExtra("modulePackageName", item.packageName);
                intent.putExtra("moduleName", item.getAppName());
                startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                selectedPackageName = item.packageName;
                return false;
            });

            holder.appVersion.setVisibility(View.VISIBLE);
            holder.appVersion.setText(item.versionName);
            holder.appVersion.setSelected(true);
        }

        @Override
        public int getItemCount() {
            return showList.size();
        }

        @Override
        public long getItemId(int position) {
            return showList.get(position).packageName.hashCode();
        }

        @Override
        public Filter getFilter() {
            return new ApplicationFilter();
        }

        public void refresh() {
            refresh(false);
        }

        public void refresh(boolean force) {
            if (force) moduleUtil.reloadInstalledModules();
            runOnUiThread(reloadModules);
        }

        private final Runnable reloadModules = new Runnable() {
            public void run() {
                fullList = new ArrayList<>(moduleUtil.getModules().values());
                Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(0, pm);
                fullList.sort((a, b) -> {
                    boolean aChecked = moduleUtil.isModuleEnabled(a.packageName);
                    boolean bChecked = moduleUtil.isModuleEnabled(b.packageName);
                    if (aChecked == bChecked) {
                        return cmp.compare(a.pkg, b.pkg);
                    } else if (aChecked) {
                        return -1;
                    } else {
                        return 1;
                    }
                });
                showList = fullList;
                String queryStr = searchView != null ? searchView.getQuery().toString() : "";
                runOnUiThread(() -> getFilter().filter(queryStr));
                moduleUtil.updateModulesList();
            }
        };

        class ViewHolder extends RecyclerView.ViewHolder {
            View root;
            ImageView appIcon;
            TextView appName;
            TextView appDescription;
            TextView appVersion;
            TextView warningText;

            ViewHolder(View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.item_root);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                appDescription = itemView.findViewById(R.id.description);
                appVersion = itemView.findViewById(R.id.version_name);
                warningText = itemView.findViewById(R.id.warning);
            }
        }

        class ApplicationFilter extends Filter {

            private boolean lowercaseContains(String s, String filter) {
                return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint.toString().isEmpty()) {
                    showList = fullList;
                } else {
                    ArrayList<ModuleUtil.InstalledModule> filtered = new ArrayList<>();
                    String filter = constraint.toString().toLowerCase();
                    for (ModuleUtil.InstalledModule info : fullList) {
                        if (lowercaseContains(info.getAppName(), filter) ||
                                lowercaseContains(info.packageName, filter) ||
                                lowercaseContains(info.getDescription(), filter)) {
                            filtered.add(info);
                        }
                    }
                    showList = filtered;
                }
                return null;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        }
    }
}
