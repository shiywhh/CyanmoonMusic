package com.magicalstory.music.dialog.bottomMenuDialog;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.DialogBottomBinding;
import com.magicalstory.music.databinding.ItemMenuBottomBinding;
import com.magicalstory.music.dialog.bottomSheetDialog;

import java.util.ArrayList;

/**
 * @Classname: bottomMenusDialog
 * @Auther: Created by 奇谈君 on 2024/10/8.
 * @Description:底部菜单弹窗
 */
public class bottomMenusDialog extends bottomSheetDialog {
    private ArrayList<bottomDialogMenu> arrayList = new ArrayList<>();
    private String title;
    private int pos_selected = 0;

    public bottomMenusDialog(@NonNull Context context, ArrayList<bottomDialogMenu> list, String selected, String title,
                             bottomMenusDialog.listener listener) {
        super(context);
        this.listener = listener;
        arrayList.clear();
        this.title = title;
        int pos = 0;
        for (bottomDialogMenu menu : list) {
            if (selected != null && !selected.isEmpty()) {
                if (menu.getTitle().equals(selected)) {
                    menu.setChecked(true);
                    pos_selected = pos;
                }
            } else {
                if (menu.getTitle().contains("全部") || menu.getTitle().contains("所有")) {
                    menu.setChecked(true);
                    pos_selected = pos;
                }
            }
            arrayList.add(menu);
            pos++;
        }
    }

    public bottomMenusDialog(@NonNull Context context) {
        super(context);
    }

    DialogBottomBinding binding;

    public listener listener;

    public interface listener {
        void onMenuClick(bottomDialogMenu menu);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogBottomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setNavigationBarColor(getContext().getResources().getColor(R.color.dialog_background));
        adapter adapter = new adapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(adapter);
        binding.title.setText(title);
    }


    //适配器
    public class adapter extends RecyclerView.Adapter<adapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder viewHolder;
            viewHolder = new ViewHolder(ItemMenuBottomBinding.inflate(LayoutInflater.from(context), parent, false));
            return viewHolder;
        }

        @SuppressLint({"NonConstantResourceId", "SetTextI18n", "ClickableViewAccessibility"})
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            bottomDialogMenu menu = arrayList.get(position);
            holder.itemBinding.title.setText(menu.getTitle());
            if (menu.isChecked()) {
                holder.itemBinding.item.setBackgroundResource(R.drawable.background_menu_selected);
            } else {
                holder.itemBinding.item.setBackgroundResource(R.drawable.background_transparent);

            }
            if (menu.getIcon() == -1) {
                holder.itemBinding.icon.setVisibility(View.GONE);
            } else {
                holder.itemBinding.icon.setVisibility(View.VISIBLE);
                holder.itemBinding.icon.setImageResource(menu.getIcon());
            }

            if (position == arrayList.size() - 1) {
                holder.itemBinding.divider.setVisibility(View.GONE);
            } else {
                holder.itemBinding.divider.setVisibility(View.VISIBLE);
            }

            holder.itemBinding.item.setOnClickListener(v -> {
                listener.onMenuClick(menu);
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemMenuBottomBinding itemBinding;

            public ViewHolder(@NonNull ItemMenuBottomBinding itemView) {
                super(itemView.getRoot());
                itemBinding = itemView;
            }
        }


    }


}

