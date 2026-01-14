package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private OnFavoriteSelectedListener callback;
    private EditText etNewFavorite;
    private Button btnAddFavorite;

    public interface OnFavoriteSelectedListener {
        void onFavoriteSelected(String address);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFavoriteSelectedListener) {
            callback = (OnFavoriteSelectedListener) context;
        } else {
            // It's okay if not implemented, just won't do anything on click
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        etNewFavorite = view.findViewById(R.id.etNewFavorite); // Need to add these to XML
        btnAddFavorite = view.findViewById(R.id.btnAddFavorite); // Need to add these to XML

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateList();

        if (btnAddFavorite != null && etNewFavorite != null) {
            btnAddFavorite.setOnClickListener(v -> {
                String location = etNewFavorite.getText().toString().trim();
                if (!location.isEmpty()) {
                    FavoritesManager.getInstance(getContext()).addFavorite(location);
                    updateList();
                    etNewFavorite.setText("");
                } else {
                    Toast.makeText(getContext(), "Please enter a location", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    private void updateList() {
        adapter = new FavoritesAdapter(FavoritesManager.getInstance(getContext()).getFavorites(), location -> {       
            if (callback != null) {
                callback.onFavoriteSelected(location);
            }
            // Close fragment
            getParentFragmentManager().popBackStack();
        });
        recyclerView.setAdapter(adapter);
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
