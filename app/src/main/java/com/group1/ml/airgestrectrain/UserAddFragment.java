package com.group1.ml.airgestrectrain;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;


public class UserAddFragment extends DialogFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_add, container, false);
        getDialog().getWindow().setLayout(400,400);
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(400, 400);

    }
}
