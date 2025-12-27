package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private OnFavoriteSelectedListener callback;

    public interface OnFavoriteSelectedListener {
        void onFavoriteSelected(String address);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFavoriteSelectedListener) {
            callback = (OnFavoriteSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFavoriteSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FavoritesAdapter(FavoritesManager.getInstance().getFavorites(), location -> {
            if (callback != null) {
                callback.onFavoriteSelected(location);
            }
            // Close fragment
            getParentFragmentManager().popBackStack();
        });
        
        recyclerView.setAdapter(adapter);

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
