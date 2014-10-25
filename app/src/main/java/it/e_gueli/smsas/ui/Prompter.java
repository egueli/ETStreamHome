package it.e_gueli.smsas.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;

import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Set of blocking dialog creation methods.
 *
 * Created by ris8 on 25/10/14.
 */
@EBean
public class Prompter {

    @RootContext
    Activity activity;


    /**
     * Show a text prompt dialog and waits for the user to finish.
     * <p/>
     * Don't call this method in the UI thread!
     * @param message
     * @return
     */
    public String showTextInputDialogBlocking(String message, boolean isPassword) {
        final SynchronousQueue<String> textInput = new SynchronousQueue<String>();
        showPasswordDialog(message, textInput, isPassword);

        try {
            return textInput.poll(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return null;
        }
    }

    @UiThread
    void showPasswordDialog(String message, final SynchronousQueue<String> textInput, boolean isPassword) {
        final EditText editText = new EditText(activity);

        int inputType = InputType.TYPE_CLASS_TEXT;
        if (isPassword) {
            inputType |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }
        editText.setInputType(inputType);

        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        textInput.add(editText.getText().toString());
                    }
                })
                .show();
    }

    /**
     * Show a yes/no prompt dialog and waits for the user to finish.
     * <p/>
     * Don't call this method in the UI thread!
     * @param message
     * @return
     */
    public boolean showYesNoDialogBlocking(String message) {
        final SynchronousQueue<Boolean> yesOrNo = new SynchronousQueue<Boolean>();
        showYesNoDialog(message, yesOrNo);

        try {
            return yesOrNo.poll(30, TimeUnit.SECONDS);
        }
        catch (Exception te) {
            return false;
        }
    }

    @UiThread
    protected void showYesNoDialog(String message, final SynchronousQueue<Boolean> yesOrNo) {
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        yesOrNo.add(true);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        yesOrNo.add(false);
                    }
                })
                .show();
    }

    /**
     * Show a message dialog and waits for the user to confirm.
     * <p/>
     * Don't call this method in the UI thread!
     * @param message
     * @return
     */
    public void showMessageDialogBlocking(String message) {
        final Semaphore ok = new Semaphore(0);
        showMessageDialog(ok, message);

        try {
            ok.acquire();
        } catch (InterruptedException e) {
            return;
        }
    }

    @UiThread
    protected void showMessageDialog(final Semaphore ok, String message) {
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ok.release();
                    }
                })
                .show();
    }

}
