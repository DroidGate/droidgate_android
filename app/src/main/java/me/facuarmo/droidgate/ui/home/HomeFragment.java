package me.facuarmo.droidgate.ui.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.lang.ref.WeakReference;

import me.facuarmo.droidgate.R;

public class HomeFragment extends Fragment {

    // UI TODO: Implement HomeViewModel for use during onPause and onResume.

    // private HomeViewModel homeViewModel;
    private TextView mTextViewStatus;

    private void setStatus(String status) {
        mTextViewStatus.setText(String.format(getString(R.string.home_status), status));
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        /*homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);*/
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        TextView textView = root.findViewById(R.id.text_home);
        mTextViewStatus = root.findViewById(R.id.text_status);
        textView.setText(String.format(getString(R.string.home_welcome),
                getString(R.string.app_name)));
        setStatus(getString(R.string.home_status_loading));
        /*homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            }
        });*/

        new ServiceStatusWorker(this).execute();

        return root;
    }

    private static class ServiceStatusWorker extends util.ActivityNetworkWorker {
        private WeakReference<Fragment> fragment;

        private final String STATUS_PASS = "0";
        private final String STATUS_ERROR = "1";

        private void setStatus(final String status) {
            final Fragment reference = fragment.get();

            if (reference == null) {
                return;
            }

            Activity activity = reference.getActivity();

            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textViewStatus = reference.getActivity().findViewById(R.id.text_status);
                        textViewStatus.setText(String.format(reference.getString(R.string.home_status), status));
                    }
                });
            }
        }

        ServiceStatusWorker(Fragment fragment) {
            this.fragment = new WeakReference<>(fragment);
            setTargetFragment(fragment);

            final Fragment reference = this.fragment.get();

            setRequest(reference.getString(R.string.server_request_getServiceStatus));
        }

        @Override
        protected void handleOutput(final String serverOutput) {
            final Fragment reference = fragment.get();

            if (reference == null) {
                return;
            }

            switch (serverOutput) {
                case STATUS_PASS:
                    setStatus(reference.getString(R.string.home_status_pass));

                    break;
                case STATUS_ERROR:
                    setStatus(reference.getString(R.string.home_status_error));

                    break;
                default:
                    setStatus(serverOutput);
            }
        }

        @Override
        protected void showInternalError(String message, Fragment reference) {

        }
    }
}