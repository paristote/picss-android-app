package com.philipoy.picss.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

import com.philipoy.picss.R;

/**
 * Dialog that asks the user a name and a label for the Picss
 * @author paristote
 *
 */
public class PicssDialogFragment extends DialogFragment {
	
	PicssDialogListener listener;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    builder.setView(inflater.inflate(R.layout.dialog_picss, null))
	     .setTitle(R.string.dialog_title)
	     .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// call the method on the parent activity when user click the "Send" button
				listener.onDialogPositiveClick(PicssDialogFragment.this);
			}
		})
		 .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// call the method on the parent activity when user click the "Cancel" button
				listener.onDialogNegativeClick(PicssDialogFragment.this);
			}
		});
		return builder.create();
	}
	
	@Override
	/**
	 * Attach this dialog fragment to the activity that created it
	 */
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			listener = (PicssDialogListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
		}
	}

	/**
	 * Implemented by activities that use this dialog fragment
	 * Methods are called when positive (Send) and negative (Cancel) buttons are tapped
	 * @author paristote
	 *
	 */
	public interface PicssDialogListener {
		public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
	}
	
}
