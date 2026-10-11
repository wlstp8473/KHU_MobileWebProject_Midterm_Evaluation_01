package com.example.photoviewer;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

/** Keeps the synced list and search text when the screen is recreated. */
public class PostListViewModel extends ViewModel {
    final List<PostItem> posts = new ArrayList<>();
    boolean hasSynced;
    String query = "";
    int pageIndex;
}
