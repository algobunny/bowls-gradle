package me.valour.bowls.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.valour.bowls.R;


/**
 * A placeholder fragment containing a simple view.
 */
public class BillFragment extends Fragment {

    public BillFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bill, container, false);

        return view;
    }
}
