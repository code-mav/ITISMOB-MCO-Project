package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    public GalleryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Find views
        Spinner spinnerDate = view.findViewById(R.id.spinnerDate);
        Spinner spinnerHabit = view.findViewById(R.id.spinnerHabit);
        Spinner spinnerFriend = view.findViewById(R.id.spinnerFriend);
        RecyclerView rvPhotos = view.findViewById(R.id.rvPhotos);

        ArrayAdapter<CharSequence> dateAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.sample_dates,
                android.R.layout.simple_spinner_item
        );
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(dateAdapter);
        spinnerDate.setSelection(0); // shows  Dates

        ArrayAdapter<CharSequence> habitAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.sample_habits,
                android.R.layout.simple_spinner_item
        );
        habitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHabit.setAdapter(habitAdapter);
        spinnerHabit.setSelection(0); // shows Habits

        ArrayAdapter<CharSequence> friendAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.sample_friends,
                android.R.layout.simple_spinner_item
        );
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFriend.setAdapter(friendAdapter);
        spinnerFriend.setSelection(0); // shows Friends

        // RecyclerView grid setup
        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Dummy data
        List<GalleryItem> items = new ArrayList<>();
        items.add(new GalleryItem(R.drawable.study1, "Maverick (You)", "Study", 20));
        items.add(new GalleryItem(R.drawable.gym1, "Hana", "Gym", 15));
        items.add(new GalleryItem(R.drawable.gym2, "Ced", "Gym", 10));
        items.add(new GalleryItem(R.drawable.gym3, "Maverick (You)", "Gym", 3));
        items.add(new GalleryItem(R.drawable.study2, "Hana", "Study", 15));
        items.add(new GalleryItem(R.drawable.study3, "Ced", "Study", 10));

        rvPhotos.setAdapter(new GalleryAdapter(items));

        return view;
    }
}
